import { useState } from 'react'
import { Images } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import { useToast } from '../components/Toast'
import { imagesToPdf } from '../api/pdfApi'

export default function ImagesToPdfPage() {
  const [files, setFiles] = useState([])
  const [loading, setLoading] = useState(false)
  const toast = useToast()

  async function handleSubmit() {
    if (files.length === 0) return toast.error('Sélectionnez au moins une image.')
    setLoading(true)
    try {
      await imagesToPdf(files)
      toast.success('Conversion réussie. Téléchargement lancé.')
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <ToolPage
      icon={Images}
      title="Images → PDF"
      subtitle="Assemblez une ou plusieurs images (JPG, PNG) en un fichier PDF. L'ordre de dépôt = l'ordre des pages."
    >
      <DropZone
        multiple
        onFiles={setFiles}
        label="Déposez vos images à convertir"
        hint="Images JPG ou PNG — une page par image"
        accept={{
          'image/jpeg': ['.jpg', '.jpeg'],
          'image/png':  ['.png'],
        }}
      />
      <div className="mt-6">
        <SubmitButton loading={loading} onClick={handleSubmit} disabled={files.length === 0}>
          Convertir en PDF {files.length > 0 ? `(${files.length})` : ''}
        </SubmitButton>
      </div>
    </ToolPage>
  )
}
