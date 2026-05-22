import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { motion } from 'framer-motion'
import ServiceCard from '../components/ServiceCard'
import { NoResultsIllustration } from '../components/Illustrations'
import { ping } from '../api/pdfApi'
import {
  Layers, ScanText, PenTool, FileText,
  Sparkles, ShieldCheck, Network, Cpu, ArrowRight,
  QrCode, Camera, Search, X, Star, Clock,
} from 'lucide-react'
import { SERVICES, CATEGORIES, getServiceByPath } from '../services'
import { useFavorites, useRecent, clearRecent } from '../hooks/useToolHistory'

const HIGHLIGHTS = [
  { icon: Network,     title: 'Architecture CORBA', text: 'Communication IIOP sécurisée entre la gateway et le serveur de traitement.' },
  { icon: Cpu,         title: 'PDFBox + Tess4J',    text: 'Pipeline robuste pour la manipulation, l’OCR et la signature numérique.' },
  { icon: ShieldCheck, title: 'Sécurité PKI',       text: 'Signatures PKCS#12, chiffrement AES, protection par mot de passe.' },
]

// Normalise un texte (sans accents ni casse) pour la recherche
function normalize(s) {
  return (s || '').toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '')
}

export default function Home() {
  const [status, setStatus] = useState(null)
  const [info, setInfo] = useState(null)
  const [search, setSearch] = useState('')
  const [category, setCategory] = useState('Tous')

  // Favoris epingles + outils recemment utilises (localStorage)
  const favorites = useFavorites()
  const recent    = useRecent(4)
  // Resolution des entrees (path -> service complet) ; on filtre les paths inconnus
  const favServices    = useMemo(() => favorites.map(f => getServiceByPath(f.path)).filter(Boolean), [favorites])
  const recentServices = useMemo(() => recent.map(r => getServiceByPath(r.path)).filter(Boolean), [recent])

  useEffect(() => {
    ping()
      .then(d => { setStatus(d.status); setInfo(d.server || null) })
      .catch(() => setStatus('ERROR'))
  }, [])

  // Compteurs par categorie pour les chips
  const countsByCategory = useMemo(() => {
    const m = { Tous: SERVICES.length }
    for (const s of SERVICES) m[s.category] = (m[s.category] || 0) + 1
    return m
  }, [])

  // Filtre combinant categorie + recherche fuzzy (titre + description + tags badge)
  const filtered = useMemo(() => {
    const q = normalize(search.trim())
    return SERVICES.filter(s => {
      if (category !== 'Tous' && s.category !== category) return false
      if (!q) return true
      const haystack = normalize(
        `${s.title} ${s.description} ${s.badge || ''} ${s.category} ${s.to}`
      )
      return haystack.includes(q)
    })
  }, [search, category])

  // Mise en avant des nouveautes uniquement quand on est sur "Tous" sans recherche
  const showNewSection = category === 'Tous' && !search.trim()
  const newServices = useMemo(
    () => SERVICES.filter(s => s.badge === 'Nouveau').slice(0, 8),
    []
  )

  // Si on est en mode "Tous" sans recherche, on retire les nouveautes de la
  // grille principale pour eviter le doublon visuel (elles sont deja mises
  // en avant dans la section Nouveautes plus haut).
  const newServiceUrls = useMemo(() => new Set(newServices.map(s => s.to)), [newServices])
  const mainGrid = useMemo(() => {
    if (!showNewSection) return filtered
    return filtered.filter(s => !newServiceUrls.has(s.to))
  }, [filtered, showNewSection, newServiceUrls])

  return (
    <>
      {/* ───────── Hero ───────── */}
      <section className="relative overflow-hidden">
        <div className="absolute inset-0 bg-radial-brand" />
        <div className="absolute inset-0 bg-grid-faint bg-[size:36px_36px] [mask-image:linear-gradient(180deg,white,transparent_70%)]" />

        {/* Orbes flottantes animees */}
        <motion.div
          aria-hidden="true"
          className="absolute -top-24 -left-24 h-72 w-72 rounded-full bg-gradient-to-br from-brand-400/40 to-accent-400/30 blur-3xl"
          animate={{ scale: [1, 1.15, 1], opacity: [0.5, 0.7, 0.5] }}
          transition={{ duration: 8, repeat: Infinity, ease: 'easeInOut' }}
        />
        <motion.div
          aria-hidden="true"
          className="absolute top-10 -right-32 h-80 w-80 rounded-full bg-gradient-to-br from-accent-400/40 to-rose-400/30 blur-3xl"
          animate={{ scale: [1, 1.1, 1], opacity: [0.4, 0.6, 0.4] }}
          transition={{ duration: 10, repeat: Infinity, ease: 'easeInOut', delay: 1 }}
        />

        {/* Icones flottantes en arriere-plan (decoratifs) */}
        <FloatingIcons />

        <div className="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 pt-14 pb-10 text-center">
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
            {SERVICES.length} outils essentiels — scanner caméra, OCR, signature,
            conversions multi-formats, QR codes, générateur de CV — propulsés par CORBA, JacORB et Apache PDFBox.
          </p>

          <div className="mt-7 flex flex-wrap items-center justify-center gap-3">
            <motion.div
              whileHover={{ scale: 1.03 }}
              whileTap={{ scale: 0.97 }}
              className="relative"
            >
              {/* Halo pulse autour du CTA principal */}
              <motion.div
                aria-hidden="true"
                className="absolute inset-0 rounded-lg bg-gradient-to-r from-brand-500 to-accent-500 blur-xl"
                animate={{ opacity: [0.4, 0.7, 0.4] }}
                transition={{ duration: 2.5, repeat: Infinity, ease: 'easeInOut' }}
              />
              <Link to="/scan" className="btn-primary relative" data-tour="scan-cta">
                <Camera size={16} /> Scanner un document
              </Link>
            </motion.div>
            <a href="#services" className="btn-secondary">
              Explorer les services <ArrowRight size={16} />
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
      <section className="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-4">
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

      {/* ───────── Favoris + Recents (apparait si non vide) ───────── */}
      {(favServices.length > 0 || recentServices.length > 0) && (
        <section className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-6 space-y-8">
          {favServices.length > 0 && (
            <PersonalSection
              title="Vos favoris"
              subtitle="Les outils que vous avez épinglés"
              icon={Star}
              tone="amber"
              services={favServices}
            />
          )}
          {recentServices.length > 0 && (
            <PersonalSection
              title="Récemment utilisés"
              subtitle="Vos derniers outils ouverts — accès rapide"
              icon={Clock}
              tone="brand"
              services={recentServices}
              action={
                <button
                  type="button"
                  onClick={clearRecent}
                  className="text-xs font-medium text-ink-500 hover:text-rose-600 dark:hover:text-rose-400 transition-colors"
                >
                  Vider l'historique
                </button>
              }
            />
          )}
        </section>
      )}

      {/* ───────── Services (recherche + filtre + grille) ───────── */}
      <section id="services" className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-10">
        <div className="flex flex-col sm:flex-row sm:items-end sm:justify-between gap-4 mb-5">
          <div>
            <span className="eyebrow">
              {filtered.length}{filtered.length !== SERVICES.length && ` / ${SERVICES.length}`} module(s) affiché(s)
            </span>
            <h2 className="mt-1 text-2xl sm:text-3xl font-bold font-display text-ink-900 dark:text-ink-100">
              Services PDF disponibles
            </h2>
          </div>
          {/* Barre de recherche */}
          <div className="relative sm:max-w-xs w-full" data-tour="search">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-ink-400 pointer-events-none" />
            <input
              type="search"
              value={search}
              onChange={e => setSearch(e.target.value)}
              placeholder="Rechercher un outil…"
              className="field pl-9 pr-9 h-10"
              aria-label="Rechercher un outil"
            />
            {search && (
              <button
                type="button"
                onClick={() => setSearch('')}
                className="absolute right-2 top-1/2 -translate-y-1/2 grid place-items-center h-6 w-6 rounded-md text-ink-400 hover:text-ink-700 hover:bg-ink-100 dark:hover:bg-ink-800 dark:hover:text-ink-200 transition-colors"
                aria-label="Effacer"
              >
                <X size={14} />
              </button>
            )}
          </div>
        </div>

        {/* Chips de categorie */}
        <div className="flex gap-1.5 overflow-x-auto pb-2 -mx-1 px-1 mb-6">
          {CATEGORIES.map(cat => {
            const active = cat === category
            const count = countsByCategory[cat] || 0
            return (
              <button
                key={cat}
                type="button"
                onClick={() => setCategory(cat)}
                className={`shrink-0 inline-flex items-center gap-1.5 h-9 px-3 rounded-full text-sm font-medium transition-all whitespace-nowrap ${
                  active
                    ? 'bg-ink-900 text-white dark:bg-white dark:text-ink-900 shadow-sm'
                    : 'bg-white text-ink-700 ring-1 ring-ink-200 hover:bg-ink-50 dark:bg-ink-900 dark:text-ink-200 dark:ring-ink-700 dark:hover:bg-ink-800'
                }`}
              >
                {cat}
                <span className={`text-[10px] font-bold tabular-nums rounded-full px-1.5 ${
                  active
                    ? 'bg-white/20 text-white dark:bg-ink-900/20 dark:text-ink-900'
                    : 'bg-ink-100 text-ink-500 dark:bg-ink-800 dark:text-ink-400'
                }`}>
                  {count}
                </span>
              </button>
            )
          })}
        </div>

        {/* Section "Nouveautes" en tete uniquement sur "Tous" sans recherche.
            Pas d'animation height : on garde un fade simple pour eviter les sauts
            de layout qui font "superposer" visuellement les deux grilles. */}
        {showNewSection && newServices.length > 0 && (
          <motion.div
            initial={{ opacity: 0, y: 6 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.25 }}
            className="mb-10 pb-2"
          >
            <div className="flex items-center gap-2 mb-3">
              <span className="inline-flex items-center gap-1 rounded-full bg-gradient-to-r from-brand-600 to-accent-500 text-white px-2 py-0.5 text-[10px] font-bold uppercase tracking-wider">
                <Sparkles size={10} /> Nouveautés
              </span>
              <p className="text-xs text-ink-500">Les derniers outils ajoutés</p>
            </div>
            <ServiceGrid services={newServices} idPrefix="new" />
            <div className="mt-8 h-px bg-gradient-to-r from-transparent via-ink-200 to-transparent dark:via-ink-700" />
          </motion.div>
        )}

        {/* Grille principale (sans les Nouveau quand on les a deja affiches en haut) */}
        <motion.div
          key={`${category}-${search}`}
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ duration: 0.15 }}
        >
          {mainGrid.length > 0
            ? (
              <>
                {showNewSection && (
                  <p className="text-xs font-semibold uppercase tracking-wider text-ink-400 dark:text-ink-500 mb-3">
                    Tous les autres outils
                  </p>
                )}
                <ServiceGrid services={mainGrid} idPrefix={`${category}-${search}`} />
              </>
            )
            : filtered.length === 0
              ? <EmptyResults query={search} onReset={() => { setSearch(''); setCategory('Tous') }} />
              : null /* tous les resultats sont deja en section Nouveautes */}
        </motion.div>
      </section>

      {/* ───────── Bandeau stack ───────── */}
      <section className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 pb-16">
        <div className="card p-6 sm:p-8 flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="font-semibold text-ink-900 dark:text-ink-100">Stack technique</p>
            <p className="text-sm text-ink-500 dark:text-ink-400">React 18 · Vite · Tailwind · Spring Boot 3 · JacORB · PDFBox · Tess4J · OpenCV.js · Docker</p>
          </div>
          <Link to="/create" className="btn-secondary">
            Créer un PDF à partir de texte
          </Link>
        </div>
      </section>
    </>
  )
}

