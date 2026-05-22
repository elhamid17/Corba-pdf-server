import { useNavigate, useLocation } from 'react-router-dom'
import { AnimatePresence, motion } from 'framer-motion'
import {
  ArrowRight, Sparkles, X,
  Archive, Droplets, Lock, Unlock, Highlighter, UserX, ShieldCheck,
  Hash, Maximize2, Crop, RotateCw, Layers, Scissors, FileSearch, FileMinus,
  FileText, BarChart3, GitCompareArrows, QrCode, Barcode, Stamp, PenTool,
  PenLine, BookOpen, FlipVertical2, Shuffle, ScanText, Presentation, Code2,
} from 'lucide-react'
import { useWorkflow } from '../hooks/useWorkflow'

/**
 * Suggestions intelligentes d'enchainement d'outils.
 *
 * Apparait apres une operation reussie (presence de lastResult) et propose
 * 2-3 outils logiquement complementaires. Le clic transfere automatiquement
 * le PDF resultant vers l'outil suivant via la file de workflow.
 */

// Map sourcePath -> outils suggeres apres cette operation
const SUGGESTIONS = {
  // Organisation
  '/merge': [
    { to: '/compress', label: 'Compresser', icon: Archive },
    { to: '/watermark', label: 'Filigrane', icon: Droplets },
    { to: '/protect', label: 'Protéger', icon: Lock },
  ],
  '/split': [
    { to: '/merge', label: 'Refusionner', icon: Layers },
    { to: '/compress', label: 'Compresser', icon: Archive },
  ],
  '/extract-pages': [
    { to: '/page-numbers', label: 'Numéroter', icon: Hash },
    { to: '/watermark', label: 'Filigrane', icon: Droplets },
    { to: '/compress', label: 'Compresser', icon: Archive },
  ],
  '/delete-pages': [
    { to: '/page-numbers', label: 'Renuméroter', icon: Hash },
    { to: '/compress', label: 'Compresser', icon: Archive },
  ],
  '/rotate': [
    { to: '/compress', label: 'Compresser', icon: Archive },
    { to: '/watermark', label: 'Filigrane', icon: Droplets },
  ],
  '/reorder': [
    { to: '/page-numbers', label: 'Renuméroter', icon: Hash },
    { to: '/compress', label: 'Compresser', icon: Archive },
  ],
  '/reverse': [
    { to: '/page-numbers', label: 'Renuméroter', icon: Hash },
  ],
  '/cover': [
    { to: '/watermark', label: 'Filigrane', icon: Droplets },
    { to: '/compress', label: 'Compresser', icon: Archive },
  ],

  // Conversions depuis PDF
  '/pdf-to-word':     [{ to: '/word-to-pdf', label: 'Re-convertir en PDF', icon: FileText }],
  '/pdf-to-excel':    [{ to: '/excel-to-pdf', label: 'Re-convertir en PDF', icon: FileText }],
  '/pdf-to-pptx':     [],
  '/pdf-to-markdown': [{ to: '/markdown-to-pdf', label: 'Re-convertir en PDF', icon: FileText }],
  '/convert':         [],  // PDF → images, le resultat est un ZIP, pas chainable

  // Convertir en PDF -> on suggere de l'organiser, signer, compresser
  '/word-to-pdf':     [{ to: '/compress', label: 'Compresser', icon: Archive }, { to: '/sign', label: 'Signer', icon: PenTool }, { to: '/watermark', label: 'Filigrane', icon: Droplets }],
  '/excel-to-pdf':    [{ to: '/compress', label: 'Compresser', icon: Archive }, { to: '/sign', label: 'Signer', icon: PenTool }],
  '/odt-to-pdf':      [{ to: '/compress', label: 'Compresser', icon: Archive }, { to: '/sign', label: 'Signer', icon: PenTool }],
  '/markdown-to-pdf': [{ to: '/sign', label: 'Signer', icon: PenTool }, { to: '/page-numbers', label: 'Numéroter', icon: Hash }],
  '/html-to-pdf':     [{ to: '/sign', label: 'Signer', icon: PenTool }, { to: '/page-numbers', label: 'Numéroter', icon: Hash }],
  '/images-to-pdf':   [{ to: '/compress', label: 'Compresser', icon: Archive }, { to: '/ocr', label: 'OCR', icon: ScanText }],
  '/scan':            [{ to: '/ocr', label: 'OCR du scan', icon: ScanText }, { to: '/compress', label: 'Compresser', icon: Archive }, { to: '/sign', label: 'Signer', icon: PenTool }],

  // Edition
  '/watermark':    [{ to: '/protect', label: 'Protéger', icon: Lock }, { to: '/sign', label: 'Signer', icon: PenTool }],
  '/stamp':        [{ to: '/sign', label: 'Signer', icon: PenTool }, { to: '/protect', label: 'Protéger', icon: Lock }],
  '/page-numbers': [{ to: '/compress', label: 'Compresser', icon: Archive }, { to: '/watermark', label: 'Filigrane', icon: Droplets }],
  '/crop':         [{ to: '/compress', label: 'Compresser', icon: Archive }, { to: '/resize', label: 'Redimensionner', icon: Maximize2 }],
  '/resize':       [{ to: '/compress', label: 'Compresser', icon: Archive }],
  '/compress':     [{ to: '/sign', label: 'Signer', icon: PenTool }, { to: '/protect', label: 'Protéger', icon: Lock }, { to: '/watermark', label: 'Filigrane', icon: Droplets }],

  // Securite
  '/protect':      [],  // file proteged, peu chainable
  '/unlock':       [{ to: '/compress', label: 'Compresser', icon: Archive }, { to: '/sign', label: 'Resigner', icon: PenTool }],
  '/sign':         [{ to: '/verify-signature', label: 'Vérifier la signature', icon: ShieldCheck }, { to: '/protect', label: 'Protéger', icon: Lock }],
  '/sign-image':   [{ to: '/protect', label: 'Protéger', icon: Lock }, { to: '/to-pdfa', label: 'Archiver PDF/A', icon: Archive }],
  '/redact':       [{ to: '/anonymize', label: 'Anonymiser', icon: UserX }, { to: '/protect', label: 'Protéger', icon: Lock }],
  '/anonymize':    [{ to: '/protect', label: 'Protéger', icon: Lock }, { to: '/to-pdfa', label: 'Archiver PDF/A', icon: Archive }],
  '/to-pdfa':      [{ to: '/sign', label: 'Signer', icon: PenTool }],

  // Generation
  '/qr':         [{ to: '/page-numbers', label: 'Numéroter', icon: Hash }, { to: '/protect', label: 'Protéger', icon: Lock }],
  '/barcode':    [{ to: '/page-numbers', label: 'Numéroter', icon: Hash }],
  '/cv':         [{ to: '/sign', label: 'Signer le CV', icon: PenTool }, { to: '/protect', label: 'Protéger', icon: Lock }, { to: '/compress', label: 'Compresser', icon: Archive }],

  // Texte & OCR
  '/extract-text': [],   // sortie .txt, pas chainable PDF
  '/ocr':          [{ to: '/redact', label: 'Caviarder du texte', icon: Highlighter }, { to: '/anonymize', label: 'Anonymiser', icon: UserX }, { to: '/sign', label: 'Signer', icon: PenTool }],
  '/create':       [{ to: '/sign', label: 'Signer', icon: PenTool }, { to: '/protect', label: 'Protéger', icon: Lock }, { to: '/watermark', label: 'Filigrane', icon: Droplets }],
}

