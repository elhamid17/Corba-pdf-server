import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Command } from 'cmdk'
import {
  Search, ArrowRight, Sun, Moon, Monitor, LogOut, History, Shield, UserPlus, LogIn,
} from 'lucide-react'
import { SERVICE_GROUPS } from './Navbar'
import { useAuth } from '../hooks/useAuth'
import { useTheme } from '../hooks/useTheme'

/**
 * Palette de commandes globale, ouverte avec Ctrl/Cmd+K.
 * Liste tous les outils PDF + actions (auth, theme, admin) avec recherche fuzzy.
 */
export default function CommandPalette() {
  const [open, setOpen] = useState(false)
  const navigate = useNavigate()
  const { isAuthenticated, isAdmin, logout } = useAuth()
  const { theme, setTheme } = useTheme()

  // Raccourci clavier Ctrl+K / Cmd+K
  useEffect(() => {
    const onKey = (e) => {
      if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
        e.preventDefault()
        setOpen(o => !o)
      } else if (e.key === 'Escape' && open) {
        setOpen(false)
      }
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [open])

  useEffect(() => {
    document.body.style.overflow = open ? 'hidden' : ''
    return () => { document.body.style.overflow = '' }
  }, [open])

  const run = (fn) => { setOpen(false); fn() }

  return (
    <>
      {open && (
        <div
          className="fixed inset-0 z-50 bg-ink-900/60 dark:bg-black/70 backdrop-blur-sm flex items-start justify-center pt-[8vh] px-3"
          onClick={() => setOpen(false)}
        >
          <Command
            label="Palette de commandes"
            className="w-full max-w-xl rounded-2xl border border-ink-200 dark:border-ink-700 bg-white dark:bg-ink-900 shadow-2xl overflow-hidden"
            onClick={e => e.stopPropagation()}
          >
            <div className="flex items-center gap-2 px-4 border-b border-ink-100 dark:border-ink-800">
              <Search size={16} className="text-ink-400" />
              <Command.Input
                autoFocus
                placeholder="Rechercher un outil ou une action…"
                className="flex-1 py-3.5 bg-transparent outline-none text-sm text-ink-900 dark:text-ink-100 placeholder:text-ink-400"
              />
              <kbd className="hidden sm:inline-flex text-[10px] font-mono text-ink-400 border border-ink-200 dark:border-ink-700 rounded px-1.5 py-0.5">
                ESC
              </kbd>
            </div>

            <Command.List className="max-h-[60vh] overflow-y-auto p-2">
              <Command.Empty className="py-8 text-center text-sm text-ink-500">
                Aucun resultat.
              </Command.Empty>

              {/* Outils PDF — un groupe par categorie */}
              {SERVICE_GROUPS.map(group => (
                <Command.Group key={group.title} heading={group.title} className="px-2 pt-2">
                  <p className="px-1 pb-1 text-[10px] font-semibold uppercase tracking-wider text-ink-400 dark:text-ink-500">
                    {group.title}
                  </p>
                  {group.items.map(item => {
                    const Icon = item.icon
                    return (
                      <Command.Item
                        key={item.to}
                        value={`${group.title} ${item.label}`}
                        onSelect={() => run(() => navigate(item.to))}
                        className="flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm text-ink-700 dark:text-ink-200 cursor-pointer aria-selected:bg-brand-50 aria-selected:text-brand-700 dark:aria-selected:bg-brand-950/40 dark:aria-selected:text-brand-300"
                      >
                        <Icon size={16} className="text-ink-400" />
                        <span className="flex-1">{item.label}</span>
                        <ArrowRight size={12} className="text-ink-300 opacity-0 aria-selected:opacity-100" />
                      </Command.Item>
                    )
                  })}
                </Command.Group>
              ))}

              {/* Compte */}
              <Command.Group className="px-2 pt-3">
                <p className="px-1 pb-1 text-[10px] font-semibold uppercase tracking-wider text-ink-400 dark:text-ink-500">
                  Compte
                </p>
                <Command.Item
                  value="historique mes fichiers"
                  onSelect={() => run(() => navigate('/history'))}
                  className="flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm text-ink-700 dark:text-ink-200 cursor-pointer aria-selected:bg-brand-50 aria-selected:text-brand-700 dark:aria-selected:bg-brand-950/40 dark:aria-selected:text-brand-300"
                >
                  <History size={16} className="text-ink-400" />
                  <span className="flex-1">Historique</span>
                </Command.Item>
                {!isAuthenticated && (
                  <>
                    <Command.Item
                      value="connexion login"
                      onSelect={() => run(() => navigate('/login'))}
                      className="flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm text-ink-700 dark:text-ink-200 cursor-pointer aria-selected:bg-brand-50 aria-selected:text-brand-700 dark:aria-selected:bg-brand-950/40 dark:aria-selected:text-brand-300"
                    >
                      <LogIn size={16} className="text-ink-400" />
                      <span className="flex-1">Se connecter</span>
                    </Command.Item>
                    <Command.Item
                      value="inscription register creer compte"
                      onSelect={() => run(() => navigate('/register'))}
                      className="flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm text-ink-700 dark:text-ink-200 cursor-pointer aria-selected:bg-brand-50 aria-selected:text-brand-700 dark:aria-selected:bg-brand-950/40 dark:aria-selected:text-brand-300"
                    >
                      <UserPlus size={16} className="text-ink-400" />
                      <span className="flex-1">Creer un compte</span>
                    </Command.Item>
                  </>
                )}
                {isAdmin && (
                  <Command.Item
                    value="administration admin console"
                    onSelect={() => run(() => navigate('/admin'))}
                    className="flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm text-rose-600 dark:text-rose-400 cursor-pointer aria-selected:bg-rose-50 dark:aria-selected:bg-rose-950/30"
                  >
                    <Shield size={16} />
                    <span className="flex-1">Console administrateur</span>
                  </Command.Item>
                )}
                {isAuthenticated && (
                  <Command.Item
                    value="deconnexion logout"
                    onSelect={() => run(() => { logout(); navigate('/') })}
                    className="flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm text-rose-600 dark:text-rose-400 cursor-pointer aria-selected:bg-rose-50 dark:aria-selected:bg-rose-950/30"
                  >
                    <LogOut size={16} />
                    <span className="flex-1">Se deconnecter</span>
                  </Command.Item>
                )}
              </Command.Group>

              {/* Theme */}
              <Command.Group className="px-2 pt-3 pb-2">
                <p className="px-1 pb-1 text-[10px] font-semibold uppercase tracking-wider text-ink-400 dark:text-ink-500">
                  Apparence
                </p>
                {[
                  { id: 'light', label: 'Theme clair', icon: Sun },
                  { id: 'dark',  label: 'Theme sombre', icon: Moon },
                  { id: 'auto',  label: 'Suivre le systeme', icon: Monitor },
                ].map(t => {
                  const Icon = t.icon
                  return (
                    <Command.Item
                      key={t.id}
                      value={`theme ${t.label}`}
                      onSelect={() => run(() => setTheme(t.id))}
                      className="flex items-center gap-2.5 px-3 py-2 rounded-lg text-sm text-ink-700 dark:text-ink-200 cursor-pointer aria-selected:bg-brand-50 aria-selected:text-brand-700 dark:aria-selected:bg-brand-950/40 dark:aria-selected:text-brand-300"
                    >
                      <Icon size={16} className="text-ink-400" />
                      <span className="flex-1">{t.label}</span>
                      {theme === t.id && <span className="text-[10px] text-brand-600 dark:text-brand-400">actif</span>}
                    </Command.Item>
                  )
                })}
              </Command.Group>
            </Command.List>

            <div className="px-3 py-2 border-t border-ink-100 dark:border-ink-800 flex items-center justify-between text-[11px] text-ink-400">
              <span>Navigation : <kbd className="font-mono bg-ink-100 dark:bg-ink-800 rounded px-1">↑↓</kbd> · Valider <kbd className="font-mono bg-ink-100 dark:bg-ink-800 rounded px-1">↵</kbd></span>
              <span><kbd className="font-mono bg-ink-100 dark:bg-ink-800 rounded px-1">Ctrl</kbd> + <kbd className="font-mono bg-ink-100 dark:bg-ink-800 rounded px-1">K</kbd> pour ouvrir</span>
            </div>
          </Command>
        </div>
      )}
    </>
  )
}
