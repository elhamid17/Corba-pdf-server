import { useState } from 'react'
import { FileSearch, FileMinus } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import OutputFilenameField from '../components/OutputFilenameField'
import { useToast } from '../components/Toast'
import { useProgress } from '../hooks/useProgress'
import { extractPages, deletePages } from '../api/pdfApi'

/**
 * Page unifiee "Sélection de pages" — fusionne Extraction + Suppression.
 *
 * Toggle : on garde les pages indiquees OU on les supprime.
 *  - keep   : appelle /extract-pages
 *  - remove : appelle /delete-pages
 */

const MODES = [
  { id: 'keep',   label: 'Garder ces pages',     desc: 'Crée un PDF avec uniquement les pages indiquées', icon: FileSearch },
  { id: 'remove', label: 'Supprimer ces pages', desc: 'Crée un PDF sans les pages indiquées',            icon: FileMinus  },
]

export default function SelectPagesPage() {
  const [files, setFiles] = useState([])
  const [pages, setPages] = useState('1,2,5')
  const [mode, setMode] = useState('keep')
  const [outputName, setOutputName] = useState('')
  const [loading, setLoading] = useState(false)
  const toast = useToast()
  const { progress, onProgress, reset } = useProgress()

  async function handleSubmit() {
    if (!files[0]) return toast.error('Sélectionnez un PDF.')
    const parsed = pages.split(',').map(v => Number(v.trim())).filter(v => Number.isFinite(v) && v > 0)
    if (!parsed.length) return toast.error('Indiquez au moins une page (ex : 1,2,5).')
    setLoading(true)
    try {
      if (mode === 'keep') {
        await extractPages(files[0], parsed, outputName, onProgress)
        toast.success(`${parsed.length} page(s) extraite(s).`)
      } else {
        await deletePages(files[0], parsed, outputName, onProgress)
        toast.success(`${parsed.length} page(s) supprimée(s).`)
      }
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
      reset()
    }
  }

  const currentMode = MODES.find(m => m.id === mode)

  return (
    <ToolPage
      icon={currentMode.icon}
      title="Sélection de pages"
      subtitle="Indiquez les pages à conserver ou à retirer d'un document."
    >
      <DropZone onFiles={setFiles} label="Déposez votre PDF source" />

      <div className="mt-6">
        <label className="field-label">Action</label>
        <div className="grid sm:grid-cols-2 gap-2">
          {MODES.map(m => {
            const Icon = m.icon
            const active = mode === m.id
            return (
              <button
                key={m.id}
                type="button"
                onClick={() => setMode(m.id)}
                className={`text-left p-3 rounded-lg border transition-all ${
                  active
                    ? 'border-brand-500 bg-brand-50/70 dark:bg-brand-950/40'
                    : 'border-ink-200 hover:border-ink-300 dark:border-ink-700 dark:hover:border-ink-600'
                }`}
              >
                <div className="flex items-center gap-2 mb-1">
                  <Icon size={14} className={active ? 'text-brand-600 dark:text-brand-400' : 'text-ink-500'} />
                  <span className="text-sm font-semibold">{m.label}</span>
                </div>
                <p className="text-xs text-ink-500 dark:text-ink-400">{m.desc}</p>
              </button>
            )
          })}
        </div>
      </div>

      <div className="mt-6">
        <label className="field-label">
          Pages {mode === 'keep' ? 'à garder' : 'à supprimer'}
        </label>
        <input
          value={pages}
          onChange={e => setPages(e.target.value)}
          className="field font-mono"
          placeholder="1,2,5"
        />
        <p className="mt-1 text-xs text-ink-500">Numéros séparés par virgule. Ex : <span className="font-mono">1,2,5,12</span></p>
      </div>

      <div className="mt-6">
        <OutputFilenameField value={outputName} onChange={setOutputName} placeholder={mode === 'keep' ? 'pages-extraites' : 'sans-pages-X'} extension=".pdf" />
      </div>

      <div className="mt-6">
        <SubmitButton loading={loading} progress={progress} onClick={handleSubmit} disabled={!files[0]}>
          {mode === 'keep' ? 'Extraire les pages' : 'Supprimer les pages'}
        </SubmitButton>
      </div>
    </ToolPage>
  )
}
