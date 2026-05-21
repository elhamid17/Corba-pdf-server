import { Link, NavLink, useNavigate } from 'react-router-dom'
import { useEffect, useRef, useState } from 'react'
import {
  Layers, Scissors, FileMinus, FileSearch, Archive, RotateCw,
  Droplets, Lock, Image as ImageIcon, FileText, ScanText, PenTool,
  Info, Plus, Github, Wand2, Menu, X, FileType2, Sheet, Images,
  Sun, Moon, Monitor, LogIn, LogOut, UserPlus, History, Shield, User as UserIcon, Search,
} from 'lucide-react'
import { ping } from '../api/pdfApi'
import { useTheme } from '../hooks/useTheme'
import { useAuth } from '../hooks/useAuth'

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
      <header className={`sticky top-0 z-40 transition-all ${
        scrolled
          ? 'backdrop-blur bg-white/85 dark:bg-ink-950/85 border-b border-ink-200/70 dark:border-ink-800/70 shadow-card'
          : 'bg-transparent'
      }`}>
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="flex h-16 items-center justify-between gap-3">
            {/* Hamburger + Logo */}
            <div className="flex items-center gap-2 min-w-0">
              <button
                type="button"
                onClick={() => setOpen(true)}
                aria-label="Ouvrir le menu des services"
                className="grid place-items-center h-10 w-10 rounded-lg text-ink-700 hover:bg-ink-100 dark:text-ink-200 dark:hover:bg-ink-800 transition-colors"
              >
                <Menu size={22} strokeWidth={2.2} />
              </button>

              <Link to="/" className="flex items-center gap-2.5 shrink-0 group">
                <span className="grid place-items-center h-9 w-9 rounded-xl bg-gradient-to-br from-brand-600 to-accent-500 text-white shadow-glow group-hover:scale-105 transition-transform">
                  <Wand2 size={18} strokeWidth={2.5} />
                </span>
                <span className="font-display font-extrabold text-lg text-ink-900 dark:text-ink-100 tracking-tight whitespace-nowrap">
                  CORBA <span className="text-brand-600 dark:text-brand-400">PDF</span>
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
                      isActive
                        ? 'bg-brand-50 text-brand-700 dark:bg-brand-950/50 dark:text-brand-300'
                        : 'text-ink-600 hover:bg-ink-100 hover:text-ink-900 dark:text-ink-300 dark:hover:bg-ink-800 dark:hover:text-ink-100'
                    }`
                  }
                >
                  {l.label}
                </NavLink>
              ))}
            </nav>

            {/* Statut serveur + theme + auth + github */}
            <div className="flex items-center gap-2 sm:gap-3 shrink-0">
              <StatusPill status={status} />
              <CommandHint />
              <AdminShortcut />
              <ThemeToggle />
              <UserMenu />
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
        className={`fixed inset-0 z-50 bg-ink-900/50 dark:bg-black/70 backdrop-blur-sm transition-opacity ${
          open ? 'opacity-100' : 'opacity-0 pointer-events-none'
        }`}
        aria-hidden="true"
      />

      {/* Panel */}
      <aside
        className={`fixed top-0 left-0 z-50 h-full w-[88%] max-w-sm bg-white dark:bg-ink-900 shadow-2xl flex flex-col transition-transform duration-300 ${
          open ? 'translate-x-0' : '-translate-x-full'
        }`}
        role="dialog"
        aria-label="Menu des services"
      >
        <div className="flex items-center justify-between px-5 py-4 border-b border-ink-100 dark:border-ink-800">
          <div className="flex items-center gap-2.5">
            <span className="grid place-items-center h-9 w-9 rounded-xl bg-gradient-to-br from-brand-600 to-accent-500 text-white shadow-glow">
              <Wand2 size={18} strokeWidth={2.5} />
            </span>
            <div>
              <p className="font-display font-extrabold text-base text-ink-900 dark:text-ink-100 leading-none">
                Tous les services
              </p>
              <p className="text-xs text-ink-500 dark:text-ink-400 mt-0.5">
                {ALL_SERVICES.length} outils PDF
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Fermer"
            className="grid place-items-center h-9 w-9 rounded-lg text-ink-500 hover:text-ink-900 hover:bg-ink-100 dark:text-ink-400 dark:hover:text-ink-100 dark:hover:bg-ink-800 transition-colors"
          >
            <X size={20} />
          </button>
        </div>

        <nav className="flex-1 overflow-y-auto px-3 py-4 space-y-5">
          {SERVICE_GROUPS.map(group => (
            <div key={group.title}>
              <p className="px-3 mb-1.5 text-[11px] font-semibold uppercase tracking-wider text-ink-400 dark:text-ink-500">
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
                              ? 'bg-brand-50 text-brand-700 dark:bg-brand-950/50 dark:text-brand-300'
                              : 'text-ink-700 hover:bg-ink-100 dark:text-ink-300 dark:hover:bg-ink-800'
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

        <div className="px-5 py-3 border-t border-ink-100 dark:border-ink-800 text-xs text-ink-500 dark:text-ink-400">
          CORBA PDF Suite — USSEIN
        </div>
      </aside>
    </>
  )
}

function StatusPill({ status }) {
  const cfg = {
    online:   { dot: 'bg-emerald-500', text: 'Opérationnel', ring: 'ring-emerald-200 dark:ring-emerald-800' },
    degraded: { dot: 'bg-amber-500',   text: 'Dégradé',      ring: 'ring-amber-200 dark:ring-amber-800'   },
    offline:  { dot: 'bg-rose-500',    text: 'Hors ligne',   ring: 'ring-rose-200 dark:ring-rose-800'    },
    checking: { dot: 'bg-ink-400',     text: 'Vérification', ring: 'ring-ink-200 dark:ring-ink-700'     },
  }[status] || { dot: 'bg-ink-400', text: '—', ring: 'ring-ink-200 dark:ring-ink-700' }

  return (
    <span className={`inline-flex items-center gap-2 rounded-full bg-white dark:bg-ink-900 px-2.5 sm:px-3 py-1 text-xs font-semibold text-ink-700 dark:text-ink-200 ring-1 ${cfg.ring} shadow-card`}>
      <span className={`relative inline-flex h-2 w-2 ${cfg.dot} rounded-full`}>
        {status === 'online' && <span className={`absolute inset-0 ${cfg.dot} rounded-full animate-pulse-dot`} />}
      </span>
      <span className="hidden sm:inline">{cfg.text}</span>
    </span>
  )
}

/**
 * Hint visuel discret pour signaler l'existence de la command palette.
 * Au clic : declenche l'event que CommandPalette ecoute via dispatch sur Ctrl+K.
 */
function CommandHint() {
  const isMac = typeof navigator !== 'undefined' && /Mac/.test(navigator.platform)
  const onClick = () => {
    // Simule le raccourci pour ouvrir la palette
    document.dispatchEvent(new KeyboardEvent('keydown', {
      key: 'k', code: 'KeyK', ctrlKey: !isMac, metaKey: isMac, bubbles: true,
    }))
  }
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label="Ouvrir la recherche rapide"
      title="Recherche rapide"
      className="hidden md:inline-flex items-center gap-1.5 h-9 px-2.5 rounded-lg border border-ink-200 dark:border-ink-700 bg-white/60 dark:bg-ink-900/60 hover:bg-ink-50 dark:hover:bg-ink-800 transition-colors text-xs text-ink-500 dark:text-ink-400"
    >
      <Search size={14} />
      <span className="hidden lg:inline">Rechercher</span>
      <kbd className="font-mono text-[10px] bg-ink-100 dark:bg-ink-800 rounded px-1 py-0.5">{isMac ? '⌘' : 'Ctrl'}+K</kbd>
    </button>
  )
}

/**
 * Bouton raccourci vers /admin, visible uniquement pour les admins.
 * Style rouge distinctif pour signaler une zone privilegiee.
 */
function AdminShortcut() {
  const { isAdmin } = useAuth()
  if (!isAdmin) return null
  return (
    <NavLink
      to="/admin"
      aria-label="Console administrateur"
      title="Console administrateur"
      className={({ isActive }) =>
        `inline-flex items-center gap-1.5 h-9 px-2.5 sm:px-3 rounded-lg text-sm font-semibold transition-all ring-1 ${
          isActive
            ? 'bg-rose-600 text-white ring-rose-700 shadow-md'
            : 'bg-rose-50 text-rose-700 ring-rose-200 hover:bg-rose-100 hover:ring-rose-300 dark:bg-rose-950/40 dark:text-rose-300 dark:ring-rose-900/60 dark:hover:bg-rose-950/60'
        }`
      }
    >
      <Shield size={16} strokeWidth={2.3} />
      <span className="hidden sm:inline">Admin</span>
    </NavLink>
  )
}

/**
 * Menu utilisateur : login/register pour invites, dropdown profil pour connectes.
 */
function UserMenu() {
  const { user, isAuthenticated, isAdmin, logout, loading } = useAuth()
  const [open, setOpen] = useState(false)
  const ref = useRef(null)
  const navigate = useNavigate()

  useEffect(() => {
    if (!open) return
    const onClick = e => { if (!ref.current?.contains(e.target)) setOpen(false) }
    const onKey = e => { if (e.key === 'Escape') setOpen(false) }
    document.addEventListener('mousedown', onClick)
    document.addEventListener('keydown', onKey)
    return () => {
      document.removeEventListener('mousedown', onClick)
      document.removeEventListener('keydown', onKey)
    }
  }, [open])

  if (loading) {
    return <span className="hidden sm:inline-flex h-9 w-20 rounded-lg bg-ink-100 dark:bg-ink-800 animate-pulse" />
  }

  if (!isAuthenticated) {
    return (
      <div className="flex items-center gap-1.5">
        <NavLink
          to="/history"
          title="Historique"
          className="grid place-items-center h-9 w-9 rounded-lg text-ink-600 hover:bg-ink-100 dark:text-ink-300 dark:hover:bg-ink-800 transition-colors"
        >
          <History size={18} />
        </NavLink>
        <Link
          to="/login"
          className="hidden sm:inline-flex items-center gap-1.5 h-9 px-3 rounded-lg text-sm font-medium text-ink-700 hover:bg-ink-100 dark:text-ink-200 dark:hover:bg-ink-800 transition-colors"
        >
          <LogIn size={16} /> Connexion
        </Link>
      </div>
    )
  }

  const initial = (user?.username || user?.email || '?').charAt(0).toUpperCase()

  return (
    <div ref={ref} className="relative">
      <button
        type="button"
        onClick={() => setOpen(o => !o)}
        className="flex items-center gap-2 h-9 px-2 rounded-lg hover:bg-ink-100 dark:hover:bg-ink-800 transition-colors"
        aria-haspopup="menu"
        aria-expanded={open}
      >
        <span className="grid place-items-center h-7 w-7 rounded-full bg-gradient-to-br from-brand-600 to-accent-500 text-white text-xs font-bold">
          {initial}
        </span>
        <span className="hidden sm:inline text-sm font-medium text-ink-700 dark:text-ink-200 max-w-[120px] truncate">
          {user.username}
        </span>
      </button>

      {open && (
        <div
          role="menu"
          className="absolute right-0 mt-2 w-60 rounded-xl border border-ink-200 dark:border-ink-700 bg-white dark:bg-ink-900 shadow-card overflow-hidden"
        >
          <div className="px-4 py-3 border-b border-ink-100 dark:border-ink-800">
            <p className="text-sm font-semibold text-ink-900 dark:text-ink-100 truncate">{user.username}</p>
            <p className="text-xs text-ink-500 dark:text-ink-400 truncate">{user.email}</p>
            {isAdmin && (
              <span className="mt-1.5 inline-flex items-center gap-1 rounded-full bg-accent-50 dark:bg-accent-950/40 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wider text-accent-700 dark:text-accent-300">
                <Shield size={10} /> Admin
              </span>
            )}
          </div>
          <MenuItem icon={History} onClick={() => { setOpen(false); navigate('/history') }}>
            Historique
          </MenuItem>
          <MenuItem icon={UserIcon} onClick={() => { setOpen(false); navigate('/history') }}>
            Mon stockage
          </MenuItem>
          {isAdmin && (
            <MenuItem icon={Shield} onClick={() => { setOpen(false); navigate('/admin') }}>
              Administration
            </MenuItem>
          )}
          <div className="border-t border-ink-100 dark:border-ink-800">
            <MenuItem
              icon={LogOut}
              danger
              onClick={() => { setOpen(false); logout(); navigate('/') }}
            >
              Se deconnecter
            </MenuItem>
          </div>
        </div>
      )}
    </div>
  )
}

function MenuItem({ icon: Icon, children, onClick, danger }) {
  return (
    <button
      type="button"
      role="menuitem"
      onClick={onClick}
      className={`w-full flex items-center gap-2.5 px-4 py-2 text-sm transition-colors ${
        danger
          ? 'text-rose-600 hover:bg-rose-50 dark:text-rose-400 dark:hover:bg-rose-950/30'
          : 'text-ink-700 hover:bg-ink-100 dark:text-ink-200 dark:hover:bg-ink-800'
      }`}
    >
      <Icon size={16} />
      {children}
    </button>
  )
}

/**
 * Bouton 3 etats : light -> dark -> auto -> light...
 * L'icone affichee correspond au mode actif (auto = Monitor).
 */
function ThemeToggle() {
  const { theme, setTheme } = useTheme()
  const next = { light: 'dark', dark: 'auto', auto: 'light' }[theme] || 'light'
  const Icon = { light: Sun, dark: Moon, auto: Monitor }[theme] || Sun
  const labels = {
    light: 'Theme clair — cliquer pour passer en sombre',
    dark:  'Theme sombre — cliquer pour suivre le systeme',
    auto:  'Theme automatique — cliquer pour passer en clair',
  }

  return (
    <button
      type="button"
      onClick={() => setTheme(next)}
      aria-label={labels[theme]}
      title={labels[theme]}
      className="grid place-items-center h-9 w-9 rounded-lg text-ink-600 hover:bg-ink-100 dark:text-ink-300 dark:hover:bg-ink-800 transition-colors"
    >
      <Icon size={18} />
    </button>
  )
}
