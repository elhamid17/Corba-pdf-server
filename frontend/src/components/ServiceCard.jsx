import { Link } from 'react-router-dom'
import { ArrowUpRight } from 'lucide-react'

const TONES = {
  brand:   { iconBg: 'from-brand-500/15 to-brand-500/0',   icon: 'text-brand-600',   ring: 'group-hover:ring-brand-200' },
  cyan:    { iconBg: 'from-accent-500/15 to-accent-500/0', icon: 'text-accent-600',  ring: 'group-hover:ring-accent-400/40' },
  emerald: { iconBg: 'from-emerald-500/15 to-emerald-500/0', icon: 'text-emerald-600', ring: 'group-hover:ring-emerald-200' },
  amber:   { iconBg: 'from-amber-500/15 to-amber-500/0',   icon: 'text-amber-600',   ring: 'group-hover:ring-amber-200' },
  rose:    { iconBg: 'from-rose-500/15 to-rose-500/0',     icon: 'text-rose-600',    ring: 'group-hover:ring-rose-200' },
  violet:  { iconBg: 'from-violet-500/15 to-violet-500/0', icon: 'text-violet-600',  ring: 'group-hover:ring-violet-200' },
}

export default function ServiceCard({ icon: Icon, title, description, to, tone = 'brand', badge }) {
  const t = TONES[tone] || TONES.brand
  return (
    <Link
      to={to}
      className={`group relative card card-hover p-5 ring-1 ring-transparent transition-all ${t.ring}`}
    >
      <div className="flex items-start justify-between mb-4">
        <div className={`grid h-11 w-11 place-items-center rounded-xl bg-gradient-to-br ${t.iconBg}`}>
          <Icon className={t.icon} size={22} strokeWidth={2} />
        </div>
        <ArrowUpRight className="text-ink-300 group-hover:text-brand-600 group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-all" size={18} />
      </div>
      <h3 className="font-display font-bold text-ink-900 text-base mb-1">
        {title}
        {badge && <span className="ml-2 badge-info text-[10px] py-0">{badge}</span>}
      </h3>
      <p className="text-sm text-ink-500 leading-relaxed">{description}</p>
    </Link>
  )
}
