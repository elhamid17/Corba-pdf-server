/**
 * Illustrations SVG custom pour les empty states.
 * Design dans le meme langage visuel que l'app (palette brand/accent,
 * gradients doux, ligne fine).
 */
export function EmptyHistoryIllustration({ className = '' }) {
  return (
    <svg viewBox="0 0 200 160" className={className} role="img" aria-label="Aucune entree d'historique">
      <defs>
        <linearGradient id="ill-history-doc" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="currentColor" stopOpacity="0.06" />
          <stop offset="1" stopColor="currentColor" stopOpacity="0.02" />
        </linearGradient>
      </defs>
      {/* Doc empile derriere */}
      <rect x="48" y="32" width="80" height="100" rx="6"
            fill="url(#ill-history-doc)" stroke="currentColor" strokeOpacity="0.25"
            transform="rotate(-6 88 82)" />
      {/* Doc principal */}
      <g transform="translate(60 28)">
        <rect width="80" height="104" rx="8" fill="white" className="dark:fill-ink-800"
              stroke="currentColor" strokeOpacity="0.3" />
        <path d="M55 0 L80 25 L55 25 Z" fill="currentColor" fillOpacity="0.1" />
        <path d="M55 0 L80 25 M55 0 L55 25 L80 25" fill="none"
              stroke="currentColor" strokeOpacity="0.3" />
        {/* Lignes de texte vides */}
        <rect x="14" y="50" width="52" height="4" rx="2" fill="currentColor" fillOpacity="0.1" />
        <rect x="14" y="60" width="40" height="4" rx="2" fill="currentColor" fillOpacity="0.1" />
        <rect x="14" y="70" width="48" height="4" rx="2" fill="currentColor" fillOpacity="0.1" />
        <rect x="14" y="80" width="32" height="4" rx="2" fill="currentColor" fillOpacity="0.1" />
      </g>
      {/* Pictogramme horloge */}
      <g transform="translate(130 90)">
        <circle r="18" fill="url(#ill-grad-1)" />
        <circle r="18" fill="white" className="dark:fill-ink-900" />
        <circle r="18" fill="none" stroke="currentColor" strokeOpacity="0.3" />
        <line x1="0" y1="0" x2="0" y2="-10" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
        <line x1="0" y1="0" x2="7" y2="0" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
        <circle r="1.5" fill="currentColor" />
      </g>
      <defs>
        <linearGradient id="ill-grad-1" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#6366f1" stopOpacity="0.1" />
          <stop offset="1" stopColor="#06b6d4" stopOpacity="0.1" />
        </linearGradient>
      </defs>
    </svg>
  )
}

export function NoResultsIllustration({ className = '' }) {
  return (
    <svg viewBox="0 0 200 160" className={className} role="img" aria-label="Aucun resultat">
      <defs>
        <radialGradient id="ill-glow">
          <stop offset="0" stopColor="#6366f1" stopOpacity="0.15" />
          <stop offset="1" stopColor="#6366f1" stopOpacity="0" />
        </radialGradient>
      </defs>
      <circle cx="100" cy="80" r="70" fill="url(#ill-glow)" />
      {/* Loupe */}
      <g transform="translate(70 50)">
        <circle cx="25" cy="25" r="25" fill="white" className="dark:fill-ink-800"
                stroke="currentColor" strokeOpacity="0.3" strokeWidth="2" />
        <line x1="45" y1="45" x2="62" y2="62" stroke="currentColor" strokeOpacity="0.4"
              strokeWidth="6" strokeLinecap="round" />
        {/* Croix dans la loupe */}
        <line x1="15" y1="15" x2="35" y2="35" stroke="#ef4444" strokeOpacity="0.6"
              strokeWidth="2.5" strokeLinecap="round" />
        <line x1="35" y1="15" x2="15" y2="35" stroke="#ef4444" strokeOpacity="0.6"
              strokeWidth="2.5" strokeLinecap="round" />
      </g>
      {/* Petites particules autour */}
      <circle cx="40" cy="40" r="3" fill="currentColor" fillOpacity="0.15" />
      <circle cx="160" cy="55" r="2.5" fill="currentColor" fillOpacity="0.15" />
      <circle cx="50" cy="120" r="2.5" fill="currentColor" fillOpacity="0.15" />
      <circle cx="155" cy="125" r="3" fill="currentColor" fillOpacity="0.15" />
    </svg>
  )
}

export function NotFoundIllustration({ className = '' }) {
  return (
    <svg viewBox="0 0 200 160" className={className} role="img" aria-label="Page introuvable">
      <defs>
        <linearGradient id="ill-404-grad" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#6366f1" />
          <stop offset="1" stopColor="#06b6d4" />
        </linearGradient>
      </defs>
      <text x="100" y="115" textAnchor="middle"
            fontSize="100" fontWeight="900" fill="url(#ill-404-grad)"
            fontFamily="ui-sans-serif, system-ui" opacity="0.9">
        404
      </text>
      <circle cx="65" cy="62" r="4" fill="currentColor" fillOpacity="0.4" />
      <circle cx="135" cy="62" r="4" fill="currentColor" fillOpacity="0.4" />
      <path d="M75 90 Q100 75 125 90" stroke="currentColor" strokeOpacity="0.4"
            strokeWidth="3" fill="none" strokeLinecap="round" />
    </svg>
  )
}
