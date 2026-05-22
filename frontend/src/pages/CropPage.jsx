import { useState } from 'react'
import { Crop } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import OutputFilenameField from '../components/OutputFilenameField'
import { useToast } from '../components/Toast'
import { useProgress } from '../hooks/useProgress'
import { cropPdf } from '../api/pdfApi'

export default function CropPage() {
  const [files, setFiles] = useState([])
  const [left, setLeft] = useState(5)
  const [right, setRight] = useState(5)
  const [top, setTop] = useState(5)
  const [bottom, setBottom] = useState(5)
  const [outputName, setOutputName] = useState('')
  const [loading, setLoading] = useState(false)
  const toast = useToast()
  const { progress, onProgress, reset } = useProgress()

  async function handleSubmit() {
    if (!files[0]) return toast.error('Sélectionnez un PDF.')
    if (left + right >= 100 || top + bottom >= 100) {
      return toast.error('Marges cumulées trop grandes (gauche+droite ou haut+bas ≥ 100%).')
    }
    setLoading(true)
    try {
      await cropPdf(files[0], { left, right, top, bottom }, outputName, onProgress)
      toast.success('PDF recadré.')
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
      reset()
    }
  }

  return (
    <ToolPage
      icon={Crop}
      title="Recadrage des pages"
      subtitle="Retirez visuellement des marges autour du contenu (en pourcentage de la page)."
    >
      <DropZone onFiles={setFiles} label="Déposez votre PDF" />

      <div className="mt-6 space-y-5">
        <MarginSlider label="Marge gauche" value={left} onChange={setLeft} />
        <MarginSlider label="Marge droite" value={right} onChange={setRight} />
        <MarginSlider label="Marge haut"   value={top} onChange={setTop} />
        <MarginSlider label="Marge bas"    value={bottom} onChange={setBottom} />

        {/* Apercu visuel */}
        <div>
          <p className="field-label">Aperçu</p>
          <div className="mx-auto w-full max-w-[200px] aspect-[210/297] bg-ink-100 dark:bg-ink-800 rounded-md border border-ink-200 dark:border-ink-700 relative overflow-hidden">
            <div
              className="absolute bg-brand-500/30 border-2 border-brand-500 border-dashed"
              style={{
                left: `${left}%`, right: `${right}%`,
                top: `${top}%`, bottom: `${bottom}%`,
              }}
            />
          </div>
        </div>
      </div>

      <div className="mt-6">
        <OutputFilenameField value={outputName} onChange={setOutputName} placeholder="document-recadre" extension=".pdf" />
      </div>

      <div className="mt-6">
        <SubmitButton loading={loading} progress={progress} onClick={handleSubmit} disabled={!files[0]}>
          Recadrer
        </SubmitButton>
      </div>
    </ToolPage>
  )
}

function MarginSlider({ label, value, onChange }) {
  return (
    <div>
      <label className="field-label">{label} — {value} %</label>
      <input
        type="range" min={0} max={40} step={1}
        value={value}
        onChange={e => onChange(Number(e.target.value))}
        className="w-full accent-brand-600"
      />
    </div>
  )
}
