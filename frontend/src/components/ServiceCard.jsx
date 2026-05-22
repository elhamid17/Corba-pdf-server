import { Link } from 'react-router-dom'
import { ArrowUpRight } from 'lucide-react'

const TONES = {
  brand:   { iconBg: 'from-brand-500/15 to-brand-500/0',     icon: 'text-brand-600 dark:text-brand-400',     ring: 'group-hover:ring-brand-300 dark:group-hover:ring-brand-700' },
  cyan:    { iconBg: 'from-accent-500/15 to-accent-500/0',   icon: 'text-accent-600 dark:text-accent-400',   ring: 'group-hover:ring-accent-400 dark:group-hover:ring-accent-500/60' },
  emerald: { iconBg: 'from-emerald-500/15 to-emerald-500/0', icon: 'text-emerald-600 dark:text-emerald-400', ring: 'group-hover:ring-emerald-300 dark:group-hover:ring-emerald-700' },
  amber:   { iconBg: 'from-amber-500/15 to-amber-500/0',     icon: 'text-amber-600 dark:text-amber-400',     ring: 'group-hover:ring-amber-300 dark:group-hover:ring-amber-700' },
  rose:    { iconBg: 'from-rose-500/15 to-rose-500/0',       icon: 'text-rose-600 dark:text-rose-400',       ring: 'group-hover:ring-rose-300 dark:group-hover:ring-rose-700' },
  violet:  { iconBg: 'from-violet-500/15 to-violet-500/0',   icon: 'text-violet-600 dark:text-violet-400',   ring: 'group-hover:ring-violet-300 dark:group-hover:ring-violet-700' },
}

/**
 * Carte service : hauteur fixe pour eviter les rangees CSS Grid avec
 * espace vide en bas des cartes courtes. Utilise flex-col + h-full pour
 * que toutes les cartes d'une rangee remplissent l'espace identiquement.
 */
export default function ServiceCard({ icon: Icon, title, description, to, tone = 'brand', badge }) {
  const t = TONES[tone] || TONES.brand
  return (
    <Link
      to={to}
      className={`group relative card card-hover h-full flex flex-col p-5 ring-1 ring-ink-200/60 dark:ring-ink-800 transition-all duration-200 hover:shadow-lg ${t.ring}`}
    >
      <div className="flex items-start justify-between mb-3">
        <div className={`grid h-11 w-11 place-items-center rounded-xl bg-gradient-to-br ${t.iconBg} group-hover:scale-105 transition-transform duration-200`}>
          <Icon className={t.icon} size={22} strokeWidth={2} />
        </div>
        <ArrowUpRight className="text-ink-300 dark:text-ink-600 group-hover:text-brand-600 dark:group-hover:text-brand-400 group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-all duration-200" size={18} />
      </div>
      <h3 className="font-display font-bold text-ink-900 dark:text-ink-100 text-base mb-1">
        {title}
        {badge && <span className="ml-2 badge-info text-[10px] py-0">{badge}</span>}
      </h3>
      <p className="text-sm text-ink-500 dark:text-ink-400 leading-relaxed">{description}</p>
    </Link>
  )
}
