import { useState } from 'react'
import { GitCompareArrows, CheckCircle2, Plus, Minus } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import { useToast } from '../components/Toast'
import { useProgress } from '../hooks/useProgress'
import { comparePdfs } from '../api/pdfApi'

export default function ComparePage() {
  const [filesA, setFilesA] = useState([])
  const [filesB, setFilesB] = useState([])
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState(null)
  const toast = useToast()
  const { progress, onProgress, reset } = useProgress()

  async function handleSubmit() {
    if (!filesA[0] || !filesB[0]) return toast.error('Sélectionnez les deux PDFs à comparer.')
    setLoading(true)
    setResult(null)
    try {
      const data = await comparePdfs(filesA[0], filesB[0], onProgress)
      setResult(data)
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
      reset()
    }
  }

  return (
    <ToolPage
      icon={GitCompareArrows}
      title="Comparaison de PDFs"
      subtitle="Compare le texte de deux PDFs ligne par ligne et page par page. Retourne un rapport des différences."
    >
      <div className="grid sm:grid-cols-2 gap-4">
        <div>
          <label className="field-label">Document A</label>
          <DropZone onFiles={setFilesA} label="Premier PDF" />
        </div>
        <div>
          <label className="field-label">Document B</label>
          <DropZone onFiles={setFilesB} label="Second PDF" />
        </div>
      </div>

      <div className="mt-6">
        <SubmitButton loading={loading} progress={progress} onClick={handleSubmit} disabled={!filesA[0] || !filesB[0]}>
          Comparer
        </SubmitButton>
      </div>

      {result && (
        <div className="mt-6 space-y-4">
          {result.identical && (
            <div className="rounded-xl border border-emerald-300 dark:border-emerald-700 bg-emerald-50 dark:bg-emerald-950/30 p-4 flex items-center gap-3">
              <CheckCircle2 size={20} className="text-emerald-600 dark:text-emerald-400" />
              <p className="text-sm font-medium text-emerald-800 dark:text-emerald-200">
                Les deux documents sont identiques.
              </p>
            </div>
          )}

          <div className="grid grid-cols-3 gap-3 text-center">
            <StatBox label="Communes" value={result.totals.common} color="ink" />
            <StatBox label="Ajoutées" value={result.totals.added} color="emerald" icon={Plus} />
            <StatBox label="Retirées" value={result.totals.removed} color="rose" icon={Minus} />
          </div>

          {!result.identical && result.pages?.some(p => p.added > 0 || p.removed > 0) && (
            <div>
              <h3 className="text-sm font-semibold mb-2">Différences par page</h3>
              <ul className="divide-y divide-ink-100 dark:divide-ink-800">
                {result.pages
                  .filter(p => p.added > 0 || p.removed > 0)
                  .map(p => (
                    <li key={p.page} className="py-3">
                      <p className="text-sm font-medium">Page {p.page} — <span className="text-emerald-600">+{p.added}</span> <span className="text-rose-600">−{p.removed}</span></p>
                      {p.sampleAdded?.length > 0 && (
                        <div className="mt-1.5">
                          <p className="text-[10px] font-semibold uppercase tracking-wider text-emerald-700">Ajoutées</p>
                          <ul className="text-xs text-ink-700 dark:text-ink-300 ml-2 mt-0.5">
                            {p.sampleAdded.map((l, i) => <li key={i} className="truncate">+ {l}</li>)}
                          </ul>
                        </div>
                      )}
                      {p.sampleRemoved?.length > 0 && (
                        <div className="mt-1.5">
                          <p className="text-[10px] font-semibold uppercase tracking-wider text-rose-700">Retirées</p>
                          <ul className="text-xs text-ink-700 dark:text-ink-300 ml-2 mt-0.5">
                            {p.sampleRemoved.map((l, i) => <li key={i} className="truncate">− {l}</li>)}
                          </ul>
                        </div>
                      )}
                    </li>
                  ))}
              </ul>
            </div>
          )}
        </div>
      )}
    </ToolPage>
  )
}

function StatBox({ label, value, color, icon: Icon }) {
  const cls = {
    ink: 'bg-ink-50 dark:bg-ink-800 text-ink-700 dark:text-ink-200',
    emerald: 'bg-emerald-50 dark:bg-emerald-950/40 text-emerald-700 dark:text-emerald-300',
    rose: 'bg-rose-50 dark:bg-rose-950/40 text-rose-700 dark:text-rose-300',
  }[color] || ''
  return (
    <div className={`rounded-xl p-4 ${cls}`}>
      <div className="flex items-center justify-center gap-1 text-xs font-medium uppercase tracking-wider">
        {Icon && <Icon size={12} />} {label}
      </div>
      <p className="mt-1 text-2xl font-bold font-display">{value}</p>
    </div>
  )
}
