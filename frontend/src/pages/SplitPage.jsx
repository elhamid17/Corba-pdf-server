import { useState } from 'react'
import { Scissors } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import { useToast } from '../components/Toast'
import { splitPDF } from '../api/pdfApi'

export default function SplitPage() {
  const [files, setFiles] = useState([])
  const [ranges, setRanges] = useState('1,3,4,6')
  const [loading, setLoading] = useState(false)
  const toast = useToast()

  async function handleSubmit() {
    if (!files[0]) return toast.error('Sélectionnez un PDF.')
    const parsed = ranges.split(',').map(v => Number(v.trim())).filter(v => Number.isFinite(v) && v > 0)
    if (parsed.length < 2 || parsed.length % 2 !== 0) {
      return toast.error('Saisissez des paires de pages, ex : 1,3,4,6')
    }
    setLoading(true)
    try {
      await splitPDF(files[0], parsed)
      toast.success('Découpage terminé. Archive ZIP téléchargée.')
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <ToolPage
      icon={Scissors}
      title="Découpage par intervalles"
      subtitle="Découpez votre PDF en plusieurs fichiers selon des paires de pages début/fin."
    >
      <DropZone onFiles={setFiles} label="Déposez votre PDF à découper" />
      <div className="mt-6">
        <label className="field-label">Intervalles de pages</label>
        <input
          value={ranges}
          onChange={e => setRanges(e.target.value)}
          className="field font-mono"
          placeholder="1,3,4,6 — produira 2 PDF : pages 1-3, pages 4-6"
        />
        <p className="mt-1.5 text-xs text-ink-400 dark:text-ink-500">Format : paires <code className="font-mono">début,fin,début,fin…</code></p>
      </div>
      <div className="mt-6">
        <SubmitButton loading={loading} onClick={handleSubmit} disabled={!files[0]}>
          Découper
        </SubmitButton>
      </div>
    </ToolPage>
  )
}
