import { useEffect, useMemo, useState } from 'react'
import {
  QrCode, Barcode, Globe, Euro, Contact, Type, Package, ShoppingCart, Settings, Info,
} from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import OutputFilenameField from '../components/OutputFilenameField'
import { useToast } from '../components/Toast'
import { useProgress } from '../hooks/useProgress'
import { addQrCode, addBarcode } from '../api/pdfApi'

/**
 * Page Code-barres / QR avec presets par cas d'usage.
 *
 * Au lieu de demander a l'utilisateur de choisir parmi 8 types techniques
 * (CODE_128, EAN_13, etc.), on lui propose des cas d'usage concrets et on
 * derive le bon format ZXing + le bon contenu encode automatiquement.
 *
 * Le "mode expert" reste disponible pour les utilisateurs qui veulent un
 * controle total (selectionner le type ZXing brut + entrer le contenu raw).
 */

const PRESETS = [
  {
    id: 'url',
    label: 'Lien web',
    icon: Globe,
    description: 'Pointer vers un site, une vidéo, une page',
    format: 'QR_CODE',
    fields: [
      { key: 'url', label: 'URL complète', type: 'url', placeholder: 'https://exemple.com', required: true },
    ],
    encode: ({ url }) => (url || '').trim(),
    defaults: { url: 'https://corba-pdf.onrender.com' },
  },
  {
    id: 'sepa',
    label: 'Paiement SEPA',
    icon: Euro,
    description: 'QR de virement bancaire pré-rempli (app bancaire FR/EU)',
    format: 'QR_CODE',
    fields: [
      { key: 'beneficiary', label: 'Bénéficiaire', placeholder: 'Jean Dupont', required: true, maxLength: 70 },
      { key: 'iban', label: 'IBAN du bénéficiaire', placeholder: 'FR76 1234 5678 9012 3456 7890 123', required: true, mono: true },
      { key: 'bic', label: 'BIC (optionnel)', placeholder: 'BNPAFRPP', mono: true, maxLength: 11 },
      { key: 'amount', label: 'Montant en € (optionnel)', type: 'number', placeholder: '99.00', step: '0.01', min: '0' },
      { key: 'reference', label: 'Référence / motif', placeholder: 'Facture 2025-001', maxLength: 140 },
    ],
    encode: ({ beneficiary, iban, bic, amount, reference }) => {
      // Format EPC QR Code (BCD/002/1/SCT)
      const cleanIban = (iban || '').replace(/\s/g, '').toUpperCase()
      const amt = amount ? `EUR${parseFloat(amount).toFixed(2)}` : ''
      return [
        'BCD',
        '002',
        '1',
        'SCT',
        bic || '',
        (beneficiary || '').trim(),
        cleanIban,
        amt,
        '',
        '',
        (reference || '').trim(),
      ].join('\n')
    },
  },
  {
    id: 'vcard',
    label: 'Contact (vCard)',
    icon: Contact,
    description: 'Carte de visite : le scan ajoute le contact au répertoire',
    format: 'QR_CODE',
    fields: [
      { key: 'fullName', label: 'Nom complet', placeholder: 'Jean Dupont', required: true },
      { key: 'phone', label: 'Téléphone', type: 'tel', placeholder: '+33 6 12 34 56 78' },
      { key: 'email', label: 'Email', type: 'email', placeholder: 'jean@exemple.com' },
      { key: 'organization', label: 'Entreprise / organisation', placeholder: 'Mon Entreprise' },
      { key: 'website', label: 'Site web', type: 'url', placeholder: 'https://...' },
    ],
    encode: ({ fullName, phone, email, organization, website }) => {
      const safe = (s) => (s || '').trim().replace(/[\n\r,;]/g, ' ')
      const parts = safe(fullName).split(/\s+/)
      const lastName = parts.length > 1 ? parts[parts.length - 1] : safe(fullName)
      const firstName = parts.length > 1 ? parts.slice(0, -1).join(' ') : ''
      return [
        'BEGIN:VCARD',
        'VERSION:3.0',
        `N:${lastName};${firstName};;;`,
        `FN:${safe(fullName)}`,
        organization && `ORG:${safe(organization)}`,
        phone && `TEL;TYPE=CELL:${safe(phone)}`,
        email && `EMAIL:${safe(email)}`,
        website && `URL:${safe(website)}`,
        'END:VCARD',
      ].filter(Boolean).join('\n')
    },
  },
  {
    id: 'text',
    label: 'Texte libre',
    icon: Type,
    description: 'Un message, note, identifiant — n\'importe quoi',
    format: 'QR_CODE',
    fields: [
      { key: 'text', label: 'Contenu à encoder', placeholder: 'Votre message…', required: true, textarea: true, maxLength: 1000 },
    ],
    encode: ({ text }) => (text || '').trim(),
  },
  {
    id: 'tracking',
    label: 'Numéro de suivi',
    icon: Package,
    description: 'Code-barres CODE 128 pour tracking colis, asset, réf interne',
    format: 'CODE_128',
    fields: [
      { key: 'tracking', label: 'Numéro / référence', placeholder: 'TRK-2025-001', required: true, mono: true, maxLength: 40 },
    ],
    encode: ({ tracking }) => (tracking || '').trim(),
  },
  {
    id: 'ean',
    label: 'Code produit EAN',
    icon: ShoppingCart,
    description: 'Code-barres EAN-13 (13 chiffres, produits retail)',
    format: 'EAN_13',
    fields: [
      { key: 'ean', label: 'Code EAN (12 ou 13 chiffres)', placeholder: '3017620422003', required: true, mono: true, pattern: '\\d{12,13}', maxLength: 13 },
    ],
    encode: ({ ean }) => (ean || '').trim(),
  },
  {
    id: 'expert',
    label: 'Mode expert',
    icon: Settings,
    description: 'Choisir manuellement le format et entrer le contenu brut',
    expert: true,
  },
]

