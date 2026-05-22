import { useEffect, useRef, useState } from 'react'
import {
  Camera, RotateCw, Aperture, Trash2, GripVertical, AlertTriangle,
  Palette, Contrast, Sun, FileDown, Sparkles, Loader2,
} from 'lucide-react'
import {
  DndContext, closestCenter, KeyboardSensor, PointerSensor,
  useSensor, useSensors,
} from '@dnd-kit/core'
import {
  arrayMove, SortableContext, sortableKeyboardCoordinates,
  rectSortingStrategy, useSortable,
} from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import OutputFilenameField from '../components/OutputFilenameField'
import DocumentEditor from '../components/DocumentEditor'
import { useToast } from '../components/Toast'
import { imagesToPdf } from '../api/pdfApi'
import { useOpenCv } from '../hooks/useOpenCv'
import {
  detectDocumentCorners, warpAndFilter, blobToCanvas, canvasToJpegBlob,
} from '../utils/cvScanner'

const FILTERS = [
  { id: 'color',     label: 'Couleur',         icon: Palette  },
  { id: 'grayscale', label: 'Niveaux de gris', icon: Sun      },
  { id: 'bw',        label: 'Noir & blanc',    icon: Contrast },
]

export default function ScanPage() {
  const videoRef = useRef(null)
  const streamRef = useRef(null)
  const [cameraOn, setCameraOn] = useState(false)
  const [facing, setFacing] = useState('environment')
  const [error, setError] = useState(null)
  const [photos, setPhotos] = useState([])
  const [filter, setFilter] = useState('color')
  const [smartMode, setSmartMode] = useState(false)
  const [editor, setEditor] = useState(null)  // { canvas, url, corners, width, height }
  const [outputName, setOutputName] = useState('')
  const [loading, setLoading] = useState(false)
  const toast = useToast()

  const cvHook = useOpenCv()

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 5 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
  )

  async function startCamera() {
    setError(null)
    try {
      if (!navigator.mediaDevices?.getUserMedia) {
        throw new Error("L'API caméra n'est pas disponible (HTTPS requis sauf sur localhost).")
      }
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: facing, width: { ideal: 1920 }, height: { ideal: 1080 } },
        audio: false,
      })
      streamRef.current = stream
      videoRef.current.srcObject = stream
      await videoRef.current.play()
      setCameraOn(true)
    } catch (e) {
      setError(e.message || 'Impossible d\'accéder à la caméra.')
      setCameraOn(false)
    }
  }

  function stopCamera() {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach(t => t.stop())
      streamRef.current = null
    }
    if (videoRef.current) videoRef.current.srcObject = null
    setCameraOn(false)
  }

  async function switchCamera() {
    const newFacing = facing === 'environment' ? 'user' : 'environment'
    setFacing(newFacing)
    if (cameraOn) {
      stopCamera()
      setTimeout(() => startCamera(), 50)
    }
  }

  // Toggle smart mode : pre-charge OpenCV
  async function toggleSmart() {
    if (smartMode) {
      setSmartMode(false)
      return
    }
    setSmartMode(true)
    if (!cvHook.ready && !cvHook.loading) {
      try {
        await cvHook.ensureLoaded()
      } catch (e) {
        toast.error('Chargement OpenCV échoué : ' + e.message)
        setSmartMode(false)
      }
    }
  }

  useEffect(() => () => {
    stopCamera()
    photos.forEach(p => URL.revokeObjectURL(p.url))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // Capture une frame du flux video
  async function capture() {
    if (!videoRef.current || !cameraOn) return
    const video = videoRef.current
    const w = video.videoWidth
    const h = video.videoHeight
    if (!w || !h) return toast.error('La caméra n\'est pas encore prête.')

    // Capture brute en canvas (sans filtre, on filtre apres ou pas selon smart)
    const canvas = document.createElement('canvas')
    canvas.width = w
    canvas.height = h
    const ctx = canvas.getContext('2d')
    ctx.drawImage(video, 0, 0, w, h)

    if (smartMode) {
      // Smart : detection + ouverture de l'editeur
      try {
        const cv = await cvHook.ensureLoaded()
        const corners = detectDocumentCorners(cv, canvas)
        const blob = await canvasToJpegBlob(canvas)
        const url = URL.createObjectURL(blob)
        setEditor({ sourceCanvas: canvas, url, corners, width: w, height: h })
      } catch (e) {
        toast.error('Détection auto échouée : ' + e.message)
      }
      return
    }

    // Mode simple : applique filtre direct + ajoute aux photos
    applyFilter(ctx, w, h, filter)
    const blob = await canvasToJpegBlob(canvas)
    addPhoto(blob)
  }

  async function onEditorValidate(corners, filterMode) {
    if (!editor) return
    try {
      const cv = await cvHook.ensureLoaded()
      const warpedCanvas = warpAndFilter(cv, editor.sourceCanvas, corners, filterMode)
      const blob = await canvasToJpegBlob(warpedCanvas)
      addPhoto(blob)
      closeEditor()
    } catch (e) {
      toast.error('Recadrage échoué : ' + e.message)
    }
  }

  function closeEditor() {
    if (editor) URL.revokeObjectURL(editor.url)
    setEditor(null)
  }

  function addPhoto(blob) {
    const id = String(Date.now() + Math.random())
    const url = URL.createObjectURL(blob)
    setPhotos(p => [...p, { id, blob, url }])
  }

  function removePhoto(id) {
    setPhotos(p => {
      const found = p.find(x => x.id === id)
      if (found) URL.revokeObjectURL(found.url)
      return p.filter(x => x.id !== id)
    })
  }

  function onDragEnd(event) {
    const { active, over } = event
    if (!over || active.id === over.id) return
    const oldIdx = photos.findIndex(p => p.id === active.id)
    const newIdx = photos.findIndex(p => p.id === over.id)
    setPhotos(arrayMove(photos, oldIdx, newIdx))
  }

  function clearAll() {
    if (photos.length === 0) return
    if (!window.confirm('Effacer toutes les photos capturées ?')) return
    photos.forEach(p => URL.revokeObjectURL(p.url))
    setPhotos([])
  }

  async function handleAssemble() {
    if (photos.length === 0) return toast.error('Capturez au moins une photo.')
    setLoading(true)
    try {
      const files = photos.map((p, i) => new File([p.blob], `scan-${i + 1}.jpg`, { type: 'image/jpeg' }))
      await imagesToPdf(files, outputName, onProgress)
      toast.success(`${photos.length} page(s) assemblées en PDF.`)
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <ToolPage
      icon={Camera}
      title="Scanner caméra → PDF"
      subtitle="Capturez plusieurs photos à la suite, réorganisez-les, puis assemblez-les en un seul PDF."
      footer="Activez le mode intelligent pour détecter automatiquement les bords du document et redresser la perspective."
    >
      {/* Toggle smart mode */}
      <div className="mb-4">
        <button
          type="button"
          onClick={toggleSmart}
          className={`w-full flex items-center justify-between gap-3 rounded-xl border p-3 transition-all ${
            smartMode
              ? 'border-brand-400 bg-gradient-to-r from-brand-50 to-accent-50 dark:from-brand-950/40 dark:to-accent-950/40'
              : 'border-ink-200 dark:border-ink-700 hover:border-ink-300 dark:hover:border-ink-600'
          }`}
        >
          <div className="flex items-center gap-3">
            <div className={`grid place-items-center h-10 w-10 rounded-lg ${
              smartMode
                ? 'bg-gradient-to-br from-brand-500 to-accent-500 text-white'
                : 'bg-ink-100 dark:bg-ink-800 text-ink-600 dark:text-ink-300'
            }`}>
              <Sparkles size={18} />
            </div>
            <div className="text-left">
              <p className="font-semibold text-ink-900 dark:text-ink-100 text-sm">
                Mode intelligent {smartMode && '— activé'}
              </p>
              <p className="text-xs text-ink-500 dark:text-ink-400">
                Détection auto des bords + correction de perspective façon Adobe Scan
              </p>
            </div>
          </div>
          <div className={`relative h-6 w-11 rounded-full transition-colors ${
            smartMode ? 'bg-brand-600' : 'bg-ink-300 dark:bg-ink-700'
          }`}>
            <span className={`absolute top-0.5 h-5 w-5 rounded-full bg-white shadow transition-all ${
              smartMode ? 'left-5' : 'left-0.5'
            }`} />
          </div>
        </button>
        {smartMode && cvHook.loading && (
          <p className="mt-2 text-xs text-ink-500 inline-flex items-center gap-1.5">
            <Loader2 size={12} className="animate-spin" /> Chargement d'OpenCV.js (~7 Mo) — patientez…
          </p>
        )}
        {smartMode && cvHook.error && (
          <p className="mt-2 text-xs text-rose-600">⚠ {cvHook.error}</p>
        )}
      </div>

      {/* Zone preview camera */}
      <div className="relative aspect-[4/3] sm:aspect-video rounded-2xl overflow-hidden bg-ink-900 dark:bg-black">
        <video ref={videoRef} playsInline muted
               className={`w-full h-full object-cover ${cameraOn ? '' : 'opacity-0'}`} />

        {!cameraOn && !error && (
          <div className="absolute inset-0 flex flex-col items-center justify-center gap-4 text-white">
            <Camera size={40} className="opacity-70" />
            <p className="text-sm opacity-80">Caméra inactive</p>
            <button type="button" onClick={startCamera}
                    className="btn-primary h-10 px-4 text-sm">
              <Camera size={16} /> Démarrer la caméra
            </button>
          </div>
        )}

        {error && (
          <div className="absolute inset-0 flex flex-col items-center justify-center gap-3 text-white px-6 text-center">
            <AlertTriangle size={32} className="text-amber-400" />
            <p className="text-sm font-medium">Caméra inaccessible</p>
            <p className="text-xs opacity-80">{error}</p>
            <button type="button" onClick={startCamera}
                    className="btn-secondary h-9 px-3 text-xs">Réessayer</button>
          </div>
        )}

        {cameraOn && (
          <button type="button" onClick={switchCamera}
                  className="absolute top-3 right-3 grid place-items-center h-10 w-10 rounded-full bg-black/40 backdrop-blur text-white hover:bg-black/60 transition"
                  aria-label="Basculer caméra" title="Basculer caméra avant/arrière">
            <RotateCw size={18} />
          </button>
        )}

        {photos.length > 0 && cameraOn && (
          <div className="absolute top-3 left-3 inline-flex items-center gap-1.5 rounded-full bg-black/40 backdrop-blur px-3 py-1.5 text-xs font-semibold text-white">
            <span className="h-2 w-2 rounded-full bg-emerald-400" />
            {photos.length} page{photos.length > 1 ? 's' : ''}
          </div>
        )}

        {cameraOn && (
          <button type="button" onClick={capture}
                  disabled={smartMode && cvHook.loading}
                  className="absolute bottom-4 left-1/2 -translate-x-1/2 grid place-items-center h-16 w-16 rounded-full bg-white shadow-2xl ring-4 ring-white/40 hover:scale-105 active:scale-95 transition-transform disabled:opacity-50 disabled:cursor-not-allowed"
                  aria-label="Capturer">
            {smartMode && cvHook.loading
              ? <Loader2 size={28} className="text-ink-900 animate-spin" />
              : <Aperture size={32} className="text-ink-900" strokeWidth={2.2} />}
          </button>
        )}
      </div>

      {/* Filtres (mode simple uniquement) */}
      {cameraOn && !smartMode && (
        <div className="mt-4">
          <p className="field-label">Filtre appliqué à la prochaine capture</p>
          <div className="grid grid-cols-3 gap-2">
            {FILTERS.map(f => {
              const Icon = f.icon
              return (
                <button key={f.id} type="button" onClick={() => setFilter(f.id)}
                        className={`flex items-center justify-center gap-1.5 rounded-lg border py-2 text-sm font-semibold transition-all ${
                          filter === f.id
                            ? 'border-brand-500 bg-brand-50 text-brand-700 dark:bg-brand-950/50 dark:text-brand-300'
                            : 'border-ink-200 hover:border-ink-300 text-ink-600 dark:border-ink-700 dark:hover:border-ink-600 dark:text-ink-300'
                        }`}>
                  <Icon size={14} /> {f.label}
                </button>
              )
            })}
          </div>
        </div>
      )}

      {cameraOn && (
        <div className="mt-3 flex items-center justify-end gap-2">
          <button type="button" onClick={stopCamera}
                  className="btn-ghost h-9 px-3 text-sm">Arrêter la caméra</button>
        </div>
      )}

      {/* Galerie */}
      {photos.length > 0 && (
        <div className="mt-6">
          <div className="flex items-center justify-between mb-3">
            <p className="text-sm font-semibold text-ink-800 dark:text-ink-100">
              {photos.length} page{photos.length > 1 ? 's' : ''} — glissez pour réorganiser
            </p>
            <button type="button" onClick={clearAll} className="btn-ghost h-8 px-3 text-xs">
              <Trash2 size={12} /> Tout effacer
            </button>
          </div>
          <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={onDragEnd}>
            <SortableContext items={photos.map(p => p.id)} strategy={rectSortingStrategy}>
              <ul className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-5 gap-3">
                {photos.map((p, i) => (
                  <PhotoCard key={p.id} photo={p} index={i + 1} onRemove={() => removePhoto(p.id)} />
                ))}
              </ul>
            </SortableContext>
          </DndContext>
        </div>
      )}

      {photos.length > 0 && (
        <div className="mt-6">
          <OutputFilenameField value={outputName} onChange={setOutputName} placeholder="document-scanne" extension=".pdf" />
        </div>
      )}

      <div className="mt-6">
        <SubmitButton loading={loading} onClick={handleAssemble} disabled={photos.length === 0} icon={FileDown}>
          Assembler en PDF {photos.length > 0 ? `(${photos.length} page${photos.length > 1 ? 's' : ''})` : ''}
        </SubmitButton>
      </div>

      {/* Modal d'edition smart */}
      {editor && (
        <DocumentEditor
          imgUrl={editor.url}
          imgWidth={editor.width}
          imgHeight={editor.height}
          initialCorners={editor.corners}
          onCancel={closeEditor}
          onValidate={onEditorValidate}
        />
      )}
    </ToolPage>
  )
}

function PhotoCard({ photo, index, onRemove }) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({ id: photo.id })
  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.4 : 1,
  }
  return (
    <li ref={setNodeRef} style={style}
        className="relative aspect-[3/4] rounded-lg overflow-hidden bg-ink-100 dark:bg-ink-800 select-none group ring-1 ring-ink-200 dark:ring-ink-700">
      <img src={photo.url} alt={`Page ${index}`} className="w-full h-full object-cover" draggable={false} />
      <span className="absolute top-1 left-1 text-[10px] font-mono bg-black/60 px-1.5 py-0.5 rounded text-white">
        #{index}
      </span>
      <button type="button" onClick={onRemove}
              className="absolute top-1 right-1 grid place-items-center h-6 w-6 rounded-full bg-rose-500 text-white opacity-0 group-hover:opacity-100 transition-opacity"
              aria-label="Retirer" title="Retirer">
        <Trash2 size={12} />
      </button>
      <div {...attributes} {...listeners}
           className="absolute bottom-1 left-1/2 -translate-x-1/2 grid place-items-center h-7 w-7 rounded-full bg-black/60 text-white opacity-0 group-hover:opacity-100 transition-opacity cursor-grab active:cursor-grabbing">
        <GripVertical size={12} />
      </div>
    </li>
  )
}

/* ─────── Filtres mode simple (Canvas natif) ─────── */
function applyFilter(ctx, w, h, filter) {
  if (filter === 'color') return
  const imageData = ctx.getImageData(0, 0, w, h)
  const data = imageData.data
  if (filter === 'grayscale') {
    for (let i = 0; i < data.length; i += 4) {
      const avg = data[i] * 0.299 + data[i + 1] * 0.587 + data[i + 2] * 0.114
      data[i] = data[i + 1] = data[i + 2] = avg
    }
  } else if (filter === 'bw') {
    for (let i = 0; i < data.length; i += 4) {
      const lum = data[i] * 0.299 + data[i + 1] * 0.587 + data[i + 2] * 0.114
      const bw = lum > 150 ? 255 : 0
      data[i] = data[i + 1] = data[i + 2] = bw
    }
  }
  ctx.putImageData(imageData, 0, 0)
}
