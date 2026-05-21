import { useState } from 'react'
import { Shuffle, GripVertical, Trash2 } from 'lucide-react'
import {
  DndContext, closestCenter, KeyboardSensor, PointerSensor,
  useSensor, useSensors,
} from '@dnd-kit/core'
import {
  arrayMove, SortableContext, sortableKeyboardCoordinates,
  rectSortingStrategy, useSortable,
} from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import OutputFilenameField from '../components/OutputFilenameField'
import { useToast } from '../components/Toast'
import { reorderPdf } from '../api/pdfApi'
import { apiFetch, readError } from '../api/client'

export default function ReorderPage() {
  const [files, setFiles] = useState([])
  const [pageCount, setPageCount] = useState(0)
  const [order, setOrder] = useState([])  // ex: [1,2,3,4]
  const [outputName, setOutputName] = useState('')
  const [loading, setLoading] = useState(false)
  const [counting, setCounting] = useState(false)
  const toast = useToast()

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 5 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
  )

  async function onFilesChange(fs) {
    setFiles(fs)
    setOrder([])
    setPageCount(0)
    if (!fs[0]) return
    setCounting(true)
    try {
      const form = new FormData()
      form.append('file', fs[0])
      const res = await apiFetch('/api/pdf/page-count', { method: 'POST', body: form })
      if (!res.ok) throw new Error(await readError(res))
      const data = await res.json()
      setPageCount(data.pageCount)
      setOrder(Array.from({ length: data.pageCount }, (_, i) => i + 1))
    } catch (e) {
      toast.error(e.message || 'Lecture du nombre de pages impossible.')
    } finally {
      setCounting(false)
    }
  }

  function onDragEnd(event) {
    const { active, over } = event
    if (!over || active.id === over.id) return
    const oldIndex = order.indexOf(Number(active.id))
    const newIndex = order.indexOf(Number(over.id))
    setOrder(arrayMove(order, oldIndex, newIndex))
  }

  function removePage(p) {
    setOrder(o => o.filter(x => x !== p))
  }

  function reset() {
    setOrder(Array.from({ length: pageCount }, (_, i) => i + 1))
  }

  async function handleSubmit() {
    if (!files[0]) return toast.error('Sélectionnez un PDF.')
    if (order.length === 0) return toast.error('Conservez au moins une page.')
    setLoading(true)
    try {
      await reorderPdf(files[0], order, outputName)
      toast.success(`PDF réorganisé : ${order.length} page(s).`)
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <ToolPage
      icon={Shuffle}
      title="Réorganisation des pages"
      subtitle="Glissez-déposez les pages pour les remettre dans l'ordre voulu. Supprimez celles dont vous ne voulez pas."
    >
      <DropZone onFiles={onFilesChange} label="Déposez votre PDF" />

      {counting && (
        <p className="mt-4 text-sm text-ink-500">Analyse du PDF…</p>
      )}

      {pageCount > 0 && (
        <div className="mt-6">
          <div className="flex items-center justify-between mb-3">
            <p className="text-sm text-ink-600 dark:text-ink-300">
              <strong>{pageCount}</strong> page(s) au total — <strong>{order.length}</strong> dans l'ordre final
            </p>
            <button type="button" onClick={reset} className="btn-ghost h-8 px-3 text-xs">
              Réinitialiser
            </button>
          </div>

          <DndContext sensors={sensors} collisionDetection={closestCenter} onDragEnd={onDragEnd}>
            <SortableContext items={order.map(String)} strategy={rectSortingStrategy}>
              <ul className="grid grid-cols-3 sm:grid-cols-4 md:grid-cols-5 gap-3">
                {order.map((pageNum, idx) => (
                  <PageCard
                    key={pageNum}
                    pageNum={pageNum}
                    position={idx + 1}
                    onRemove={() => removePage(pageNum)}
                  />
                ))}
              </ul>
            </SortableContext>
          </DndContext>
        </div>
      )}

      {pageCount > 0 && (
        <div className="mt-6">
          <OutputFilenameField value={outputName} onChange={setOutputName} placeholder="document-reorganise" extension=".pdf" />
        </div>
      )}

      <div className="mt-6">
        <SubmitButton loading={loading} onClick={handleSubmit} disabled={!files[0] || order.length === 0}>
          Réorganiser
        </SubmitButton>
      </div>
    </ToolPage>
  )
}

function PageCard({ pageNum, position, onRemove }) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({ id: String(pageNum) })
  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.4 : 1,
  }
  return (
    <li
      ref={setNodeRef}
      style={style}
      className="relative aspect-[3/4] rounded-lg border-2 border-ink-200 dark:border-ink-700 bg-white dark:bg-ink-900 flex flex-col items-center justify-center select-none group hover:border-brand-400 dark:hover:border-brand-500/50 transition-colors"
    >
      <span className="absolute top-1 left-1 text-[10px] font-mono bg-ink-100 dark:bg-ink-800 px-1.5 py-0.5 rounded text-ink-500">
        #{position}
      </span>
      <button
        type="button"
        onClick={onRemove}
        className="absolute top-1 right-1 opacity-0 group-hover:opacity-100 transition-opacity text-rose-500 hover:bg-rose-50 dark:hover:bg-rose-950/40 rounded p-0.5"
        aria-label="Retirer cette page"
        title="Retirer"
      >
        <Trash2 size={12} />
      </button>
      <div {...attributes} {...listeners} className="flex-1 flex flex-col items-center justify-center cursor-grab active:cursor-grabbing w-full">
        <GripVertical size={16} className="text-ink-300 dark:text-ink-600 mb-1" />
        <p className="text-2xl font-bold font-display text-ink-700 dark:text-ink-200">{pageNum}</p>
        <p className="text-[10px] text-ink-400 uppercase tracking-wider">page</p>
      </div>
    </li>
  )
}
