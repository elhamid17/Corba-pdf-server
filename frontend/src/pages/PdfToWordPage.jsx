import { useState } from 'react'
import { FileType2 } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import OutputFilenameField from '../components/OutputFilenameField'
import { useToast } from '../components/Toast'
import { pdfToWord } from '../api/pdfApi'

export default function PdfToWordPage() {
  const [files, setFiles] = useState([])
  const [outputName, setOutputName] = useState('')
  const [loading, setLoading] = useState(false)
  const toast = useToast()

  async function handleSubmit() {
    if (files.length === 0) return toast.error('Sélectionnez un PDF.')
    setLoading(true)
    try {
      await pdfToWord(files[0], outputName)
      toast.success('Conversion réussie. Téléchargement lancé.')
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <ToolPage
      icon={FileType2}
      title="PDF → Word"
      subtitle="Convertissez votre PDF en document Word (DOCX). Le texte est extrait page par page, la mise en page n'est pas conservée."
    >
      <DropZone
        onFiles={setFiles}
        label="Déposez le PDF à convertir"
        hint="Fichier PDF, 50 Mo max"
      />
      <div className="mt-6">
        <OutputFilenameField value={outputName} onChange={setOutputName} placeholder="document-word" extension=".docx" />
      </div>
      <div className="mt-6">
        <SubmitButton loading={loading} onClick={handleSubmit} disabled={files.length === 0}>
          Convertir en Word
        </SubmitButton>
      </div>
    </ToolPage>
  )
}
