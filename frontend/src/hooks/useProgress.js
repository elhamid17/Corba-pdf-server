import { useCallback, useState } from 'react'

/**
 * Hook simple pour suivre la progression d'une operation PDF.
 *
 * Usage :
 *   const { progress, onProgress, reset } = useProgress()
 *   await mergePDFs(files, outputName, onProgress)
 *   reset()  // dans le finally
 *
 *   <SubmitButton loading={loading} progress={progress} ...>
 *
 * Le state `progress` est :
 *   null                               | rien en cours
 *   { phase: 'upload',     percent }   | upload des octets vers le serveur
 *   { phase: 'processing', percent: null } | le serveur traite (indetermine)
 *   { phase: 'download',   percent }   | reception de la reponse
 *   { phase: 'done',       percent: 100 } | termine
 */
export function useProgress() {
  const [progress, setProgress] = useState(null)
  const onProgress = useCallback((p) => setProgress(p), [])
  const reset = useCallback(() => setProgress(null), [])
  return { progress, onProgress, reset }
}