export default function WorkflowSuggestions() {
  const { lastResult, consume, clear } = useWorkflow()
  const navigate = useNavigate()
  const location = useLocation()

  // Affiche uniquement si le resultat vient de la page courante
  // (sinon on l'a deja vu sur la page d'origine)
  if (!lastResult || lastResult.sourcePath !== location.pathname) return null

  // N'affiche que pour les fichiers PDF (les autres types ne sont pas chainables dans nos outils)
  const isChainablePdf = lastResult.filename?.toLowerCase().endsWith('.pdf')

  const suggestions = SUGGESTIONS[location.pathname] || []
  const filteredSuggestions = isChainablePdf ? suggestions : []
  if (filteredSuggestions.length === 0) return null

  function handleClickSuggestion(to) {
    // On consomme le resultat (sera utilise par DropZone de la page cible
    // pour pre-charger le fichier dans la zone d'upload).
    // On NE retire pas tout de suite — la page cible mountera et lira lastResult.
    navigate(to)
  }

  return (
    <AnimatePresence>
      <motion.div
        key="wf-suggest"
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0 }}
        transition={{ duration: 0.25 }}
        className="mt-8 rounded-2xl border border-brand-200 dark:border-brand-800/60 bg-gradient-to-br from-brand-50 to-accent-50 dark:from-brand-950/40 dark:to-accent-950/40 p-5 relative"
      >
        <button
          type="button"
          onClick={clear}
          className="absolute top-2 right-2 grid place-items-center h-7 w-7 rounded-md text-ink-400 hover:text-ink-700 hover:bg-white/50 dark:hover:bg-ink-800 transition-colors"
          aria-label="Fermer les suggestions"
        >
          <X size={14} />
        </button>
        <div className="flex items-center gap-2 mb-1.5">
          <Sparkles size={14} className="text-brand-600 dark:text-brand-400" />
          <p className="text-sm font-semibold text-ink-900 dark:text-ink-100">
            Et après ?
          </p>
        </div>
        <p className="text-xs text-ink-600 dark:text-ink-400 mb-4">
          Enchaînez avec un outil compatible — votre résultat sera automatiquement chargé.
        </p>
        <div className="flex flex-wrap gap-2">
          {filteredSuggestions.map(s => {
            const Icon = s.icon
            return (
              <button
                key={s.to}
                type="button"
                onClick={() => handleClickSuggestion(s.to)}
                className="inline-flex items-center gap-2 h-10 px-4 rounded-lg bg-white dark:bg-ink-900 border border-ink-200 dark:border-ink-700 text-sm font-semibold text-ink-800 dark:text-ink-100 hover:border-brand-400 hover:shadow-sm transition-all group"
              >
                <Icon size={16} className="text-brand-600 dark:text-brand-400" />
                {s.label}
                <ArrowRight size={14} className="text-ink-400 group-hover:translate-x-0.5 transition-transform" />
              </button>
            )
          })}
        </div>
      </motion.div>
    </AnimatePresence>
  )
}
