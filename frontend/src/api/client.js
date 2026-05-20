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
