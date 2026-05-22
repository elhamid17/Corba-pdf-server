import { useEffect, useRef, useState } from 'react'
import { PenLine, Upload, Eraser, Image as ImageIcon } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import OutputFilenameField from '../components/OutputFilenameField'
import { useToast } from '../components/Toast'
import { useProgress } from '../hooks/useProgress'
import { addSignatureImage } from '../api/pdfApi'

const POSITIONS = [
  { id: 'bottom-right',  label: 'Bas droite',  x: 65, y: 5  },
  { id: 'bottom-center', label: 'Bas centre',  x: 35, y: 5  },
  { id: 'bottom-left',   label: 'Bas gauche',  x: 5,  y: 5  },
  { id: 'top-right',     label: 'Haut droite', x: 65, y: 85 },
  { id: 'top-center',    label: 'Haut centre', x: 35, y: 85 },
  { id: 'top-left',      label: 'Haut gauche', x: 5,  y: 85 },
]

export default function SignatureImagePage() {
  const [pdfFiles, setPdfFiles] = useState([])
  const [mode, setMode] = useState('draw') // 'draw' | 'upload'
  const [uploadedImg, setUploadedImg] = useState([])
  const [page, setPage] = useState(1)
  const [position, setPosition] = useState('bottom-right')
  const [widthPercent, setWidthPercent] = useState(30)
  const [outputName, setOutputName] = useState('')
  const [loading, setLoading] = useState(false)
  const [hasDrawn, setHasDrawn] = useState(false)
  const toast = useToast()
  const { progress, onProgress, reset } = useProgress()

  const canvasRef = useRef(null)
  const drawingRef = useRef(false)
  const lastRef = useRef(null)

  // Init canvas (HiDPI ready)
  useEffect(() => {
    if (mode !== 'draw') return
    const canvas = canvasRef.current
    if (!canvas) return
    const ratio = window.devicePixelRatio || 1
    const cssWidth = canvas.clientWidth
    const cssHeight = canvas.clientHeight
    canvas.width = cssWidth * ratio
    canvas.height = cssHeight * ratio
    const ctx = canvas.getContext('2d')
    ctx.scale(ratio, ratio)
    ctx.lineCap = 'round'
    ctx.lineJoin = 'round'
    ctx.strokeStyle = '#0f172a'
    ctx.lineWidth = 2.5
  }, [mode])

  function pos(e) {
    const canvas = canvasRef.current
    const rect = canvas.getBoundingClientRect()
    const isTouch = e.touches?.[0]
    const cx = isTouch ? isTouch.clientX : e.clientX
    const cy = isTouch ? isTouch.clientY : e.clientY
    return { x: cx - rect.left, y: cy - rect.top }
  }

  function start(e) {
    e.preventDefault()
    drawingRef.current = true
    lastRef.current = pos(e)
  }
  function move(e) {
    if (!drawingRef.current) return
    e.preventDefault()
    const ctx = canvasRef.current.getContext('2d')
    const p = pos(e)
    ctx.beginPath()
    ctx.moveTo(lastRef.current.x, lastRef.current.y)
    ctx.lineTo(p.x, p.y)
    ctx.stroke()
    lastRef.current = p
    setHasDrawn(true)
  }
  function end() { drawingRef.current = false; lastRef.current = null }

  function clear() {
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext('2d')
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    setHasDrawn(false)
  }

  /** Convertit le canvas en PNG Blob, ou null si vide */
  function canvasToBlob() {
    return new Promise(resolve => {
      canvasRef.current.toBlob(b => resolve(b), 'image/png')
    })
  }

  async function handleSubmit() {
    if (!pdfFiles[0]) return toast.error('Sélectionnez un PDF.')

    let sigFile = null
    if (mode === 'draw') {
      if (!hasDrawn) return toast.error('Dessinez votre signature.')
      const blob = await canvasToBlob()
      sigFile = new File([blob], 'signature.png', { type: 'image/png' })
    } else {
      if (!uploadedImg[0]) return toast.error('Importez une image de signature.')
      sigFile = uploadedImg[0]
    }

    const preset = POSITIONS.find(p => p.id === position) || POSITIONS[0]
    setLoading(true)
    try {
      await addSignatureImage(pdfFiles[0], sigFile, {
        page, xPercent: preset.x, yPercent: preset.y, widthPercent,
      }, outputName, onProgress)
      toast.success('Signature apposée.')
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
      reset()
    }
  }

  return (
    <ToolPage
      icon={PenLine}
      title="Signature manuscrite"
      subtitle="Dessinez votre signature à la souris/au doigt ou importez une image, puis apposez-la sur une page."
      footer="Pour une vraie signature cryptographique (PKCS#12), utilisez l'outil Signature."
    >
      <DropZone onFiles={setPdfFiles} label="Déposez le PDF à signer" />

      {/* Onglets draw/upload */}
      <div className="mt-6">
        <div className="inline-flex rounded-lg border border-ink-200 dark:border-ink-700 p-1 bg-ink-50 dark:bg-ink-900">
          <button type="button" onClick={() => setMode('draw')}
                  className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md text-sm font-medium transition-all ${
                    mode === 'draw'
                      ? 'bg-white dark:bg-ink-800 shadow-sm text-ink-900 dark:text-ink-100'
                      : 'text-ink-500 dark:text-ink-400'
                  }`}>
            <PenLine size={14} /> Dessiner
          </button>
          <button type="button" onClick={() => setMode('upload')}
                  className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md text-sm font-medium transition-all ${
                    mode === 'upload'
                      ? 'bg-white dark:bg-ink-800 shadow-sm text-ink-900 dark:text-ink-100'
                      : 'text-ink-500 dark:text-ink-400'
                  }`}>
            <Upload size={14} /> Importer
          </button>
        </div>
      </div>

      {mode === 'draw' && (
        <div className="mt-4">
          <label className="field-label flex items-center justify-between">
            <span>Tracez votre signature ci-dessous</span>
            <button type="button" onClick={clear}
                    className="btn-ghost h-7 px-2 text-xs">
              <Eraser size={12} /> Effacer
            </button>
          </label>
          <canvas
            ref={canvasRef}
            className="w-full h-44 rounded-xl border-2 border-dashed border-ink-300 dark:border-ink-700 bg-white touch-none cursor-crosshair"
            onMouseDown={start} onMouseMove={move} onMouseUp={end} onMouseLeave={end}
            onTouchStart={start} onTouchMove={move} onTouchEnd={end}
          />
        </div>
      )}

      {mode === 'upload' && (
        <div className="mt-4">
          <label className="field-label">Image de signature (PNG transparent recommandé)</label>
          <DropZone
            onFiles={setUploadedImg}
            label="Déposez l'image"
            hint="PNG (avec transparence) ou JPG"
            accept={{ 'image/jpeg': ['.jpg', '.jpeg'], 'image/png': ['.png'] }}
          />
        </div>
      )}

      <div className="mt-6 grid sm:grid-cols-2 gap-5">
        <div>
          <label className="field-label">Page à signer</label>
          <input type="number" min={1} value={page}
                 onChange={e => setPage(Number(e.target.value))} className="field" />
        </div>
        <div>
          <label className="field-label">Largeur — {widthPercent} %</label>
          <input type="range" min={10} max={70} step={5} value={widthPercent}
                 onChange={e => setWidthPercent(Number(e.target.value))} className="w-full accent-brand-600" />
        </div>
      </div>

      <div className="mt-6">
        <label className="field-label">Position</label>
        <div className="grid grid-cols-3 gap-2">
          {POSITIONS.map(p => (
            <button
              key={p.id}
              type="button"
              onClick={() => setPosition(p.id)}
              className={`rounded-lg border py-2 text-xs font-semibold transition-all ${
                position === p.id
                  ? 'border-brand-500 bg-brand-50 text-brand-700 dark:bg-brand-950/50 dark:text-brand-300'
                  : 'border-ink-200 hover:border-ink-300 text-ink-600 dark:border-ink-700 dark:hover:border-ink-600 dark:text-ink-300'
              }`}
            >
              {p.label}
            </button>
          ))}
        </div>
      </div>

      <div className="mt-6">
        <OutputFilenameField value={outputName} onChange={setOutputName} placeholder="document-signe-main" extension=".pdf" />
      </div>

      <div className="mt-6">
        <SubmitButton loading={loading} progress={progress} onClick={handleSubmit}
                      disabled={!pdfFiles[0] || (mode === 'draw' ? !hasDrawn : !uploadedImg[0])}
                      icon={ImageIcon}>
          Apposer la signature
        </SubmitButton>
      </div>
    </ToolPage>
  )
}
