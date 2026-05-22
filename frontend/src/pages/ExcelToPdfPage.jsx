import { useState } from 'react'
import { Sheet } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import OutputFilenameField from '../components/OutputFilenameField'
import { useToast } from '../components/Toast'
import { useProgress } from '../hooks/useProgress'
import { excelToPdf } from '../api/pdfApi'

export default function ExcelToPdfPage() {
  const [files, setFiles] = useState([])
  const [outputName, setOutputName] = useState('')
  const [loading, setLoading] = useState(false)
  const toast = useToast()
  const { progress, onProgress, reset } = useProgress()

  async function handleSubmit() {
    if (!files[0]) return toast.error('Sélectionnez un classeur Excel.')
    setLoading(true)
    try {
      await excelToPdf(files[0], outputName, onProgress)
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
      icon={Sheet}
      title="Excel → PDF"
      subtitle="Convertit un classeur Excel (XLSX) en PDF paysage : une grille par feuille avec les valeurs des cellules."
    >
      <DropZone
        onFiles={setFiles}
        label="Déposez le classeur Excel"
        hint="Fichier XLSX, 50 Mo max"
        accept={{
          'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': ['.xlsx'],
        }}
      />
      <div className="mt-6">
        <OutputFilenameField value={outputName} onChange={setOutputName} placeholder="tableur-pdf" extension=".pdf" />
      </div>
      <div className="mt-6">
        <SubmitButton loading={loading} progress={progress} onClick={handleSubmit} disabled={!files[0]}>
          Convertir en PDF
        </SubmitButton>
      </div>
    </ToolPage>
  )
}
