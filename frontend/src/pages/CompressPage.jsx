import { useState } from 'react'
import { Archive } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import { useToast } from '../components/Toast'
import { compressPDF } from '../api/pdfApi'

export default function CompressPage() {
  const [files, setFiles] = useState([])
  const [imageQuality, setImageQuality] = useState(70)
  const [compressImages, setCompressImages] = useState(true)
  const [removeMetadata, setRemoveMetadata] = useState(false)
  const [loading, setLoading] = useState(false)
  const toast = useToast()

  async function handleSubmit() {
    if (!files[0]) return toast.error('Sélectionnez un PDF.')
    setLoading(true)
    try {
      await compressPDF(files[0], { compressImages, imageQuality, removeMetadata })
      toast.success('Compression terminée.')
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <ToolPage
      icon={Archive}
      title="Compression PDF"
      subtitle="Allégez vos PDF en optimisant les images et en supprimant les métadonnées superflues."
    >
      <DropZone onFiles={setFiles} label="Déposez votre PDF à compresser" />

      <div className="mt-6 space-y-5">
        <label className="flex items-start gap-3 cursor-pointer">
          <input type="checkbox" checked={compressImages} onChange={e => setCompressImages(e.target.checked)} className="h-4 w-4 mt-0.5 rounded text-brand-600 focus:ring-brand-500/30" />
          <div>
            <p className="text-sm font-medium text-ink-800">Compresser les images</p>
            <p className="text-xs text-ink-500">Réduit la qualité des images embarquées selon le pourcentage choisi.</p>
          </div>
        </label>

        <div>
          <label className="field-label">Qualité d’image — {imageQuality} %</label>
          <input
            type="range" min={10} max={100} step={5}
            value={imageQuality}
            onChange={e => setImageQuality(Number(e.target.value))}
            disabled={!compressImages}
            className="w-full accent-brand-600 disabled:opacity-40"
          />
          <div className="flex justify-between text-xs text-ink-400 mt-1">
            <span>Forte compression</span>
            <span>Qualité maximale</span>
          </div>
        </div>

        <label className="flex items-start gap-3 cursor-pointer">
          <input type="checkbox" checked={removeMetadata} onChange={e => setRemoveMetadata(e.target.checked)} className="h-4 w-4 mt-0.5 rounded text-brand-600 focus:ring-brand-500/30" />
          <div>
            <p className="text-sm font-medium text-ink-800">Supprimer les métadonnées</p>
            <p className="text-xs text-ink-500">Auteur, titre, dates, logiciel d’origine.</p>
          </div>
        </label>
      </div>

      <div className="mt-6">
        <SubmitButton loading={loading} onClick={handleSubmit} disabled={!files[0]}>
          Compresser
        </SubmitButton>
      </div>
    </ToolPage>
  )
}
