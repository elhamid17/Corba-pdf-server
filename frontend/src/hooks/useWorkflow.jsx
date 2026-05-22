import { createContext, useContext, useEffect, useState, useCallback } from 'react'

/**
 * Workflow chaining : capture le resultat de chaque operation reussie
 * pour permettre de l'enchaîner dans un outil suivant sans re-uploader.
 *
 * - pdfApi.js capture le blob via setWorkflowCallback (callback module-level)
 * - WorkflowProvider tient le state `lastResult` = { blob, filename, sourcePath, ts }
 * - WorkflowSuggestions lit le state et affiche un bandeau d'actions suivantes
 * - DropZone consomme le blob automatiquement au mount si type compatible
 */

const WorkflowContext = createContext(null)

// Callback enregistre par WorkflowProvider, appele depuis pdfApi.downloadBlob
let workflowCallback = null
export function notifyResult(blob, filename, sourcePath) {
  if (workflowCallback) workflowCallback(blob, filename, sourcePath)
}

export function WorkflowProvider({ children }) {
  // { blob, filename, sourcePath, ts } | null
  const [lastResult, setLastResult] = useState(null)

  useEffect(() => {
    workflowCallback = (blob, filename, sourcePath) => {
      setLastResult({ blob, filename, sourcePath: sourcePath || window.location.pathname, ts: Date.now() })
    }
    return () => { workflowCallback = null }
  }, [])

  const consume = useCallback(() => {
    const r = lastResult
    setLastResult(null)
    return r
  }, [lastResult])

  const clear = useCallback(() => setLastResult(null), [])

  return (
    <WorkflowContext.Provider value={{ lastResult, consume, clear }}>
      {children}
    </WorkflowContext.Provider>
  )
}

export function useWorkflow() {
  const ctx = useContext(WorkflowContext)
  if (!ctx) throw new Error('useWorkflow doit etre utilise dans WorkflowProvider')
  return ctx
}
