import { useState } from 'react'
import { Image as ImageIcon } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import { useToast } from '../components/Toast'
import { convertToImages } from '../api/pdfApi'

const FORMATS = ['PNG', 'JPEG', 'TIFF']
const DPIS    = [72, 150, 300]

export default function ConvertPage() {
  const [files, setFiles] = useState([])
  const [format, setFormat] = useState('PNG')
  const [dpi, setDpi] = useState(150)
  const [loading, setLoading] = useState(false)
  const toast = useToast()

  async function handleSubmit() {
    if (!files[0]) return toast.error('Sélectionnez un PDF.')
    setLoading(true)
    try {
      await convertToImages(files[0], format, dpi)
      toast.success('Conversion terminée — archive ZIP téléchargée.')
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <ToolPage
      icon={ImageIcon}
      title="PDF vers images"
      subtitle="Convertissez chaque page en image, livrées dans une archive ZIP."
    >
      <DropZone onFiles={setFiles} label="Déposez votre PDF à convertir" />

      <div className="mt-6 grid sm:grid-cols-2 gap-5">
        <div>
          <label className="field-label">Format</label>
          <div className="grid grid-cols-3 gap-2">
            {FORMATS.map(f => (
              <button
                key={f}
                type="button"
                onClick={() => setFormat(f)}
                className={`rounded-lg border py-2 text-sm font-semibold transition-all ${
                  format === f
                    ? 'border-brand-500 bg-brand-50 text-brand-700 dark:bg-brand-950/50 dark:text-brand-300'
                    : 'border-ink-200 hover:border-ink-300 text-ink-600 dark:border-ink-700 dark:hover:border-ink-600 dark:text-ink-300'
                }`}
              >{f}</button>
            ))}
          </div>
        </div>
        <div>
          <label className="field-label">Résolution (DPI)</label>
          <div className="grid grid-cols-3 gap-2">
            {DPIS.map(d => (
              <button
                key={d}
                type="button"
                onClick={() => setDpi(d)}
                className={`rounded-lg border py-2 text-sm font-semibold transition-all ${
                  dpi === d
                    ? 'border-brand-500 bg-brand-50 text-brand-700 dark:bg-brand-950/50 dark:text-brand-300'
                    : 'border-ink-200 hover:border-ink-300 text-ink-600 dark:border-ink-700 dark:hover:border-ink-600 dark:text-ink-300'
                }`}
              >{d}</button>
            ))}
          </div>
        </div>
      </div>

      <div className="mt-6">
        <SubmitButton loading={loading} onClick={handleSubmit} disabled={!files[0]}>
          Convertir en images
        </SubmitButton>
      </div>
    </ToolPage>
  )
}
