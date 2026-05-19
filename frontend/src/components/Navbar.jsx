import { Link, NavLink } from 'react-router-dom'
import { useEffect, useState } from 'react'
import {
  Layers, Scissors, FileMinus, FileSearch, Archive, RotateCw,
  Droplets, Lock, Image as ImageIcon, FileText, ScanText, PenTool,
  Info, Plus, Github, Wand2, Menu, X, FileType2, Sheet, Images,
} from 'lucide-react'
import { ping } from '../api/pdfApi'

/**
 * Services organisés par catégorie — partagés entre Navbar et Home.
 */
export const SERVICE_GROUPS = [
  {
    title: 'Organisation',
    items: [
      { to: '/merge',         label: 'Fusion',         icon: Layers     },
      { to: '/split',         label: 'Découpage',      icon: Scissors   },
      { to: '/extract-pages', label: 'Extraction',     icon: FileSearch },
      { to: '/delete-pages',  label: 'Suppression',    icon: FileMinus  },
      { to: '/rotate',        label: 'Rotation',       icon: RotateCw   },
    ],
  },
  {
    title: 'Conversions',
    items: [
      { to: '/pdf-to-word',   label: 'PDF → Word',     icon: FileType2  },
      { to: '/pdf-to-excel',  label: 'PDF → Excel',    icon: Sheet      },
      { to: '/word-to-pdf',   label: 'Word → PDF',     icon: FileText   },
      { to: '/images-to-pdf', label: 'Images → PDF',   icon: Images     },
      { to: '/convert',       label: 'PDF → Images',   icon: ImageIcon  },
    ],
  },
  {
    title: 'Édition',
    items: [
      { to: '/watermark',     label: 'Filigrane',      icon: Droplets   },
      { to: '/compress',      label: 'Compression',    icon: Archive    },
      { to: '/metadata',      label: 'Métadonnées',    icon: Info       },
    ],
  },
  {
    title: 'Sécurité',
    items: [
      { to: '/protect',       label: 'Protection',     icon: Lock       },
      { to: '/sign',          label: 'Signature',      icon: PenTool    },
    ],
  },
  {
    title: 'Texte & OCR',
    items: [
      { to: '/extract-text',  label: 'Extraction texte', icon: FileText },
      { to: '/ocr',           label: 'OCR',              icon: ScanText },
      { to: '/create',        label: 'Création PDF',     icon: Plus     },
    ],
  },
]

/** Liste plate de tous les services — utile pour mobile et compteurs. */
export const ALL_SERVICES = SERVICE_GROUPS.flatMap(g => g.items)

