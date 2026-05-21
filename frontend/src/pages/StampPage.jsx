import { useState } from 'react'
import { Stamp } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import OutputFilenameField from '../components/OutputFilenameField'
import { useToast } from '../components/Toast'
import { addStamp } from '../api/pdfApi'

const PRESETS = ['APPROUVÉ', 'REJETÉ', 'CONFIDENTIEL', 'BROUILLON', 'PAYÉ', 'URGENT', 'COPIE']
const POSITIONS = [
  { id: 'top-left',      label: 'Haut gauche'  },
  { id: 'top-center',    label: 'Haut centre'  },
  { id: 'top-right',     label: 'Haut droite'  },
  { id: 'center',        label: 'Centre'       },
  { id: 'bottom-left',   label: 'Bas gauche'   },
  { id: 'bottom-center', label: 'Bas centre'   },
  { id: 'bottom-right',  label: 'Bas droite'   },
]
const COLORS = [
  { id: 'red',    label: 'Rouge',   swatch: 'bg-rose-500'    },
  { id: 'green',  label: 'Vert',    swatch: 'bg-emerald-500' },
  { id: 'blue',   label: 'Bleu',    swatch: 'bg-blue-600'    },
  { id: 'orange', label: 'Orange',  swatch: 'bg-orange-500'  },
  { id: 'black',  label: 'Noir',    swatch: 'bg-ink-900'     },
]

export default function StampPage() {
  const [files, setFiles] = useState([])
  const [text, setText] = useState('APPROUVÉ')
  const [page, setPage] = useState(1)
  const [position, setPosition] = useState('center')
  const [color, setColor] = useState('red')
  const [fontSize, setFontSize] = useState(48)
  const [outputName, setOutputName] = useState('')
  const [loading, setLoading] = useState(false)
  const toast = useToast()

  async function handleSubmit() {
    if (!files[0]) return toast.error('Sélectionnez un PDF.')
    if (!text.trim()) return toast.error('Saisissez le texte du tampon.')
    setLoading(true)
    try {
      await addStamp(files[0], { text: text.trim(), page, position, color, fontSize }, outputName)
      toast.success(`Tampon "${text}" apposé.`)
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <ToolPage
      icon={Stamp}
      title="Tampon"
      subtitle="Appose un tampon encadré (APPROUVÉ, CONFIDENTIEL, etc.) sur une page du document."
    >
      <DropZone onFiles={setFiles} label="Déposez votre PDF" />

      <div className="mt-6">
        <label className="field-label">Texte du tampon</label>
        <input value={text} onChange={e => setText(e.target.value.toUpperCase())}
               className="field font-bold tracking-wider" maxLength={32} />
        <div className="mt-2 flex flex-wrap gap-1.5">
          {PRESETS.map(p => (
            <button
              key={p}
              type="button"
              onClick={() => setText(p)}
              className={`text-xs px-2 py-1 rounded-md font-semibold transition-colors ${
                text === p
                  ? 'bg-brand-600 text-white'
                  : 'bg-ink-100 text-ink-700 hover:bg-ink-200 dark:bg-ink-800 dark:text-ink-200 dark:hover:bg-ink-700'
              }`}
            >
              {p}
            </button>
          ))}
        </div>
      </div>

      <div className="mt-6 grid sm:grid-cols-2 gap-5">
        <div>
          <label className="field-label">Page</label>
          <input type="number" min={1} value={page}
                 onChange={e => setPage(Number(e.target.value))} className="field" />
        </div>
        <div>
          <label className="field-label">Taille — {fontSize} pt</label>
          <input type="range" min={18} max={96} step={2} value={fontSize}
                 onChange={e => setFontSize(Number(e.target.value))} className="w-full accent-brand-600" />
        </div>
      </div>

      <div className="mt-6">
        <label className="field-label">Position</label>
        <div className="grid grid-cols-3 gap-2">
          {POSITIONS.map(p => (
            <button
              key={p.id}
              type="button"
              onClick={() => setPosition(p.id)}
              className={`rounded-lg border py-2 text-xs font-semibold transition-all ${
                position === p.id
                  ? 'border-brand-500 bg-brand-50 text-brand-700 dark:bg-brand-950/50 dark:text-brand-300'
                  : 'border-ink-200 hover:border-ink-300 text-ink-600 dark:border-ink-700 dark:hover:border-ink-600 dark:text-ink-300'
              }`}
            >
              {p.label}
            </button>
          ))}
        </div>
      </div>

      <div className="mt-6">
        <label className="field-label">Couleur</label>
        <div className="flex flex-wrap gap-2">
          {COLORS.map(c => (
            <button
              key={c.id}
              type="button"
              onClick={() => setColor(c.id)}
              className={`flex items-center gap-2 rounded-lg border px-3 py-2 text-sm font-medium transition-all ${
                color === c.id
                  ? 'border-brand-500 bg-brand-50 dark:bg-brand-950/50'
                  : 'border-ink-200 hover:border-ink-300 dark:border-ink-700 dark:hover:border-ink-600'
              }`}
            >
              <span className={`h-4 w-4 rounded-full ${c.swatch}`} />
              {c.label}
            </button>
          ))}
        </div>
      </div>

      <div className="mt-6">
        <OutputFilenameField value={outputName} onChange={setOutputName} placeholder="document-tampon" extension=".pdf" />
      </div>

      <div className="mt-6">
        <SubmitButton loading={loading} onClick={handleSubmit} disabled={!files[0] || !text.trim()}>
          Apposer le tampon
        </SubmitButton>
      </div>
    </ToolPage>
  )
}
