// Source unique des prefixes d'URL de l'API (cote frontend).
//
// Versioning d'API : toutes les routes applicatives sont servies sous /api/v1
// (cf. ADR-0007). Ces bases sont concatenees a API_BASE (racine du backend) par
// le client HTTP. Ne JAMAIS coder un chemin "/api/..." en dur ailleurs : importer
// ces constantes.

export const API_V1 = '/api/v1'

export const PDF = `${API_V1}/pdf`
export const AUTH = `${API_V1}/auth`
export const ADMIN = `${API_V1}/admin`
export const JOBS = `${API_V1}/jobs`
