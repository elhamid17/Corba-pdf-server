import { useCallback, useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import {
  Shield, Users, ListChecks, BarChart3, RefreshCw,
  Search, Trash2, ToggleLeft, ToggleRight, ShieldCheck, ShieldOff,
  Database, Activity, UserCheck, FileWarning,
} from 'lucide-react'
import { useToast } from '../components/Toast'
import { useAuth } from '../hooks/useAuth'
import * as adminApi from '../api/adminApi'

const TABS = [
  { id: 'stats', label: 'Statistiques', icon: BarChart3 },
  { id: 'users', label: 'Utilisateurs', icon: Users },
  { id: 'jobs',  label: 'Activite',     icon: ListChecks },
]

function AdminShell({ children, tab, setTab, user }) {
  return (
    <div className="min-h-[calc(100vh-4rem)] bg-ink-50 dark:bg-ink-950">
      {/* Bandeau admin sombre */}
      <header className="bg-gradient-to-br from-ink-900 to-ink-800 dark:from-black dark:to-ink-900 text-white">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-6">
          <div className="flex items-center gap-3">
            <span className="grid place-items-center h-11 w-11 rounded-xl bg-gradient-to-br from-accent-500 to-rose-500 shadow-glow">
              <Shield size={22} strokeWidth={2.4} />
            </span>
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2">
                <h1 className="text-2xl font-extrabold font-display tracking-tight">Console administrateur</h1>
                <span className="inline-flex items-center gap-1 rounded-full bg-rose-500/20 text-rose-200 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wider">
                  <Shield size={10} /> Admin
                </span>
              </div>
              <p className="mt-0.5 text-sm text-ink-300">
                Connecte en tant que <span className="font-medium text-white">{user?.username || '—'}</span> — gerez utilisateurs, jobs et statistiques globales.
              </p>
            </div>
          </div>

          {/* Tabs en mode dashboard */}
          <div className="mt-5 flex gap-1 overflow-x-auto">
            {TABS.map(t => {
              const Icon = t.icon
              const active = tab === t.id
              return (
                <button
                  key={t.id}
                  type="button"
                  onClick={() => setTab(t.id)}
                  className={`flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-medium whitespace-nowrap transition-colors ${
                    active
                      ? 'bg-white/15 text-white shadow-inner ring-1 ring-white/20'
                      : 'text-ink-300 hover:bg-white/5 hover:text-white'
                  }`}
                >
                  <Icon size={16} /> {t.label}
                </button>
              )
            })}
          </div>
        </div>
      </header>

      {/* Zone de contenu pleine largeur */}
      <main className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-6">
        {children}
      </main>
    </div>
  )
}

function Panel({ children }) {
  return (
    <div className="rounded-xl border border-ink-200 dark:border-ink-800 bg-white dark:bg-ink-900 shadow-card p-4 sm:p-6">
      {children}
    </div>
  )
}

export default function AdminPage() {
  const { isAdmin, loading, user } = useAuth()
  const [tab, setTab] = useState('stats')

  if (loading) {
    return (
      <AdminShell tab={tab} setTab={setTab} user={user}>
        <Panel><p className="text-sm text-ink-500">Chargement…</p></Panel>
      </AdminShell>
    )
  }
  if (!isAdmin) {
    return <Navigate to="/" replace />
  }

  return (
    <AdminShell tab={tab} setTab={setTab} user={user}>
      {tab === 'stats' && <StatsTab />}
      {tab === 'users' && <Panel><UsersTab /></Panel>}
      {tab === 'jobs'  && <Panel><JobsTab /></Panel>}
    </AdminShell>
  )
}

/* ───────────────── STATS ───────────────── */

function StatsTab() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const toast = useToast()

  const load = useCallback(async () => {
    setLoading(true)
    try {
      setData(await adminApi.stats())
    } catch (e) {
      toast.error(e.message || 'Statistiques indisponibles.')
    } finally {
      setLoading(false)
    }
  }, [toast])

  useEffect(() => { load() }, [load])

  if (loading) return <p className="text-sm text-ink-500">Calcul des statistiques…</p>
  if (!data) return null

  return (
    <div className="space-y-5">
      <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
        <StatCard icon={Users}    label="Utilisateurs" value={data.totalUsers}
                  hint={`${data.enabledUsers} actifs, ${data.admins} admin(s)`} />
        <StatCard icon={Activity} label="Jobs totaux"  value={data.totalJobs}
                  hint={`${data.jobsLast24h} sur 24h, ${data.jobsLast7d} sur 7j`} />
        <StatCard icon={UserCheck} label="Jobs invites" value={data.guestJobs}
                  hint="Sessions sans compte" />
        <StatCard icon={Database} label="Stockage"     value={formatBytes(data.totalStorageBytes)}
                  hint="Cumul comptes utilisateurs" />
      </div>

      <Panel>
        <div className="flex items-center justify-between mb-3">
          <h3 className="text-sm font-semibold text-ink-800 dark:text-ink-100">Top operations</h3>
          <button type="button" onClick={load} className="btn-ghost h-8 px-2.5 text-xs">
            <RefreshCw size={12} /> Actualiser
          </button>
        </div>
        {data.topOperations.length === 0 ? (
          <p className="text-sm text-ink-500">Aucune donnee.</p>
        ) : (
          <div className="space-y-1.5">
            {data.topOperations.map(op => {
              const max = data.topOperations[0].count || 1
              const pct = Math.round((op.count / max) * 100)
              return (
                <div key={op.operation} className="flex items-center gap-3">
                  <span className="w-40 text-xs text-ink-600 dark:text-ink-300 truncate">{op.operation}</span>
                  <div className="flex-1 h-2 rounded-full bg-ink-100 dark:bg-ink-800 overflow-hidden">
                    <div className="h-full bg-gradient-to-r from-brand-500 to-accent-500"
                         style={{ width: `${pct}%` }} />
                  </div>
                  <span className="w-12 text-right text-xs font-medium text-ink-700 dark:text-ink-200">{op.count}</span>
                </div>
              )
            })}
          </div>
        )}
      </Panel>
    </div>
  )
}

