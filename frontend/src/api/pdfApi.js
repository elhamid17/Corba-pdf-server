// ═══════════════════════════════════════════════════════════
// Couche API — appels REST vers l'API Gateway /api/pdf/*
// Toutes les méthodes encapsulent fetch + gestion d'erreur HTTP
// ═══════════════════════════════════════════════════════════

const BASE = '/api/pdf'

/** Construit un message d'erreur lisible à partir d'une réponse non-OK. */
async function readError(res) {
  const ct = res.headers.get('content-type') || ''
  try {
    if (ct.includes('application/json')) {
      const j = await res.json()
      return j.message || j.error || `HTTP ${res.status}`
    }
    const t = await res.text()
    return t || `HTTP ${res.status}`
  } catch {
    return `HTTP ${res.status}`
  }
}

async function postForm(path, form, { responseType = 'blob' } = {}) {
  const res = await fetch(`${BASE}${path}`, { method: 'POST', body: form })
  if (!res.ok) throw new Error(await readError(res))
  if (responseType === 'json') return res.json()
  return res.blob()
}

function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  a.remove()
  setTimeout(() => URL.revokeObjectURL(url), 200)
}

/* ───────── Ping ───────── */
export async function ping() {
  const res = await fetch(`${BASE}/ping`)
  if (!res.ok) throw new Error(await readError(res))
  return res.json()
}

/* ───────── Fusion ───────── */
export async function mergePDFs(files) {
  const form = new FormData()
  files.forEach(f => form.append('files', f))
  const blob = await postForm('/merge', form)
  downloadBlob(blob, 'merged.pdf')
}

/* ───────── Découpage ───────── */
export async function splitPDF(file, ranges) {
  const form = new FormData()
  form.append('file', file)
  ranges.forEach(r => form.append('ranges', r))
  const blob = await postForm('/split', form)
  downloadBlob(blob, 'split.zip')
}

/* ───────── Extraction de pages ───────── */
export async function extractPages(file, pages) {
  const form = new FormData()
  form.append('file', file)
  pages.forEach(p => form.append('pages', p))
  const blob = await postForm('/extract-pages', form)
  downloadBlob(blob, 'extracted.pdf')
}

/* ───────── Suppression de pages ───────── */
export async function deletePages(file, pages) {
  const form = new FormData()
  form.append('file', file)
  pages.forEach(p => form.append('pages', p))
  const blob = await postForm('/delete-pages', form)
  downloadBlob(blob, 'result.pdf')
}

/* ───────── Compression ───────── */
export async function compressPDF(file, options = {}) {
  const form = new FormData()
  form.append('file', file)
  form.append('compressImages', options.compressImages ?? true)
  form.append('imageQuality',   options.imageQuality   ?? 70)
  form.append('removeMetadata', options.removeMetadata ?? false)
  const blob = await postForm('/compress', form)
  downloadBlob(blob, 'compressed.pdf')
}

/* ───────── Rotation ───────── */
export async function rotatePDF(file, angle, pages = []) {
  const form = new FormData()
  form.append('file', file)
  form.append('angle', angle)
  pages.forEach(p => form.append('pages', p))
  const blob = await postForm('/rotate', form)
  downloadBlob(blob, 'rotated.pdf')
}

/* ───────── Filigrane ───────── */
export async function addWatermark(file, options = {}) {
  const form = new FormData()
  form.append('file',     file)
  form.append('text',     options.text     ?? 'CONFIDENTIEL')
  form.append('opacity',  options.opacity  ?? 0.3)
  form.append('fontSize', options.fontSize ?? 48)
  form.append('diagonal', options.diagonal ?? true)
  const blob = await postForm('/watermark', form)
  downloadBlob(blob, 'watermarked.pdf')
}

/* ───────── Mot de passe ───────── */
export async function protectPDF(file, userPassword, ownerPassword) {
  const form = new FormData()
  form.append('file',          file)
  form.append('userPassword',  userPassword)
  form.append('ownerPassword', ownerPassword || userPassword)
  const blob = await postForm('/protect', form)
  downloadBlob(blob, 'protected.pdf')
}

/* ───────── Extraction de texte ───────── */
export async function extractText(file) {
  const form = new FormData()
  form.append('file', file)
  return postForm('/extract-text', form, { responseType: 'json' })
}

/* ───────── OCR ───────── */
export async function performOCR(file, language = 'fra') {
  const form = new FormData()
  form.append('file',     file)
  form.append('language', language)
  return postForm('/ocr', form, { responseType: 'json' })
}

/* ───────── Métadonnées ───────── */
export async function getMetadata(file) {
  const form = new FormData()
  form.append('file', file)
  return postForm('/metadata', form, { responseType: 'json' })
}

/* ───────── Nombre de pages ───────── */
export async function getPageCount(file) {
  const form = new FormData()
  form.append('file', file)
  return postForm('/page-count', form, { responseType: 'json' })
}

/* ───────── Signature numérique ───────── */
export async function signPDF(file, certificate, password, reason, location) {
  const form = new FormData()
  form.append('file',        file)
  form.append('certificate', certificate)
  form.append('password',    password)
  if (reason)   form.append('reason',   reason)
  if (location) form.append('location', location)
  const blob = await postForm('/sign', form)
  downloadBlob(blob, 'signed.pdf')
}

/* ───────── Conversion PDF → Images ───────── */
export async function convertToImages(file, format = 'PNG', dpi = 150) {
  const form = new FormData()
  form.append('file',   file)
  form.append('format', format)
  form.append('dpi',    dpi)
  const blob = await postForm('/convert-to-images', form)
  downloadBlob(blob, 'images.zip')
}

/* ───────── Création depuis texte ───────── */
export async function createPDF(text, title) {
  const form = new FormData()
  form.append('text',  text)
  form.append('title', title)
  const blob = await postForm('/create', form)
  downloadBlob(blob, 'created.pdf')
}
