import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { LogIn, User, Lock } from 'lucide-react'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import { useToast } from '../components/Toast'
import { useAuth } from '../hooks/useAuth'

export default function LoginPage() {
  const [identifier, setIdentifier] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const { login } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()
  const location = useLocation()
  const from = location.state?.from?.pathname || '/'

  async function handleSubmit(e) {
    e?.preventDefault?.()
    if (!identifier.trim() || !password) {
      return toast.error('Renseignez votre identifiant et votre mot de passe.')
    }
    setLoading(true)
    try {
      const user = await login({ identifier: identifier.trim(), password })
      toast.success(`Bienvenue ${user.username} !`)
      navigate(from, { replace: true })
    } catch (e) {
      toast.error(e.message || 'Identifiants incorrects.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <ToolPage icon={LogIn} title="Connexion" subtitle="Acces a votre historique et a votre stockage personnel.">
      <form onSubmit={handleSubmit} className="space-y-5">
        <div>
          <label className="field-label">Email ou nom d'utilisateur</label>
          <div className="relative">
            <User size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-ink-400" />
            <input
              value={identifier}
              onChange={e => setIdentifier(e.target.value)}
              className="field pl-9"
              placeholder="vous@exemple.com"
              autoComplete="username"
              autoFocus
            />
          </div>
        </div>
        <div>
          <label className="field-label">Mot de passe</label>
          <div className="relative">
            <Lock size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-ink-400" />
            <input
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              className="field pl-9"
              placeholder="••••••••"
              autoComplete="current-password"
            />
          </div>
        </div>
        <SubmitButton loading={loading} type="submit" loadingText="Connexion…" icon={LogIn}>
          Se connecter
        </SubmitButton>
      </form>
      <p className="mt-5 text-sm text-center text-ink-500 dark:text-ink-400">
        Pas encore de compte ?{' '}
        <Link to="/register" className="text-brand-600 dark:text-brand-400 font-medium hover:underline">
          Creer un compte
        </Link>
      </p>
    </ToolPage>
  )
}