/**
 * Icones flottantes en arriere-plan du hero — purement decoratif.
 * Chaque icone derive lentement avec une amplitude differente pour
 * eviter l'effet "boucle synchrone".
 */
function FloatingIcons() {
  const items = [
    { Icon: Layers,     style: 'top-[12%] left-[8%]',   delay: 0,   color: 'text-brand-500/40' },
    { Icon: ScanText,   style: 'top-[20%] right-[10%]', delay: 1.2, color: 'text-accent-500/40' },
    { Icon: QrCode,     style: 'top-[55%] left-[12%]',  delay: 0.6, color: 'text-violet-500/40' },
    { Icon: PenTool,    style: 'top-[60%] right-[14%]', delay: 1.8, color: 'text-rose-500/40' },
    { Icon: FileText,   style: 'top-[8%]  left-[42%]',  delay: 2.4, color: 'text-emerald-500/40' },
    { Icon: Sparkles,   style: 'top-[78%] left-[44%]',  delay: 3.0, color: 'text-amber-500/40' },
  ]
  return (
    <div className="absolute inset-0 pointer-events-none hidden sm:block" aria-hidden="true">
      {items.map(({ Icon, style, delay, color }, i) => (
        <motion.div
          key={i}
          className={`absolute ${style} ${color}`}
          animate={{
            y: [0, -12, 0],
            rotate: [0, i % 2 === 0 ? 5 : -5, 0],
          }}
          transition={{
            duration: 6 + (i % 3),
            repeat: Infinity,
            ease: 'easeInOut',
            delay,
          }}
        >
          <Icon size={28} strokeWidth={1.6} />
        </motion.div>
      ))}
    </div>
  )
}

