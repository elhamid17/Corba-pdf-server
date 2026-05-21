import { useState } from 'react'
import { UserX, Info } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import OutputFilenameField from '../components/OutputFilenameField'
import { useToast } from '../components/Toast'
import { anonymizePdf } from '../api/pdfApi'

export default function AnonymizePage() {
  const [files, setFiles] = useState([])
  const [outputName, setOutputName] = useState('')
  const [loading, setLoading] = useState(false)
  const toast = useToast()

  async function handleSubmit() {
    if (!files[0]) return toast.error('Sélectionnez un PDF.')
    setLoading(true)
    try {
      await anonymizePdf(files[0], outputName)
      toast.success('PDF anonymisé. Métadonnées retirées.')
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <ToolPage
      icon={UserX}
      title="Anonymisation"
      subtitle="Retire en un clic toutes les métadonnées identifiantes : auteur, titre, dates, logiciel d'origine, mots-clés…"
    >
      <DropZone onFiles={setFiles} label="Déposez votre PDF" />

      <div className="mt-4 flex items-start gap-2.5 rounded-xl border border-brand-200 dark:border-brand-800 bg-brand-50/60 dark:bg-brand-950/30 p-3 text-sm">
        <Info size={16} className="shrink-0 mt-0.5 text-brand-600 dark:text-brand-400" />
        <div className="text-ink-700 dark:text-ink-200">
          Cette opération supprime <strong>tous les champs Info</strong> et les
          <strong> métadonnées XMP</strong> embarquées. Le contenu visible n'est pas modifié.
        </div>
      </div>

      <div className="mt-6">
        <OutputFilenameField value={outputName} onChange={setOutputName} placeholder="document-anonyme" extension=".pdf" />
      </div>

      <div className="mt-6">
        <SubmitButton loading={loading} onClick={handleSubmit} disabled={!files[0]}>
          Anonymiser
        </SubmitButton>
      </div>
    </ToolPage>
  )
}
