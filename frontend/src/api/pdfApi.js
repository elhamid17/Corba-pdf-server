// ═══════════════════════════════════════════
// Couche API — appels vers /api/pdf/*
// ═══════════════════════════════════════════

const BASE = '/api/pdf'

// Helper — télécharge le blob PDF retourné par le serveur
function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a   = document.createElement('a')
  a.href    = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

// ── Ping ──
export async function ping() {
  const res = await fetch(`${BASE}/ping`)
  return res.json()
}

// ── Fusion ──
export async function mergePDFs(files) {
  const form = new FormData()
  files.forEach(f => form.append('files', f))
  const res = await fetch(`${BASE}/merge`, { method: 'POST', body: form })
  if (!res.ok) throw new Error(await res.text())
  downloadBlob(await res.blob(), 'merged.pdf')
}

// ── Découpage ──
export async function splitPDF(file, ranges) {
  const form = new FormData()
  form.append('file', file)
  ranges.forEach(r => form.append('ranges', r))
  const res = await fetch(`${BASE}/split`, { method: 'POST', body: form })
  if (!res.ok) throw new Error(await res.text())
  return res.json()
}

// ── Extraction de pages ──
export async function extractPages(file, pages) {
  const form = new FormData()
  form.append('file', file)
  pages.forEach(p => form.append('pages', p))
  const res = await fetch(`${BASE}/extract-pages`, { method: 'POST', body: form })
  if (!res.ok) throw new Error(await res.text())
  downloadBlob(await res.blob(), 'extracted.pdf')
}

// ── Suppression de pages ──
export async function deletePages(file, pages) {
  const form = new FormData()
  form.append('file', file)
  pages.forEach(p => form.append('pages', p))
  const res = await fetch(`${BASE}/delete-pages`, { method: 'POST', body: form })
  if (!res.ok) throw new Error(await res.text())
  downloadBlob(await res.blob(), 'result.pdf')
}

// ── Compression ──
export async function compressPDF(file, options = {}) {
  const form = new FormData()
  form.append('file', file)
  form.append('compressImages',  options.compressImages  ?? true)
  form.append('imageQuality',    options.imageQuality    ?? 70)
  form.append('removeMetadata',  options.removeMetadata  ?? false)
  const res = await fetch(`${BASE}/compress`, { method: 'POST', body: form })
  if (!res.ok) throw new Error(await res.text())
  downloadBlob(await res.blob(), 'compressed.pdf')
}

// ── Rotation ──
export async function rotatePDF(file, angle, pages = []) {
  const form = new FormData()
  form.append('file', file)
  form.append('angle', angle)
  pages.forEach(p => form.append('pages', p))
  const res = await fetch(`${BASE}/rotate`, { method: 'POST', body: form })
  if (!res.ok) throw new Error(await res.text())
  downloadBlob(await res.blob(), 'rotated.pdf')
}

// ── Filigrane ──
export async function addWatermark(file, options = {}) {
  const form = new FormData()
  form.append('file',     file)
  form.append('text',     options.text     ?? 'CONFIDENTIEL')
  form.append('opacity',  options.opacity  ?? 0.3)
  form.append('fontSize', options.fontSize ?? 48)
  form.append('diagonal', options.diagonal ?? true)
  const res = await fetch(`${BASE}/watermark`, { method: 'POST', body: form })
  if (!res.ok) throw new Error(await res.text())
  downloadBlob(await res.blob(), 'watermarked.pdf')
}

// ── Mot de passe ──
export async function protectPDF(file, userPassword, ownerPassword) {
  const form = new FormData()
  form.append('file',          file)
  form.append('userPassword',  userPassword)
  form.append('ownerPassword', ownerPassword || userPassword)
  const res = await fetch(`${BASE}/protect`, { method: 'POST', body: form })
  if (!res.ok) throw new Error(await res.text())
  downloadBlob(await res.blob(), 'protected.pdf')
}

// ── Extraction de texte ──
export async function extractText(file) {
  const form = new FormData()
  form.append('file', file)
  const res = await fetch(`${BASE}/extract-text`, { method: 'POST', body: form })
  if (!res.ok) throw new Error(await res.text())
  return res.json()
}

// ── OCR ──
export async function performOCR(file, language = 'fra') {
  const form = new FormData()
  form.append('file',     file)
  form.append('language', language)
  const res = await fetch(`${BASE}/ocr`, { method: 'POST', body: form })
  if (!res.ok) throw new Error(await res.text())
  return res.json()
}

// ── Métadonnées ──
export async function getMetadata(file) {
  const form = new FormData()
  form.append('file', file)
  const res = await fetch(`${BASE}/metadata`, { method: 'POST', body: form })
  if (!res.ok) throw new Error(await res.text())
  return res.json()
}

// ── Conversion PDF → Images ──
export async function convertToImages(file, format = 'PNG', dpi = 150) {
  const form = new FormData()
  form.append('file',   file)
  form.append('format', format)
  form.append('dpi',    dpi)
  const res = await fetch(`${BASE}/convert-to-images`, { method: 'POST', body: form })
  if (!res.ok) throw new Error(await res.text())
  return res.json()
}

// ── Création depuis texte ──
export async function createPDF(text, title) {
  const form = new FormData()
  form.append('text',  text)
  form.append('title', title)
  const res = await fetch(`${BASE}/create`, { method: 'POST', body: form })
  if (!res.ok) throw new Error(await res.text())
  downloadBlob(await res.blob(), 'created.pdf')
}