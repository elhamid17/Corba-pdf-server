import { useEffect, useState } from 'react'

/**
 * Charge OpenCV.js a la demande (CDN), une seule fois pour toute l'app.
 * Etat retourne :
 *   { loading, ready, error, cv (objet une fois pret) }
 * Methode :
 *   ensureLoaded() : demarre / poursuit le chargement, retourne une Promise<cv>
 */
const OPENCV_URL = 'https://docs.opencv.org/4.10.0/opencv.js'

let cachedPromise = null
let cachedCv = null

function loadOpenCv() {
  if (cachedCv) return Promise.resolve(cachedCv)
  if (cachedPromise) return cachedPromise

  cachedPromise = new Promise((resolve, reject) => {
    // Deja sur la page ? (test idempotence)
    if (typeof window !== 'undefined' && window.cv && window.cv.Mat) {
      cachedCv = window.cv
      resolve(window.cv)
      return
    }

    const existing = document.querySelector('script[data-opencv-loader]')
    if (existing) {
      // Quelqu'un d'autre l'a deja insere — on poll
      pollForReady(resolve, reject)
      return
    }

    const script = document.createElement('script')
    script.src = OPENCV_URL
    script.async = true
    script.dataset.opencvLoader = '1'
    script.onload = () => pollForReady(resolve, reject)
    script.onerror = () => {
      cachedPromise = null
      reject(new Error("Echec chargement OpenCV.js (verifier la connexion)"))
    }
    document.head.appendChild(script)
  })

  return cachedPromise
}

function pollForReady(resolve, reject, attempts = 0) {
  if (window.cv && window.cv.Mat) {
    cachedCv = window.cv
    resolve(window.cv)
    return
  }
  if (attempts > 600) {  // ~60 secondes
    cachedPromise = null
    reject(new Error("OpenCV.js charge mais pas initialise (timeout)"))
    return
  }
  setTimeout(() => pollForReady(resolve, reject, attempts + 1), 100)
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
    if (state.loading) return cachedPromise
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

  useEffect(() => {
    if (autoLoad) {
      ensureLoaded().catch(() => { /* l'erreur est deja dans le state */ })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [autoLoad])

  return { ...state, ensureLoaded }
}
