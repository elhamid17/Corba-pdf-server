import { useState } from 'react'
import { Unlock, Eye, EyeOff } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import OutputFilenameField from '../components/OutputFilenameField'
import { useToast } from '../components/Toast'
import { useProgress } from '../hooks/useProgress'
import { unlockPdf } from '../api/pdfApi'

export default function UnlockPage() {
  const [files, setFiles] = useState([])
  const [password, setPassword] = useState('')
  const [show, setShow] = useState(false)
  const [outputName, setOutputName] = useState('')
  const [loading, setLoading] = useState(false)
  const toast = useToast()
  const { progress, onProgress, reset } = useProgress()

  async function handleSubmit() {
    if (!files[0]) return toast.error('Sélectionnez un PDF.')
    setLoading(true)
    try {
      await unlockPdf(files[0], password, outputName, onProgress)
      toast.success('Protection retirée.')
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
      reset()
    }
  }

  return (
    <ToolPage
      icon={Unlock}
      title="Déverrouillage"
      subtitle="Retire la protection par mot de passe d'un PDF (nécessite de connaître le mot de passe propriétaire)."
    >
      <DropZone onFiles={setFiles} label="Déposez le PDF protégé" />

      <div className="mt-6">
        <label className="field-label">Mot de passe du PDF</label>
        <div className="relative">
          <input
            type={show ? 'text' : 'password'}
            value={password}
            onChange={e => setPassword(e.target.value)}
            className="field pr-12"
            placeholder="Mot de passe propriétaire"
            autoComplete="off"
          />
          <button type="button" onClick={() => setShow(s => !s)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-ink-400 hover:text-ink-700 dark:text-ink-500 dark:hover:text-ink-200 transition-colors"
                  aria-label={show ? 'Masquer' : 'Afficher'}>
            {show ? <EyeOff size={16} /> : <Eye size={16} />}
          </button>
        </div>
        <p className="mt-1 text-xs text-ink-400">Laissez vide si le PDF n'utilise pas de mot de passe propriétaire.</p>
      </div>

      <div className="mt-6">
        <OutputFilenameField value={outputName} onChange={setOutputName} placeholder="document-libre" extension=".pdf" />
      </div>

      <div className="mt-6">
        <SubmitButton loading={loading} progress={progress} onClick={handleSubmit} disabled={!files[0]}>
          Déverrouiller
        </SubmitButton>
      </div>
    </ToolPage>
  )
}
