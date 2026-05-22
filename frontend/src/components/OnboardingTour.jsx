import { useEffect, useLayoutEffect, useState } from 'react'
import { AnimatePresence, motion } from 'framer-motion'
import { X, ArrowRight, Check, Sparkles, Search, Camera, Command } from 'lucide-react'

/**
 * Tour onboarding interactif a la 1ere visite.
 *
 * Mecanisme :
 *  - flag `corba_pdf_onboarded` en localStorage ; si present -> ne s'affiche pas
 *  - une suite d'etapes : certaines pointent un selecteur DOM (spotlight + tooltip
 *    pres de l'element), d'autres sont centrees (welcome, fin)
 *  - le spotlight = element a la position du target avec un enorme box-shadow
 *    qui assombrit tout le reste de l'ecran
 *  - le tooltip est positionne automatiquement au-dessus ou en dessous du target
 *  - le user peut Suivant / Passer ; au bout, le flag est pose
 */

const STORAGE_KEY = 'corba_pdf_onboarded'

const STEPS = [
  {
    id: 'welcome',
    target: null,
    icon: Sparkles,
    title: 'Bienvenue sur CORBA PDF Suite',
    body: '43 outils pour traiter vos documents PDF. On vous fait une visite éclair en 4 étapes.',
  },
  {
    id: 'search',
    target: '[data-tour="search"]',
    icon: Search,
    title: 'Trouvez un outil en 2 secondes',
    body: 'Tapez le nom (ex: « compress », « ocr », « scan ») ou choisissez une catégorie. La recherche est insensible aux accents.',
    placement: 'bottom',
  },
  {
    id: 'scanner',
    target: '[data-tour="scan-cta"]',
    icon: Camera,
    title: 'Scanner caméra',
    body: 'Capturez vos documents directement depuis votre appareil avec détection auto des bords. C\'est la feature flagship.',
    placement: 'bottom',
  },
  {
    id: 'cmdk',
    target: null,
    icon: Command,
    title: 'Astuce de pro',
    body: 'Appuyez sur Ctrl + K (Cmd + K sur Mac) à tout moment pour ouvrir la palette de commandes et naviguer ultra-vite.',
  },
]

function dismissTour() {
  try { localStorage.setItem(STORAGE_KEY, '1') } catch {}
}

export function shouldShowTour() {
  try { return !localStorage.getItem(STORAGE_KEY) } catch { return false }
}

/** Hook utilitaire si on veut declencher le tour manuellement plus tard. */
export function resetTour() {
  try { localStorage.removeItem(STORAGE_KEY) } catch {}
  window.location.reload()
}

