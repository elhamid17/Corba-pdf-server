import { useEffect, useRef, useState } from 'react'
import { FileText, Loader2, AlertTriangle, ChevronLeft, ChevronRight, Eye, BarChart3, X, Languages, Clock } from 'lucide-react'
import { getStats } from '../api/pdfApi'

/**
 * Aperçu des premières pages d'un PDF via pdf.js.
 *
 * Props :
 *   file       : File | Blob (le PDF à prévisualiser)
 *   maxPages   : nb max de pages à rendre (par défaut 3)
 *   className  : classes supplémentaires sur le container
 *
 * Caractéristiques :
 *  - Lazy load pdf.js (~600 Ko) à la demande, pas dans le bundle initial
 *  - Worker pdf.js chargé via CDN unpkg.com (matche la version installee)
 *  - Affichage en mode collapsable : header avec nb de pages + bouton expand
 *  - Navigation entre les pages prévisualisées (← →)
 */

let pdfjsPromise = null
function loadPdfJs() {
  if (pdfjsPromise) return pdfjsPromise
  pdfjsPromise = import('pdfjs-dist').then(mod => {
    mod.GlobalWorkerOptions.workerSrc =
      `https://cdn.jsdelivr.net/npm/pdfjs-dist@${mod.version}/build/pdf.worker.min.mjs`
    return mod
  })
  return pdfjsPromise
}

