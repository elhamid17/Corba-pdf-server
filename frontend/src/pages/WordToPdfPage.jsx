import { useState } from 'react'
import { FileText } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import OutputFilenameField from '../components/OutputFilenameField'
import { useToast } from '../components/Toast'
import { useProgress } from '../hooks/useProgress'
import { wordToPdf } from '../api/pdfApi'

export default function WordToPdfPage() {
  const [files, setFiles] = useState([])
  const [outputName, setOutputName] = useState('')
  const [loading, setLoading] = useState(false)
  const toast = useToast()
  const { progress, onProgress, reset } = useProgress()

  async function handleSubmit() {
    if (files.length === 0) return toast.error('Sélectionnez un document Word.')
    setLoading(true)
    try {
      await wordToPdf(files[0], outputName, onProgress)
      toast.success('Conversion réussie. Téléchargement lancé.')
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
      reset()
    }
  }

  return (
    <ToolPage
      icon={FileText}
      title="Word → PDF"
      subtitle="Convertissez votre document Word (DOCX) en PDF. Le texte est conservé, la mise en forme avancée (images, tableaux, styles) ne l'est pas."
    >
      <DropZone
        onFiles={setFiles}
        label="Déposez le document Word à convertir"
        hint="Fichier DOCX, 50 Mo max"
        accept={{
          'application/vnd.openxmlformats-officedocument.wordprocessingml.document': ['.docx'],
        }}
      />
      <div className="mt-6">
        <OutputFilenameField value={outputName} onChange={setOutputName} placeholder="document-pdf" extension=".pdf" />
      </div>
      <div className="mt-6">
        <SubmitButton loading={loading} progress={progress} onClick={handleSubmit} disabled={files.length === 0}>
          Convertir en PDF
        </SubmitButton>
      </div>
    </ToolPage>
  )
}