export default function OnboardingTour() {
  const [active, setActive] = useState(false)
  const [step, setStep] = useState(0)
  const [rect, setRect] = useState(null)  // bounding rect du target courant

  // Au mount : verifie le flag, demarre apres petit delay (laisse la page se rendre)
  useEffect(() => {
    if (!shouldShowTour()) return
    const t = setTimeout(() => setActive(true), 800)
    return () => clearTimeout(t)
  }, [])

  // Recalcule la position du spotlight quand le step change ou au resize
  useLayoutEffect(() => {
    if (!active) return
    const current = STEPS[step]
    if (!current?.target) { setRect(null); return }

    const compute = () => {
      const el = document.querySelector(current.target)
      if (!el) { setRect(null); return }
      const r = el.getBoundingClientRect()
      // Scroll dans le viewport si necessaire
      if (r.top < 80 || r.bottom > window.innerHeight - 200) {
        el.scrollIntoView({ behavior: 'smooth', block: 'center' })
      }
      // Recalcule apres le scroll
      requestAnimationFrame(() => {
        const r2 = el.getBoundingClientRect()
        setRect(r2)
      })
    }
    compute()
    window.addEventListener('resize', compute)
    window.addEventListener('scroll', compute, true)
    return () => {
      window.removeEventListener('resize', compute)
      window.removeEventListener('scroll', compute, true)
    }
  }, [active, step])

  // Empeche le scroll body pendant le tour
  useEffect(() => {
    if (!active) return
    const prev = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => { document.body.style.overflow = prev }
  }, [active])

  function next() {
    if (step >= STEPS.length - 1) finish()
    else setStep(s => s + 1)
  }
  function finish() {
    dismissTour()
    setActive(false)
  }

  if (!active) return null

  const current = STEPS[step]
  const Icon = current.icon
  const total = STEPS.length

  // Position de la tooltip
  // - si pas de target : centree (welcome / fin)
  // - si target en haut : tooltip en dessous, sinon au dessus
  let tooltipStyle = {}
  let tooltipPlacement = 'center'
  if (rect) {
    const PAD = 12
    const TOOLTIP_W = 360
    const placement = current.placement || (rect.top < window.innerHeight / 2 ? 'bottom' : 'top')
    tooltipPlacement = placement
    const x = Math.max(16, Math.min(window.innerWidth - TOOLTIP_W - 16, rect.left + rect.width / 2 - TOOLTIP_W / 2))
    if (placement === 'bottom') {
      tooltipStyle = { left: x, top: rect.bottom + PAD, width: TOOLTIP_W }
    } else {
      tooltipStyle = { left: x, top: rect.top - PAD, width: TOOLTIP_W, transform: 'translateY(-100%)' }
    }
  }

  return (
    <AnimatePresence>
      <motion.div
        key="tour-overlay"
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        exit={{ opacity: 0 }}
        transition={{ duration: 0.25 }}
        className="fixed inset-0 z-[60] pointer-events-auto"
        role="dialog"
        aria-modal="true"
        aria-label={`Onboarding etape ${step + 1} sur ${total}`}
      >
        {/* Backdrop sombre ou spotlight (selon presence target) */}
        {rect ? (
          <motion.div
            key={`spotlight-${step}`}
            initial={false}
            animate={{
              top: rect.top - 8,
              left: rect.left - 8,
              width: rect.width + 16,
              height: rect.height + 16,
            }}
            transition={{ duration: 0.3, ease: 'easeOut' }}
            className="absolute rounded-xl pointer-events-none"
            style={{
              boxShadow: '0 0 0 9999px rgba(15, 23, 42, 0.7), 0 0 0 4px rgba(99, 102, 241, 0.6)',
            }}
          />
        ) : (
          <div className="absolute inset-0 bg-ink-900/70 backdrop-blur-sm" />
        )}

        {/* Tooltip */}
        <motion.div
          key={`tooltip-${step}`}
          initial={{ opacity: 0, scale: 0.96, y: 6 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.97 }}
          transition={{ duration: 0.2, ease: 'easeOut' }}
          className={
            rect
              ? 'absolute rounded-2xl bg-white dark:bg-ink-900 shadow-2xl ring-1 ring-ink-200 dark:ring-ink-700 p-5'
              : 'absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[min(420px,calc(100vw-32px))] rounded-2xl bg-white dark:bg-ink-900 shadow-2xl ring-1 ring-ink-200 dark:ring-ink-700 p-6'
          }
          style={rect ? tooltipStyle : undefined}
        >
          <button
            type="button"
            onClick={finish}
            className="absolute top-2 right-2 grid place-items-center h-7 w-7 rounded-md text-ink-400 hover:text-ink-700 hover:bg-ink-100 dark:hover:bg-ink-800 transition-colors"
            aria-label="Passer le tour"
          >
            <X size={14} />
          </button>

          <div className="flex items-start gap-3 mb-3">
            <div className="shrink-0 grid h-10 w-10 place-items-center rounded-xl bg-gradient-to-br from-brand-500 to-accent-500 text-white shadow-glow">
              <Icon size={18} />
            </div>
            <div className="min-w-0 pr-6">
              <p className="text-[10px] font-bold uppercase tracking-wider text-brand-600 dark:text-brand-400">
                Étape {step + 1} / {total}
              </p>
              <h3 className="mt-0.5 text-base font-bold font-display text-ink-900 dark:text-ink-100">
                {current.title}
              </h3>
            </div>
          </div>

          <p className="text-sm text-ink-600 dark:text-ink-300 leading-relaxed">
            {current.body}
          </p>

          {/* Indicateurs + actions */}
          <div className="mt-5 flex items-center justify-between gap-3">
            <div className="flex gap-1">
              {STEPS.map((_, i) => (
                <span
                  key={i}
                  className={`h-1.5 rounded-full transition-all ${
                    i === step
                      ? 'w-6 bg-brand-600'
                      : i < step
                        ? 'w-1.5 bg-brand-300 dark:bg-brand-700'
                        : 'w-1.5 bg-ink-200 dark:bg-ink-700'
                  }`}
                />
              ))}
            </div>
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={finish}
                className="text-xs font-medium text-ink-500 hover:text-ink-700 dark:text-ink-400 dark:hover:text-ink-200 transition-colors"
              >
                Passer
              </button>
              <button
                type="button"
                onClick={next}
                className="inline-flex items-center gap-1.5 h-9 px-4 rounded-lg bg-brand-600 hover:bg-brand-700 text-white text-sm font-semibold transition-colors"
              >
                {step === STEPS.length - 1 ? (
                  <><Check size={14} /> Terminé</>
                ) : (
                  <>Suivant <ArrowRight size={14} /></>
                )}
              </button>
            </div>
          </div>
        </motion.div>
      </motion.div>
    </AnimatePresence>
  )
}