export default function PdfPreview({ file, maxPages = 3, className = '' }) {
  const [doc, setDoc] = useState(null)
  const [pageNum, setPageNum] = useState(1)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [collapsed, setCollapsed] = useState(false)
  const [statsOpen, setStatsOpen] = useState(false)
  const [stats, setStats] = useState(null)
  const [statsLoading, setStatsLoading] = useState(false)
  const canvasRef = useRef(null)
  const renderTaskRef = useRef(null)

  async function loadStats() {
    if (stats || statsLoading) { setStatsOpen(s => !s); return }
    setStatsLoading(true)
    try {
      const data = await getStats(file)
      setStats(data)
      setStatsOpen(true)
    } catch (e) {
      setStats({ error: e.message })
      setStatsOpen(true)
    } finally {
      setStatsLoading(false)
    }
  }

  // Charge le doc quand file change
  useEffect(() => {
    let cancelled = false
    if (!file) {
      setDoc(null); setError(null); setLoading(false); setPageNum(1)
      return
    }
    setLoading(true); setError(null); setPageNum(1)
    ;(async () => {
      try {
        const pdfjs = await loadPdfJs()
        const buffer = await file.arrayBuffer()
        const loadingTask = pdfjs.getDocument({ data: buffer })
        const pdfDoc = await loadingTask.promise
        if (cancelled) { pdfDoc.destroy?.(); return }
        setDoc(pdfDoc)
      } catch (e) {
        if (!cancelled) setError(e.message || 'Lecture impossible')
      } finally {
        if (!cancelled) setLoading(false)
      }
    })()
    return () => {
      cancelled = true
      if (renderTaskRef.current) try { renderTaskRef.current.cancel() } catch {}
    }
  }, [file])

  // Rend la page courante
  useEffect(() => {
    if (!doc || !canvasRef.current) return
    let cancelled = false
    ;(async () => {
      try {
        if (renderTaskRef.current) try { renderTaskRef.current.cancel() } catch {}
        const page = await doc.getPage(pageNum)
        if (cancelled) return
        const viewport = page.getViewport({ scale: 1 })
        const canvas = canvasRef.current
        if (!canvas) return
        // Echelle pour qualite retina (mais limitee pour rester leger)
        const container = canvas.parentElement
        const containerWidth = container ? container.clientWidth : 300
        const scale = Math.min(2, containerWidth / viewport.width)
        const scaled = page.getViewport({ scale })
        canvas.width = scaled.width
        canvas.height = scaled.height
        const ctx = canvas.getContext('2d')
        const renderTask = page.render({ canvasContext: ctx, viewport: scaled })
        renderTaskRef.current = renderTask
        await renderTask.promise
        renderTaskRef.current = null
      } catch (e) {
        if (e?.name !== 'RenderingCancelledException' && !cancelled) {
          setError(e.message || 'Rendu impossible')
        }
      }
    })()
    return () => { cancelled = true }
  }, [doc, pageNum])

  // Cleanup au demontage
  useEffect(() => {
    return () => {
      if (doc) doc.destroy?.()
    }
  }, [doc])

  if (!file) return null

  const totalPages = doc?.numPages || 0
  const previewablePages = Math.min(totalPages, maxPages)

  return (
    <div className={`rounded-xl border border-ink-200/70 dark:border-ink-700 bg-white dark:bg-ink-900 overflow-hidden ${className}`}>
      {/* Header */}
      <div className="flex items-center justify-between px-3 py-2 hover:bg-ink-50 dark:hover:bg-ink-800 transition-colors">
        <button
          type="button"
          onClick={() => setCollapsed(c => !c)}
          className="flex items-center gap-2 min-w-0 flex-1 text-left"
        >
          <Eye size={14} className="text-ink-400 shrink-0" />
          <span className="text-xs font-semibold text-ink-700 dark:text-ink-200 truncate">
            Aperçu
          </span>
          {totalPages > 0 && (
            <span className="text-[10px] text-ink-500 dark:text-ink-400 font-mono shrink-0">
              {totalPages} page{totalPages > 1 ? 's' : ''}
            </span>
          )}
        </button>
        <div className="flex items-center gap-1 shrink-0">
          {doc && (
            <button
              type="button"
              onClick={loadStats}
              disabled={statsLoading}
              className="inline-flex items-center gap-1 text-[10px] font-semibold uppercase tracking-wider text-ink-500 hover:text-brand-600 dark:hover:text-brand-400 px-1.5 py-0.5 rounded hover:bg-ink-100 dark:hover:bg-ink-800 transition-colors disabled:opacity-50"
              title="Statistiques du document"
            >
              {statsLoading
                ? <Loader2 size={10} className="animate-spin" />
                : <BarChart3 size={10} />}
              Stats
            </button>
          )}
          <button
            type="button"
            onClick={() => setCollapsed(c => !c)}
            className="text-[10px] text-ink-400 uppercase tracking-wider px-1.5 py-0.5"
          >
            {collapsed ? 'Afficher' : 'Masquer'}
          </button>
        </div>
      </div>

      {/* Panel Stats (apparait sous le header quand on clique Stats) */}
      {statsOpen && stats && (
        <div className="px-3 py-2.5 border-t border-ink-100 dark:border-ink-800 bg-ink-50/50 dark:bg-ink-950/50 relative">
          <button type="button" onClick={() => setStatsOpen(false)}
                  className="absolute top-1.5 right-1.5 grid place-items-center h-5 w-5 rounded text-ink-400 hover:text-ink-700 hover:bg-ink-200 dark:hover:bg-ink-800"
                  aria-label="Fermer">
            <X size={10} />
          </button>
          {stats.error ? (
            <p className="text-xs text-rose-600">{stats.error}</p>
          ) : (
            <>
              <div className="grid grid-cols-2 gap-x-3 gap-y-1 text-[11px]">
                <StatLine label="Pages"      value={stats.pages} />
                <StatLine label="Mots"       value={stats.words?.toLocaleString('fr-FR')} />
                <StatLine label="Caractères" value={stats.characters?.toLocaleString('fr-FR')} />
                <StatLine label="Lignes"     value={stats.lines?.toLocaleString('fr-FR')} />
              </div>
              <div className="mt-2 flex flex-wrap items-center gap-3 text-[11px] text-ink-600 dark:text-ink-300 pt-2 border-t border-ink-200 dark:border-ink-800">
                <span className="inline-flex items-center gap-1"><Clock size={10} /> {stats.readingMinutes} min de lecture</span>
                <span className="inline-flex items-center gap-1"><Languages size={10} /> {stats.language}</span>
                {stats.title && <span className="truncate">— {stats.title}</span>}
              </div>
            </>
          )}
        </div>
      )}

      {!collapsed && (
        <div className="px-3 pb-3">
          <div className="relative aspect-[3/4] bg-ink-100 dark:bg-ink-950 rounded-lg overflow-hidden flex items-center justify-center">
            {loading && (
              <div className="flex flex-col items-center gap-2 text-ink-400">
                <Loader2 size={20} className="animate-spin" />
                <p className="text-xs">Chargement…</p>
              </div>
            )}
            {error && (
              <div className="flex flex-col items-center gap-2 text-rose-500 px-4 text-center">
                <AlertTriangle size={20} />
                <p className="text-xs">{error}</p>
              </div>
            )}
            {!loading && !error && doc && (
              <canvas ref={canvasRef} className="max-w-full max-h-full object-contain" />
            )}
            {!loading && !error && !doc && (
              <FileText size={24} className="text-ink-300 dark:text-ink-700" />
            )}
          </div>

          {/* Navigation entre les pages prévisualisées */}
          {previewablePages > 1 && (
            <div className="mt-2 flex items-center justify-between">
              <button
                type="button"
                onClick={() => setPageNum(p => Math.max(1, p - 1))}
                disabled={pageNum <= 1}
                className="grid place-items-center h-7 w-7 rounded-md text-ink-500 hover:bg-ink-100 dark:hover:bg-ink-800 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                aria-label="Page précédente"
              >
                <ChevronLeft size={14} />
              </button>
              <span className="text-[10px] font-mono text-ink-500 dark:text-ink-400">
                Page {pageNum} / {previewablePages}
                {totalPages > previewablePages && (
                  <span className="text-ink-400"> · sur {totalPages}</span>
                )}
              </span>
              <button
                type="button"
                onClick={() => setPageNum(p => Math.min(previewablePages, p + 1))}
                disabled={pageNum >= previewablePages}
                className="grid place-items-center h-7 w-7 rounded-md text-ink-500 hover:bg-ink-100 dark:hover:bg-ink-800 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                aria-label="Page suivante"
              >
                <ChevronRight size={14} />
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

function StatLine({ label, value }) {
  return (
    <div className="flex items-baseline gap-1.5">
      <span className="text-ink-500 dark:text-ink-400">{label} :</span>
      <span className="font-bold font-mono text-ink-900 dark:text-ink-100">{value ?? '—'}</span>
    </div>
  )
}
