// Appels REST pour l'authentification.
import { apiJson } from './client'
import { AUTH } from './routes'

export function register({ email, username, password }) {
  return apiJson(`${AUTH}/register`, {
    method: 'POST',
    body: { email, username, password },
  })
}

export function login({ identifier, password }) {
  return apiJson(`${AUTH}/login`, {
    method: 'POST',
    body: { identifier, password },
  })
}

export function me() {
  return apiJson(`${AUTH}/me`)
}
