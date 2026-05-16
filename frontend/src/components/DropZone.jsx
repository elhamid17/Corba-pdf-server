import { useCallback, useEffect, useState } from 'react'
import { useDropzone } from 'react-dropzone'
import { UploadCloud, FileText, X, FilePlus2 } from 'lucide-react'

function formatSize(bytes) {
  if (!Number.isFinite(bytes)) return ''
  if (bytes < 1024)         return `${bytes} o`
  if (bytes < 1024 * 1024)  return `${(bytes / 1024).toFixed(1)} Ko`
  return `${(bytes / 1024 / 1024).toFixed(2)} Mo`
}

export default function DropZone({
  onFiles,
  multiple = false,
  accept = { 'application/pdf': ['.pdf'] },
  maxSize = 50 * 1024 * 1024,
  label,
  hint = 'Fichier PDF, 50 Mo max',
}) {
  const [files, setFiles] = useState([])

  const onDrop = useCallback(accepted => {
    const next = multiple ? [...files, ...accepted] : accepted.slice(0, 1)
    setFiles(next)
    onFiles?.(next)
  }, [files, multiple, onFiles])

  const { getRootProps, getInputProps, isDragActive, isDragReject } = useDropzone({
    onDrop, accept, multiple, maxSize,
  })

  const removeAt = (idx) => {
    const next = files.filter((_, i) => i !== idx)
    setFiles(next)
    onFiles?.(next)
  }
  const clearAll = () => { setFiles([]); onFiles?.([]) }

  // Reset le state interne quand le parent réinitialise
  useEffect(() => {
    // pas de hook ici — laissé volontairement pour permettre un usage simple
  }, [])

  const stateCls = isDragReject
    ? 'border-rose-300 bg-rose-50/60'
    : isDragActive
    ? 'border-brand-500 bg-brand-50/70 ring-4 ring-brand-500/15'
    : 'border-ink-200 bg-white hover:border-brand-300 hover:bg-brand-50/30'

  return (
    <div className="space-y-3">
      <div
        {...getRootProps()}
        className={`group relative cursor-pointer rounded-2xl border-2 border-dashed p-8 text-center transition-all duration-200 ${stateCls}`}
      >
        <input {...getInputProps()} />
        <div className="mx-auto mb-3 grid h-14 w-14 place-items-center rounded-2xl bg-gradient-to-br from-brand-500/10 to-accent-500/10 text-brand-600 group-hover:scale-105 transition-transform">
          <UploadCloud size={26} strokeWidth={2} />
        </div>
        <p className="text-base font-semibold text-ink-800">
          {label || (multiple ? 'Déposez vos fichiers PDF ici' : 'Déposez votre fichier PDF ici')}
        </p>
        <p className="mt-1 text-sm text-ink-500">
          ou <span className="text-brand-600 font-medium">parcourez</span> votre ordinateur
        </p>
        <p className="mt-3 text-xs text-ink-400">{hint}</p>
      </div>

      {files.length > 0 && (
        <div className="rounded-xl border border-ink-200/70 bg-white">
          <div className="flex items-center justify-between px-4 py-2.5 border-b border-ink-100">
            <div className="flex items-center gap-2 text-sm font-semibold text-ink-700">
              <FilePlus2 size={16} className="text-brand-600" />
              {files.length} fichier{files.length > 1 ? 's' : ''} sélectionné{files.length > 1 ? 's' : ''}
            </div>
            <button onClick={clearAll} className="text-xs font-medium text-ink-500 hover:text-rose-600 transition-colors">
              Tout supprimer
            </button>
          </div>
          <ul className="divide-y divide-ink-100">
            {files.map((f, i) => (
              <li key={`${f.name}-${i}`} className="flex items-center gap-3 px-4 py-2.5 animate-fade-in">
                <div className="grid h-9 w-9 place-items-center rounded-lg bg-brand-50 text-brand-700">
                  <FileText size={16} />
                </div>
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-ink-800">{f.name}</p>
                  <p className="text-xs text-ink-500">{formatSize(f.size)}</p>
                </div>
                <button
                  onClick={(e) => { e.stopPropagation(); removeAt(i) }}
                  className="text-ink-400 hover:text-rose-600 transition-colors"
                  aria-label="Retirer"
                >
                  <X size={16} />
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}
