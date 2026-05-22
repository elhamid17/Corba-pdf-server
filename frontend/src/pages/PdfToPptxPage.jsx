import { useState } from 'react'
import { Presentation } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import OutputFilenameField from '../components/OutputFilenameField'
import { useToast } from '../components/Toast'
import { useProgress } from '../hooks/useProgress'
import { pdfToPptx } from '../api/pdfApi'

export default function PdfToPptxPage() {
  const [files, setFiles] = useState([])
  const [outputName, setOutputName] = useState('')
  const [loading, setLoading] = useState(false)
  const toast = useToast()
  const { progress, onProgress, reset } = useProgress()

  async function handleSubmit() {
    if (!files[0]) return toast.error('Sélectionnez un PDF.')
    setLoading(true)
    try {
      await pdfToPptx(files[0], outputName, onProgress)
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
      icon={Presentation}
      title="PDF → PowerPoint"
      subtitle="Une slide PowerPoint par page PDF. Le texte est extrait page par page ; les images et mises en page complexes ne sont pas conservées."
    >
      <DropZone onFiles={setFiles} label="Déposez le PDF à convertir" />
      <div className="mt-6">
        <OutputFilenameField value={outputName} onChange={setOutputName} placeholder="presentation" extension=".pptx" />
      </div>
      <div className="mt-6">
        <SubmitButton loading={loading} progress={progress} onClick={handleSubmit} disabled={!files[0]}>
          Convertir en PowerPoint
        </SubmitButton>
      </div>
    </ToolPage>
  )
}
