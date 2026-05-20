// Appels REST pour l'authentification.
import { apiJson } from './client'

export function register({ email, username, password }) {
  return apiJson('/api/auth/register', {
    method: 'POST',
    body: { email, username, password },
  })
}

export function login({ identifier, password }) {
  return apiJson('/api/auth/login', {
    method: 'POST',
    body: { identifier, password },
  })
}

export function me() {
  return apiJson('/api/auth/me')
}
