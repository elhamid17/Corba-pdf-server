import { useEffect, useState } from 'react'

/**
 * Charge OpenCV.js a la demande (CDN), une seule fois pour toute l'app.
 *
 * Etat retourne :
 *   { loading, ready, error, cv (objet une fois pret) }
 * Methodes :
 *   ensureLoaded() : demarre / poursuit le chargement, retourne une Promise<cv>
 *   abort()        : annule le chargement en cours, retire le script du DOM
 *
 * Robustesse :
 *  - Timeout total 90s sur le chargement complet (script + init WASM)
 *  - Le script est retire du DOM en cas d'echec/abort (permet un retry propre)
 */
const OPENCV_URL = 'https://docs.opencv.org/4.10.0/opencv.js'
const LOAD_TIMEOUT_MS = 90_000

let cachedPromise = null
let cachedCv = null
let abortController = null  // controleur courant pour annuler le chargement

function loadOpenCv() {
  if (cachedCv) return Promise.resolve(cachedCv)
  if (cachedPromise) return cachedPromise

  cachedPromise = new Promise((resolve, reject) => {
    if (typeof window !== 'undefined' && window.cv && window.cv.Mat) {
      cachedCv = window.cv
      resolve(window.cv)
      return
    }

    const ctrl = { aborted: false }
    abortController = ctrl

    let pollHandle = null
    let timeoutHandle = null

    const cleanup = () => {
      if (pollHandle) clearTimeout(pollHandle)
      if (timeoutHandle) clearTimeout(timeoutHandle)
      // Retire le script du DOM pour permettre un retry propre
      const s = document.querySelector('script[data-opencv-loader]')
      if (s) s.remove()
      // Nettoie window.cv si partiel pour eviter etat zombie
      try { if (window.cv && !window.cv.Mat) delete window.cv } catch {}
    }

    const fail = (err) => {
      cachedPromise = null
      abortController = null
      cleanup()
      reject(err)
    }

    const succeed = (cv) => {
      cachedCv = cv
      abortController = null
      if (timeoutHandle) clearTimeout(timeoutHandle)
      resolve(cv)
    }

    // Timeout global : si pas pret au bout de LOAD_TIMEOUT_MS, on abandonne
    timeoutHandle = setTimeout(() => {
      if (!cachedCv) fail(new Error("Chargement OpenCV.js trop lent (>90s). Vérifiez votre connexion ou désactivez le mode intelligent."))
    }, LOAD_TIMEOUT_MS)

    const poll = (attempts = 0) => {
      if (ctrl.aborted) return
      if (window.cv && window.cv.Mat) {
        succeed(window.cv)
        return
      }
      pollHandle = setTimeout(() => poll(attempts + 1), 150)
    }

    const existing = document.querySelector('script[data-opencv-loader]')
    if (existing) {
      poll()
      return
    }

    const script = document.createElement('script')
    script.src = OPENCV_URL
    script.async = true
    script.dataset.opencvLoader = '1'
    script.onload = () => {
      if (ctrl.aborted) return
      poll()
    }
    script.onerror = () => {
      if (ctrl.aborted) return
      fail(new Error("Échec du téléchargement d'OpenCV.js. Vérifiez votre connexion."))
    }
    document.head.appendChild(script)
  })

  return cachedPromise
}

function abortLoad() {
  if (!abortController) return
  abortController.aborted = true
  abortController = null
  cachedPromise = null
  const s = document.querySelector('script[data-opencv-loader]')
  if (s) s.remove()
}

export function useOpenCv(autoLoad = false) {
  const [state, setState] = useState({
    loading: false,
    ready: Boolean(cachedCv),
    error: null,
    cv: cachedCv,
  })

  function ensureLoaded() {
    if (cachedCv) return Promise.resolve(cachedCv)
    setState(s => ({ ...s, loading: true, error: null }))
    return loadOpenCv()
      .then(cv => {
        setState({ loading: false, ready: true, error: null, cv })
        return cv
      })
      .catch(err => {
        setState({ loading: false, ready: false, error: err.message, cv: null })
        throw err
      })
  }

  function abort() {
    abortLoad()
    setState(s => ({ ...s, loading: false, error: 'Chargement annulé' }))
  }

  useEffect(() => {
    if (autoLoad) {
      ensureLoaded().catch(() => { /* l'erreur est deja dans le state */ })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [autoLoad])

  return { ...state, ensureLoaded, abort }
}
