import { Link } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import { NotFoundIllustration } from '../components/Illustrations'

export default function NotFound() {
  return (
    <div className="mx-auto max-w-md px-4 py-16 text-center">
      <NotFoundIllustration className="mx-auto w-64 sm:w-72 text-ink-500 dark:text-ink-400" />
      <h1 className="mt-2 text-3xl font-extrabold font-display text-ink-900 dark:text-ink-100">
        Page introuvable
      </h1>
      <p className="mt-2 text-ink-500 dark:text-ink-400">
        La page que vous cherchez n'existe pas ou a été déplacée.
      </p>
      <Link to="/" className="btn-primary mt-6">
        <ArrowLeft size={16} /> Retour à l'accueil
      </Link>
    </div>
  )
}