function StatCard({ icon: Icon, label, value, hint }) {
  return (
    <div className="rounded-xl border border-ink-200 dark:border-ink-700 bg-white dark:bg-ink-900 p-4">
      <div className="flex items-center gap-2 text-ink-500 dark:text-ink-400 text-xs font-medium">
        <Icon size={14} /> {label}
      </div>
      <p className="mt-1 text-2xl font-bold font-display text-ink-900 dark:text-ink-100">{value}</p>
      {hint && <p className="mt-0.5 text-xs text-ink-500 dark:text-ink-400">{hint}</p>}
    </div>
  )
}

/* ───────────────── USERS ───────────────── */

function UsersTab() {
  const { user: me } = useAuth()
  const toast = useToast()
  const [data, setData] = useState({ content: [], totalPages: 0, page: 0 })
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)
  const [busyId, setBusyId] = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      setData(await adminApi.listUsers({ q: search, page, size: 20 }))
    } catch (e) {
      toast.error(e.message || 'Liste indisponible.')
    } finally {
      setLoading(false)
    }
  }, [search, page, toast])

  useEffect(() => { load() }, [load])

  async function toggleEnabled(u) {
    setBusyId(u.id)
    try {
      const updated = await adminApi.updateUser(u.id, { enabled: !u.enabled })
      setData(d => ({ ...d, content: d.content.map(x => x.id === u.id ? updated : x) }))
      toast.success(`${updated.username} ${updated.enabled ? 'reactive' : 'desactive'}.`)
    } catch (e) {
      toast.error(e.message)
    } finally {
      setBusyId(null)
    }
  }

  async function toggleAdmin(u) {
    const isAdmin = u.roles.includes('ADMIN')
    const newRoles = isAdmin ? ['USER'] : ['USER', 'ADMIN']
    setBusyId(u.id)
    try {
      const updated = await adminApi.updateUser(u.id, { roles: newRoles })
      setData(d => ({ ...d, content: d.content.map(x => x.id === u.id ? updated : x) }))
      toast.success(`Role mis a jour pour ${updated.username}.`)
    } catch (e) {
      toast.error(e.message)
    } finally {
      setBusyId(null)
    }
  }

  async function remove(u) {
    if (!window.confirm(`Supprimer ${u.username} et tous ses fichiers ? Cette action est irreversible.`)) return
    setBusyId(u.id)
    try {
      await adminApi.deleteUser(u.id)
      setData(d => ({ ...d, content: d.content.filter(x => x.id !== u.id) }))
      toast.success(`Compte ${u.username} supprime.`)
    } catch (e) {
      toast.error(e.message)
    } finally {
      setBusyId(null)
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2">
        <div className="relative flex-1">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-ink-400" />
          <input
            value={search}
            onChange={e => { setSearch(e.target.value); setPage(0) }}
            className="field pl-9 h-10"
            placeholder="Rechercher par email ou nom d'utilisateur…"
          />
        </div>
        <button type="button" onClick={load} className="btn-ghost h-10 px-3 text-sm">
          <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
        </button>
      </div>

      {loading ? <p className="text-sm text-ink-500">Chargement…</p> :
       data.content.length === 0 ? <EmptyHint label="Aucun utilisateur." /> :
       <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="text-left text-xs font-semibold uppercase tracking-wider text-ink-400 dark:text-ink-500 border-b border-ink-100 dark:border-ink-800">
              <th className="py-2 pr-3">Utilisateur</th>
              <th className="py-2 pr-3">Role</th>
              <th className="py-2 pr-3">Stockage</th>
              <th className="py-2 pr-3">Jobs</th>
              <th className="py-2 pr-3">Inscription</th>
              <th className="py-2 pr-3 text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            {data.content.map(u => {
              const isSelf = me?.id === u.id
              const isAdminRow = u.roles.includes('ADMIN')
              return (
                <tr key={u.id} className="border-b border-ink-100 dark:border-ink-800 last:border-0">
                  <td className="py-2 pr-3">
                    <div className="flex items-center gap-2">
                      <span className="grid place-items-center h-7 w-7 rounded-full bg-gradient-to-br from-brand-600 to-accent-500 text-white text-xs font-bold">
                        {(u.username || '?').charAt(0).toUpperCase()}
                      </span>
                      <div className="min-w-0">
                        <p className="font-medium text-ink-900 dark:text-ink-100 truncate">{u.username}</p>
                        <p className="text-xs text-ink-500 dark:text-ink-400 truncate">{u.email}</p>
                      </div>
                    </div>
                  </td>
                  <td className="py-2 pr-3">
                    {isAdminRow ? (
                      <span className="inline-flex items-center gap-1 rounded-full bg-accent-50 dark:bg-accent-950/40 px-2 py-0.5 text-xs font-semibold text-accent-700 dark:text-accent-300">
                        <Shield size={10} /> Admin
                      </span>
                    ) : (
                      <span className="text-xs text-ink-500">USER</span>
                    )}
                    {!u.enabled && (
                      <span className="ml-1.5 inline-flex rounded-full bg-rose-50 dark:bg-rose-950/40 px-2 py-0.5 text-xs font-semibold text-rose-700 dark:text-rose-300">
                        Desactive
                      </span>
                    )}
                  </td>
                  <td className="py-2 pr-3 text-ink-700 dark:text-ink-200">{formatBytes(u.storageBytesUsed)}</td>
                  <td className="py-2 pr-3 text-ink-700 dark:text-ink-200">{u.jobsCount}</td>
                  <td className="py-2 pr-3 text-xs text-ink-500">{u.createdAt ? new Date(u.createdAt).toLocaleDateString('fr-FR') : '—'}</td>
                  <td className="py-2 pr-0">
                    <div className="flex items-center justify-end gap-1">
                      <button
                        type="button"
                        onClick={() => toggleEnabled(u)}
                        disabled={busyId === u.id || isSelf}
                        title={u.enabled ? 'Desactiver' : 'Reactiver'}
                        className="btn-ghost h-8 w-8 p-0 disabled:opacity-40"
                      >
                        {u.enabled ? <ToggleRight size={16} className="text-emerald-600" /> : <ToggleLeft size={16} className="text-ink-400" />}
                      </button>
                      <button
                        type="button"
                        onClick={() => toggleAdmin(u)}
                        disabled={busyId === u.id || isSelf}
                        title={isAdminRow ? 'Retirer le role admin' : 'Promouvoir admin'}
                        className="btn-ghost h-8 w-8 p-0 disabled:opacity-40"
                      >
                        {isAdminRow
                          ? <ShieldOff size={16} className="text-amber-600" />
                          : <ShieldCheck size={16} className="text-brand-600" />}
                      </button>
                      <button
                        type="button"
                        onClick={() => remove(u)}
                        disabled={busyId === u.id || isSelf}
                        title="Supprimer"
                        className="btn-ghost h-8 w-8 p-0 text-rose-600 hover:bg-rose-50 dark:text-rose-400 dark:hover:bg-rose-950/30 disabled:opacity-40"
                      >
                        <Trash2 size={16} />
                      </button>
                    </div>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
       </div>}

      {data.totalPages > 1 && (
        <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />
      )}
    </div>
  )
}

/* ───────────────── JOBS ───────────────── */

function JobsTab() {
  const toast = useToast()
  const [data, setData] = useState({ content: [], totalPages: 0, page: 0 })
  const [page, setPage] = useState(0)
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      setData(await adminApi.listJobs({ page, size: 30 }))
    } catch (e) {
      toast.error(e.message || 'Liste indisponible.')
    } finally {
      setLoading(false)
    }
  }, [page, toast])

  useEffect(() => { load() }, [load])

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <p className="text-sm text-ink-500 dark:text-ink-400">
          {loading ? 'Chargement…' : `${data.totalElements ?? data.content.length} job(s)`}
        </p>
        <button type="button" onClick={load} className="btn-ghost h-9 px-3 text-sm">
          <RefreshCw size={14} className={loading ? 'animate-spin' : ''} /> Actualiser
        </button>
      </div>

      {loading ? <p className="text-sm text-ink-500">Chargement…</p> :
       data.content.length === 0 ? <EmptyHint label="Aucune activite." /> :
       <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="text-left text-xs font-semibold uppercase tracking-wider text-ink-400 dark:text-ink-500 border-b border-ink-100 dark:border-ink-800">
              <th className="py-2 pr-3">Operation</th>
              <th className="py-2 pr-3">Auteur</th>
              <th className="py-2 pr-3">Fichier</th>
              <th className="py-2 pr-3">Taille</th>
              <th className="py-2 pr-3">Statut</th>
              <th className="py-2 pr-3">Quand</th>
            </tr>
          </thead>
          <tbody>
            {data.content.map(j => (
              <tr key={j.id} className="border-b border-ink-100 dark:border-ink-800 last:border-0">
                <td className="py-2 pr-3 font-medium text-ink-800 dark:text-ink-100">{j.operation}</td>
                <td className="py-2 pr-3">
                  {j.username ? (
                    <span className="text-ink-700 dark:text-ink-200">{j.username}</span>
                  ) : (
                    <span className="text-xs text-ink-400">invite {j.guestId?.slice(0, 8) || '—'}</span>
                  )}
                </td>
                <td className="py-2 pr-3 text-ink-600 dark:text-ink-300 truncate max-w-[200px]">{j.outputFilename || '—'}</td>
                <td className="py-2 pr-3 text-ink-700 dark:text-ink-200">{formatBytes(j.resultSizeBytes)}</td>
                <td className="py-2 pr-3">
                  <StatusBadge status={j.status} />
                </td>
                <td className="py-2 pr-3 text-xs text-ink-500">
                  {j.createdAt ? new Date(j.createdAt).toLocaleString('fr-FR') : '—'}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
       </div>}

      {data.totalPages > 1 && (
        <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />
      )}
    </div>
  )
}

function StatusBadge({ status }) {
  const cfg = {
    SUCCESS: 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300',
    FAILED:  'bg-rose-50 text-rose-700 dark:bg-rose-950/40 dark:text-rose-300',
    PENDING: 'bg-amber-50 text-amber-700 dark:bg-amber-950/40 dark:text-amber-300',
  }[status] || 'bg-ink-100 text-ink-600 dark:bg-ink-800 dark:text-ink-300'
  return <span className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${cfg}`}>{status}</span>
}

function Pagination({ page, totalPages, onChange }) {
  return (
    <div className="flex items-center justify-between">
      <button
        type="button"
        onClick={() => onChange(Math.max(0, page - 1))}
        disabled={page === 0}
        className="btn-ghost h-9 px-3 text-sm disabled:opacity-50"
      >Precedent</button>
      <span className="text-xs text-ink-500 dark:text-ink-400">Page {page + 1} / {totalPages}</span>
      <button
        type="button"
        onClick={() => onChange(Math.min(totalPages - 1, page + 1))}
        disabled={page >= totalPages - 1}
        className="btn-ghost h-9 px-3 text-sm disabled:opacity-50"
      >Suivant</button>
    </div>
  )
}

function EmptyHint({ label }) {
  return (
    <div className="text-center py-10">
      <FileWarning size={32} className="mx-auto text-ink-300 dark:text-ink-600" />
      <p className="mt-2 text-sm text-ink-500">{label}</p>
    </div>
  )
}

function formatBytes(n) {
  if (!n) return '0 o'
  const units = ['o', 'Ko', 'Mo', 'Go']
  let i = 0
  let val = n
  while (val >= 1024 && i < units.length - 1) { val /= 1024; i++ }
  return `${val.toFixed(val >= 100 || i === 0 ? 0 : 1)} ${units[i]}`
}