const EXPERT_TYPES = [
  { id: 'QR_CODE',  label: 'QR code' },
  { id: 'CODE_128', label: 'CODE 128' },
  { id: 'EAN_13',   label: 'EAN 13' },
  { id: 'EAN_8',    label: 'EAN 8' },
  { id: 'CODE_39',  label: 'CODE 39' },
  { id: 'ITF',      label: 'ITF' },
  { id: 'UPC_A',    label: 'UPC-A' },
  { id: 'PDF_417',  label: 'PDF 417' },
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
  const [presetId, setPresetId] = useState('url')
  const [formValues, setFormValues] = useState({})
  const [expertType, setExpertType] = useState('CODE_128')
  const [expertContent, setExpertContent] = useState('ABC123')
  const [page, setPage] = useState(1)
  const [position, setPosition] = useState('bottom-right')
  const [sizePx, setSizePx] = useState(150)
  const [outputName, setOutputName] = useState('')
  const [loading, setLoading] = useState(false)
  const { progress, onProgress, reset } = useProgress()
  const toast = useToast()

  const preset = useMemo(() => PRESETS.find(p => p.id === presetId), [presetId])

  // Reset formValues quand on change de preset (utilise les defaults si fournis)
  useEffect(() => {
    if (!preset || preset.expert) {
      setFormValues({})
      return
    }
    setFormValues(preset.defaults || {})
  }, [presetId])

  const isExpert = preset?.expert
  const activeType = isExpert ? expertType : preset?.format
  const isQr = activeType === 'QR_CODE'

  function updateField(key, value) {
    setFormValues(v => ({ ...v, [key]: value }))
  }

  function validate() {
    if (!files[0]) { toast.error('Sélectionnez un PDF.'); return null }
    let content = ''
    if (isExpert) {
      content = expertContent.trim()
      if (!content) { toast.error('Saisissez le contenu à encoder.'); return null }
    } else {
      // Verifie les champs required
      for (const f of preset.fields) {
        if (f.required && !(formValues[f.key] || '').trim()) {
          toast.error(`Le champ « ${f.label} » est obligatoire.`)
          return null
        }
      }
      content = preset.encode(formValues)
      if (!content) { toast.error('Données invalides.'); return null }
    }
    return content
  }

  async function handleSubmit() {
    const content = validate()
    if (content == null) return
    setLoading(true)
    try {
      if (isQr) {
        await addQrCode(files[0], { text: content, page, position, sizePx }, outputName, onProgress)
        toast.success('QR code apposé.')
      } else {
        await addBarcode(files[0], { code: content, type: activeType, page, position, sizePx }, outputName, onProgress)
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
      subtitle="Sélectionnez votre cas d'usage et on s'occupe du bon format pour vous."
    >
      <DropZone onFiles={setFiles} label="Déposez votre PDF" />

      {/* Selecteur de preset */}
      <div className="mt-6">
        <label className="field-label">Que voulez-vous encoder ?</label>
        <div className="grid grid-cols-2 md:grid-cols-3 gap-2">
          {PRESETS.map(p => {
            const Icon = p.icon
            const active = p.id === presetId
            return (
              <button
                key={p.id}
                type="button"
                onClick={() => setPresetId(p.id)}
                className={`text-left p-3 rounded-lg border transition-all ${
                  active
                    ? 'border-brand-500 bg-brand-50/70 dark:bg-brand-950/40 shadow-sm'
                    : 'border-ink-200 hover:border-ink-300 dark:border-ink-700 dark:hover:border-ink-600'
                }`}
              >
                <div className="flex items-center gap-2 mb-1">
                  <Icon size={14} className={active ? 'text-brand-600 dark:text-brand-400' : 'text-ink-500'} />
                  <span className="text-sm font-semibold text-ink-900 dark:text-ink-100">{p.label}</span>
                </div>
                <p className="text-[11px] text-ink-500 dark:text-ink-400 leading-snug">{p.description}</p>
              </button>
            )
          })}
        </div>
      </div>

      {/* Champs dynamiques selon preset */}
      <div className="mt-6 space-y-4">
        {isExpert ? (
          <>
            <div>
              <label className="field-label">Type de code</label>
              <select value={expertType} onChange={e => setExpertType(e.target.value)} className="field">
                {EXPERT_TYPES.map(t => (
                  <option key={t.id} value={t.id}>{t.label}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="field-label">Contenu brut à encoder</label>
              <input value={expertContent} onChange={e => setExpertContent(e.target.value)}
                     className="field font-mono"
                     placeholder="Saisissez le contenu exact à encoder" />
            </div>
          </>
        ) : preset?.fields.map(f => (
          <div key={f.key}>
            <label className="field-label">{f.label}{f.required && <span className="text-rose-500"> *</span>}</label>
            {f.textarea ? (
              <textarea
                value={formValues[f.key] || ''}
                onChange={e => updateField(f.key, e.target.value)}
                className="field min-h-24"
                placeholder={f.placeholder}
                maxLength={f.maxLength}
              />
            ) : (
              <input
                type={f.type || 'text'}
                value={formValues[f.key] || ''}
                onChange={e => updateField(f.key, e.target.value)}
                className={`field ${f.mono ? 'font-mono' : ''}`}
                placeholder={f.placeholder}
                maxLength={f.maxLength}
                pattern={f.pattern}
                step={f.step}
                min={f.min}
              />
            )}
          </div>
        ))}

        {/* Note informative pour les presets specialises */}
        {preset?.id === 'sepa' && (
          <div className="flex items-start gap-2 rounded-lg bg-brand-50/60 dark:bg-brand-950/30 border border-brand-200 dark:border-brand-800 p-2.5 text-xs text-ink-700 dark:text-ink-300">
            <Info size={12} className="shrink-0 mt-0.5 text-brand-600 dark:text-brand-400" />
            <span>
              Le QR est au format <strong>EPC QR Code</strong> (norme européenne). Compatible avec la plupart des applis bancaires françaises et européennes pour pré-remplir un virement.
            </span>
          </div>
        )}
        {preset?.id === 'vcard' && (
          <div className="flex items-start gap-2 rounded-lg bg-brand-50/60 dark:bg-brand-950/30 border border-brand-200 dark:border-brand-800 p-2.5 text-xs text-ink-700 dark:text-ink-300">
            <Info size={12} className="shrink-0 mt-0.5 text-brand-600 dark:text-brand-400" />
            <span>Format vCard 3.0 — scannable par l'appareil photo iOS / Android pour ajouter le contact directement.</span>
          </div>
        )}
      </div>

      {/* Position + taille + page + nom fichier */}
      <div className="mt-6 grid sm:grid-cols-2 gap-5">
        <div>
          <label className="field-label">Page du PDF</label>
          <input type="number" min={1} value={page}
                 onChange={e => setPage(Number(e.target.value))} className="field" />
        </div>
        <div>
          <label className="field-label">Taille du code — {sizePx} px</label>
          <input type="range" min={60} max={400} step={10} value={sizePx}
                 onChange={e => setSizePx(Number(e.target.value))} className="w-full accent-brand-600" />
        </div>
      </div>

      <div className="mt-6">
        <label className="field-label">Position sur la page</label>
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
        <SubmitButton loading={loading} progress={progress} onClick={handleSubmit} disabled={!files[0]}>
          {isQr ? 'Apposer le QR code' : 'Apposer le code-barres'}
        </SubmitButton>
      </div>
    </ToolPage>
  )
}
