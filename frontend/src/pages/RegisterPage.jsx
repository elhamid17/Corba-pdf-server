import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { UserPlus, Mail, User, Lock } from 'lucide-react'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import { useToast } from '../components/Toast'
import { useAuth } from '../hooks/useAuth'

export default function RegisterPage() {
  const [email, setEmail] = useState('')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [loading, setLoading] = useState(false)
  const { register } = useAuth()
  const toast = useToast()
  const navigate = useNavigate()

  async function handleSubmit(e) {
    e?.preventDefault?.()
    if (!email || !username || !password) {
      return toast.error('Tous les champs sont obligatoires.')
    }
    if (password.length < 8) {
      return toast.error('Le mot de passe doit contenir au moins 8 caracteres.')
    }
    if (password !== confirm) {
      return toast.error('Les mots de passe ne correspondent pas.')
    }
    setLoading(true)
    try {
      const user = await register({ email: email.trim(), username: username.trim(), password })
      toast.success(`Compte cree, bienvenue ${user.username} !`)
      navigate('/', { replace: true })
    } catch (e) {
      toast.error(e.message || 'Inscription impossible.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <ToolPage
      icon={UserPlus}
      title="Creer un compte"
      subtitle="Stockage personnel 200 Mo + historique conserve 30 jours."
    >
      <form onSubmit={handleSubmit} className="space-y-5">
        <div>
          <label className="field-label">Email</label>
          <div className="relative">
            <Mail size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-ink-400" />
            <input
              type="email"
              value={email}
              onChange={e => setEmail(e.target.value)}
              className="field pl-9"
              placeholder="vous@exemple.com"
              autoComplete="email"
            />
          </div>
        </div>
        <div>
          <label className="field-label">Nom d'utilisateur</label>
          <div className="relative">
            <User size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-ink-400" />
            <input
              value={username}
              onChange={e => setUsername(e.target.value)}
              className="field pl-9"
              placeholder="jdupont"
              autoComplete="username"
              minLength={3}
              maxLength={32}
            />
          </div>
          <p className="mt-1 text-xs text-ink-500 dark:text-ink-400">
            3 a 32 caracteres : lettres, chiffres, points, tirets ou underscores.
          </p>
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
              placeholder="8 caracteres minimum"
              autoComplete="new-password"
              minLength={8}
            />
          </div>
        </div>
        <div>
          <label className="field-label">Confirmer le mot de passe</label>
          <div className="relative">
            <Lock size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-ink-400" />
            <input
              type="password"
              value={confirm}
              onChange={e => setConfirm(e.target.value)}
              className="field pl-9"
              autoComplete="new-password"
            />
          </div>
        </div>
        <SubmitButton loading={loading} type="submit" loadingText="Creation…" icon={UserPlus}>
          Creer mon compte
        </SubmitButton>
      </form>
      <p className="mt-5 text-sm text-center text-ink-500 dark:text-ink-400">
        Deja inscrit ?{' '}
        <Link to="/login" className="text-brand-600 dark:text-brand-400 font-medium hover:underline">
          Se connecter
        </Link>
      </p>
    </ToolPage>
  )
}
