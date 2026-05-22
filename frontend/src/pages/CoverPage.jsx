import { useState } from 'react'
import { BookOpen } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import OutputFilenameField from '../components/OutputFilenameField'
import { useToast } from '../components/Toast'
import { useProgress } from '../hooks/useProgress'
import { addCoverPage } from '../api/pdfApi'

export default function CoverPage() {
  const [pdfFiles, setPdfFiles] = useState([])
  const [coverFiles, setCoverFiles] = useState([])
  const [outputName, setOutputName] = useState('')
  const [loading, setLoading] = useState(false)
  const toast = useToast()
  const { progress, onProgress, reset } = useProgress()

  async function handleSubmit() {
    if (!pdfFiles[0])   return toast.error('Sélectionnez le PDF.')
    if (!coverFiles[0]) return toast.error('Sélectionnez une image de couverture.')
    setLoading(true)
    try {
      await addCoverPage(pdfFiles[0], coverFiles[0], outputName, onProgress)
      toast.success('Page de garde ajoutée.')
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
      reset()
    }
  }

  return (
    <ToolPage
      icon={BookOpen}
      title="Page de garde"
      subtitle="Ajoutez une image (JPG/PNG) comme première page du document."
    >
      <div className="grid gap-5">
        <div>
          <label className="field-label">Document principal</label>
          <DropZone onFiles={setPdfFiles} label="Déposez le PDF" />
        </div>
        <div>
          <label className="field-label">Image de couverture</label>
          <DropZone
            onFiles={setCoverFiles}
            label="Déposez l'image (JPG/PNG)"
            hint="Image JPG ou PNG — pleine page"
            accept={{ 'image/jpeg': ['.jpg', '.jpeg'], 'image/png': ['.png'] }}
          />
        </div>
      </div>

      <div className="mt-6">
        <OutputFilenameField value={outputName} onChange={setOutputName} placeholder="document-avec-couverture" extension=".pdf" />
      </div>

      <div className="mt-6">
        <SubmitButton loading={loading} progress={progress} onClick={handleSubmit} disabled={!pdfFiles[0] || !coverFiles[0]}>
          Ajouter la page de garde
        </SubmitButton>
      </div>
    </ToolPage>
  )
}
