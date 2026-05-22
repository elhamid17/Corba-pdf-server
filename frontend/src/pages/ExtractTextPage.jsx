import { useState } from 'react'
import { FileText, Copy, Check } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import { useToast } from '../components/Toast'
import { useProgress } from '../hooks/useProgress'
import { extractText } from '../api/pdfApi'

export default function ExtractTextPage() {
  const [files, setFiles] = useState([])
  const [text,  setText]  = useState('')
  const [loading, setLoading] = useState(false)
  const [copied, setCopied] = useState(false)
  const toast = useToast()
  const { progress, onProgress, reset } = useProgress()

  async function handleSubmit() {
    if (!files[0]) return toast.error('Sélectionnez un PDF.')
    setLoading(true)
    setText('')
    try {
      const res = await extractText(files[0], onProgress)
      setText(res.text || '')
      toast.success('Texte extrait.')
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
      icon={FileText}
      title="Extraction de texte"
      subtitle="Récupérez le contenu textuel brut de votre PDF (sans OCR)."
    >
      <DropZone onFiles={setFiles} label="Déposez votre PDF" />
      <div className="mt-6">
        <SubmitButton loading={loading} progress={progress} onClick={handleSubmit} disabled={!files[0]}>
          Extraire le texte
        </SubmitButton>
      </div>

      {text && (
        <div className="mt-6">
          <div className="flex items-center justify-between mb-2">
            <p className="text-xs font-semibold uppercase tracking-wider text-ink-500 dark:text-ink-400">Résultat — {text.length} caractères</p>
            <button onClick={copy} className="btn-ghost py-1 px-3 text-xs">
              {copied ? <><Check size={14}/> Copié</> : <><Copy size={14}/> Copier</>}
            </button>
          </div>
          <textarea
            readOnly
            value={text}
            className="field font-mono text-sm min-h-72 whitespace-pre-wrap"
          />
        </div>
      )}
    </ToolPage>
  )
}
