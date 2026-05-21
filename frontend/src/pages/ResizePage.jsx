import { useState } from 'react'
import { Maximize2 } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import OutputFilenameField from '../components/OutputFilenameField'
import { useToast } from '../components/Toast'
import { resizePdf } from '../api/pdfApi'

const SIZES = [
  { id: 'A4',     label: 'A4',     desc: '210 × 297 mm — standard' },
  { id: 'A3',     label: 'A3',     desc: '297 × 420 mm — grand' },
  { id: 'A5',     label: 'A5',     desc: '148 × 210 mm — compact' },
  { id: 'LETTER', label: 'Letter', desc: '216 × 279 mm — US' },
  { id: 'LEGAL',  label: 'Legal',  desc: '216 × 356 mm — US juridique' },
]

export default function ResizePage() {
  const [files, setFiles] = useState([])
  const [size, setSize] = useState('A4')
  const [outputName, setOutputName] = useState('')
  const [loading, setLoading] = useState(false)
  const toast = useToast()

  async function handleSubmit() {
    if (!files[0]) return toast.error('Sélectionnez un PDF.')
    setLoading(true)
    try {
      await resizePdf(files[0], size, outputName)
      toast.success(`PDF redimensionné en ${size}.`)
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <ToolPage
      icon={Maximize2}
      title="Redimensionnement"
      subtitle="Convertit toutes les pages vers un format standard, en conservant les proportions."
    >
      <DropZone onFiles={setFiles} label="Déposez votre PDF" />

      <div className="mt-6">
        <label className="field-label">Format cible</label>
        <div className="grid sm:grid-cols-2 gap-3">
          {SIZES.map(s => (
            <button
              key={s.id}
              type="button"
              onClick={() => setSize(s.id)}
              className={`rounded-xl border p-3 text-left transition-all ${
                size === s.id
                  ? 'border-brand-500 bg-brand-50/70 ring-2 ring-brand-500/20 dark:bg-brand-950/40'
                  : 'border-ink-200 hover:border-ink-300 dark:border-ink-700 dark:hover:border-ink-600'
              }`}
            >
              <p className="font-bold text-ink-900 dark:text-ink-100">{s.label}</p>
              <p className="text-xs text-ink-500 dark:text-ink-400">{s.desc}</p>
            </button>
          ))}
        </div>
      </div>

      <div className="mt-6">
        <OutputFilenameField value={outputName} onChange={setOutputName} placeholder="document-redim" extension=".pdf" />
      </div>

      <div className="mt-6">
        <SubmitButton loading={loading} onClick={handleSubmit} disabled={!files[0]}>
          Redimensionner
        </SubmitButton>
      </div>
    </ToolPage>
  )
}
