// ═══════════════════════════════════════════════════════════
// Couche API — appels REST vers l'API Gateway (PDF = /api/v1/pdf/*)
// Utilise le client partage pour ajouter le JWT et le cookie invite.
// ═══════════════════════════════════════════════════════════

import { apiFetch, postFormWithProgress, readError } from './client'
import { PDF } from './routes'
import { notifyResult } from '../hooks/useWorkflow'

/**
 * postForm intelligent : utilise XHR (avec progres) si onProgress est fourni,
 * sinon fetch standard (plus simple).
 */
async function postForm(path, form, { responseType = 'blob', onProgress } = {}) {
  if (onProgress) {
    return postFormWithProgress(`${PDF}${path}`, form, { responseType, onProgress })
  }
  const res = await apiFetch(`${PDF}${path}`, { method: 'POST', body: form })
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
  // Capture pour le workflow chaining (banner "Et apres ?")
  try { notifyResult(blob, filename) } catch {}
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
  const res = await apiFetch(`${PDF}/ping`)
  if (!res.ok) throw new Error(await readError(res))
  return res.json()
}

/* ───────── Fusion ───────── */
export async function mergePDFs(files, outputName, onProgress) {
  const form = new FormData()
  files.forEach(f => form.append('files', f))
  maybeOutputName(form, outputName)
  const blob = await postForm('/merge', form, { onProgress })
  downloadBlob(blob, clientFilename(outputName, 'merged.pdf'))
}

/* ───────── Découpage ───────── */
export async function splitPDF(file, ranges, outputName, onProgress) {
  const form = new FormData()
  form.append('file', file)
  ranges.forEach(r => form.append('ranges', r))
  maybeOutputName(form, outputName)
  const blob = await postForm('/split', form, { onProgress })
  downloadBlob(blob, clientFilename(outputName, 'split.zip'))
}

/* ───────── Extraction de pages ───────── */
export async function extractPages(file, pages, outputName, onProgress) {
  const form = new FormData()
  form.append('file', file)
  pages.forEach(p => form.append('pages', p))
  maybeOutputName(form, outputName)
  const blob = await postForm('/extract-pages', form, { onProgress })
  downloadBlob(blob, clientFilename(outputName, 'extracted.pdf'))
}

/* ───────── Suppression de pages ───────── */
export async function deletePages(file, pages, outputName, onProgress) {
  const form = new FormData()
  form.append('file', file)
  pages.forEach(p => form.append('pages', p))
  maybeOutputName(form, outputName)
  const blob = await postForm('/delete-pages', form, { onProgress })
  downloadBlob(blob, clientFilename(outputName, 'result.pdf'))
}

/* ───────── Compression ───────── */
export async function compressPDF(file, options = {}, outputName, onProgress) {
  const form = new FormData()
  form.append('file', file)
  form.append('compressImages', options.compressImages ?? true)
  form.append('imageQuality',   options.imageQuality   ?? 70)
  form.append('removeMetadata', options.removeMetadata ?? false)
  maybeOutputName(form, outputName)
  const blob = await postForm('/compress', form, { onProgress })
  downloadBlob(blob, clientFilename(outputName, 'compressed.pdf'))
}

/* ───────── Rotation ───────── */
export async function rotatePDF(file, angle, pages = [], outputName, onProgress) {
  const form = new FormData()
  form.append('file', file)
  form.append('angle', angle)
  pages.forEach(p => form.append('pages', p))
  maybeOutputName(form, outputName)
  const blob = await postForm('/rotate', form, { onProgress })
  downloadBlob(blob, clientFilename(outputName, 'rotated.pdf'))
}

/* ───────── Filigrane ───────── */
export async function addWatermark(file, options = {}, outputName, onProgress) {
  const form = new FormData()
  form.append('file',     file)
  form.append('text',     options.text     ?? 'CONFIDENTIEL')
  form.append('opacity',  options.opacity  ?? 0.3)
  form.append('fontSize', options.fontSize ?? 48)
  form.append('diagonal', options.diagonal ?? true)
  maybeOutputName(form, outputName)
  const blob = await postForm('/watermark', form, { onProgress })
  downloadBlob(blob, clientFilename(outputName, 'watermarked.pdf'))
}

