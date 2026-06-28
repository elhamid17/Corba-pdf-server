// Historique des jobs : liste, re-telechargement, suppression.
import { apiFetch, apiJson, readError } from './client'
import { JOBS } from './routes'

export function listJobs({ page = 0, size = 20 } = {}) {
  const qs = new URLSearchParams({ page: String(page), size: String(size) })
  return apiJson(`${JOBS}?${qs.toString()}`)
}

export async function downloadJob(jobId, filename) {
  const res = await apiFetch(`${JOBS}/${encodeURIComponent(jobId)}/download`)
  if (!res.ok) throw new Error(await readError(res))
  const blob = await res.blob()
  triggerDownload(blob, filename || dispositionFilename(res) || 'fichier')
}

export async function deleteJob(jobId) {
  const res = await apiFetch(`${JOBS}/${encodeURIComponent(jobId)}`, { method: 'DELETE' })
  if (!res.ok) throw new Error(await readError(res))
}

function dispositionFilename(res) {
  const cd = res.headers.get('content-disposition') || ''
  const m = /filename\*?=(?:UTF-8'')?["']?([^"';]+)["']?/i.exec(cd)
  return m ? decodeURIComponent(m[1]) : null
}

function triggerDownload(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  a.remove()
  setTimeout(() => URL.revokeObjectURL(url), 200)
}
