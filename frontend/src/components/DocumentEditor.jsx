import { useEffect, useRef, useState } from 'react'
import { X, Check, RotateCcw, Palette, Sun, Contrast } from 'lucide-react'
import { motion } from 'framer-motion'

/**
 * Modal d'edition des coins detectes par OpenCV.
 *
 * Props :
 *   imgUrl       : URL (blob:) de l'image source
 *   imgWidth     : largeur naturelle de l'image (px)
 *   imgHeight    : hauteur naturelle
 *   initialCorners : [{x,y}, ...] (en coords image, 4 points dans l'ordre tl, tr, br, bl)
 *   onCancel     : annule, ferme le modal
 *   onValidate   : recoit (corners, filterMode) — applique le warp et ajoute
 */
const FILTERS = [
  { id: 'color',     label: 'Couleur',         icon: Palette  },
  { id: 'grayscale', label: 'Niveaux de gris', icon: Sun      },
  { id: 'scan',      label: 'Scan N&B',        icon: Contrast },
]

export default function DocumentEditor({ imgUrl, imgWidth, imgHeight, initialCorners, onCancel, onValidate }) {
  const [corners, setCorners] = useState(initialCorners)
  const [filter, setFilter] = useState('color')
  const [dragIdx, setDragIdx] = useState(null)
  const svgRef = useRef(null)

  // Reset corners si initialCorners change (rare mais robuste)
  useEffect(() => { setCorners(initialCorners) }, [initialCorners])

  // Echap pour fermer
  useEffect(() => {
    const onKey = (e) => { if (e.key === 'Escape') onCancel() }
    document.addEventListener('keydown', onKey)
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', onKey)
      document.body.style.overflow = ''
    }
  }, [onCancel])

  /* Conversion coords client -> coords du viewBox SVG */
  function clientToSvg(clientX, clientY) {
    const svg = svgRef.current
    if (!svg) return null
    const pt = svg.createSVGPoint()
    pt.x = clientX
    pt.y = clientY
    const ctm = svg.getScreenCTM()
    if (!ctm) return null
    return pt.matrixTransform(ctm.inverse())
  }

  function onPointerDown(idx, e) {
    e.preventDefault()
    e.target.setPointerCapture?.(e.pointerId)
    setDragIdx(idx)
  }

  function onPointerMove(e) {
    if (dragIdx == null) return
    const p = clientToSvg(e.clientX, e.clientY)
    if (!p) return
    setCorners(c => c.map((pt, i) =>
      i === dragIdx
        ? { x: clamp(p.x, 0, imgWidth), y: clamp(p.y, 0, imgHeight) }
        : pt
    ))
  }

  function onPointerUp() {
    setDragIdx(null)
  }

  return (
    <motion.div
         initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
         transition={{ duration: 0.15 }}
         className="fixed inset-0 z-50 bg-ink-950/95 backdrop-blur flex flex-col p-3 sm:p-6"
         role="dialog" aria-modal="true">
      {/* Header */}
      <div className="flex items-center justify-between mb-3 text-white">
        <div>
          <h2 className="text-lg sm:text-xl font-bold font-display">Ajuster les bords du document</h2>
          <p className="text-xs sm:text-sm text-ink-400 mt-0.5">
            Glissez les 4 points pour entourer précisément le document.
          </p>
        </div>
        <button type="button" onClick={onCancel}
                className="grid place-items-center h-10 w-10 rounded-lg text-white hover:bg-white/10 transition-colors"
                aria-label="Annuler">
          <X size={20} />
        </button>
      </div>

      {/* SVG editor */}
      <div className="flex-1 min-h-0 flex items-center justify-center">
        <svg
          ref={svgRef}
          viewBox={`0 0 ${imgWidth} ${imgHeight}`}
          preserveAspectRatio="xMidYMid meet"
          className="max-w-full max-h-full"
          style={{ touchAction: 'none' }}
          onPointerMove={onPointerMove}
          onPointerUp={onPointerUp}
          onPointerCancel={onPointerUp}
        >
          <image href={imgUrl} width={imgWidth} height={imgHeight} preserveAspectRatio="none" />

          <polygon
            points={corners.map(c => `${c.x},${c.y}`).join(' ')}
            fill="rgba(99, 102, 241, 0.18)"
            stroke="#6366f1"
            strokeWidth={Math.max(3, imgWidth / 350)}
            strokeLinejoin="round"
          />

          {corners.map((c, i) => {
            const r = Math.max(18, imgWidth / 70)
            return (
              <g key={i}>
                <circle cx={c.x} cy={c.y} r={r + 6}
                        fill="rgba(99, 102, 241, 0.2)" />
                <circle cx={c.x} cy={c.y} r={r}
                        fill="white" stroke="#6366f1"
                        strokeWidth={Math.max(3, imgWidth / 250)}
                        style={{ cursor: dragIdx === i ? 'grabbing' : 'grab' }}
                        onPointerDown={e => onPointerDown(i, e)} />
              </g>
            )
          })}
        </svg>
      </div>

      {/* Filtres + actions */}
      <div className="mt-4 space-y-3">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wider text-ink-400 mb-1.5">
            Style de rendu
          </p>
          <div className="grid grid-cols-3 gap-2">
            {FILTERS.map(f => {
              const Icon = f.icon
              return (
                <button
                  key={f.id}
                  type="button"
                  onClick={() => setFilter(f.id)}
                  className={`flex items-center justify-center gap-1.5 rounded-lg border py-2 text-sm font-semibold transition-all ${
                    filter === f.id
                      ? 'border-brand-400 bg-brand-500/20 text-brand-100'
                      : 'border-white/20 text-white/70 hover:bg-white/10'
                  }`}
                >
                  <Icon size={14} /> {f.label}
                </button>
              )
            })}
          </div>
        </div>

        <div className="flex items-center gap-2">
          <button type="button" onClick={() => setCorners(initialCorners)}
                  className="flex-1 sm:flex-initial inline-flex items-center justify-center gap-1.5 h-11 px-4 rounded-lg border border-white/20 text-white hover:bg-white/10 transition-colors text-sm font-medium">
            <RotateCcw size={14} /> Réinitialiser
          </button>
          <button type="button" onClick={onCancel}
                  className="flex-1 sm:flex-initial h-11 px-4 rounded-lg border border-white/20 text-white hover:bg-white/10 transition-colors text-sm font-medium">
            Annuler
          </button>
          <button type="button" onClick={() => onValidate(corners, filter)}
                  className="flex-1 inline-flex items-center justify-center gap-1.5 h-11 px-4 rounded-lg bg-brand-600 hover:bg-brand-700 text-white text-sm font-semibold transition-colors">
            <Check size={16} /> Valider
          </button>
        </div>
      </div>
    </motion.div>
  )
}

function clamp(v, lo, hi) { return Math.max(lo, Math.min(hi, v)) }
