import { useState } from 'react'
import { Archive } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import OutputFilenameField from '../components/OutputFilenameField'
import { useToast } from '../components/Toast'
import { useProgress } from '../hooks/useProgress'
import { compressPDF, toPdfA } from '../api/pdfApi'
import { postFormWithProgress } from '../api/client'
import { PDF } from '../api/routes'

export default function CompressPage() {
  const [files, setFiles] = useState([])
  const [imageQuality, setImageQuality] = useState(70)
  const [compressImages, setCompressImages] = useState(true)
  const [removeMetadata, setRemoveMetadata] = useState(false)
  const [pdfA, setPdfA] = useState(false)
  const [outputName, setOutputName] = useState('')
  const [loading, setLoading] = useState(false)
  const toast = useToast()
  const { progress, onProgress, reset } = useProgress()

  async function handleSubmit() {
    if (!files[0]) return toast.error('Sélectionnez un PDF.')
    setLoading(true)
    try {
      if (pdfA) {
        // Pipeline en 2 etapes : compresse cote serveur, puis marque PDF/A
        // sur le blob resultant. La 1ere phase prend 70% de la progress.
        const compressForm = new FormData()
        compressForm.append('file', files[0])
        compressForm.append('compressImages', compressImages)
        compressForm.append('imageQuality',   imageQuality)
        compressForm.append('removeMetadata', removeMetadata)
        const compressed = await postFormWithProgress(`${PDF}/compress`, compressForm, {
          onProgress: (p) => {
            if (p.phase === 'done') onProgress({ phase: 'processing', percent: null })
            else onProgress(p)
          },
        })
        // 2e etape : PDF/A
        const pdfaForm = new FormData()
        pdfaForm.append('file', new File([compressed], 'compressed.pdf', { type: 'application/pdf' }))
        await toPdfA(pdfaForm.get('file'), outputName, onProgress)
        toast.success('Compression + marquage PDF/A appliques.')
      } else {
        await compressPDF(files[0], { compressImages, imageQuality, removeMetadata }, outputName, onProgress)
        toast.success('Compression terminée.')
      }
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
      reset()
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
            <p className="text-sm font-medium text-ink-800 dark:text-ink-100">Compresser les images</p>
            <p className="text-xs text-ink-500 dark:text-ink-400">Réduit la qualité des images embarquées selon le pourcentage choisi.</p>
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
          <div className="flex justify-between text-xs text-ink-400 dark:text-ink-500 mt-1">
            <span>Forte compression</span>
            <span>Qualité maximale</span>
          </div>
        </div>

        <label className="flex items-start gap-3 cursor-pointer">
          <input type="checkbox" checked={removeMetadata} onChange={e => setRemoveMetadata(e.target.checked)} className="h-4 w-4 mt-0.5 rounded text-brand-600 focus:ring-brand-500/30" />
          <div>
            <p className="text-sm font-medium text-ink-800 dark:text-ink-100">Supprimer les métadonnées</p>
            <p className="text-xs text-ink-500 dark:text-ink-400">Auteur, titre, dates, logiciel d’origine.</p>
          </div>
        </label>

        <label className="flex items-start gap-3 cursor-pointer">
          <input type="checkbox" checked={pdfA} onChange={e => setPdfA(e.target.checked)} className="h-4 w-4 mt-0.5 rounded text-brand-600 focus:ring-brand-500/30" />
          <div>
            <p className="text-sm font-medium text-ink-800 dark:text-ink-100">Conformité PDF/A pour archivage long terme</p>
            <p className="text-xs text-ink-500 dark:text-ink-400">Ajoute le marqueur PDF/A-1B + métadonnées XMP. Idéal pour les documents officiels.</p>
          </div>
        </label>

        <OutputFilenameField value={outputName} onChange={setOutputName} placeholder="document-compresse" extension=".pdf" />
      </div>

      <div className="mt-6">
        <SubmitButton loading={loading} progress={progress} onClick={handleSubmit} disabled={!files[0]}>
          Compresser
        </SubmitButton>
      </div>
    </ToolPage>
  )
}
