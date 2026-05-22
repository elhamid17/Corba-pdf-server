import { useState } from 'react'
import { Highlighter, AlertTriangle } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import OutputFilenameField from '../components/OutputFilenameField'
import { useToast } from '../components/Toast'
import { useProgress } from '../hooks/useProgress'
import { redactPdf } from '../api/pdfApi'

export default function RedactPage() {
  const [files, setFiles] = useState([])
  const [termsText, setTermsText] = useState('')
  const [outputName, setOutputName] = useState('')
  const [loading, setLoading] = useState(false)
  const toast = useToast()
  const { progress, onProgress, reset } = useProgress()

  async function handleSubmit() {
    if (!files[0]) return toast.error('Sélectionnez un PDF.')
    const terms = termsText.split('\n')
      .map(s => s.trim())
      .filter(s => s.length > 0)
    if (terms.length === 0) return toast.error('Saisissez au moins un terme à caviarder.')
    setLoading(true)
    try {
      await redactPdf(files[0], terms, outputName, onProgress)
      toast.success(`Caviardage appliqué (${terms.length} terme(s)).`)
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
      reset()
    }
  }

  return (
    <ToolPage
      icon={Highlighter}
      title="Caviardage textuel"
      subtitle="Cherche les mots ou expressions indiqués et les recouvre d'un rectangle noir, page par page."
    >
      <DropZone onFiles={setFiles} label="Déposez votre PDF" />

      <div className="mt-6">
        <label className="field-label">Termes à caviarder — un par ligne</label>
        <textarea
          value={termsText}
          onChange={e => setTermsText(e.target.value)}
          className="field min-h-32 font-mono text-sm"
          placeholder={`Jean Dupont\n06 12 34 56 78\nconfidentiel`}
        />
        <p className="mt-1.5 text-xs text-ink-400 dark:text-ink-500">
          La recherche est insensible à la casse. Les correspondances exactes sont recouvertes.
        </p>
      </div>

      <div className="mt-4 flex items-start gap-2.5 rounded-xl border border-amber-200 dark:border-amber-800 bg-amber-50/60 dark:bg-amber-950/30 p-3 text-sm">
        <AlertTriangle size={16} className="shrink-0 mt-0.5 text-amber-600 dark:text-amber-400" />
        <div className="text-ink-700 dark:text-ink-200">
          <strong>Limite :</strong> ce caviardage est <em>visuel</em>. Le texte d'origine
          reste dans le fichier (extractible par copier-coller). Pour une suppression
          irréversible, recrée le PDF à partir d'une image (PDF → Images → PDF).
        </div>
      </div>

      <div className="mt-6">
        <OutputFilenameField value={outputName} onChange={setOutputName} placeholder="document-caviarde" extension=".pdf" />
      </div>

      <div className="mt-6">
        <SubmitButton loading={loading} progress={progress} onClick={handleSubmit} disabled={!files[0] || !termsText.trim()}>
          Caviarder
        </SubmitButton>
      </div>
    </ToolPage>
  )
}