/* ───────── Mot de passe ───────── */
export async function protectPDF(file, userPassword, ownerPassword, outputName, onProgress) {
  const form = new FormData()
  form.append('file',          file)
  form.append('userPassword',  userPassword)
  form.append('ownerPassword', ownerPassword || userPassword)
  maybeOutputName(form, outputName)
  const blob = await postForm('/protect', form, { onProgress })
  downloadBlob(blob, clientFilename(outputName, 'protected.pdf'))
}

/* ───────── Extraction de texte ───────── */
export async function extractText(file, onProgress) {
  const form = new FormData()
  form.append('file', file)
  return postForm('/extract-text', form, { responseType: 'json', onProgress })
}

/* ───────── OCR ───────── */
export async function performOCR(file, language = 'fra', onProgress) {
  const form = new FormData()
  form.append('file',     file)
  form.append('language', language)
  return postForm('/ocr', form, { responseType: 'json', onProgress })
}

/* ───────── Métadonnées ───────── */
export async function getMetadata(file, onProgress) {
  const form = new FormData()
  form.append('file', file)
  return postForm('/metadata', form, { responseType: 'json', onProgress })
}

/* ───────── Nombre de pages ───────── */
export async function getPageCount(file) {
  const form = new FormData()
  form.append('file', file)
  return postForm('/page-count', form, { responseType: 'json' })
}

/* ───────── Signature numérique ───────── */
export async function signPDF(file, certificate, password, reason, location, outputName, onProgress) {
  const form = new FormData()
  form.append('file',        file)
  form.append('certificate', certificate)
  form.append('password',    password)
  if (reason)   form.append('reason',   reason)
  if (location) form.append('location', location)
  maybeOutputName(form, outputName)
  const blob = await postForm('/sign', form, { onProgress })
  downloadBlob(blob, clientFilename(outputName, 'signed.pdf'))
}

/* ───────── Conversion PDF → Images ───────── */
export async function convertToImages(file, format = 'PNG', dpi = 150, outputName, onProgress) {
  const form = new FormData()
  form.append('file',   file)
  form.append('format', format)
  form.append('dpi',    dpi)
  maybeOutputName(form, outputName)
  const blob = await postForm('/convert-to-images', form, { onProgress })
  downloadBlob(blob, clientFilename(outputName, 'images.zip'))
}

/* ───────── Création depuis texte ───────── */
export async function createPDF(text, title, outputName, onProgress) {
  const form = new FormData()
  form.append('text',  text)
  form.append('title', title)
  maybeOutputName(form, outputName)
  const blob = await postForm('/create', form, { onProgress })
  downloadBlob(blob, clientFilename(outputName, 'created.pdf'))
}

/* ───────── PDF → Word ───────── */
export async function pdfToWord(file, outputName, onProgress) {
  const form = new FormData()
  form.append('file', file)
  maybeOutputName(form, outputName)
  const blob = await postForm('/pdf-to-word', form, { onProgress })
  downloadBlob(blob, clientFilename(outputName, 'converted.docx'))
}

/* ───────── PDF → Excel ───────── */
export async function pdfToExcel(file, outputName, onProgress) {
  const form = new FormData()
  form.append('file', file)
  maybeOutputName(form, outputName)
  const blob = await postForm('/pdf-to-excel', form, { onProgress })
  downloadBlob(blob, clientFilename(outputName, 'converted.xlsx'))
}

/* ───────── Word → PDF ───────── */
export async function wordToPdf(file, outputName, onProgress) {
  const form = new FormData()
  form.append('file', file)
  maybeOutputName(form, outputName)
  const blob = await postForm('/word-to-pdf', form, { onProgress })
  downloadBlob(blob, clientFilename(outputName, 'converted.pdf'))
}

