import { useState } from 'react'
import { RotateCw } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import { useToast } from '../components/Toast'
import { rotatePDF } from '../api/pdfApi'

const ANGLES = [
  { value: 90,  label: '90°',  desc: 'Sens horaire' },
  { value: 180, label: '180°', desc: 'Inverse' },
  { value: 270, label: '270°', desc: 'Antihoraire' },
]

export default function RotatePage() {
  const [files, setFiles] = useState([])
  const [angle, setAngle] = useState(90)
  const [pages, setPages] = useState('')
  const [loading, setLoading] = useState(false)
  const toast = useToast()

  async function handleSubmit() {
    if (!files[0]) return toast.error('Sélectionnez un PDF.')
    const parsed = pages
      .split(',')
      .map(v => Number(v.trim()))
      .filter(v => Number.isFinite(v) && v > 0)
    setLoading(true)
    try {
      await rotatePDF(files[0], angle, parsed)
      toast.success('Rotation appliquée.')
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <ToolPage
      icon={RotateCw}
      title="Rotation des pages"
      subtitle="Pivotez l’ensemble du document ou seulement certaines pages."
    >
      <DropZone onFiles={setFiles} label="Déposez votre PDF" />

      <div className="mt-6">
        <label className="field-label">Angle de rotation</label>
        <div className="grid grid-cols-3 gap-3">
          {ANGLES.map(a => (
            <button
              key={a.value}
              type="button"
              onClick={() => setAngle(a.value)}
              className={`rounded-xl border p-3 text-left transition-all ${
                angle === a.value
                  ? 'border-brand-500 bg-brand-50/70 ring-2 ring-brand-500/20'
                  : 'border-ink-200 hover:border-ink-300'
              }`}
            >
              <p className="font-bold text-ink-900">{a.label}</p>
              <p className="text-xs text-ink-500">{a.desc}</p>
            </button>
          ))}
        </div>
      </div>

      <div className="mt-6">
        <label className="field-label">Pages spécifiques <span className="text-ink-400 normal-case tracking-normal">(optionnel)</span></label>
        <input
          value={pages}
          onChange={e => setPages(e.target.value)}
          className="field font-mono"
          placeholder="Laisser vide pour toutes les pages — ou ex : 1,3,5"
        />
      </div>

      <div className="mt-6">
        <SubmitButton loading={loading} onClick={handleSubmit} disabled={!files[0]}>
          Pivoter
        </SubmitButton>
      </div>
    </ToolPage>
  )
}
