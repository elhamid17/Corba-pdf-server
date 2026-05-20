// Endpoints administration (role ADMIN).
import { apiJson } from './client'

export function stats() {
  return apiJson('/api/admin/stats')
}

export function listUsers({ q = '', page = 0, size = 20 } = {}) {
  const qs = new URLSearchParams({ page: String(page), size: String(size) })
  if (q) qs.set('q', q)
  return apiJson(`/api/admin/users?${qs.toString()}`)
}

export function updateUser(id, patch) {
  return apiJson(`/api/admin/users/${encodeURIComponent(id)}`, {
    method: 'PATCH',
    body: patch,
  })
}

export function deleteUser(id) {
  return apiJson(`/api/admin/users/${encodeURIComponent(id)}`, { method: 'DELETE' })
}

export function listJobs({ page = 0, size = 20 } = {}) {
  const qs = new URLSearchParams({ page: String(page), size: String(size) })
  return apiJson(`/api/admin/jobs?${qs.toString()}`)
}
