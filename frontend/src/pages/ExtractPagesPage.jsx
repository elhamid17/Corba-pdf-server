import { useState } from 'react'
import { FileSearch } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import { useToast } from '../components/Toast'
import { extractPages } from '../api/pdfApi'

export default function ExtractPagesPage() {
  const [files, setFiles] = useState([])
  const [pages, setPages] = useState('1,2,5')
  const [loading, setLoading] = useState(false)
  const toast = useToast()

  async function handleSubmit() {
    if (!files[0]) return toast.error('Sélectionnez un PDF.')
    const parsed = pages.split(',').map(v => Number(v.trim())).filter(v => Number.isFinite(v) && v > 0)
    if (!parsed.length) return toast.error('Indiquez au moins une page (ex : 1,2,5).')
    setLoading(true)
    try {
      await extractPages(files[0], parsed)
      toast.success('Extraction terminée.')
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <ToolPage
      icon={FileSearch}
      title="Extraction de pages"
      subtitle="Récupérez précisément les pages souhaitées dans un nouveau PDF."
    >
      <DropZone onFiles={setFiles} label="Déposez votre PDF source" />
      <div className="mt-6">
        <label className="field-label">Pages à extraire</label>
        <input
          value={pages}
          onChange={e => setPages(e.target.value)}
          className="field font-mono"
          placeholder="1,2,5"
        />
      </div>
      <div className="mt-6">
        <SubmitButton loading={loading} onClick={handleSubmit} disabled={!files[0]}>
          Extraire les pages
        </SubmitButton>
      </div>
    </ToolPage>
  )
}
