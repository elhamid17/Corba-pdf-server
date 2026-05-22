// Client HTTP partage : prefixe de base + token JWT + cookie invite (credentials).
//
// VITE_API_URL peut etre :
//  - vide (dev/local)            -> les fetch utilisent des chemins relatifs (proxy Vite/nginx)
//  - "/api/pdf"                  -> ancienne valeur ; on strip "/api/pdf" pour obtenir la racine
//  - "https://backend.example"   -> racine du backend en prod (Render)
//
// On expose API_BASE (racine), et un helper apiFetch qui ajoute :
//  - credentials: 'include'      -> indispensable pour le cookie guest_id
//  - Authorization: Bearer <jwt> -> si un token est present dans localStorage

const RAW = (import.meta.env.VITE_API_URL || '').trim()
export const API_BASE = RAW.replace(/\/api\/pdf\/?$/, '').replace(/\/+$/, '')

const TOKEN_KEY = 'corba_pdf_token'

export function getToken() {
  try { return localStorage.getItem(TOKEN_KEY) } catch { return null }
}

export function setToken(token) {
  try {
    if (token) localStorage.setItem(TOKEN_KEY, token)
    else localStorage.removeItem(TOKEN_KEY)
  } catch { /* mode prive */ }
}

export function clearToken() {
  setToken(null)
}

export async function readError(res) {
  const ct = res.headers.get('content-type') || ''
  try {
    if (ct.includes('application/json')) {
      const j = await res.json()
      if (j && j.fields && typeof j.fields === 'object') {
        const first = Object.values(j.fields).find(Boolean)
        if (first) return String(first)
      }
      return j.message || j.error || `HTTP ${res.status}`
    }
    const t = await res.text()
    return t || `HTTP ${res.status}`
  } catch {
    return `HTTP ${res.status}`
  }
}

/**
 * Wrapper fetch ajoutant credentials + Authorization.
 * `path` doit etre absolu (ex: "/api/auth/login") ; il sera concatene a API_BASE.
 */
export async function apiFetch(path, init = {}) {
  const headers = new Headers(init.headers || {})
  const token = getToken()
  if (token) headers.set('Authorization', `Bearer ${token}`)
  // Ne pas forcer Content-Type ici : on laisse fetch deduire pour FormData,
  // et on l'ajoute dans apiJson pour le JSON.
  return fetch(`${API_BASE}${path}`, {
    credentials: 'include',
    ...init,
    headers,
  })
}

/**
 * POST avec FormData + callbacks de progres temps reel (upload + download).
 * Utilise XMLHttpRequest sous le capot car fetch ne supporte pas xhr.upload.onprogress.
 *
 * onProgress recoit { phase, percent } a chaque etape :
 *   phase = 'upload'     | percent = 0-100 (octets uploades)
 *   phase = 'processing' | percent = null  (le serveur traite, indetermine)
 *   phase = 'download'   | percent = 0-100 (octets downloades)
 *
 * Retourne un Blob ou un objet JSON parse selon responseType.
 */
export function postFormWithProgress(path, formData, { responseType = 'blob', onProgress } = {}) {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    xhr.open('POST', `${API_BASE}${path}`)
    xhr.withCredentials = true  // pour le cookie guest_id

    const token = getToken()
    if (token) xhr.setRequestHeader('Authorization', `Bearer ${token}`)

    xhr.responseType = responseType === 'json' ? 'text' : 'blob'

    // Phase 1 : upload
    if (xhr.upload) {
      xhr.upload.onprogress = (e) => {
        if (!e.lengthComputable) return
        onProgress?.({ phase: 'upload', percent: Math.round((e.loaded / e.total) * 100) })
      }
      xhr.upload.onload = () => {
        // Upload fini -> serveur traite
        onProgress?.({ phase: 'processing', percent: null })
      }
    }

    // Phase 3 : download de la reponse
    xhr.onprogress = (e) => {
      if (!e.lengthComputable) return
      onProgress?.({ phase: 'download', percent: Math.round((e.loaded / e.total) * 100) })
    }

    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        onProgress?.({ phase: 'done', percent: 100 })
        if (responseType === 'json') {
          try { resolve(JSON.parse(xhr.responseText)) }
          catch { resolve(xhr.responseText) }
        } else {
          resolve(xhr.response)
        }
      } else {
        // Tente d'extraire un message d'erreur du body
        const ct = xhr.getResponseHeader('content-type') || ''
        if (ct.includes('application/json') && xhr.response) {
          // response peut etre blob (responseType=blob), on tente
          const fail = msg => reject(Object.assign(new Error(msg), { status: xhr.status }))
          if (xhr.response instanceof Blob) {
            xhr.response.text().then(t => {
              try {
                const j = JSON.parse(t)
                fail(j.message || j.error || `HTTP ${xhr.status}`)
              } catch { fail(`HTTP ${xhr.status}`) }
            })
          } else {
            try {
              const j = JSON.parse(xhr.responseText)
              fail(j.message || j.error || `HTTP ${xhr.status}`)
            } catch { fail(`HTTP ${xhr.status}`) }
          }
        } else {
          reject(Object.assign(new Error(`HTTP ${xhr.status}`), { status: xhr.status }))
        }
      }
    }

    xhr.onerror = () => reject(new Error("Erreur reseau"))
    xhr.onabort = () => reject(new Error("Requete annulee"))

    onProgress?.({ phase: 'upload', percent: 0 })
    xhr.send(formData)
  })
}

/** POST/PUT JSON et lit la reponse en JSON. Renvoie le body parse. */
export async function apiJson(path, { method = 'GET', body, headers } = {}) {
  const h = new Headers(headers || {})
  if (body !== undefined) h.set('Content-Type', 'application/json')
  const res = await apiFetch(path, {
    method,
    body: body !== undefined ? JSON.stringify(body) : undefined,
    headers: h,
  })
  if (!res.ok) {
    const err = new Error(await readError(res))
    err.status = res.status
    throw err
  }
  if (res.status === 204) return null
  return res.json()
}
