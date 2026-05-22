import { useEffect, useRef, useState } from 'react'
import { X, Download, AlertTriangle, Loader2 } from 'lucide-react'
import { motion, AnimatePresence } from 'framer-motion'
import { apiFetch, readError } from '../api/client'

/**
 * Modal de previsualisation PDF (ou image).
 *
 * Charge le fichier via fetch + blob URL plutot que de pointer l'iframe
 * directement sur l'endpoint : ca permet d'envoyer le token Authorization
 * (impossible dans le src d'un iframe) et de gerer proprement les erreurs.
 *
 * Props :
 *   job       : objet job avec { id, outputFilename, outputContentType }
 *   onClose   : callback fermeture
 *   onDownload: callback bouton download (optionnel)
 */
export default function PdfPreviewModal({ job, onClose, onDownload }) {
  const [url, setUrl] = useState(null)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)
  const blobRef = useRef(null)

  useEffect(() => {
    if (!job) return
    let cancelled = false
    setLoading(true); setError(null); setUrl(null)

    apiFetch(`/api/jobs/${encodeURIComponent(job.id)}/download`)
      .then(async res => {
        if (!res.ok) throw new Error(await readError(res))
        return res.blob()
      })
      .then(blob => {
        if (cancelled) return
        const blobUrl = URL.createObjectURL(blob)
        blobRef.current = blobUrl
        setUrl(blobUrl)
      })
      .catch(e => {
        if (!cancelled) setError(e.message || 'Lecture impossible.')
      })
      .finally(() => { if (!cancelled) setLoading(false) })

    return () => {
      cancelled = true
      if (blobRef.current) {
        URL.revokeObjectURL(blobRef.current)
        blobRef.current = null
      }
    }
  }, [job])

  useEffect(() => {
    if (!job) return
    const onKey = e => { if (e.key === 'Escape') onClose?.() }
    document.addEventListener('keydown', onKey)
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', onKey)
      document.body.style.overflow = ''
    }
  }, [job, onClose])

  const isPdf = !job?.outputContentType || job?.outputContentType.includes('pdf')
  const isImage = job?.outputContentType?.startsWith('image/')

  return (
    <AnimatePresence>
      {job && (
        <motion.div
          className="fixed inset-0 z-50 bg-ink-900/70 dark:bg-black/80 backdrop-blur-sm flex items-center justify-center p-3 sm:p-6"
          onClick={onClose}
          role="dialog"
          aria-modal="true"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.15 }}
        >
          <motion.div
            initial={{ opacity: 0, scale: 0.96, y: 10 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.97, y: 5 }}
            transition={{ duration: 0.2, ease: 'easeOut' }}
            className="bg-white dark:bg-ink-900 rounded-2xl shadow-2xl w-full max-w-5xl h-[90vh] flex flex-col overflow-hidden"
            onClick={e => e.stopPropagation()}
          >
        {/* Header */}
        <div className="flex items-center gap-3 px-4 py-3 border-b border-ink-100 dark:border-ink-800 shrink-0">
          <div className="flex-1 min-w-0">
            <p className="text-sm font-semibold text-ink-900 dark:text-ink-100 truncate">
              {job.outputFilename || 'Apercu'}
            </p>
            {job.resultSizeBytes > 0 && (
              <p className="text-xs text-ink-500 dark:text-ink-400">{formatBytes(job.resultSizeBytes)}</p>
            )}
          </div>
          {onDownload && (
            <button
              type="button"
              onClick={() => onDownload(job)}
              className="btn-secondary h-9 px-3 text-sm"
              title="Telecharger"
            >
              <Download size={16} /> Telecharger
            </button>
          )}
          <button
            type="button"
            onClick={onClose}
            className="btn-ghost h-9 w-9 p-0"
            aria-label="Fermer"
          >
            <X size={18} />
          </button>
        </div>

        {/* Body */}
        <div className="flex-1 bg-ink-50 dark:bg-ink-950 overflow-hidden">
          {loading && (
            <div className="h-full flex flex-col items-center justify-center gap-2 text-ink-500">
              <Loader2 size={28} className="animate-spin text-brand-600" />
              <p className="text-sm">Chargement de l'apercu…</p>
            </div>
          )}
          {error && !loading && (
            <div className="h-full flex flex-col items-center justify-center gap-2 text-rose-600 dark:text-rose-400 px-6 text-center">
              <AlertTriangle size={28} />
              <p className="text-sm font-medium">{error}</p>
            </div>
          )}
          {!loading && !error && url && isPdf && (
            <iframe src={url} title="Apercu PDF" className="w-full h-full border-0" />
          )}
          {!loading && !error && url && isImage && (
            <div className="h-full grid place-items-center p-4">
              <img src={url} alt={job.outputFilename} className="max-w-full max-h-full object-contain rounded-lg shadow-lg" />
            </div>
          )}
          {!loading && !error && url && !isPdf && !isImage && (
            <div className="h-full flex flex-col items-center justify-center gap-2 text-ink-500 px-6 text-center">
              <AlertTriangle size={24} className="text-amber-500" />
              <p className="text-sm">Apercu indisponible pour ce type de fichier ({job.outputContentType || 'inconnu'}).</p>
              <p className="text-xs">Utilisez le bouton telecharger pour le recuperer.</p>
            </div>
          )}
        </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  )
}

function formatBytes(n) {
  if (!n) return '0 o'
  const units = ['o', 'Ko', 'Mo', 'Go']
  let i = 0, val = n
  while (val >= 1024 && i < units.length - 1) { val /= 1024; i++ }
  return `${val.toFixed(val >= 100 || i === 0 ? 0 : 1)} ${units[i]}`
}
