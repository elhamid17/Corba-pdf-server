import { useState } from 'react'
import { Plus } from 'lucide-react'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import OutputFilenameField from '../components/OutputFilenameField'
import { useToast } from '../components/Toast'
import { createPDF } from '../api/pdfApi'

export default function CreatePage() {
  const [title, setTitle] = useState('Mon document')
  const [text,  setText]  = useState('')
  const [outputName, setOutputName] = useState('')
  const [loading, setLoading] = useState(false)
  const toast = useToast()

  async function handleSubmit() {
    if (!text.trim()) return toast.error('Saisissez le contenu textuel du document.')
    setLoading(true)
    try {
      await createPDF(text, title.trim() || 'Document', outputName)
      toast.success('PDF généré.')
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <ToolPage
      icon={Plus}
      title="Création de PDF"
      subtitle="Générez un PDF à partir d’un titre et d’un texte brut."
    >
      <div className="space-y-5">
        <div>
          <label className="field-label">Titre du document</label>
          <input value={title} onChange={e => setTitle(e.target.value)} className="field" placeholder="Mon document" />
        </div>
        <div>
          <label className="field-label">Contenu — {text.length} caractères</label>
          <textarea
            value={text}
            onChange={e => setText(e.target.value)}
            className="field min-h-72 font-mono text-sm"
            placeholder="Saisissez le contenu du PDF…"
          />
        </div>
        <OutputFilenameField value={outputName} onChange={setOutputName} placeholder="mon-document" extension=".pdf" />
      </div>
      <div className="mt-6">
        <SubmitButton loading={loading} onClick={handleSubmit} disabled={!text.trim()}>
          Générer le PDF
        </SubmitButton>
      </div>
    </ToolPage>
  )
}
