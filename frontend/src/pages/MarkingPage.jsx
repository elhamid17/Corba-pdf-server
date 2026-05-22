import { useState } from 'react'
import { Droplets, Stamp as StampIcon } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import OutputFilenameField from '../components/OutputFilenameField'
import { useToast } from '../components/Toast'
import { useProgress } from '../hooks/useProgress'
import { addWatermark, addStamp } from '../api/pdfApi'

/**
 * Page unifiee "Marquage du document" — fusionne Filigrane + Tampon.
 *
 * 2 modes :
 *  - Filigrane : texte diagonal repete sur TOUTES les pages, faible opacite
 *  - Tampon    : texte coloré sur UNE page choisie, position au choix
 */

const MODES = [
  { id: 'watermark', label: 'Filigrane',  icon: Droplets,  desc: 'Texte diagonal sur toutes les pages' },
  { id: 'stamp',     label: 'Tampon',     icon: StampIcon, desc: 'Texte coloré sur une page précise' },
]

const STAMP_POSITIONS = [
  { id: 'top-left',      label: 'Haut gauche'  },
  { id: 'top-center',    label: 'Haut centre'  },
  { id: 'top-right',     label: 'Haut droite'  },
  { id: 'center',        label: 'Centre'       },
  { id: 'bottom-left',   label: 'Bas gauche'   },
  { id: 'bottom-center', label: 'Bas centre'   },
  { id: 'bottom-right',  label: 'Bas droite'   },
]

const STAMP_COLORS = [
  { id: 'red',    label: 'Rouge',  hex: '#dc2626' },
  { id: 'green',  label: 'Vert',   hex: '#16a34a' },
  { id: 'blue',   label: 'Bleu',   hex: '#2563eb' },
  { id: 'black',  label: 'Noir',   hex: '#0f172a' },
  { id: 'orange', label: 'Orange', hex: '#ea580c' },
]

