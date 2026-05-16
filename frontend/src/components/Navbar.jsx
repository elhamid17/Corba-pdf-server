import { Link, NavLink } from 'react-router-dom'
import { useEffect, useState } from 'react'
import {
  Layers, Scissors, FileMinus, FileSearch, Archive, RotateCw,
  Droplets, Lock, Image as ImageIcon, FileText, ScanText, PenTool,
  Info, Plus, Github, Wand2,
} from 'lucide-react'
import { ping } from '../api/pdfApi'

export const NAV_LINKS = [
  { to: '/merge',         label: 'Fusion',       icon: Layers     },
  { to: '/split',         label: 'Découpage',    icon: Scissors   },
  { to: '/extract-pages', label: 'Extraction',   icon: FileSearch },
  { to: '/delete-pages',  label: 'Suppression',  icon: FileMinus  },
  { to: '/compress',      label: 'Compression',  icon: Archive    },
  { to: '/rotate',        label: 'Rotation',     icon: RotateCw   },
  { to: '/watermark',     label: 'Filigrane',    icon: Droplets   },
  { to: '/protect',       label: 'Protection',   icon: Lock       },
  { to: '/sign',          label: 'Signature',    icon: PenTool    },
  { to: '/convert',       label: 'Images',       icon: ImageIcon  },
  { to: '/extract-text',  label: 'Texte',        icon: FileText   },
  { to: '/ocr',           label: 'OCR',          icon: ScanText   },
  { to: '/metadata',      label: 'Métadonnées',  icon: Info       },
  { to: '/create',        label: 'Création',     icon: Plus       },
]

export default function Navbar() {
  const [status, setStatus] = useState('checking')
  const [scrolled, setScrolled] = useState(false)

  useEffect(() => {
    let alive = true
    ping()
      .then(d => alive && setStatus(d.status === 'OK' ? 'online' : 'degraded'))
      .catch(() => alive && setStatus('offline'))
    const onScroll = () => setScrolled(window.scrollY > 8)
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => { alive = false; window.removeEventListener('scroll', onScroll) }
  }, [])

  return (
    <header className={`sticky top-0 z-40 transition-all ${scrolled ? 'backdrop-blur bg-white/85 border-b border-ink-200/70 shadow-card' : 'bg-transparent'}`}>
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex h-16 items-center justify-between gap-4">
          {/* Logo */}
          <Link to="/" className="flex items-center gap-2.5 shrink-0 group">
            <span className="grid place-items-center h-9 w-9 rounded-xl bg-gradient-to-br from-brand-600 to-accent-500 text-white shadow-glow group-hover:scale-105 transition-transform">
              <Wand2 size={18} strokeWidth={2.5} />
            </span>
            <span className="font-display font-extrabold text-lg text-ink-900 tracking-tight">
              CORBA <span className="text-brand-600">PDF</span>
            </span>
            <span className="hidden md:inline-flex badge-info -ml-1">Suite</span>
          </Link>

          {/* Liens — uniquement les principaux dans la topbar */}
          <nav className="hidden lg:flex items-center gap-1">
            {NAV_LINKS.slice(0, 6).map(l => (
              <NavLink
                key={l.to}
                to={l.to}
                className={({ isActive }) =>
                  `px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
                    isActive ? 'bg-brand-50 text-brand-700' : 'text-ink-600 hover:bg-ink-100 hover:text-ink-900'
                  }`
                }
              >
                {l.label}
              </NavLink>
            ))}
          </nav>

          {/* Statut serveur */}
          <div className="flex items-center gap-3 shrink-0">
            <StatusPill status={status} />
            <a
              href="https://github.com"
              target="_blank" rel="noreferrer"
              className="hidden sm:inline-flex btn-ghost h-9 w-9 p-0 rounded-lg"
              aria-label="GitHub"
            >
              <Github size={18} />
            </a>
          </div>
        </div>

        {/* Liens secondaires — defile en mobile */}
        <div className="lg:hidden flex items-center gap-1 overflow-x-auto -mx-4 px-4 pb-3 scrollbar-hide">
          {NAV_LINKS.map(l => (
            <NavLink
              key={l.to}
              to={l.to}
              className={({ isActive }) =>
                `whitespace-nowrap px-3 py-1.5 rounded-lg text-sm font-medium transition-colors ${
                  isActive ? 'bg-brand-50 text-brand-700' : 'text-ink-600 hover:bg-ink-100'
                }`
              }
            >
              {l.label}
            </NavLink>
          ))}
        </div>
      </div>
    </header>
  )
}

function StatusPill({ status }) {
  const cfg = {
    online:   { dot: 'bg-emerald-500', text: 'Opérationnel', ring: 'ring-emerald-200' },
    degraded: { dot: 'bg-amber-500',   text: 'Dégradé',      ring: 'ring-amber-200'   },
    offline:  { dot: 'bg-rose-500',    text: 'Hors ligne',   ring: 'ring-rose-200'    },
    checking: { dot: 'bg-ink-400',     text: 'Vérification', ring: 'ring-ink-200'     },
  }[status] || { dot: 'bg-ink-400', text: '—', ring: 'ring-ink-200' }

  return (
    <span className={`inline-flex items-center gap-2 rounded-full bg-white px-2.5 sm:px-3 py-1 text-xs font-semibold text-ink-700 ring-1 ${cfg.ring} shadow-card`}>
      <span className={`relative inline-flex h-2 w-2 ${cfg.dot} rounded-full`}>
        {status === 'online' && <span className={`absolute inset-0 ${cfg.dot} rounded-full animate-pulse-dot`} />}
      </span>
      <span className="hidden sm:inline">{cfg.text}</span>
    </span>
  )
}
