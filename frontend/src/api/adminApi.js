// Endpoints administration (role ADMIN).
import { apiJson } from './client'
import { ADMIN } from './routes'

export function stats() {
  return apiJson(`${ADMIN}/stats`)
}

export function listUsers({ q = '', page = 0, size = 20 } = {}) {
  const qs = new URLSearchParams({ page: String(page), size: String(size) })
  if (q) qs.set('q', q)
  return apiJson(`${ADMIN}/users?${qs.toString()}`)
}

export function updateUser(id, patch) {
  return apiJson(`${ADMIN}/users/${encodeURIComponent(id)}`, {
    method: 'PATCH',
    body: patch,
  })
}

export function deleteUser(id) {
  return apiJson(`${ADMIN}/users/${encodeURIComponent(id)}`, { method: 'DELETE' })
}

export function listJobs({ page = 0, size = 20 } = {}) {
  const qs = new URLSearchParams({ page: String(page), size: String(size) })
  return apiJson(`${ADMIN}/jobs?${qs.toString()}`)
}
