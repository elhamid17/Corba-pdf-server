import { Link, useLocation } from 'react-router-dom'
import { ChevronLeft, Star } from 'lucide-react'
import WorkflowSuggestions from './WorkflowSuggestions'
import { toggleFavorite, useIsFavorite, useRecordVisit } from '../hooks/useToolHistory'

/**
 * Layout standardisé pour chaque page outil.
 * Rend un header en gradient, breadcrumb, container + footer cta retour.
 */
export default function ToolPage({ icon: Icon, title, subtitle, children, footer }) {
  const { pathname } = useLocation()
  useRecordVisit(pathname)
  const fav = useIsFavorite(pathname)
  return (
    <div className="relative">
      {/* Header */}
      <section className="relative overflow-hidden">
        <div className="absolute inset-0 bg-radial-brand pointer-events-none" />
        <div className="absolute inset-0 bg-grid-faint bg-[size:32px_32px] [mask-image:linear-gradient(180deg,white,transparent_75%)] pointer-events-none" />
        <div className="mx-auto max-w-3xl px-4 sm:px-6 lg:px-8 pt-10 pb-6 relative">
          <div className="flex items-center justify-between mb-5">
            <Link to="/" className="inline-flex items-center gap-1.5 text-sm font-medium text-ink-500 hover:text-brand-600 dark:text-ink-400 dark:hover:text-brand-400 transition-colors">
              <ChevronLeft size={16} /> Retour
            </Link>
            <button
              type="button"
              onClick={() => toggleFavorite(pathname)}
              className={`inline-flex items-center gap-1.5 h-8 px-3 rounded-full text-xs font-semibold transition-all ${
                fav
                  ? 'bg-amber-100 text-amber-800 ring-1 ring-amber-300 dark:bg-amber-950/40 dark:text-amber-300 dark:ring-amber-700/50'
                  : 'bg-white dark:bg-ink-900 text-ink-500 ring-1 ring-ink-200 dark:ring-ink-700 hover:text-amber-600 hover:ring-amber-300 dark:hover:text-amber-400'
              }`}
              aria-label={fav ? 'Retirer des favoris' : 'Ajouter aux favoris'}
              title={fav ? 'Retirer des favoris' : 'Ajouter aux favoris'}
            >
              <Star size={14} className={fav ? 'fill-amber-500 text-amber-500' : ''} strokeWidth={2} />
              {fav ? 'Favori' : 'Ajouter aux favoris'}
            </button>
          </div>
          <div className="flex items-start gap-4">
            {Icon && (
              <div className="shrink-0 grid h-14 w-14 place-items-center rounded-2xl bg-gradient-to-br from-brand-600 to-accent-500 text-white shadow-glow">
                <Icon size={26} strokeWidth={2} />
              </div>
            )}
            <div className="min-w-0">
              <h1 className="text-3xl font-extrabold font-display text-ink-900 dark:text-ink-100 tracking-tight">{title}</h1>
              {subtitle && <p className="mt-1.5 text-ink-500 dark:text-ink-400 leading-relaxed">{subtitle}</p>}
            </div>
          </div>
        </div>
      </section>

      {/* Contenu */}
      <section className="mx-auto max-w-3xl px-4 sm:px-6 lg:px-8 pb-16">
        <div className="card p-6 sm:p-8 animate-fade-in">
          {children}
        </div>
        {footer && <div className="mt-4 text-sm text-ink-500 dark:text-ink-400 px-1">{footer}</div>}
        <WorkflowSuggestions />
      </section>
    </div>
  )
}
