import { useState } from 'react'
import { QrCode, Barcode } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import OutputFilenameField from '../components/OutputFilenameField'
import { useToast } from '../components/Toast'
import { useProgress } from '../hooks/useProgress'
import { addQrCode, addBarcode } from '../api/pdfApi'

/**
 * Page unifiee "Code-barres / QR" — fusionne QrCode + Barcode.
 *
 * Mode selectionne via le type :
 *  - QR_CODE  : carre, contenu libre (URL, texte)
 *  - CODE_128 / EAN_13 / EAN_8 / CODE_39 / ITF / UPC_A / PDF_417 : code-barres lineaire ou 2D
 */

const TYPES = [
  { id: 'QR_CODE',  label: 'QR code',   desc: 'URL ou texte libre', icon: QrCode },
  { id: 'CODE_128', label: 'Code 128',  desc: 'Alphanumérique',     icon: Barcode },
  { id: 'EAN_13',   label: 'EAN 13',    desc: '13 chiffres (produits)', icon: Barcode },
  { id: 'EAN_8',    label: 'EAN 8',     desc: '8 chiffres',         icon: Barcode },
  { id: 'CODE_39',  label: 'Code 39',   desc: 'Alphanumérique simple', icon: Barcode },
  { id: 'ITF',      label: 'ITF',       desc: 'Numérique entrelacé', icon: Barcode },
  { id: 'UPC_A',    label: 'UPC-A',     desc: '12 chiffres (US)',   icon: Barcode },
  { id: 'PDF_417',  label: 'PDF417',    desc: '2D, large capacité', icon: Barcode },
]

const POSITIONS = [
  { id: 'top-left',      label: 'Haut gauche'  },
  { id: 'top-center',    label: 'Haut centre'  },
  { id: 'top-right',     label: 'Haut droite'  },
  { id: 'bottom-left',   label: 'Bas gauche'   },
  { id: 'bottom-center', label: 'Bas centre'   },
  { id: 'bottom-right',  label: 'Bas droite'   },
]

export default function CodePage() {
  const [files, setFiles] = useState([])
  const [type, setType] = useState('QR_CODE')
  const [content, setContent] = useState('https://corba-pdf.onrender.com')
  const [page, setPage] = useState(1)
  const [position, setPosition] = useState('bottom-right')
  const [sizePx, setSizePx] = useState(150)
  const [outputName, setOutputName] = useState('')
  const [loading, setLoading] = useState(false)
  const { progress, onProgress, reset } = useProgress()
  const toast = useToast()

  const isQr = type === 'QR_CODE'

  async function handleSubmit() {
    if (!files[0]) return toast.error('Sélectionnez un PDF.')
    if (!content.trim()) return toast.error('Saisissez le contenu à encoder.')
    setLoading(true)
    try {
      if (isQr) {
        await addQrCode(files[0], { text: content.trim(), page, position, sizePx }, outputName, onProgress)
        toast.success('QR code apposé.')
      } else {
        await addBarcode(files[0], { code: content.trim(), type, page, position, sizePx }, outputName, onProgress)
        toast.success('Code-barres apposé.')
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
      icon={isQr ? QrCode : Barcode}
      title="Code-barres / QR"
      subtitle="Génère un QR code ou un code-barres (Code 128, EAN, PDF417…) et l'appose sur une page du PDF."
    >
      <DropZone onFiles={setFiles} label="Déposez votre PDF" />

      <div className="mt-6">
        <label className="field-label">Type de code</label>
        <div className="grid sm:grid-cols-2 gap-2">
          {TYPES.map(t => {
            const Icon = t.icon
            const active = type === t.id
            return (
              <button
                key={t.id}
                type="button"
                onClick={() => setType(t.id)}
                className={`text-left p-2.5 rounded-lg border transition-all ${
                  active
                    ? 'border-brand-500 bg-brand-50/70 dark:bg-brand-950/40'
                    : 'border-ink-200 hover:border-ink-300 dark:border-ink-700 dark:hover:border-ink-600'
                }`}
              >
                <div className="flex items-center gap-2">
                  <Icon size={14} className={active ? 'text-brand-600 dark:text-brand-400' : 'text-ink-500'} />
                  <span className="text-sm font-semibold text-ink-900 dark:text-ink-100">{t.label}</span>
                </div>
                <p className="text-xs text-ink-500 dark:text-ink-400 mt-0.5 ml-6">{t.desc}</p>
              </button>
            )
          })}
        </div>
      </div>

      <div className="mt-6">
        <label className="field-label">{isQr ? 'Contenu (URL ou texte)' : 'Contenu à encoder'}</label>
        <input value={content} onChange={e => setContent(e.target.value)}
               className={isQr ? 'field' : 'field font-mono'}
               placeholder={isQr ? 'https://exemple.com ou texte libre' : 'ABC123 ou 1234567890123'} />
      </div>

      <div className="mt-6 grid sm:grid-cols-2 gap-5">
        <div>
          <label className="field-label">Page</label>
          <input type="number" min={1} value={page}
                 onChange={e => setPage(Number(e.target.value))} className="field" />
        </div>
        <div>
          <label className="field-label">Taille — {sizePx} px</label>
          <input type="range" min={60} max={400} step={10} value={sizePx}
                 onChange={e => setSizePx(Number(e.target.value))} className="w-full accent-brand-600" />
        </div>
      </div>

      <div className="mt-6">
        <label className="field-label">Position</label>
        <div className="grid grid-cols-3 gap-2">
          {POSITIONS.map(p => (
            <button key={p.id} type="button" onClick={() => setPosition(p.id)}
                    className={`rounded-lg border py-2 text-xs font-semibold transition-all ${
                      position === p.id
                        ? 'border-brand-500 bg-brand-50 text-brand-700 dark:bg-brand-950/50 dark:text-brand-300'
                        : 'border-ink-200 hover:border-ink-300 text-ink-600 dark:border-ink-700 dark:hover:border-ink-600 dark:text-ink-300'
                    }`}>
              {p.label}
            </button>
          ))}
        </div>
      </div>

      <div className="mt-6">
        <OutputFilenameField value={outputName} onChange={setOutputName} placeholder={isQr ? 'document-qr' : 'document-barcode'} extension=".pdf" />
      </div>

      <div className="mt-6">
        <SubmitButton loading={loading} progress={progress} onClick={handleSubmit} disabled={!files[0] || !content.trim()}>
          {isQr ? 'Apposer le QR code' : 'Apposer le code-barres'}
        </SubmitButton>
      </div>
    </ToolPage>
  )
}
