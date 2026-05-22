import {
  Layers, Scissors, FileSearch, Archive, RotateCw,
  Droplets, Lock, Image as ImageIcon, FileText, ScanText, PenTool,
  Info,
  FileType2, Sheet, Images,
  Hash, Crop, BookOpen, Shuffle,
  UserX, PenLine, Highlighter,
  Presentation, Code2, FileCode2, Globe,
  Unlock, ShieldCheck, GitCompareArrows, QrCode, UserCircle2,
  Camera,
} from 'lucide-react'

// Categories utilisees pour le filtre Home / sections Navbar
export const CATEGORIES = [
  'Tous',
  'Organisation',
  'Conversion depuis PDF',
  'Convertir en PDF',
  'Édition',
  'Sécurité',
  'Analyse',
  'Génération',
  'Texte & OCR',
]

export const SERVICES = [
  // ─── Organisation ───
  { icon: Layers,        title: 'Fusion',           description: 'Assemblez plusieurs PDF en un document unique.',                  to: '/merge',         tone: 'brand',   category: 'Organisation' },
  { icon: Scissors,      title: 'Découpage',        description: 'Découpez un PDF en plusieurs documents par intervalles.',          to: '/split',         tone: 'cyan',    category: 'Organisation' },
  { icon: FileSearch,    title: 'Sélection de pages', description: 'Gardez ou supprimez précisément les pages choisies.',           to: '/select-pages',  tone: 'violet',  category: 'Organisation' },
  { icon: RotateCw,      title: 'Rotation',         description: 'Pivotez les pages en 90°, 180° ou 270°.',                          to: '/rotate',        tone: 'amber',   category: 'Organisation' },
  { icon: Shuffle,       title: 'Réorganisation',   description: 'Glissez-déposez les pages pour les remettre dans l’ordre.',        to: '/reorder',       tone: 'violet',  category: 'Organisation' },
  { icon: BookOpen,      title: 'Page de garde',    description: 'Ajoutez une image en couverture du document.',                     to: '/cover',         tone: 'rose',    category: 'Organisation' },

  // ─── Conversion depuis PDF ───
  { icon: FileType2,        title: 'PDF → Word',      description: 'Convertissez un PDF en document Word (DOCX).',         to: '/pdf-to-word',     tone: 'violet',  badge: 'Nouveau', category: 'Conversion depuis PDF' },
  { icon: Sheet,            title: 'PDF → Excel',     description: 'Convertissez un PDF en classeur Excel (XLSX).',        to: '/pdf-to-excel',    tone: 'emerald', badge: 'Nouveau', category: 'Conversion depuis PDF' },
  { icon: Presentation,     title: 'PDF → PowerPoint', description: 'Conversion en présentation PPTX, une slide par page.', to: '/pdf-to-pptx',    tone: 'rose',    badge: 'Nouveau', category: 'Conversion depuis PDF' },
  { icon: Code2,            title: 'PDF → Markdown',  description: 'Extrait le texte au format Markdown (titres, listes).', to: '/pdf-to-markdown', tone: 'violet',  badge: 'Nouveau', category: 'Conversion depuis PDF' },
  { icon: ImageIcon,        title: 'PDF → Images',    description: 'Convertissez chaque page en PNG, JPEG ou TIFF.',       to: '/convert',         tone: 'brand',                       category: 'Conversion depuis PDF' },

  // ─── Convertir en PDF ───
  { icon: Camera,    title: 'Scanner caméra', description: 'Capturez plusieurs photos depuis la caméra et assemblez-les en PDF.', to: '/scan',           tone: 'brand',  badge: 'Nouveau', category: 'Convertir en PDF' },
  { icon: FileText,  title: 'Word → PDF',    description: 'Convertissez un document Word (DOCX) en PDF.',                       to: '/word-to-pdf',     tone: 'brand',  badge: 'Nouveau', category: 'Convertir en PDF' },
  { icon: Sheet,     title: 'Excel → PDF',   description: 'Convertit un classeur XLSX en PDF (une grille par feuille).',          to: '/excel-to-pdf',    tone: 'emerald', badge: 'Nouveau', category: 'Convertir en PDF' },
  { icon: FileText,  title: 'ODT → PDF',     description: 'Convertit un document OpenDocument (LibreOffice/OpenOffice) en PDF.',  to: '/odt-to-pdf',      tone: 'amber',  badge: 'Nouveau', category: 'Convertir en PDF' },
  { icon: FileCode2, title: 'Markdown → PDF', description: 'Génère un PDF stylé à partir de Markdown (titres, code, citations).', to: '/markdown-to-pdf', tone: 'cyan',   badge: 'Nouveau', category: 'Convertir en PDF' },
  { icon: Globe,     title: 'HTML → PDF',    description: 'Convertit du HTML (h1-h6, p, listes, blockquote, code) en PDF.',       to: '/html-to-pdf',     tone: 'brand',  badge: 'Nouveau', category: 'Convertir en PDF' },
  { icon: Images,    title: 'Images → PDF',  description: 'Assemblez JPG/PNG en un seul PDF (une page par image).',              to: '/images-to-pdf',   tone: 'amber',  badge: 'Nouveau', category: 'Convertir en PDF' },

  // ─── Édition ───
  { icon: Droplets,  title: 'Marquage du document', description: 'Filigrane diagonal sur toutes les pages ou tampon coloré sur une page.', to: '/marking',      tone: 'cyan',    category: 'Édition' },
  { icon: Hash,      title: 'Numérotation',         description: 'Ajoutez des numéros de page (position, format, taille).',                 to: '/page-numbers',  tone: 'emerald', category: 'Édition' },
  { icon: Crop,      title: 'Recadrage',            description: 'Retirez visuellement les marges autour du contenu.',                      to: '/crop',          tone: 'amber',   category: 'Édition' },
  { icon: Archive,   title: 'Compression',          description: 'Allégez vos PDF, avec option d\'archivage PDF/A.',                        to: '/compress',      tone: 'emerald', category: 'Édition' },
  { icon: Info,      title: 'Métadonnées',          description: 'Inspectez auteur, titre, dates et plus encore.',                          to: '/metadata',      tone: 'cyan',    category: 'Édition' },

  // ─── Sécurité ───
  { icon: Lock,        title: 'Protection',           description: 'Verrouillez vos PDF avec un mot de passe.',           to: '/protect',          tone: 'rose',                  category: 'Sécurité' },
  { icon: Unlock,      title: 'Déverrouillage',       description: 'Retire la protection par mot de passe d\'un PDF.',    to: '/unlock',           tone: 'brand',                 category: 'Sécurité' },
  { icon: PenTool,     title: 'Signature PKI',        description: 'Signez vos PDF via un certificat PKCS#12.',           to: '/sign',             tone: 'violet', badge: 'PKI', category: 'Sécurité' },
  { icon: PenLine,     title: 'Signature manuscrite', description: 'Dessinez votre signature ou importez une image.',     to: '/sign-image',       tone: 'violet',                category: 'Sécurité' },
  { icon: ShieldCheck, title: 'Vérifier signature',   description: 'Liste et valide les signatures numériques d\'un PDF.', to: '/verify-signature', tone: 'emerald',               category: 'Sécurité' },
  { icon: Highlighter, title: 'Caviardage',           description: 'Masque les noms, numéros et termes confidentiels.',   to: '/redact',           tone: 'amber',                 category: 'Sécurité' },
  { icon: UserX,       title: 'Anonymisation',        description: 'Retire toutes les métadonnées du document en un clic.', to: '/anonymize',      tone: 'emerald',               category: 'Sécurité' },

  // ─── Analyse ───
  { icon: GitCompareArrows, title: 'Comparer 2 PDF', description: 'Diff textuel page par page, lignes ajoutées et retirées.', to: '/compare', tone: 'rose', category: 'Analyse' },

  // ─── Génération ───
  { icon: QrCode,      title: 'Code-barres / QR', description: 'Génère un QR code ou un code-barres (Code 128, EAN, PDF417…) et l\'appose sur une page.', to: '/code', tone: 'brand',   category: 'Génération' },
  { icon: UserCircle2, title: 'Générateur de CV', description: 'Remplissez le formulaire, obtenez un CV PDF mis en page.',                                to: '/cv',   tone: 'emerald', category: 'Génération' },

  // ─── Texte & OCR ───
  { icon: FileText, title: 'Extraction texte', description: 'Extrayez le contenu textuel brut de vos PDF.', to: '/extract-text', tone: 'emerald',                       category: 'Texte & OCR' },
  { icon: ScanText, title: 'OCR',              description: 'Reconnaissance optique pour PDF scannés.',    to: '/ocr',          tone: 'amber',  badge: 'Tess4J',        category: 'Texte & OCR' },
]

// Map path -> service entry (pour lookup rapide)
const _byPath = new Map(SERVICES.map(s => [s.to, s]))
export function getServiceByPath(path) {
  return _byPath.get(path) || null
}
