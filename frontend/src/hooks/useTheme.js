import { useEffect, useState, useCallback } from 'react'

/**
 * Gere le theme avec 3 etats : 'light' | 'dark' | 'auto'.
 *
 * - 'auto' suit prefers-color-scheme et reagit aux changements OS en temps reel
 * - 'light'/'dark' forcent et persistent dans localStorage
 *
 * Le script anti-FOUC dans index.html applique deja la bonne classe au boot ;
 * ce hook gere ensuite les changements utilisateur et l'ecoute systeme.
 */
const STORAGE_KEY = 'theme'

function getStored() {
  try { return localStorage.getItem(STORAGE_KEY) || 'auto' } catch { return 'auto' }
}

function systemPrefersDark() {
  return window.matchMedia('(prefers-color-scheme: dark)').matches
}

function applyDark(isDark) {
  document.documentElement.classList.toggle('dark', isDark)
  // Synchronise la meta theme-color (barre d'URL mobile)
  const meta = document.querySelector('meta[name="theme-color"]')
  if (meta) meta.setAttribute('content', isDark ? '#0f172a' : '#4f46e5')
}

export function useTheme() {
  const [theme, setThemeState] = useState(getStored)

  const setTheme = useCallback((next) => {
    try {
      if (next === 'auto') localStorage.removeItem(STORAGE_KEY)
      else localStorage.setItem(STORAGE_KEY, next)
    } catch {/* mode prive */}
    setThemeState(next)
    applyDark(next === 'dark' || (next === 'auto' && systemPrefersDark()))
  }, [])

  // En mode auto, suivre les changements OS
  useEffect(() => {
    if (theme !== 'auto') return
    const mq = window.matchMedia('(prefers-color-scheme: dark)')
    const onChange = (e) => applyDark(e.matches)
    mq.addEventListener('change', onChange)
    return () => mq.removeEventListener('change', onChange)
  }, [theme])

  return { theme, setTheme }
}