export default function MarkingPage() {
  const [mode, setMode] = useState('watermark')
  const [files, setFiles] = useState([])

  // Filigrane
  const [wmText, setWmText] = useState('CONFIDENTIEL')
  const [wmOpacity, setWmOpacity] = useState(0.3)
  const [wmFontSize, setWmFontSize] = useState(48)
  const [wmDiagonal, setWmDiagonal] = useState(true)

  // Tampon
  const [stText, setStText] = useState('APPROUVÉ')
  const [stPage, setStPage] = useState(1)
  const [stPosition, setStPosition] = useState('center')
  const [stColor, setStColor] = useState('red')
  const [stFontSize, setStFontSize] = useState(48)

  const [outputName, setOutputName] = useState('')
  const [loading, setLoading] = useState(false)
  const { progress, onProgress, reset } = useProgress()
  const toast = useToast()

  async function handleSubmit() {
    if (!files[0]) return toast.error('Sélectionnez un PDF.')
    setLoading(true)
    try {
      if (mode === 'watermark') {
        if (!wmText.trim()) { toast.error('Saisissez le texte du filigrane.'); return }
        await addWatermark(files[0], {
          text: wmText.trim(), opacity: wmOpacity, fontSize: wmFontSize, diagonal: wmDiagonal,
        }, outputName, onProgress)
        toast.success('Filigrane appliqué.')
      } else {
        if (!stText.trim()) { toast.error('Saisissez le texte du tampon.'); return }
        await addStamp(files[0], {
          text: stText.trim(), page: stPage, position: stPosition, color: stColor, fontSize: stFontSize,
        }, outputName, onProgress)
        toast.success('Tampon apposé.')
      }
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
      reset()
    }
  }

  return (
    <ToolPage
      icon={mode === 'watermark' ? Droplets : StampIcon}
      title="Marquage du document"
      subtitle="Apposez un filigrane sur toutes les pages, ou un tampon coloré sur une page précise."
    >
      <DropZone onFiles={setFiles} label="Déposez votre PDF" />

      {/* Selecteur de mode */}
      <div className="mt-6">
        <label className="field-label">Type de marquage</label>
        <div className="grid sm:grid-cols-2 gap-2">
          {MODES.map(m => {
            const Icon = m.icon
            const active = mode === m.id
            return (
              <button
                key={m.id}
                type="button"
                onClick={() => setMode(m.id)}
                className={`text-left p-3 rounded-lg border transition-all ${
                  active
                    ? 'border-brand-500 bg-brand-50/70 dark:bg-brand-950/40'
                    : 'border-ink-200 hover:border-ink-300 dark:border-ink-700 dark:hover:border-ink-600'
                }`}
              >
                <div className="flex items-center gap-2 mb-1">
                  <Icon size={14} className={active ? 'text-brand-600 dark:text-brand-400' : 'text-ink-500'} />
                  <span className="text-sm font-semibold">{m.label}</span>
                </div>
                <p className="text-xs text-ink-500 dark:text-ink-400">{m.desc}</p>
              </button>
            )
          })}
        </div>
      </div>

      {/* Formulaire selon mode */}
      {mode === 'watermark' ? (
        <div className="mt-6 space-y-5">
          <div>
            <label className="field-label">Texte du filigrane</label>
            <input value={wmText} onChange={e => setWmText(e.target.value)} className="field" maxLength={100} />
          </div>
          <div className="grid sm:grid-cols-2 gap-5">
            <div>
              <label className="field-label">Opacité — {Math.round(wmOpacity * 100)}%</label>
              <input type="range" min={0.1} max={0.8} step={0.05} value={wmOpacity}
                     onChange={e => setWmOpacity(Number(e.target.value))} className="w-full accent-brand-600" />
            </div>
            <div>
              <label className="field-label">Taille — {wmFontSize}pt</label>
              <input type="range" min={24} max={120} step={4} value={wmFontSize}
                     onChange={e => setWmFontSize(Number(e.target.value))} className="w-full accent-brand-600" />
            </div>
          </div>
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" checked={wmDiagonal} onChange={e => setWmDiagonal(e.target.checked)}
                   className="rounded text-brand-600 focus:ring-brand-500" />
            <span>Orientation diagonale (style classique)</span>
          </label>
        </div>
      ) : (
        <div className="mt-6 space-y-5">
          <div>
            <label className="field-label">Texte du tampon</label>
            <input value={stText} onChange={e => setStText(e.target.value)} className="field font-bold" maxLength={30} />
            <p className="mt-1 text-xs text-ink-500">Ex: APPROUVÉ, CONFIDENTIEL, BROUILLON, REÇU…</p>
          </div>
          <div className="grid sm:grid-cols-2 gap-5">
            <div>
              <label className="field-label">Page</label>
              <input type="number" min={1} value={stPage}
                     onChange={e => setStPage(Number(e.target.value))} className="field" />
            </div>
            <div>
              <label className="field-label">Taille — {stFontSize}pt</label>
              <input type="range" min={24} max={120} step={4} value={stFontSize}
                     onChange={e => setStFontSize(Number(e.target.value))} className="w-full accent-brand-600" />
            </div>
          </div>
          <div>
            <label className="field-label">Couleur</label>
            <div className="grid grid-cols-5 gap-2">
              {STAMP_COLORS.map(c => {
                const active = stColor === c.id
                return (
                  <button key={c.id} type="button" onClick={() => setStColor(c.id)}
                          className={`h-10 rounded-lg border transition-all flex items-center justify-center text-xs font-semibold ${
                            active ? 'border-ink-900 dark:border-ink-100 scale-105 shadow-sm' : 'border-ink-200 dark:border-ink-700 hover:border-ink-400'
                          }`}
                          style={{ backgroundColor: `${c.hex}1A`, color: c.hex }}
                          title={c.label}>
                    {c.label}
                  </button>
                )
              })}
            </div>
          </div>
          <div>
            <label className="field-label">Position</label>
            <div className="grid grid-cols-3 gap-2">
              {STAMP_POSITIONS.map(p => (
                <button key={p.id} type="button" onClick={() => setStPosition(p.id)}
                        className={`rounded-lg border py-2 text-xs font-semibold transition-all ${
                          stPosition === p.id
                            ? 'border-brand-500 bg-brand-50 text-brand-700 dark:bg-brand-950/50 dark:text-brand-300'
                            : 'border-ink-200 hover:border-ink-300 text-ink-600 dark:border-ink-700 dark:hover:border-ink-600 dark:text-ink-300'
                        }`}>
                  {p.label}
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      <div className="mt-6">
        <OutputFilenameField value={outputName} onChange={setOutputName} placeholder={mode === 'watermark' ? 'doc-filigrane' : 'doc-tampon'} extension=".pdf" />
      </div>

      <div className="mt-6">
        <SubmitButton loading={loading} progress={progress} onClick={handleSubmit} disabled={!files[0]}>
          {mode === 'watermark' ? 'Appliquer le filigrane' : 'Apposer le tampon'}
        </SubmitButton>
      </div>
    </ToolPage>
  )
}
