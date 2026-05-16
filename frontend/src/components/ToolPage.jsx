import { Link } from 'react-router-dom'
import { ChevronLeft } from 'lucide-react'

/**
 * Layout standardisé pour chaque page outil.
 * Rend un header en gradient, breadcrumb, container + footer cta retour.
 */
export default function ToolPage({ icon: Icon, title, subtitle, children, footer }) {
  return (
    <div className="relative">
      {/* Header */}
      <section className="relative overflow-hidden">
        <div className="absolute inset-0 bg-radial-brand pointer-events-none" />
        <div className="absolute inset-0 bg-grid-faint bg-[size:32px_32px] [mask-image:linear-gradient(180deg,white,transparent_75%)] pointer-events-none" />
        <div className="mx-auto max-w-3xl px-4 sm:px-6 lg:px-8 pt-10 pb-6 relative">
          <Link to="/" className="inline-flex items-center gap-1.5 text-sm font-medium text-ink-500 hover:text-brand-600 transition-colors mb-5">
            <ChevronLeft size={16} /> Retour
          </Link>
          <div className="flex items-start gap-4">
            {Icon && (
              <div className="shrink-0 grid h-14 w-14 place-items-center rounded-2xl bg-gradient-to-br from-brand-600 to-accent-500 text-white shadow-glow">
                <Icon size={26} strokeWidth={2} />
              </div>
            )}
            <div className="min-w-0">
              <h1 className="text-3xl font-extrabold font-display text-ink-900 tracking-tight">{title}</h1>
              {subtitle && <p className="mt-1.5 text-ink-500 leading-relaxed">{subtitle}</p>}
            </div>
          </div>
        </div>
      </section>

      {/* Contenu */}
      <section className="mx-auto max-w-3xl px-4 sm:px-6 lg:px-8 pb-16">
        <div className="card p-6 sm:p-8 animate-fade-in">
          {children}
        </div>
        {footer && <div className="mt-4 text-sm text-ink-500 px-1">{footer}</div>}
      </section>
    </div>
  )
}