export default function Navbar() {
  const [status, setStatus] = useState('checking')
  const [scrolled, setScrolled] = useState(false)
  const [open, setOpen] = useState(false)

  useEffect(() => {
    let alive = true
    ping()
      .then(d => alive && setStatus(d.status === 'OK' ? 'online' : 'degraded'))
      .catch(() => alive && setStatus('offline'))
    const onScroll = () => setScrolled(window.scrollY > 8)
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => { alive = false; window.removeEventListener('scroll', onScroll) }
  }, [])

  // Ferme le drawer avec Échap
  useEffect(() => {
    if (!open) return
    const onKey = e => { if (e.key === 'Escape') setOpen(false) }
    window.addEventListener('keydown', onKey)
    document.body.style.overflow = 'hidden'
    return () => {
      window.removeEventListener('keydown', onKey)
      document.body.style.overflow = ''
    }
  }, [open])

  return (
    <>
      <header className={`sticky top-0 z-40 transition-all ${scrolled ? 'backdrop-blur bg-white/85 border-b border-ink-200/70 shadow-card' : 'bg-transparent'}`}>
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="flex h-16 items-center justify-between gap-3">
            {/* Hamburger + Logo */}
            <div className="flex items-center gap-2 min-w-0">
              <button
                type="button"
                onClick={() => setOpen(true)}
                aria-label="Ouvrir le menu des services"
                className="grid place-items-center h-10 w-10 rounded-lg text-ink-700 hover:bg-ink-100 transition-colors"
              >
                <Menu size={22} strokeWidth={2.2} />
              </button>

              <Link to="/" className="flex items-center gap-2.5 shrink-0 group">
                <span className="grid place-items-center h-9 w-9 rounded-xl bg-gradient-to-br from-brand-600 to-accent-500 text-white shadow-glow group-hover:scale-105 transition-transform">
                  <Wand2 size={18} strokeWidth={2.5} />
                </span>
                <span className="font-display font-extrabold text-lg text-ink-900 tracking-tight whitespace-nowrap">
                  CORBA <span className="text-brand-600">PDF</span>
                </span>
                <span className="hidden md:inline-flex badge-info -ml-1">Suite</span>
              </Link>
            </div>

            {/* Liens — uniquement les principaux dans la topbar (desktop) */}
            <nav className="hidden lg:flex items-center gap-1">
              {ALL_SERVICES.slice(0, 5).map(l => (
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
        </div>
      </header>

      {/* Drawer latéral */}
      <DrawerMenu open={open} onClose={() => setOpen(false)} />
    </>
  )
}

function DrawerMenu({ open, onClose }) {
  return (
    <>
      {/* Backdrop */}
      <div
        onClick={onClose}
        className={`fixed inset-0 z-50 bg-ink-900/50 backdrop-blur-sm transition-opacity ${
          open ? 'opacity-100' : 'opacity-0 pointer-events-none'
        }`}
        aria-hidden="true"
      />

      {/* Panel */}
      <aside
        className={`fixed top-0 left-0 z-50 h-full w-[88%] max-w-sm bg-white shadow-2xl flex flex-col transition-transform duration-300 ${
          open ? 'translate-x-0' : '-translate-x-full'
        }`}
        role="dialog"
        aria-label="Menu des services"
      >
        <div className="flex items-center justify-between px-5 py-4 border-b border-ink-100">
          <div className="flex items-center gap-2.5">
            <span className="grid place-items-center h-9 w-9 rounded-xl bg-gradient-to-br from-brand-600 to-accent-500 text-white shadow-glow">
              <Wand2 size={18} strokeWidth={2.5} />
            </span>
            <div>
              <p className="font-display font-extrabold text-base text-ink-900 leading-none">
                Tous les services
              </p>
              <p className="text-xs text-ink-500 mt-0.5">
                {ALL_SERVICES.length} outils PDF
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Fermer"
            className="grid place-items-center h-9 w-9 rounded-lg text-ink-500 hover:text-ink-900 hover:bg-ink-100 transition-colors"
          >
            <X size={20} />
          </button>
        </div>

        <nav className="flex-1 overflow-y-auto px-3 py-4 space-y-5">
          {SERVICE_GROUPS.map(group => (
            <div key={group.title}>
              <p className="px-3 mb-1.5 text-[11px] font-semibold uppercase tracking-wider text-ink-400">
                {group.title}
              </p>
              <ul className="space-y-0.5">
                {group.items.map(item => {
                  const Icon = item.icon
                  return (
                    <li key={item.to}>
                      <NavLink
                        to={item.to}
                        onClick={onClose}
                        className={({ isActive }) =>
                          `flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors ${
                            isActive
                              ? 'bg-brand-50 text-brand-700'
                              : 'text-ink-700 hover:bg-ink-100'
                          }`
                        }
                      >
                        <Icon size={18} className="shrink-0" />
                        <span className="truncate">{item.label}</span>
                      </NavLink>
                    </li>
                  )
                })}
              </ul>
            </div>
          ))}
        </nav>

        <div className="px-5 py-3 border-t border-ink-100 text-xs text-ink-500">
          CORBA PDF Suite — USSEIN
        </div>
      </aside>
    </>
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
