// ═══════════════════════════════════════════════════════════
// Couche API — appels REST vers l'API Gateway /api/pdf/*
// Utilise le client partage pour ajouter le JWT et le cookie invite.
// ═══════════════════════════════════════════════════════════

import { apiFetch, readError } from './client'

async function postForm(path, form, { responseType = 'blob' } = {}) {
  const res = await apiFetch(`/api/pdf${path}`, { method: 'POST', body: form })
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

/** Ajoute un nom de fichier custom au form si fourni (sinon ignore). */
function maybeOutputName(form, outputName) {
  if (outputName && outputName.trim()) {
    form.append('outputName', outputName.trim())
  }
}

/** Si un nom custom a ete fourni, ajuste juste l'extension par defaut. */
function clientFilename(outputName, defaultName) {
  if (!outputName || !outputName.trim()) return defaultName
  const dotDefault = defaultName.lastIndexOf('.')
  const ext = dotDefault > 0 ? defaultName.substring(dotDefault) : ''
  const trimmed = outputName.trim()
  return trimmed.endsWith(ext) ? trimmed : trimmed + ext
}

/* ───────── Ping ───────── */
export async function ping() {
  const res = await apiFetch('/api/pdf/ping')
  if (!res.ok) throw new Error(await readError(res))
  return res.json()
}

/* ───────── Fusion ───────── */
export async function mergePDFs(files, outputName) {
  const form = new FormData()
  files.forEach(f => form.append('files', f))
  maybeOutputName(form, outputName)
  const blob = await postForm('/merge', form)
  downloadBlob(blob, clientFilename(outputName, 'merged.pdf'))
}

/* ───────── Découpage ───────── */
export async function splitPDF(file, ranges, outputName) {
  const form = new FormData()
  form.append('file', file)
  ranges.forEach(r => form.append('ranges', r))
  maybeOutputName(form, outputName)
  const blob = await postForm('/split', form)
  downloadBlob(blob, clientFilename(outputName, 'split.zip'))
}

/* ───────── Extraction de pages ───────── */
export async function extractPages(file, pages, outputName) {
  const form = new FormData()
  form.append('file', file)
  pages.forEach(p => form.append('pages', p))
  maybeOutputName(form, outputName)
  const blob = await postForm('/extract-pages', form)
  downloadBlob(blob, clientFilename(outputName, 'extracted.pdf'))
}

/* ───────── Suppression de pages ───────── */
export async function deletePages(file, pages, outputName) {
  const form = new FormData()
  form.append('file', file)
  pages.forEach(p => form.append('pages', p))
  maybeOutputName(form, outputName)
  const blob = await postForm('/delete-pages', form)
  downloadBlob(blob, clientFilename(outputName, 'result.pdf'))
}

/* ───────── Compression ───────── */
export async function compressPDF(file, options = {}, outputName) {
  const form = new FormData()
  form.append('file', file)
  form.append('compressImages', options.compressImages ?? true)
  form.append('imageQuality',   options.imageQuality   ?? 70)
  form.append('removeMetadata', options.removeMetadata ?? false)
  maybeOutputName(form, outputName)
  const blob = await postForm('/compress', form)
  downloadBlob(blob, clientFilename(outputName, 'compressed.pdf'))
}

/* ───────── Rotation ───────── */
export async function rotatePDF(file, angle, pages = [], outputName) {
  const form = new FormData()
  form.append('file', file)
  form.append('angle', angle)
  pages.forEach(p => form.append('pages', p))
  maybeOutputName(form, outputName)
  const blob = await postForm('/rotate', form)
  downloadBlob(blob, clientFilename(outputName, 'rotated.pdf'))
}

/* ───────── Filigrane ───────── */
export async function addWatermark(file, options = {}, outputName) {
  const form = new FormData()
  form.append('file',     file)
  form.append('text',     options.text     ?? 'CONFIDENTIEL')
  form.append('opacity',  options.opacity  ?? 0.3)
  form.append('fontSize', options.fontSize ?? 48)
  form.append('diagonal', options.diagonal ?? true)
  maybeOutputName(form, outputName)
  const blob = await postForm('/watermark', form)
  downloadBlob(blob, clientFilename(outputName, 'watermarked.pdf'))
}

/* ───────── Mot de passe ───────── */
export async function protectPDF(file, userPassword, ownerPassword, outputName) {
  const form = new FormData()
  form.append('file',          file)
  form.append('userPassword',  userPassword)
  form.append('ownerPassword', ownerPassword || userPassword)
  maybeOutputName(form, outputName)
  const blob = await postForm('/protect', form)
  downloadBlob(blob, clientFilename(outputName, 'protected.pdf'))
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
export async function signPDF(file, certificate, password, reason, location, outputName) {
  const form = new FormData()
  form.append('file',        file)
  form.append('certificate', certificate)
  form.append('password',    password)
  if (reason)   form.append('reason',   reason)
  if (location) form.append('location', location)
  maybeOutputName(form, outputName)
  const blob = await postForm('/sign', form)
  downloadBlob(blob, clientFilename(outputName, 'signed.pdf'))
}

/* ───────── Conversion PDF → Images ───────── */
export async function convertToImages(file, format = 'PNG', dpi = 150, outputName) {
  const form = new FormData()
  form.append('file',   file)
  form.append('format', format)
  form.append('dpi',    dpi)
  maybeOutputName(form, outputName)
  const blob = await postForm('/convert-to-images', form)
  downloadBlob(blob, clientFilename(outputName, 'images.zip'))
}

/* ───────── Création depuis texte ───────── */
export async function createPDF(text, title, outputName) {
  const form = new FormData()
  form.append('text',  text)
  form.append('title', title)
  maybeOutputName(form, outputName)
  const blob = await postForm('/create', form)
  downloadBlob(blob, clientFilename(outputName, 'created.pdf'))
}

/* ───────── PDF → Word ───────── */
export async function pdfToWord(file, outputName) {
  const form = new FormData()
  form.append('file', file)
  maybeOutputName(form, outputName)
  const blob = await postForm('/pdf-to-word', form)
  downloadBlob(blob, clientFilename(outputName, 'converted.docx'))
}

/* ───────── PDF → Excel ───────── */
export async function pdfToExcel(file, outputName) {
  const form = new FormData()
  form.append('file', file)
  maybeOutputName(form, outputName)
  const blob = await postForm('/pdf-to-excel', form)
  downloadBlob(blob, clientFilename(outputName, 'converted.xlsx'))
}

/* ───────── Word → PDF ───────── */
export async function wordToPdf(file, outputName) {
  const form = new FormData()
  form.append('file', file)
  maybeOutputName(form, outputName)
  const blob = await postForm('/word-to-pdf', form)
  downloadBlob(blob, clientFilename(outputName, 'converted.pdf'))
}

/* ───────── Images → PDF ───────── */
export async function imagesToPdf(files, outputName) {
  const form = new FormData()
  files.forEach(f => form.append('files', f))
  maybeOutputName(form, outputName)
  const blob = await postForm('/images-to-pdf', form)
  downloadBlob(blob, clientFilename(outputName, 'images.pdf'))
}

/* ───────── Inversion des pages ───────── */
export async function reversePdf(file, outputName) {
  const form = new FormData()
  form.append('file', file)
  maybeOutputName(form, outputName)
  const blob = await postForm('/reverse', form)
  downloadBlob(blob, clientFilename(outputName, 'reversed.pdf'))
}

/* ───────── Numerotation des pages ───────── */
export async function addPageNumbers(file, options = {}, outputName) {
  const form = new FormData()
  form.append('file', file)
  form.append('position',    options.position    ?? 'bottom-center')
  form.append('format',      options.format      ?? '%d')
  form.append('startNumber', options.startNumber ?? 1)
  form.append('fontSize',    options.fontSize    ?? 12)
  maybeOutputName(form, outputName)
  const blob = await postForm('/page-numbers', form)
  downloadBlob(blob, clientFilename(outputName, 'numbered.pdf'))
}

/* ───────── Redimensionnement ───────── */
export async function resizePdf(file, targetSize, outputName) {
  const form = new FormData()
  form.append('file', file)
  form.append('targetSize', targetSize || 'A4')
  maybeOutputName(form, outputName)
  const blob = await postForm('/resize', form)
  downloadBlob(blob, clientFilename(outputName, 'resized.pdf'))
}

/* ───────── Recadrage ───────── */
export async function cropPdf(file, margins = {}, outputName) {
  const form = new FormData()
  form.append('file', file)
  form.append('marginLeft',   margins.left   ?? 0)
  form.append('marginTop',    margins.top    ?? 0)
  form.append('marginRight',  margins.right  ?? 0)
  form.append('marginBottom', margins.bottom ?? 0)
  maybeOutputName(form, outputName)
  const blob = await postForm('/crop', form)
  downloadBlob(blob, clientFilename(outputName, 'cropped.pdf'))
}

/* ───────── Page de garde ───────── */
export async function addCoverPage(file, coverImage, outputName) {
  const form = new FormData()
  form.append('file', file)
  form.append('cover', coverImage)
  maybeOutputName(form, outputName)
  const blob = await postForm('/cover', form)
  downloadBlob(blob, clientFilename(outputName, 'with-cover.pdf'))
}

/* ───────── Reorganisation ───────── */
export async function reorderPdf(file, order, outputName) {
  const form = new FormData()
  form.append('file', file)
  order.forEach(p => form.append('order', p))
  maybeOutputName(form, outputName)
  const blob = await postForm('/reorder', form)
  downloadBlob(blob, clientFilename(outputName, 'reordered.pdf'))
}
