import { useState } from 'react'
import { Layers } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import OutputFilenameField from '../components/OutputFilenameField'
import { useToast } from '../components/Toast'
import { useProgress } from '../hooks/useProgress'
import { mergePDFs } from '../api/pdfApi'

export default function MergePage() {
  const [files, setFiles] = useState([])
  const [outputName, setOutputName] = useState('')
  const [loading, setLoading] = useState(false)
  const { progress, onProgress, reset } = useProgress()
  const toast = useToast()

  async function handleMerge() {
    if (files.length < 2) return toast.error('Sélectionnez au moins 2 fichiers PDF.')
    setLoading(true)
    try {
      await mergePDFs(files, outputName, onProgress)
      toast.success('Fusion réussie. Téléchargement lancé.')
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
      reset()
    }
  }

  return (
    <ToolPage
      icon={Layers}
      title="Fusion de PDF"
      subtitle="Assemblez plusieurs documents PDF en un fichier unique, dans l’ordre que vous choisissez."
    >
      <DropZone
        multiple
        onFiles={setFiles}
        label="Déposez vos fichiers PDF à fusionner"
        hint="Glissez-déposez plusieurs PDF. L’ordre de la liste = l’ordre de fusion."
      />
      <div className="mt-6">
        <OutputFilenameField value={outputName} onChange={setOutputName} placeholder="rapport-complet" extension=".pdf" />
      </div>
      <div className="mt-6">
        <SubmitButton loading={loading} progress={progress} onClick={handleMerge} disabled={files.length < 2}>
          Fusionner {files.length > 0 ? `(${files.length})` : ''}
        </SubmitButton>
      </div>
    </ToolPage>
  )
}
