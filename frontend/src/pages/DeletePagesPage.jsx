import { useState } from 'react'
import { FileMinus } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import { useToast } from '../components/Toast'
import { deletePages } from '../api/pdfApi'

export default function DeletePagesPage() {
  const [files, setFiles] = useState([])
  const [pages, setPages] = useState('')
  const [loading, setLoading] = useState(false)
  const toast = useToast()

  async function handleSubmit() {
    if (!files[0]) return toast.error('Sélectionnez un PDF.')
    const parsed = pages.split(',').map(v => Number(v.trim())).filter(v => Number.isFinite(v) && v > 0)
    if (!parsed.length) return toast.error('Indiquez au moins une page à supprimer.')
    setLoading(true)
    try {
      await deletePages(files[0], parsed)
      toast.success('Pages supprimées avec succès.')
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <ToolPage
      icon={FileMinus}
      title="Suppression de pages"
      subtitle="Retirez définitivement les pages indiquées du document."
    >
      <DropZone onFiles={setFiles} label="Déposez votre PDF" />
      <div className="mt-6">
        <label className="field-label">Pages à supprimer</label>
        <input
          value={pages}
          onChange={e => setPages(e.target.value)}
          className="field font-mono"
          placeholder="3,7,8"
        />
      </div>
      <div className="mt-6">
        <SubmitButton loading={loading} onClick={handleSubmit} disabled={!files[0]}>
          Supprimer les pages
        </SubmitButton>
      </div>
    </ToolPage>
  )
}
