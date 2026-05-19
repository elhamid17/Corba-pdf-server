import { useState } from 'react'
import { Droplets } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import { useToast } from '../components/Toast'
import { addWatermark } from '../api/pdfApi'

export default function WatermarkPage() {
  const [files, setFiles] = useState([])
  const [text, setText] = useState('CONFIDENTIEL')
  const [opacity, setOpacity] = useState(0.3)
  const [fontSize, setFontSize] = useState(48)
  const [diagonal, setDiagonal] = useState(true)
  const [loading, setLoading] = useState(false)
  const toast = useToast()

  async function handleSubmit() {
    if (!files[0]) return toast.error('Sélectionnez un PDF.')
    if (!text.trim()) return toast.error('Saisissez le texte du filigrane.')
    setLoading(true)
    try {
      await addWatermark(files[0], { text, opacity, fontSize, diagonal })
      toast.success('Filigrane appliqué.')
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <ToolPage
      icon={Droplets}
      title="Filigrane"
      subtitle="Apposez un filigrane texte sur chaque page du document."
    >
      <DropZone onFiles={setFiles} label="Déposez votre PDF" />

      <div className="mt-6 space-y-5">
        <div>
          <label className="field-label">Texte du filigrane</label>
          <input value={text} onChange={e => setText(e.target.value)} className="field" placeholder="Ex. CONFIDENTIEL" />
        </div>

        <div className="grid sm:grid-cols-2 gap-5">
          <div>
            <label className="field-label">Opacité — {(opacity * 100).toFixed(0)} %</label>
            <input
              type="range" min={0.05} max={1} step={0.05}
              value={opacity}
              onChange={e => setOpacity(Number(e.target.value))}
              className="w-full accent-brand-600"
            />
          </div>
          <div>
            <label className="field-label">Taille de police</label>
            <input
              type="number" min={8} max={200}
              value={fontSize}
              onChange={e => setFontSize(Number(e.target.value))}
              className="field"
            />
          </div>
        </div>

        <label className="flex items-start gap-3 cursor-pointer">
          <input type="checkbox" checked={diagonal} onChange={e => setDiagonal(e.target.checked)} className="h-4 w-4 mt-0.5 rounded text-brand-600 focus:ring-brand-500/30" />
          <div>
            <p className="text-sm font-medium text-ink-800 dark:text-ink-100">Filigrane en diagonale</p>
            <p className="text-xs text-ink-500 dark:text-ink-400">Décochez pour un filigrane horizontal classique.</p>
          </div>
        </label>
      </div>

      <div className="mt-6">
        <SubmitButton loading={loading} onClick={handleSubmit} disabled={!files[0]}>
          Apposer le filigrane
        </SubmitButton>
      </div>
    </ToolPage>
  )
}
