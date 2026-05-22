import { useState } from 'react'
import { Info } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import { useToast } from '../components/Toast'
import { useProgress } from '../hooks/useProgress'
import { getMetadata } from '../api/pdfApi'

const LABELS = {
  title:        'Titre',
  author:       'Auteur',
  subject:      'Sujet',
  keywords:     'Mots-clés',
  creator:      'Créateur',
  producer:     'Producteur',
  creationDate: 'Date de création',
  pageCount:    'Nombre de pages',
}

export default function MetadataPage() {
  const [files, setFiles] = useState([])
  const [meta,  setMeta]  = useState(null)
  const [loading, setLoading] = useState(false)
  const toast = useToast()
  const { progress, onProgress, reset } = useProgress()

  async function handleSubmit() {
    if (!files[0]) return toast.error('Sélectionnez un PDF.')
    setLoading(true)
    setMeta(null)
    try {
      const m = await getMetadata(files[0], onProgress)
      setMeta(m)
      toast.success('Métadonnées chargées.')
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
      reset()
    }
  }

  return (
    <ToolPage
      icon={Info}
      title="Inspection des métadonnées"
      subtitle="Affichez les informations XMP intégrées dans votre document PDF."
    >
      <DropZone onFiles={setFiles} label="Déposez votre PDF" />
      <div className="mt-6">
        <SubmitButton loading={loading} progress={progress} onClick={handleSubmit} disabled={!files[0]}>
          Inspecter
        </SubmitButton>
      </div>

      {meta && (
        <dl className="mt-6 grid sm:grid-cols-2 gap-x-6 gap-y-3 rounded-xl bg-ink-50/60 dark:bg-ink-800/40 p-5 ring-1 ring-ink-200/70 dark:ring-ink-700/70">
          {Object.entries(LABELS).map(([k, label]) => (
            <div key={k} className="min-w-0">
              <dt className="text-xs font-semibold uppercase tracking-wider text-ink-500 dark:text-ink-400">{label}</dt>
              <dd className="mt-0.5 text-sm font-medium text-ink-900 dark:text-ink-100 truncate">
                {meta[k] !== undefined && meta[k] !== null && meta[k] !== ''
                  ? String(meta[k])
                  : <span className="text-ink-400 dark:text-ink-500 italic">—</span>}
              </dd>
            </div>
          ))}
        </dl>
      )}
    </ToolPage>
  )
}