/**
 * Grille de ServiceCard.
 * - auto-rows-fr force chaque rangee a la meme hauteur (toutes les cartes
 *   d'une meme rangee ont la meme taille, evite les decalages perçus comme "superposition")
 * - motion.div wrapping a h-full pour que la carte enfant remplisse vraiment l'espace
 * - stagger purement en opacite (pas de translateY) pour eviter les decalages
 *   visuels pendant l'animation
 */
function ServiceGrid({ services, idPrefix }) {
  return (
    <motion.div
      className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 auto-rows-fr"
      initial="hidden"
      animate="visible"
      variants={{
        hidden: {},
        visible: { transition: { staggerChildren: 0.02 } },
      }}
    >
      {services.map(s => (
        <motion.div
          key={`${idPrefix}-${s.to}`}
          className="h-full"
          variants={{
            hidden: { opacity: 0 },
            visible: { opacity: 1, transition: { duration: 0.2, ease: 'easeOut' } },
          }}
        >
          <ServiceCard {...s} />
        </motion.div>
      ))}
    </motion.div>
  )
}

/**
 * Section "Favoris" ou "Recemment utilises".
 * Bordure ciselee, header avec icone + sous-titre + action eventuelle a droite.
 */
function PersonalSection({ title, subtitle, icon: Icon, tone, services, action }) {
  const toneCls = {
    amber: 'text-amber-600 dark:text-amber-400 bg-amber-50 dark:bg-amber-950/30 ring-amber-200 dark:ring-amber-800',
    brand: 'text-brand-600 dark:text-brand-400 bg-brand-50 dark:bg-brand-950/40 ring-brand-200 dark:ring-brand-800',
  }[tone] || ''
  return (
    <div>
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-3">
          <div className={`grid h-9 w-9 place-items-center rounded-lg ring-1 ${toneCls}`}>
            <Icon size={16} />
          </div>
          <div>
            <p className="text-sm font-bold text-ink-900 dark:text-ink-100">{title}</p>
            <p className="text-xs text-ink-500 dark:text-ink-400">{subtitle}</p>
          </div>
        </div>
        {action}
      </div>
      <ServiceGrid services={services} idPrefix={`personal-${title}`} />
    </div>
  )
}

function EmptyResults({ query, onReset }) {
  return (
    <div className="text-center py-12">
      <NoResultsIllustration className="mx-auto w-44 sm:w-52 text-ink-400 dark:text-ink-500" />
      <p className="mt-2 text-base font-semibold text-ink-800 dark:text-ink-100">
        Aucun outil ne correspond
      </p>
      <p className="mt-1 text-sm text-ink-500 dark:text-ink-400">
        {query
          ? <>Aucun résultat pour <strong className="text-ink-700 dark:text-ink-200">"{query}"</strong>.</>
          : 'Essayez une autre catégorie ou ajustez votre recherche.'}
      </p>
      <button
        type="button"
        onClick={onReset}
        className="mt-4 btn-ghost h-9 px-4 text-sm"
      >
        Réinitialiser les filtres
      </button>
    </div>
  )
}
