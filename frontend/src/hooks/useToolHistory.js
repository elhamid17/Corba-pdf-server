import { useCallback, useEffect, useState } from 'react'

/**
 * Suit l'historique d'utilisation des outils + les favoris epingles.
 * Stockage : localStorage cote client uniquement (pas synchronise serveur).
 *
 * Expose :
 *   recordVisit(path)        — appele a chaque visite d'une page outil
 *   useRecent(limit?)        — hook : liste des derniers outils visites
 *   useFavorites()           — hook : liste des outils favoris (ordre d'ajout)
 *   toggleFavorite(path)     — ajoute / retire des favoris
 *   useIsFavorite(path)      — hook : true si en favoris (pour le bouton ★)
 */

const RECENT_KEY = 'corba_pdf_recent_tools'
const FAVS_KEY   = 'corba_pdf_fav_tools'
const RECENT_MAX = 12

// Pub-sub interne pour notifier les hooks quand le storage change
const listeners = new Set()
function notify() { listeners.forEach(l => l()) }

function safeRead(key, fallback) {
  try {
    const raw = localStorage.getItem(key)
    if (!raw) return fallback
    const parsed = JSON.parse(raw)
    return Array.isArray(parsed) ? parsed : fallback
  } catch { return fallback }
}
function safeWrite(key, value) {
  try { localStorage.setItem(key, JSON.stringify(value)) } catch {}
}

/* ───────── Recent ───────── */

export function recordVisit(path) {
  if (!path || path === '/') return
  const arr = safeRead(RECENT_KEY, [])
  const filtered = arr.filter(e => e.path !== path)
  filtered.unshift({ path, ts: Date.now() })
  if (filtered.length > RECENT_MAX) filtered.length = RECENT_MAX
  safeWrite(RECENT_KEY, filtered)
  notify()
}

export function useRecent(limit = 4) {
  const [recent, setRecent] = useState(() => safeRead(RECENT_KEY, []))
  useEffect(() => {
    const l = () => setRecent(safeRead(RECENT_KEY, []))
    listeners.add(l)
    return () => listeners.delete(l)
  }, [])
  return recent.slice(0, limit)
}

export function clearRecent() {
  safeWrite(RECENT_KEY, [])
  notify()
}

/* ───────── Favoris ───────── */

export function toggleFavorite(path) {
  if (!path) return
  const arr = safeRead(FAVS_KEY, [])
  const idx = arr.findIndex(e => e.path === path)
  if (idx >= 0) {
    arr.splice(idx, 1)
  } else {
    arr.push({ path, ts: Date.now() })
  }
  safeWrite(FAVS_KEY, arr)
  notify()
}

export function useFavorites() {
  const [favs, setFavs] = useState(() => safeRead(FAVS_KEY, []))
  useEffect(() => {
    const l = () => setFavs(safeRead(FAVS_KEY, []))
    listeners.add(l)
    return () => listeners.delete(l)
  }, [])
  return favs
}

export function useIsFavorite(path) {
  const favs = useFavorites()
  return favs.some(f => f.path === path)
}

/* Hook qui enregistre automatiquement la visite au mount */
export function useRecordVisit(path) {
  useEffect(() => {
    if (path) recordVisit(path)
  }, [path])
}
