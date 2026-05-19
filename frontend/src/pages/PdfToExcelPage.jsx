import { useState } from 'react'
import { Sheet } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import { useToast } from '../components/Toast'
import { pdfToExcel } from '../api/pdfApi'

export default function PdfToExcelPage() {
  const [files, setFiles] = useState([])
  const [loading, setLoading] = useState(false)
  const toast = useToast()

  async function handleSubmit() {
    if (files.length === 0) return toast.error('Sélectionnez un PDF.')
    setLoading(true)
    try {
      await pdfToExcel(files[0])
      toast.success('Conversion réussie. Téléchargement lancé.')
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <ToolPage
      icon={Sheet}
      title="PDF → Excel"
      subtitle="Convertissez votre PDF en classeur Excel (XLSX). Une feuille par page — chaque ligne de texte devient une ligne dans le tableur."
    >
      <DropZone
        onFiles={setFiles}
        label="Déposez le PDF à convertir"
        hint="Fichier PDF, 50 Mo max"
      />
      <div className="mt-6">
        <SubmitButton loading={loading} onClick={handleSubmit} disabled={files.length === 0}>
          Convertir en Excel
        </SubmitButton>
      </div>
    </ToolPage>
  )
}
