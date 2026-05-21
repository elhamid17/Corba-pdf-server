import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  History, Download, Trash2, RefreshCw, FileWarning, Database, Eye,
  CheckCircle2, XCircle, Clock, LogIn,
} from 'lucide-react'
import ToolPage from '../components/ToolPage'
import PdfPreviewModal from '../components/PdfPreviewModal'
import { useToast } from '../components/Toast'
import { useAuth } from '../hooks/useAuth'
import { deleteJob, downloadJob, listJobs } from '../api/jobsApi'

const PAGE_SIZE = 20

export default function HistoryPage() {
  const { user, isAuthenticated, refresh, loading: authLoading } = useAuth()
  const toast = useToast()
  const [jobs, setJobs] = useState([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(true)
  const [busyId, setBusyId] = useState(null)
  const [previewJob, setPreviewJob] = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const data = await listJobs({ page, size: PAGE_SIZE })
      // Le backend renvoie maintenant { content, page, size, totalElements, totalPages }
      // pour les users ET les guests (uniformite).
      if (data && Array.isArray(data.content)) {
        setJobs(data.content)
        setTotalPages(data.totalPages ?? 1)
        setTotalElements(data.totalElements ?? data.content.length)
      } else if (Array.isArray(data)) {
        // Compat retro si l'ancien format est encore servi par un cache
        setJobs(data)
        setTotalPages(1)
        setTotalElements(data.length)
      } else {
        setJobs([])
        setTotalPages(0)
        setTotalElements(0)
      }
    } catch (e) {
      toast.error(e.message || 'Impossible de charger l\'historique.')
    } finally {
      setLoading(false)
    }
  }, [page, toast])

  useEffect(() => { load() }, [load])

  async function handleDownload(job) {
    setBusyId(job.id)
    try {
      await downloadJob(job.id, job.outputFilename)
    } catch (e) {
      toast.error(e.message || 'Telechargement impossible.')
    } finally {
      setBusyId(null)
    }
  }

  async function handleDelete(job) {
    if (!window.confirm(`Supprimer "${job.outputFilename}" de l'historique ?`)) return
    setBusyId(job.id)
    try {
      await deleteJob(job.id)
      setJobs(j => j.filter(x => x.id !== job.id))
      toast.success('Fichier supprime.')
      if (isAuthenticated) refresh()
    } catch (e) {
      toast.error(e.message || 'Suppression impossible.')
    } finally {
      setBusyId(null)
    }
  }

  return (
    <ToolPage
      icon={History}
      title="Historique des conversions"
      subtitle={isAuthenticated
        ? 'Retrouvez et retelechargez vos fichiers traites.'
        : 'Vos fichiers invites sont conserves 24h sur ce navigateur.'}
    >
      {!authLoading && !isAuthenticated && <GuestBanner />}
      {isAuthenticated && <QuotaBanner user={user} />}

      <div className="mt-4 flex items-center justify-between">
        <p className="text-sm text-ink-500 dark:text-ink-400">
          {loading ? 'Chargement…' : `${totalElements || jobs.length} entree(s)`}
        </p>
        <button
          type="button"
          onClick={load}
          disabled={loading}
          className="btn-ghost h-9 px-3 text-sm"
        >
          <RefreshCw size={14} className={loading ? 'animate-spin' : ''} />
          Actualiser
        </button>
      </div>

      <div className="mt-3">
        {loading ? <SkeletonList /> :
         jobs.length === 0 ? <EmptyState authenticated={isAuthenticated} /> :
         <ul className="divide-y divide-ink-100 dark:divide-ink-800">
           {jobs.map(j => (
             <JobRow
               key={j.id}
               job={j}
               busy={busyId === j.id}
               onPreview={() => setPreviewJob(j)}
               onDownload={() => handleDownload(j)}
               onDelete={() => handleDelete(j)}
             />
           ))}
         </ul>}
      </div>

      {totalPages > 1 && (
        <div className="mt-5 flex items-center justify-between">
          <button
            type="button"
            onClick={() => setPage(p => Math.max(0, p - 1))}
            disabled={page === 0}
            className="btn-ghost h-9 px-3 text-sm disabled:opacity-50"
          >Precedent</button>
          <span className="text-xs text-ink-500 dark:text-ink-400">
            Page {page + 1} / {totalPages}
          </span>
          <button
            type="button"
            onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1}
            className="btn-ghost h-9 px-3 text-sm disabled:opacity-50"
          >Suivant</button>
        </div>
      )}

      <PdfPreviewModal
        job={previewJob}
        onClose={() => setPreviewJob(null)}
        onDownload={handleDownload}
      />
    </ToolPage>
  )
}

