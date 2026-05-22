import { useState } from 'react'
import { ScanText, Copy, Check, AlertTriangle } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import { useToast } from '../components/Toast'
import { useProgress } from '../hooks/useProgress'
import { performOCR } from '../api/pdfApi'

const LANGUAGES = [
  { code: 'fra', label: 'Français' },
  { code: 'eng', label: 'Anglais'  },
]

export default function OcrPage() {
  const [files, setFiles] = useState([])
  const [language, setLanguage] = useState('fra')
  const [text, setText] = useState('')
  const [loading, setLoading] = useState(false)
  const [copied, setCopied] = useState(false)
  const toast = useToast()
  const { progress, onProgress, reset } = useProgress()

  async function handleSubmit() {
    if (!files[0]) return toast.error('Sélectionnez un PDF.')
    setLoading(true)
    setText('')
    try {
      const res = await performOCR(files[0], language, onProgress)
      setText(res.text || '')
      toast.success(`OCR ${res.language || language} terminée.`)
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
      reset()
    }
  }

  async function copy() {
    await navigator.clipboard.writeText(text)
    setCopied(true)
    setTimeout(() => setCopied(false), 1500)
  }

  return (
    <ToolPage
      icon={ScanText}
      title="Reconnaissance optique (OCR)"
      subtitle="Extrayez le texte d’un PDF scanné grâce à Tesseract / Tess4J."
    >
      <div className="mb-4 flex items-start gap-2.5 rounded-xl border border-amber-200 dark:border-amber-800 bg-amber-50/60 dark:bg-amber-950/30 p-3 text-sm">
        <AlertTriangle size={16} className="shrink-0 mt-0.5 text-amber-600 dark:text-amber-400" />
        <div className="text-ink-700 dark:text-ink-200">
          L'OCR est lent (~5-10s par page) et limite à <strong>30 pages</strong> max.
          Pour un long document, découpez-le d'abord via l'outil <strong>Découpage</strong>.
        </div>
      </div>

      <DropZone onFiles={setFiles} label="Déposez votre PDF scanné" />

      <div className="mt-6">
        <label className="field-label">Langue de reconnaissance</label>
        <div className="grid grid-cols-2 gap-2">
          {LANGUAGES.map(l => (
            <button
              key={l.code}
              type="button"
              onClick={() => setLanguage(l.code)}
              className={`rounded-lg border py-2.5 text-sm font-semibold transition-all ${
                language === l.code
                  ? 'border-brand-500 bg-brand-50 text-brand-700 dark:bg-brand-950/50 dark:text-brand-300'
                  : 'border-ink-200 hover:border-ink-300 text-ink-600 dark:border-ink-700 dark:hover:border-ink-600 dark:text-ink-300'
              }`}
            >
              {l.label} <span className="text-ink-400 dark:text-ink-500 font-mono text-xs">{l.code}</span>
            </button>
          ))}
        </div>
      </div>

      <div className="mt-6">
        <SubmitButton loading={loading} progress={progress} onClick={handleSubmit} loadingText="OCR en cours…" disabled={!files[0]}>
          Lancer l’OCR
        </SubmitButton>
      </div>

      {text && (
        <div className="mt-6">
          <div className="flex items-center justify-between mb-2">
            <p className="text-xs font-semibold uppercase tracking-wider text-ink-500 dark:text-ink-400">Texte reconnu</p>
            <button onClick={copy} className="btn-ghost py-1 px-3 text-xs">
              {copied ? <><Check size={14}/> Copié</> : <><Copy size={14}/> Copier</>}
            </button>
          </div>
          <textarea readOnly value={text} className="field font-mono text-sm min-h-72 whitespace-pre-wrap" />
        </div>
      )}
    </ToolPage>
  )
}