/* ───────── Images → PDF ───────── */
export async function imagesToPdf(files, outputName, onProgress) {
  const form = new FormData()
  files.forEach(f => form.append('files', f))
  maybeOutputName(form, outputName)
  const blob = await postForm('/images-to-pdf', form, { onProgress })
  downloadBlob(blob, clientFilename(outputName, 'images.pdf'))
}

/* ───────── Inversion des pages ───────── */
export async function reversePdf(file, outputName, onProgress) {
  const form = new FormData()
  form.append('file', file)
  maybeOutputName(form, outputName)
  const blob = await postForm('/reverse', form, { onProgress })
  downloadBlob(blob, clientFilename(outputName, 'reversed.pdf'))
}

/* ───────── Numerotation des pages ───────── */
export async function addPageNumbers(file, options = {}, outputName, onProgress) {
  const form = new FormData()
  form.append('file', file)
  form.append('position',    options.position    ?? 'bottom-center')
  form.append('format',      options.format      ?? '%d')
  form.append('startNumber', options.startNumber ?? 1)
  form.append('fontSize',    options.fontSize    ?? 12)
  maybeOutputName(form, outputName)
  const blob = await postForm('/page-numbers', form, { onProgress })
  downloadBlob(blob, clientFilename(outputName, 'numbered.pdf'))
}

/* ───────── Redimensionnement ───────── */
export async function resizePdf(file, targetSize, outputName, onProgress) {
  const form = new FormData()
  form.append('file', file)
  form.append('targetSize', targetSize || 'A4')
  maybeOutputName(form, outputName)
  const blob = await postForm('/resize', form, { onProgress })
  downloadBlob(blob, clientFilename(outputName, 'resized.pdf'))
}

/* ───────── Recadrage ───────── */
export async function cropPdf(file, margins = {}, outputName, onProgress) {
  const form = new FormData()
  form.append('file', file)
  form.append('marginLeft',   margins.left   ?? 0)
  form.append('marginTop',    margins.top    ?? 0)
  form.append('marginRight',  margins.right  ?? 0)
  form.append('marginBottom', margins.bottom ?? 0)
  maybeOutputName(form, outputName)
  const blob = await postForm('/crop', form, { onProgress })
  downloadBlob(blob, clientFilename(outputName, 'cropped.pdf'))
}

/* ───────── Page de garde ───────── */
export async function addCoverPage(file, coverImage, outputName, onProgress) {
  const form = new FormData()
  form.append('file', file)
  form.append('cover', coverImage)
  maybeOutputName(form, outputName)
  const blob = await postForm('/cover', form, { onProgress })
  downloadBlob(blob, clientFilename(outputName, 'with-cover.pdf'))
}

/* ───────── Reorganisation ───────── */
export async function reorderPdf(file, order, outputName, onProgress) {
  const form = new FormData()
  form.append('file', file)
  order.forEach(p => form.append('order', p))
  maybeOutputName(form, outputName)
  const blob = await postForm('/reorder', form, { onProgress })
  downloadBlob(blob, clientFilename(outputName, 'reordered.pdf'))
}

/* ───────── Anonymisation ───────── */
export async function anonymizePdf(file, outputName, onProgress) {
  const form = new FormData()
  form.append('file', file)
  maybeOutputName(form, outputName)
  const blob = await postForm('/anonymize', form, { onProgress })
  downloadBlob(blob, clientFilename(outputName, 'anonymized.pdf'))
}

/* ───────── Signature manuscrite (image) ───────── */
export async function addSignatureImage(file, signature, opts = {}, outputName, onProgress) {
  const form = new FormData()
  form.append('file', file)
  form.append('signature', signature)
  form.append('page',         opts.page         ?? 1)
  form.append('xPercent',     opts.xPercent     ?? 50)
  form.append('yPercent',     opts.yPercent     ?? 10)
  form.append('widthPercent', opts.widthPercent ?? 30)
  maybeOutputName(form, outputName)
  const blob = await postForm('/sign-image', form, { onProgress })
  downloadBlob(blob, clientFilename(outputName, 'signed-image.pdf'))
}

