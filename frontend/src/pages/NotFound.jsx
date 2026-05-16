import { Link } from 'react-router-dom'
import { FileQuestion, ArrowLeft } from 'lucide-react'

export default function NotFound() {
  return (
    <div className="mx-auto max-w-md px-4 py-24 text-center">
      <div className="mx-auto mb-6 grid h-20 w-20 place-items-center rounded-3xl bg-gradient-to-br from-brand-500 to-accent-500 text-white shadow-glow">
        <FileQuestion size={36} strokeWidth={1.7} />
      </div>
      <h1 className="text-3xl font-extrabold font-display text-ink-900">Page introuvable</h1>
      <p className="mt-2 text-ink-500">La page que vous cherchez n’existe pas ou a été déplacée.</p>
      <Link to="/" className="btn-primary mt-6">
        <ArrowLeft size={16} /> Retour à l’accueil
      </Link>
    </div>
  )
}
