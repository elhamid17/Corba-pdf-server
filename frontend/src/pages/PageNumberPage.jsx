import { useState } from 'react'
import { Hash } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import OutputFilenameField from '../components/OutputFilenameField'
import { useToast } from '../components/Toast'
import { useProgress } from '../hooks/useProgress'
import { addPageNumbers } from '../api/pdfApi'

const POSITIONS = [
  { id: 'top-left',      label: 'Haut gauche'  },
  { id: 'top-center',    label: 'Haut centre'  },
  { id: 'top-right',     label: 'Haut droite'  },
  { id: 'bottom-left',   label: 'Bas gauche'   },
  { id: 'bottom-center', label: 'Bas centre'   },
  { id: 'bottom-right',  label: 'Bas droite'   },
]

const FORMATS = [
  { value: '%d',          label: '1, 2, 3…' },
  { value: '%d/%t',       label: '1/10, 2/10…' },
  { value: 'Page %d',     label: 'Page 1, Page 2…' },
  { value: 'Page %d / %t', label: 'Page 1 / 10…' },
  { value: '- %d -',      label: '- 1 -, - 2 -…' },
]

export default function PageNumberPage() {
  const [files, setFiles] = useState([])
  const [position, setPosition] = useState('bottom-center')
  const [format, setFormat] = useState('%d')
  const [startNumber, setStartNumber] = useState(1)
  const [fontSize, setFontSize] = useState(12)
  const [outputName, setOutputName] = useState('')
  const [loading, setLoading] = useState(false)
  const toast = useToast()
  const { progress, onProgress, reset } = useProgress()

  async function handleSubmit() {
    if (!files[0]) return toast.error('Sélectionnez un PDF.')
    setLoading(true)
    try {
      await addPageNumbers(files[0], { position, format, startNumber, fontSize }, outputName, onProgress)
      toast.success('Pages numérotées.')
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
      reset()
    }
  }

  return (
    <ToolPage
      icon={Hash}
      title="Numérotation des pages"
      subtitle="Ajoutez des numéros sur chaque page : position, format et taille au choix."
    >
      <DropZone onFiles={setFiles} label="Déposez votre PDF" />

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
        <label className="field-label">Format</label>
        <div className="grid sm:grid-cols-2 gap-2">
          {FORMATS.map(f => (
            <button
              key={f.value}
              type="button"
              onClick={() => setFormat(f.value)}
              className={`rounded-lg border py-2 px-3 text-sm font-medium text-left transition-all ${
                format === f.value
                  ? 'border-brand-500 bg-brand-50 text-brand-700 dark:bg-brand-950/50 dark:text-brand-300'
                  : 'border-ink-200 hover:border-ink-300 text-ink-600 dark:border-ink-700 dark:hover:border-ink-600 dark:text-ink-300'
              }`}
            >
              {f.label}
            </button>
          ))}
        </div>
      </div>

      <div className="mt-6 grid sm:grid-cols-2 gap-5">
        <div>
          <label className="field-label">Page de départ</label>
          <input type="number" min={1} value={startNumber}
                 onChange={e => setStartNumber(Number(e.target.value))} className="field" />
        </div>
        <div>
          <label className="field-label">Taille — {fontSize} pt</label>
          <input type="range" min={6} max={32} step={1} value={fontSize}
                 onChange={e => setFontSize(Number(e.target.value))} className="w-full accent-brand-600" />
        </div>
      </div>

      <div className="mt-6">
        <OutputFilenameField value={outputName} onChange={setOutputName} placeholder="document-numerote" extension=".pdf" />
      </div>

      <div className="mt-6">
        <SubmitButton loading={loading} progress={progress} onClick={handleSubmit} disabled={!files[0]}>
          Numéroter
        </SubmitButton>
      </div>
    </ToolPage>
  )
}
