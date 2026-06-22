import {
  Archive, Droplets, Lock, Highlighter, UserX, ShieldCheck,
  Hash, Layers, FileText, ScanText, PenTool,
} from 'lucide-react'

/** Map sourcePath -> suggestions (routes v2.0). */
export const WORKFLOW_SUGGESTIONS = {
  '/merge': [
    { to: '/compress', label: 'Compresser', icon: Archive },
    { to: '/marking', label: 'Filigrane / tampon', icon: Droplets },
    { to: '/protect', label: 'Protéger', icon: Lock },
  ],
  '/split': [
    { to: '/merge', label: 'Refusionner', icon: Layers },
    { to: '/compress', label: 'Compresser', icon: Archive },
  ],
  '/select-pages': [
    { to: '/page-numbers', label: 'Numéroter', icon: Hash },
    { to: '/marking', label: 'Filigrane / tampon', icon: Droplets },
    { to: '/compress', label: 'Compresser', icon: Archive },
  ],
  '/rotate': [
    { to: '/compress', label: 'Compresser', icon: Archive },
    { to: '/marking', label: 'Filigrane / tampon', icon: Droplets },
  ],
  '/reorder': [
    { to: '/page-numbers', label: 'Renuméroter', icon: Hash },
    { to: '/compress', label: 'Compresser', icon: Archive },
  ],
  '/cover': [
    { to: '/marking', label: 'Filigrane / tampon', icon: Droplets },
    { to: '/compress', label: 'Compresser', icon: Archive },
  ],
  '/pdf-to-word':     [{ to: '/word-to-pdf', label: 'Re-convertir en PDF', icon: FileText }],
  '/pdf-to-excel':    [{ to: '/excel-to-pdf', label: 'Re-convertir en PDF', icon: FileText }],
  '/pdf-to-pptx':     [],
  '/pdf-to-markdown': [{ to: '/markdown-to-pdf', label: 'Re-convertir en PDF', icon: FileText }],
  '/convert':         [],
  '/word-to-pdf':     [
    { to: '/compress', label: 'Compresser', icon: Archive },
    { to: '/sign', label: 'Signer', icon: PenTool },
    { to: '/marking', label: 'Filigrane / tampon', icon: Droplets },
  ],
  '/excel-to-pdf':    [
    { to: '/compress', label: 'Compresser', icon: Archive },
    { to: '/sign', label: 'Signer', icon: PenTool },
  ],
  '/odt-to-pdf':      [
    { to: '/compress', label: 'Compresser', icon: Archive },
    { to: '/sign', label: 'Signer', icon: PenTool },
  ],
  '/markdown-to-pdf': [
    { to: '/sign', label: 'Signer', icon: PenTool },
    { to: '/page-numbers', label: 'Numéroter', icon: Hash },
  ],
  '/html-to-pdf':     [
    { to: '/sign', label: 'Signer', icon: PenTool },
    { to: '/page-numbers', label: 'Numéroter', icon: Hash },
  ],
  '/images-to-pdf':   [
    { to: '/compress', label: 'Compresser', icon: Archive },
    { to: '/ocr', label: 'OCR', icon: ScanText },
  ],
  '/scan': [
    { to: '/ocr', label: 'OCR du scan', icon: ScanText },
    { to: '/compress', label: 'Compresser', icon: Archive },
    { to: '/sign', label: 'Signer', icon: PenTool },
  ],
  '/marking': [
    { to: '/protect', label: 'Protéger', icon: Lock },
    { to: '/sign', label: 'Signer', icon: PenTool },
    { to: '/compress', label: 'Compresser', icon: Archive },
  ],
  '/page-numbers': [
    { to: '/compress', label: 'Compresser', icon: Archive },
    { to: '/marking', label: 'Filigrane / tampon', icon: Droplets },
  ],
  '/crop': [
    { to: '/compress', label: 'Compresser', icon: Archive },
    { to: '/page-numbers', label: 'Numéroter', icon: Hash },
  ],
  '/compress': [
    { to: '/sign', label: 'Signer', icon: PenTool },
    { to: '/protect', label: 'Protéger', icon: Lock },
    { to: '/marking', label: 'Filigrane / tampon', icon: Droplets },
  ],
  '/metadata': [],
  '/protect': [],
  '/unlock': [
    { to: '/compress', label: 'Compresser', icon: Archive },
    { to: '/sign', label: 'Resigner', icon: PenTool },
  ],
  '/sign': [
    { to: '/verify-signature', label: 'Vérifier la signature', icon: ShieldCheck },
    { to: '/protect', label: 'Protéger', icon: Lock },
  ],
  '/sign-image': [
    { to: '/protect', label: 'Protéger', icon: Lock },
    { to: '/compress', label: 'Compresser', icon: Archive },
  ],
  '/verify-signature': [
    { to: '/protect', label: 'Protéger', icon: Lock },
  ],
  '/redact': [
    { to: '/anonymize', label: 'Anonymiser', icon: UserX },
    { to: '/protect', label: 'Protéger', icon: Lock },
  ],
  '/anonymize': [
    { to: '/protect', label: 'Protéger', icon: Lock },
    { to: '/compress', label: 'Compresser', icon: Archive },
  ],
  '/code': [
    { to: '/page-numbers', label: 'Numéroter', icon: Hash },
    { to: '/protect', label: 'Protéger', icon: Lock },
  ],
  '/cv': [
    { to: '/sign', label: 'Signer le CV', icon: PenTool },
    { to: '/protect', label: 'Protéger', icon: Lock },
    { to: '/compress', label: 'Compresser', icon: Archive },
  ],
  '/extract-text': [],
  '/ocr': [
    { to: '/redact', label: 'Caviarder du texte', icon: Highlighter },
    { to: '/anonymize', label: 'Anonymiser', icon: UserX },
    { to: '/sign', label: 'Signer', icon: PenTool },
  ],
  '/compare': [],
}

export const SUGGESTIONS_SOURCE_PATHS = Object.keys(WORKFLOW_SUGGESTIONS)

export const SUGGESTIONS_TARGET_PATHS = [
  ...new Set(
    Object.values(WORKFLOW_SUGGESTIONS).flatMap(list => list.map(s => s.to))
  ),
]
