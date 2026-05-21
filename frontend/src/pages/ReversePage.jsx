import { useState } from 'react'
import { FlipVertical2 } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import OutputFilenameField from '../components/OutputFilenameField'
import { useToast } from '../components/Toast'
import { reversePdf } from '../api/pdfApi'

export default function ReversePage() {
  const [files, setFiles] = useState([])
  const [outputName, setOutputName] = useState('')
  const [loading, setLoading] = useState(false)
  const toast = useToast()

  async function handleSubmit() {
    if (!files[0]) return toast.error('Sélectionnez un PDF.')
    setLoading(true)
    try {
      await reversePdf(files[0], outputName)
      toast.success('Pages inversées. Téléchargement lancé.')
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <ToolPage
      icon={FlipVertical2}
      title="Inversion des pages"
      subtitle="Renverse l'ordre des pages : la dernière devient la première, etc."
    >
      <DropZone onFiles={setFiles} label="Déposez votre PDF" />
      <div className="mt-6">
        <OutputFilenameField value={outputName} onChange={setOutputName} placeholder="document-inverse" extension=".pdf" />
      </div>
      <div className="mt-6">
        <SubmitButton loading={loading} onClick={handleSubmit} disabled={!files[0]}>
          Inverser
        </SubmitButton>
      </div>
    </ToolPage>
  )
}