/* ───────── Caviardage textuel ───────── */
export async function redactPdf(file, terms, outputName, onProgress) {
  const form = new FormData()
  form.append('file', file)
  terms.forEach(t => form.append('terms', t))
  maybeOutputName(form, outputName)
  const blob = await postForm('/redact', form, { onProgress })
  downloadBlob(blob, clientFilename(outputName, 'redacted.pdf'))
}

/* ───────── Tampon ───────── */
export async function addStamp(file, opts = {}, outputName, onProgress) {
  const form = new FormData()
  form.append('file', file)
  form.append('text',     opts.text     ?? 'APPROUVE')
  form.append('page',     opts.page     ?? 1)
  form.append('position', opts.position ?? 'center')
  form.append('color',    opts.color    ?? 'red')
  form.append('fontSize', opts.fontSize ?? 36)
  maybeOutputName(form, outputName)
  const blob = await postForm('/stamp', form, { onProgress })
  downloadBlob(blob, clientFilename(outputName, 'stamped.pdf'))
}

/* ───────── PDF → PowerPoint (PPTX) ───────── */
export async function pdfToPptx(file, outputName, onProgress) {
  const form = new FormData()
  form.append('file', file)
  maybeOutputName(form, outputName)
  const blob = await postForm('/pdf-to-pptx', form, { onProgress })
  downloadBlob(blob, clientFilename(outputName, 'converted.pptx'))
}

/* ───────── PDF → Markdown ───────── */
export async function pdfToMarkdown(file, outputName, onProgress) {
  const form = new FormData()
  form.append('file', file)
  maybeOutputName(form, outputName)
  const blob = await postForm('/pdf-to-markdown', form, { onProgress })
  downloadBlob(blob, clientFilename(outputName, 'converted.md'))
}

/* ───────── Markdown → PDF ───────── */
export async function markdownToPdf(markdown, title, outputName, onProgress) {
  const form = new FormData()
  form.append('markdown', markdown)
  form.append('title', title || 'Document')
  maybeOutputName(form, outputName)
  const blob = await postForm('/markdown-to-pdf', form, { onProgress })
  downloadBlob(blob, clientFilename(outputName, 'from-markdown.pdf'))
}

/* ───────── HTML → PDF ───────── */
export async function htmlToPdf(html, title, outputName, onProgress) {
  const form = new FormData()
  form.append('html', html)
  form.append('title', title || 'Document')
  maybeOutputName(form, outputName)
  const blob = await postForm('/html-to-pdf', form, { onProgress })
  downloadBlob(blob, clientFilename(outputName, 'from-html.pdf'))
}

/* ───────── Excel → PDF ───────── */
export async function excelToPdf(file, outputName, onProgress) {
  const form = new FormData()
  form.append('file', file)
  maybeOutputName(form, outputName)
  const blob = await postForm('/excel-to-pdf', form, { onProgress })
  downloadBlob(blob, clientFilename(outputName, 'from-excel.pdf'))
}

/* ───────── ODT → PDF ───────── */
export async function odtToPdf(file, outputName, onProgress) {
  const form = new FormData()
  form.append('file', file)
  maybeOutputName(form, outputName)
  const blob = await postForm('/odt-to-pdf', form, { onProgress })
  downloadBlob(blob, clientFilename(outputName, 'from-odt.pdf'))
}

/* ───────── Deverrouillage ───────── */
export async function unlockPdf(file, password, outputName, onProgress) {
  const form = new FormData()
  form.append('file', file)
  form.append('password', password)
  maybeOutputName(form, outputName)
  const blob = await postForm('/unlock', form, { onProgress })
  downloadBlob(blob, clientFilename(outputName, 'unlocked.pdf'))
}

/* ───────── Verification signature (JSON) ───────── */
export async function verifySignature(file, onProgress) {
  const form = new FormData()
  form.append('file', file)
  return postForm('/verify-signature', form, { responseType: 'json', onProgress })
}

