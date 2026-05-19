import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import ServiceCard from '../components/ServiceCard'
import { ping } from '../api/pdfApi'
import {
  Layers, Scissors, FileMinus, FileSearch, Archive, RotateCw,
  Droplets, Lock, Image as ImageIcon, FileText, ScanText, PenTool,
  Info, Plus, Sparkles, ShieldCheck, Network, Cpu, ArrowRight,
  FileType2, Sheet, Images,
} from 'lucide-react'

const SERVICES = [
  { icon: Layers,     title: 'Fusion',          description: 'Assemblez plusieurs PDF en un document unique.',         to: '/merge',         tone: 'brand'   },
  { icon: Scissors,   title: 'Découpage',       description: 'Découpez un PDF en plusieurs documents par intervalles.', to: '/split',         tone: 'cyan'    },
  { icon: FileSearch, title: 'Extraction',      description: 'Extrayez précisément les pages souhaitées.',              to: '/extract-pages', tone: 'violet'  },
  { icon: FileMinus,  title: 'Suppression',     description: 'Retirez les pages inutiles d’un document.',               to: '/delete-pages',  tone: 'rose'    },
  { icon: Archive,    title: 'Compression',     description: 'Réduisez la taille de vos PDF sans sacrifier la qualité.', to: '/compress',     tone: 'emerald' },
  { icon: RotateCw,   title: 'Rotation',        description: 'Pivotez les pages en 90°, 180° ou 270°.',                 to: '/rotate',        tone: 'amber'   },
  { icon: FileType2,  title: 'PDF → Word',      description: 'Convertissez un PDF en document Word (DOCX).',            to: '/pdf-to-word',   tone: 'violet', badge: 'Nouveau' },
  { icon: Sheet,      title: 'PDF → Excel',     description: 'Convertissez un PDF en classeur Excel (XLSX).',           to: '/pdf-to-excel',  tone: 'emerald', badge: 'Nouveau' },
  { icon: FileText,   title: 'Word → PDF',      description: 'Convertissez un document Word (DOCX) en PDF.',            to: '/word-to-pdf',   tone: 'brand',  badge: 'Nouveau' },
  { icon: Images,     title: 'Images → PDF',    description: 'Assemblez JPG/PNG en un seul PDF (une page par image).',  to: '/images-to-pdf', tone: 'amber',  badge: 'Nouveau' },
  { icon: ImageIcon,  title: 'PDF → Images',    description: 'Convertissez chaque page en PNG, JPEG ou TIFF.',          to: '/convert',       tone: 'brand'   },
  { icon: Droplets,   title: 'Filigrane',       description: 'Ajoutez un filigrane texte sur chaque page.',             to: '/watermark',     tone: 'cyan'    },
  { icon: Lock,       title: 'Protection',      description: 'Verrouillez vos PDF avec un mot de passe.',               to: '/protect',       tone: 'rose'    },
  { icon: PenTool,    title: 'Signature',       description: 'Signez vos PDF via un certificat PKCS#12.',               to: '/sign',          tone: 'violet', badge: 'PKI' },
  { icon: FileText,   title: 'Extraction texte',description: 'Extrayez le contenu textuel brut de vos PDF.',            to: '/extract-text',  tone: 'emerald' },
  { icon: ScanText,   title: 'OCR',             description: 'Reconnaissance optique pour PDF scannés.',                to: '/ocr',           tone: 'amber',  badge: 'Tess4J' },
  { icon: Info,       title: 'Métadonnées',     description: 'Inspectez auteur, titre, dates et plus encore.',          to: '/metadata',      tone: 'cyan'    },
  { icon: Plus,       title: 'Création',        description: 'Générez un PDF à partir d’un texte brut.',                to: '/create',        tone: 'brand'   },
]

const HIGHLIGHTS = [
  { icon: Network,     title: 'Architecture CORBA',     text: 'Communication IIOP sécurisée entre la gateway et le serveur de traitement.' },
  { icon: Cpu,         title: 'PDFBox + Tess4J',        text: 'Pipeline robuste pour la manipulation, l’OCR et la signature numérique.' },
  { icon: ShieldCheck, title: 'Sécurité PKI',           text: 'Signatures PKCS#12, chiffrement AES, protection par mot de passe.' },
]

