/**
 * Utilitaires OpenCV pour le scanner intelligent :
 *  - detectDocumentCorners : trouve le quadrilatere englobant le document
 *  - warpPerspective       : applique la transformation perspective pour redresser
 *  - applyScanFilter       : seuil adaptatif pour effet "document scan" net N&B
 *
 * Toutes les fonctions liberent leurs Mats intermediaires explicitement
 * (OpenCV.js n'a pas de GC, sinon fuite memoire WASM).
 */

/**
 * Detecte les 4 coins du document dans un canvas.
 * @returns [{x,y}, {x,y}, {x,y}, {x,y}] ordonne [tl, tr, br, bl]
 *          ou un quadrilatere par defaut si rien n'est detecte.
 */
export function detectDocumentCorners(cv, srcCanvas) {
  const src = cv.imread(srcCanvas)
  const gray = new cv.Mat()
  const edges = new cv.Mat()
  const contours = new cv.MatVector()
  const hierarchy = new cv.Mat()

  try {
    cv.cvtColor(src, gray, cv.COLOR_RGBA2GRAY)
    cv.GaussianBlur(gray, gray, new cv.Size(5, 5), 0)
    cv.Canny(gray, edges, 75, 200)

    cv.findContours(edges, contours, hierarchy, cv.RETR_LIST, cv.CHAIN_APPROX_SIMPLE)

    const minArea = src.rows * src.cols * 0.1  // au moins 10% de l'image
    let bestApprox = null
    let bestArea = 0

    for (let i = 0; i < contours.size(); i++) {
      const cnt = contours.get(i)
      const peri = cv.arcLength(cnt, true)
      const approx = new cv.Mat()
      cv.approxPolyDP(cnt, approx, 0.02 * peri, true)
      const area = Math.abs(cv.contourArea(approx))
      if (approx.rows === 4 && area > bestArea && area > minArea) {
        if (bestApprox) bestApprox.delete()
        bestApprox = approx
        bestArea = area
      } else {
        approx.delete()
      }
      cnt.delete()
    }

    if (bestApprox) {
      const corners = []
      for (let i = 0; i < 4; i++) {
        corners.push({ x: bestApprox.data32S[i * 2], y: bestApprox.data32S[i * 2 + 1] })
      }
      bestApprox.delete()
      return orderCorners(corners)
    }

    // Fallback : quadrilatere par defaut (marges 5%)
    const mw = Math.round(src.cols * 0.05)
    const mh = Math.round(src.rows * 0.05)
    return [
      { x: mw,            y: mh },
      { x: src.cols - mw, y: mh },
      { x: src.cols - mw, y: src.rows - mh },
      { x: mw,            y: src.rows - mh },
    ]
  } finally {
    src.delete()
    gray.delete()
    edges.delete()
    contours.delete()
    hierarchy.delete()
  }
}

/**
 * Tri 4 points en [tl, tr, br, bl] en fonction de leur position.
 */
function orderCorners(pts) {
  const sorted = [...pts].sort((a, b) => a.y - b.y)
  const top = sorted.slice(0, 2).sort((a, b) => a.x - b.x)
  const bottom = sorted.slice(2).sort((a, b) => a.x - b.x)
  return [top[0], top[1], bottom[1], bottom[0]]  // tl, tr, br, bl
}

/**
 * Applique la transformation perspective + filtre.
 * @param filterMode 'color' | 'grayscale' | 'scan' (seuil adaptatif N&B)
 * @returns un nouveau canvas avec l'image redressee
 */
export function warpAndFilter(cv, srcCanvas, corners, filterMode = 'color') {
  const src = cv.imread(srcCanvas)
  const [tl, tr, br, bl] = corners

  const widthTop    = Math.hypot(tr.x - tl.x, tr.y - tl.y)
  const widthBottom = Math.hypot(br.x - bl.x, br.y - bl.y)
  const heightLeft  = Math.hypot(bl.x - tl.x, bl.y - tl.y)
  const heightRight = Math.hypot(br.x - tr.x, br.y - tr.y)
  const w = Math.max(1, Math.round(Math.max(widthTop, widthBottom)))
  const h = Math.max(1, Math.round(Math.max(heightLeft, heightRight)))

  const srcPts = cv.matFromArray(4, 1, cv.CV_32FC2, [
    tl.x, tl.y, tr.x, tr.y, br.x, br.y, bl.x, bl.y,
  ])
  const dstPts = cv.matFromArray(4, 1, cv.CV_32FC2, [
    0, 0, w, 0, w, h, 0, h,
  ])
  const M = cv.getPerspectiveTransform(srcPts, dstPts)
  const warped = new cv.Mat()
  cv.warpPerspective(src, warped, M, new cv.Size(w, h))

  // Filtre final
  let finalMat = warped
  if (filterMode === 'scan') {
    const gray = new cv.Mat()
    cv.cvtColor(warped, gray, cv.COLOR_RGBA2GRAY)
    const thresh = new cv.Mat()
    cv.adaptiveThreshold(gray, thresh, 255, cv.ADAPTIVE_THRESH_GAUSSIAN_C,
      cv.THRESH_BINARY, 15, 10)
    gray.delete()
    warped.delete()
    finalMat = thresh
  } else if (filterMode === 'grayscale') {
    const gray = new cv.Mat()
    cv.cvtColor(warped, gray, cv.COLOR_RGBA2GRAY)
    warped.delete()
    finalMat = gray
  }

  const out = document.createElement('canvas')
  cv.imshow(out, finalMat)

  src.delete()
  srcPts.delete()
  dstPts.delete()
  M.delete()
  finalMat.delete()

  return out
}

/**
 * Cree un canvas a partir d'un blob image.
 * Utile car cv.imread accepte un canvas, pas un blob.
 */
export async function blobToCanvas(blob) {
  const url = URL.createObjectURL(blob)
  try {
    const img = await new Promise((resolve, reject) => {
      const im = new Image()
      im.onload = () => resolve(im)
      im.onerror = reject
      im.src = url
    })
    const canvas = document.createElement('canvas')
    canvas.width = img.naturalWidth
    canvas.height = img.naturalHeight
    const ctx = canvas.getContext('2d')
    ctx.drawImage(img, 0, 0)
    return canvas
  } finally {
    URL.revokeObjectURL(url)
  }
}

/** Promesse vers un blob JPEG depuis un canvas. */
export function canvasToJpegBlob(canvas, quality = 0.92) {
  return new Promise(resolve => canvas.toBlob(b => resolve(b), 'image/jpeg', quality))
}
