import { useState } from 'react'
import { Code2 } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import OutputFilenameField from '../components/OutputFilenameField'
import { useToast } from '../components/Toast'
import { useProgress } from '../hooks/useProgress'
import { pdfToMarkdown } from '../api/pdfApi'

export default function PdfToMarkdownPage() {
  const [files, setFiles] = useState([])
  const [outputName, setOutputName] = useState('')
  const [loading, setLoading] = useState(false)
  const toast = useToast()
  const { progress, onProgress, reset } = useProgress()

  async function handleSubmit() {
    if (!files[0]) return toast.error('Sélectionnez un PDF.')
    setLoading(true)
    try {
      await pdfToMarkdown(files[0], outputName, onProgress)
      toast.success('Markdown généré. Téléchargement lancé.')
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
      reset()
    }
  }

  return (
    <ToolPage
      icon={Code2}
      title="PDF → Markdown"
      subtitle="Extrait le texte d'un PDF au format Markdown : paragraphes, titres en majuscules détectés et listes basiques."
    >
      <DropZone onFiles={setFiles} label="Déposez le PDF à convertir" />
      <div className="mt-6">
        <OutputFilenameField value={outputName} onChange={setOutputName} placeholder="document" extension=".md" />
      </div>
      <div className="mt-6">
        <SubmitButton loading={loading} progress={progress} onClick={handleSubmit} disabled={!files[0]}>
          Convertir en Markdown
        </SubmitButton>
      </div>
    </ToolPage>
  )
}