export default function Home() {
  const [status, setStatus] = useState(null)
  const [info,   setInfo]   = useState(null)

  useEffect(() => {
    ping()
      .then(d => { setStatus(d.status); setInfo(d.server || null) })
      .catch(() => setStatus('ERROR'))
  }, [])

  return (
    <>
      {/* ───────── Hero ───────── */}
      <section className="relative overflow-hidden">
        <div className="absolute inset-0 bg-radial-brand" />
        <div className="absolute inset-0 bg-grid-faint bg-[size:36px_36px] [mask-image:linear-gradient(180deg,white,transparent_70%)]" />
        <div className="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 pt-16 pb-12 text-center">
          <span className="eyebrow mb-5">
            <Sparkles size={14} /> Plateforme PDF distribuée
          </span>
          <h1 className="text-4xl sm:text-5xl lg:text-6xl font-extrabold font-display text-ink-900 dark:text-ink-100 tracking-tight leading-[1.05]">
            Traitez vos PDF, <br className="hidden sm:block"/>
            <span className="bg-gradient-to-r from-brand-600 via-brand-500 to-accent-500 dark:from-brand-400 dark:via-brand-300 dark:to-accent-400 bg-clip-text text-transparent">
              à l’échelle entreprise.
            </span>
          </h1>
          <p className="mt-5 max-w-2xl mx-auto text-lg text-ink-500 dark:text-ink-400 leading-relaxed">
            Suite complète de 18 services PDF — fusion, OCR, signature numérique, conversion Word/Excel et plus — propulsée par CORBA, JacORB et Apache PDFBox.
          </p>

          <div className="mt-7 flex flex-wrap items-center justify-center gap-3">
            <Link to="/merge" className="btn-primary">
              Commencer <ArrowRight size={16} />
            </Link>
            <a href="#services" className="btn-secondary">
              Explorer les services
            </a>
          </div>

          <div className="mt-8 inline-flex items-center gap-2 text-xs">
            <span className={`relative flex h-2.5 w-2.5 rounded-full ${
              status === 'OK' ? 'bg-emerald-500' : status === 'ERROR' ? 'bg-rose-500' : 'bg-amber-500'
            }`}>
              {status === 'OK' && <span className="absolute inset-0 rounded-full bg-emerald-500 animate-pulse-dot" />}
            </span>
            <span className="text-ink-500 dark:text-ink-400 font-mono">
              {status === 'OK' ? 'Serveur CORBA en ligne' : status === 'ERROR' ? 'Serveur indisponible' : 'Vérification…'}
              {info && <span className="text-ink-400 dark:text-ink-500"> · {info}</span>}
            </span>
          </div>
        </div>
      </section>

      {/* ───────── Highlights ───────── */}
      <section className="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-6">
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {HIGHLIGHTS.map(({ icon: Icon, title, text }) => (
            <div key={title} className="card p-5 flex gap-4 items-start animate-fade-in">
              <div className="shrink-0 grid h-11 w-11 place-items-center rounded-xl bg-brand-50 text-brand-700 ring-1 ring-brand-100 dark:bg-brand-950/50 dark:text-brand-300 dark:ring-brand-800">
                <Icon size={20} />
              </div>
              <div>
                <p className="font-semibold text-ink-900 dark:text-ink-100">{title}</p>
                <p className="text-sm text-ink-500 dark:text-ink-400 leading-relaxed">{text}</p>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* ───────── Services ───────── */}
      <section id="services" className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-12">
        <div className="flex items-end justify-between mb-6">
          <div>
            <span className="eyebrow">18 modules de traitement</span>
            <h2 className="mt-1 text-2xl sm:text-3xl font-bold font-display text-ink-900 dark:text-ink-100">
              Services PDF disponibles
            </h2>
          </div>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {SERVICES.map(s => <ServiceCard key={s.to} {...s} />)}
        </div>
      </section>

      {/* ───────── Bandeau stack ───────── */}
      <section className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 pb-16">
        <div className="card p-6 sm:p-8 flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="font-semibold text-ink-900 dark:text-ink-100">Stack technique</p>
            <p className="text-sm text-ink-500 dark:text-ink-400">React 18 · Vite · Tailwind · Spring Boot 3 · JacORB · PDFBox · Tess4J · Docker</p>
          </div>
          <Link to="/create" className="btn-secondary">
            Créer un PDF à partir de texte
          </Link>
        </div>
      </section>
    </>
  )
}
