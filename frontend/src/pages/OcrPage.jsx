import { useState } from 'react'
import { ScanText, Copy, Check } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import { useToast } from '../components/Toast'
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

  async function handleSubmit() {
    if (!files[0]) return toast.error('Sélectionnez un PDF.')
    setLoading(true)
    setText('')
    try {
      const res = await performOCR(files[0], language)
      setText(res.text || '')
      toast.success(`OCR ${res.language || language} terminée.`)
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
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
                language === l.code ? 'border-brand-500 bg-brand-50 text-brand-700' : 'border-ink-200 hover:border-ink-300 text-ink-600'
              }`}
            >
              {l.label} <span className="text-ink-400 font-mono text-xs">{l.code}</span>
            </button>
          ))}
        </div>
      </div>

      <div className="mt-6">
        <SubmitButton loading={loading} onClick={handleSubmit} loadingText="OCR en cours…" disabled={!files[0]}>
          Lancer l’OCR
        </SubmitButton>
      </div>

      {text && (
        <div className="mt-6">
          <div className="flex items-center justify-between mb-2">
            <p className="text-xs font-semibold uppercase tracking-wider text-ink-500">Texte reconnu</p>
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
