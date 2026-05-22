import { useState } from 'react'
import { FileText } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import OutputFilenameField from '../components/OutputFilenameField'
import { useToast } from '../components/Toast'
import { useProgress } from '../hooks/useProgress'
import { odtToPdf } from '../api/pdfApi'

export default function OdtToPdfPage() {
  const [files, setFiles] = useState([])
  const [outputName, setOutputName] = useState('')
  const [loading, setLoading] = useState(false)
  const toast = useToast()
  const { progress, onProgress, reset } = useProgress()

  async function handleSubmit() {
    if (!files[0]) return toast.error('Sélectionnez un document ODT.')
    setLoading(true)
    try {
      await odtToPdf(files[0], outputName, onProgress)
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
      title="ODT → PDF"
      subtitle="Convertit un document OpenDocument Text (LibreOffice / OpenOffice) en PDF avec titres, paragraphes et listes."
    >
      <DropZone
        onFiles={setFiles}
        label="Déposez le document ODT"
        hint="Fichier ODT, 50 Mo max"
        accept={{
          'application/vnd.oasis.opendocument.text': ['.odt'],
        }}
      />
      <div className="mt-6">
        <OutputFilenameField value={outputName} onChange={setOutputName} placeholder="document-odt" extension=".pdf" />
      </div>
      <div className="mt-6">
        <SubmitButton loading={loading} progress={progress} onClick={handleSubmit} disabled={!files[0]}>
          Convertir en PDF
        </SubmitButton>
      </div>
    </ToolPage>
  )
}