/* ───────── Conversion PDF/A ───────── */
export async function toPdfA(file, outputName, onProgress) {
  const form = new FormData()
  form.append('file', file)
  maybeOutputName(form, outputName)
  const blob = await postForm('/to-pdfa', form, { onProgress })
  downloadBlob(blob, clientFilename(outputName, 'archive.pdf'))
}

/* ───────── Comparaison de 2 PDFs (JSON) ───────── */
export async function comparePdfs(fileA, fileB, onProgress) {
  const form = new FormData()
  form.append('fileA', fileA)
  form.append('fileB', fileB)
  return postForm('/compare', form, { responseType: 'json', onProgress })
}

/* ───────── Stats du document (JSON) ───────── */
export async function getStats(file, onProgress) {
  const form = new FormData()
  form.append('file', file)
  return postForm('/stats', form, { responseType: 'json', onProgress })
}

/* ───────── Ajout QR code ───────── */
export async function addQrCode(file, opts = {}, outputName, onProgress) {
  const form = new FormData()
  form.append('file', file)
  form.append('text',     opts.text     ?? '')
  form.append('page',     opts.page     ?? 1)
  form.append('position', opts.position ?? 'bottom-right')
  form.append('sizePx',   opts.sizePx   ?? 120)
  maybeOutputName(form, outputName)
  const blob = await postForm('/add-qr', form, { onProgress })
  downloadBlob(blob, clientFilename(outputName, 'with-qr.pdf'))
}

/* ───────── Ajout code-barres ───────── */
export async function addBarcode(file, opts = {}, outputName, onProgress) {
  const form = new FormData()
  form.append('file', file)
  form.append('code',     opts.code     ?? '')
  form.append('page',     opts.page     ?? 1)
  form.append('position', opts.position ?? 'bottom-right')
  form.append('type',     opts.type     ?? 'CODE_128')
  form.append('sizePx',   opts.sizePx   ?? 200)
  maybeOutputName(form, outputName)
  const blob = await postForm('/add-barcode', form, { onProgress })
  downloadBlob(blob, clientFilename(outputName, 'with-barcode.pdf'))
}

/* ───────── Generation CV ───────── */
export async function generateCv(cvData, outputName, onProgress) {
  // Implementation via XHR pour supporter le progres (le body est JSON)
  const params = outputName && outputName.trim()
    ? `?outputName=${encodeURIComponent(outputName.trim())}`
    : ''
  const { API_BASE, getToken } = await import('./client')

  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    xhr.open('POST', `${API_BASE}${PDF}/generate-cv${params}`)
    xhr.withCredentials = true
    xhr.setRequestHeader('Content-Type', 'application/json')
    const token = getToken()
    if (token) xhr.setRequestHeader('Authorization', `Bearer ${token}`)
    xhr.responseType = 'blob'

    if (xhr.upload) {
      xhr.upload.onprogress = (e) => {
        if (e.lengthComputable) onProgress?.({ phase: 'upload', percent: Math.round((e.loaded / e.total) * 100) })
      }
      xhr.upload.onload = () => onProgress?.({ phase: 'processing', percent: null })
    }
    xhr.onprogress = (e) => {
      if (e.lengthComputable) onProgress?.({ phase: 'download', percent: Math.round((e.loaded / e.total) * 100) })
    }
    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        onProgress?.({ phase: 'done', percent: 100 })
        downloadBlob(xhr.response, clientFilename(outputName, 'cv.pdf'))
        resolve()
      } else {
        xhr.response.text().then(t => {
          try { reject(new Error(JSON.parse(t).message || `HTTP ${xhr.status}`)) }
          catch { reject(new Error(`HTTP ${xhr.status}`)) }
        }).catch(() => reject(new Error(`HTTP ${xhr.status}`)))
      }
    }
    xhr.onerror = () => reject(new Error('Erreur reseau'))
    onProgress?.({ phase: 'upload', percent: 0 })
    xhr.send(JSON.stringify(cvData))
  })
}