function GuestBanner() {
  return (
    <div className="flex items-start gap-3 rounded-xl border border-brand-200 dark:border-brand-800 bg-brand-50/60 dark:bg-brand-950/30 p-4">
      <LogIn className="text-brand-600 dark:text-brand-400 shrink-0 mt-0.5" size={18} />
      <div className="text-sm text-ink-700 dark:text-ink-200">
        <p className="font-medium">Mode invite</p>
        <p className="mt-1 text-ink-600 dark:text-ink-300">
          Sans compte : 3 fichiers max, 20 Mo par fichier, conserves 24h.{' '}
          <Link to="/register" className="text-brand-600 dark:text-brand-400 font-medium hover:underline">
            Creer un compte
          </Link>{' '}
          pour 200 Mo de stockage total et 30 jours d'historique.
        </p>
      </div>
    </div>
  )
}

function QuotaBanner({ user }) {
  if (!user) return null
  const used = user.storageBytesUsed || 0
  const total = user.roles?.includes('ADMIN') ? Infinity : 200 * 1024 * 1024
  const pct = total === Infinity ? 0 : Math.min(100, Math.round((used / total) * 100))
  return (
    <div className="rounded-xl border border-ink-200 dark:border-ink-700 bg-white dark:bg-ink-900 p-4">
      <div className="flex items-center gap-3">
        <Database size={18} className="text-brand-600 dark:text-brand-400" />
        <div className="flex-1 min-w-0">
          <p className="text-sm font-medium text-ink-800 dark:text-ink-100">
            Stockage : {formatBytes(used)}{total !== Infinity && ` / ${formatBytes(total)}`}
          </p>
          {total !== Infinity && (
            <div className="mt-2 h-1.5 w-full rounded-full bg-ink-100 dark:bg-ink-800 overflow-hidden">
              <div
                className={`h-full rounded-full transition-all ${pct > 90 ? 'bg-rose-500' : pct > 70 ? 'bg-amber-500' : 'bg-brand-500'}`}
                style={{ width: `${pct}%` }}
              />
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

function JobRow({ job, busy, onPreview, onDownload, onDelete }) {
  const dt = job.createdAt ? new Date(job.createdAt) : null
  const exp = job.expiresAt ? new Date(job.expiresAt) : null
  // Preview disponible pour PDF + images (les autres formats n'ont pas de viewer natif)
  const ct = job.outputContentType || ''
  const previewable = job.downloadable && (ct.includes('pdf') || ct.startsWith('image/'))
  return (
    <li className="py-3 flex items-center gap-3">
      <StatusIcon status={job.status} />
      <div className="flex-1 min-w-0">
        <p className="text-sm font-medium text-ink-900 dark:text-ink-100 truncate">
          {job.outputFilename || job.operation}
        </p>
        <p className="mt-0.5 text-xs text-ink-500 dark:text-ink-400 flex flex-wrap items-center gap-x-3 gap-y-0.5">
          <span className="font-medium">{labelForOp(job.operation)}</span>
          {dt && <span>{dt.toLocaleString('fr-FR')}</span>}
          {job.resultSizeBytes > 0 && <span>{formatBytes(job.resultSizeBytes)}</span>}
          {exp && exp > new Date() && <span className="text-ink-400">expire le {exp.toLocaleDateString('fr-FR')}</span>}
        </p>
      </div>
      <div className="flex items-center gap-1.5">
        {previewable && (
          <button
            type="button"
            onClick={onPreview}
            disabled={busy}
            title="Apercu"
            className="btn-ghost h-9 w-9 p-0 disabled:opacity-40"
          >
            <Eye size={16} />
          </button>
        )}
        <button
          type="button"
          onClick={onDownload}
          disabled={busy || !job.downloadable}
          title={job.downloadable ? 'Telecharger' : 'Fichier non disponible (quota depasse ou expire)'}
          className="btn-ghost h-9 w-9 p-0 disabled:opacity-40"
        >
          <Download size={16} />
        </button>
        <button
          type="button"
          onClick={onDelete}
          disabled={busy}
          title="Supprimer"
          className="btn-ghost h-9 w-9 p-0 text-rose-600 hover:bg-rose-50 dark:text-rose-400 dark:hover:bg-rose-950/30"
        >
          <Trash2 size={16} />
        </button>
      </div>
    </li>
  )
}

function StatusIcon({ status }) {
  if (status === 'SUCCESS') return <CheckCircle2 size={18} className="text-emerald-500 shrink-0" />
  if (status === 'FAILED') return <XCircle size={18} className="text-rose-500 shrink-0" />
  return <Clock size={18} className="text-amber-500 shrink-0" />
}

function EmptyState({ authenticated }) {
  return (
    <div className="text-center py-10">
      <FileWarning size={36} className="mx-auto text-ink-300 dark:text-ink-600" />
      <p className="mt-3 text-sm text-ink-500 dark:text-ink-400">
        {authenticated
          ? 'Aucune conversion enregistree pour l\'instant.'
          : 'Aucun fichier dans cette session invitee.'}
      </p>
      <Link to="/" className="mt-4 inline-flex btn-primary h-9 px-4 text-sm">
        Decouvrir les outils
      </Link>
    </div>
  )
}

function SkeletonList() {
  return (
    <ul className="divide-y divide-ink-100 dark:divide-ink-800">
      {Array.from({ length: 3 }).map((_, i) => (
        <li key={i} className="py-3 flex items-center gap-3 animate-pulse">
          <div className="h-5 w-5 rounded-full bg-ink-200 dark:bg-ink-700" />
          <div className="flex-1 space-y-1.5">
            <div className="h-3 bg-ink-200 dark:bg-ink-700 rounded w-1/3" />
            <div className="h-2.5 bg-ink-100 dark:bg-ink-800 rounded w-1/4" />
          </div>
          <div className="h-9 w-9 rounded-lg bg-ink-100 dark:bg-ink-800" />
        </li>
      ))}
    </ul>
  )
}

const OP_LABELS = {
  merge: 'Fusion',
  split: 'Decoupage',
  'extract-pages': 'Extraction',
  'delete-pages': 'Suppression de pages',
  compress: 'Compression',
  rotate: 'Rotation',
  watermark: 'Filigrane',
  protect: 'Protection',
  'convert-to-images': 'PDF -> Images',
  sign: 'Signature',
  create: 'Creation',
  'pdf-to-word': 'PDF -> Word',
  'pdf-to-excel': 'PDF -> Excel',
  'word-to-pdf': 'Word -> PDF',
  'images-to-pdf': 'Images -> PDF',
}

function labelForOp(op) {
  return OP_LABELS[op] || op
}

function formatBytes(n) {
  if (!n) return '0 o'
  const units = ['o', 'Ko', 'Mo', 'Go']
  let i = 0
  let val = n
  while (val >= 1024 && i < units.length - 1) { val /= 1024; i++ }
  return `${val.toFixed(val >= 100 || i === 0 ? 0 : 1)} ${units[i]}`
}
