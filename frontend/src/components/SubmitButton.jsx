import { Loader2, Upload, Cog, Download, Check } from 'lucide-react'

/**
 * Bouton de soumission avec etat loading et barre de progres optionnelle.
 *
 * Props :
 *   loading  : bool (anciennement seul indicateur)
 *   progress : { phase: 'upload'|'processing'|'download'|'done', percent } | null
 *              quand fourni, affiche une barre de progres interne avec label
 *   children : label en mode idle
 *   icon     : icone en mode idle
 *   loadingText : label fallback si pas de progress
 */
const PHASE_LABELS = {
  upload:     'Envoi du fichier',
  processing: 'Traitement en cours',
  download:   'Téléchargement du résultat',
  done:       'Terminé',
}

const PHASE_ICONS = {
  upload:     Upload,
  processing: Cog,
  download:   Download,
  done:       Check,
}

export default function SubmitButton({
  loading, disabled, children, loadingText = 'Traitement en cours…',
  icon: Icon, progress = null, ...rest
}) {
  const inProgress = loading && progress
  const phase = progress?.phase
  const percent = progress?.percent
  const PhaseIcon = PHASE_ICONS[phase] || Cog

  return (
    <button
      {...rest}
      disabled={loading || disabled}
      className="btn-primary w-full py-3 text-base relative overflow-hidden"
    >
      {/* Barre de fond qui se remplit selon le percent */}
      {inProgress && percent != null && (
        <span
          aria-hidden="true"
          className="absolute inset-y-0 left-0 bg-white/15 transition-all duration-150"
          style={{ width: `${percent}%` }}
        />
      )}
      {/* Animation indeterminate pendant la phase processing */}
      {inProgress && phase === 'processing' && (
        <span
          aria-hidden="true"
          className="absolute inset-y-0 left-0 right-0 bg-gradient-to-r from-transparent via-white/20 to-transparent animate-progress-shimmer"
        />
      )}

      {/* Contenu */}
      <span className="relative inline-flex items-center justify-center gap-2">
        {!loading && <>{Icon && <Icon size={18} />} {children}</>}
        {loading && !progress && <><Loader2 className="animate-spin" size={18}/> {loadingText}</>}
        {inProgress && (
          <>
            <PhaseIcon className={phase === 'processing' ? 'animate-spin-slow' : ''} size={18} />
            <span>
              {PHASE_LABELS[phase] || loadingText}
              {percent != null && phase !== 'done' && <span className="ml-2 font-mono text-sm opacity-80">{percent}%</span>}
            </span>
          </>
        )}
      </span>
    </button>
  )
}
