import { useState } from 'react'
import { Lock, Eye, EyeOff } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import { useToast } from '../components/Toast'
import { protectPDF } from '../api/pdfApi'

export default function ProtectPage() {
  const [files, setFiles] = useState([])
  const [password, setPassword] = useState('')
  const [show, setShow] = useState(false)
  const [loading, setLoading] = useState(false)
  const toast = useToast()

  async function handleSubmit() {
    if (!files[0]) return toast.error('Sélectionnez un PDF.')
    if (!password)  return toast.error('Saisissez un mot de passe.')
    if (password.length < 4) return toast.error('Le mot de passe doit faire au moins 4 caractères.')
    setLoading(true)
    try {
      await protectPDF(files[0], password)
      toast.success('PDF protégé. Téléchargement lancé.')
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <ToolPage
      icon={Lock}
      title="Protection par mot de passe"
      subtitle="Chiffrez votre PDF avec un mot de passe utilisateur (lecture)."
    >
      <DropZone onFiles={setFiles} label="Déposez votre PDF à protéger" />

      <div className="mt-6">
        <label className="field-label">Mot de passe</label>
        <div className="relative">
          <input
            type={show ? 'text' : 'password'}
            value={password}
            onChange={e => setPassword(e.target.value)}
            className="field pr-12"
            placeholder="Minimum 4 caractères"
            autoComplete="new-password"
          />
          <button
            type="button"
            onClick={() => setShow(s => !s)}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-ink-400 hover:text-ink-700 transition-colors"
            aria-label={show ? 'Masquer' : 'Afficher'}
          >
            {show ? <EyeOff size={16} /> : <Eye size={16} />}
          </button>
        </div>
      </div>

      <div className="mt-6">
        <SubmitButton loading={loading} onClick={handleSubmit} disabled={!files[0] || !password}>
          Protéger le PDF
        </SubmitButton>
      </div>
    </ToolPage>
  )
}
