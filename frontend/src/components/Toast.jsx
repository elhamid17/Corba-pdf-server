import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { CheckCircle2, AlertTriangle, Info, X } from 'lucide-react'

const ToastContext = createContext(null)

export function useToast() {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast must be used within <ToastProvider>')
  return ctx
}

let _id = 0

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])

  const remove = useCallback(id => setToasts(t => t.filter(x => x.id !== id)), [])

  const push = useCallback((toast) => {
    const id = ++_id
    setToasts(t => [...t, { id, duration: 4200, ...toast }])
    return id
  }, [])

  const api = {
    success: (text) => push({ type: 'success', text }),
    error:   (text) => push({ type: 'error',   text, duration: 6000 }),
    info:    (text) => push({ type: 'info',    text }),
  }

  return (
    <ToastContext.Provider value={api}>
      {children}
      <div
        className="fixed top-4 right-4 z-50 flex flex-col gap-2 max-w-sm w-full pointer-events-none"
        aria-live="polite"
        aria-relevant="additions"
        role="status"
      >
        {toasts.map(t => <ToastItem key={t.id} toast={t} onClose={() => remove(t.id)} />)}
      </div>
    </ToastContext.Provider>
  )
}

function ToastItem({ toast, onClose }) {
  useEffect(() => {
    const id = setTimeout(onClose, toast.duration)
    return () => clearTimeout(id)
  }, [toast.duration, onClose])

  const cfg = {
    success: { icon: CheckCircle2,  cls: 'border-emerald-200 bg-white text-ink-800 dark:border-emerald-800 dark:bg-ink-900 dark:text-ink-100', iconCls: 'text-emerald-600 dark:text-emerald-400' },
    error:   { icon: AlertTriangle, cls: 'border-rose-200 bg-white text-ink-800 dark:border-rose-800 dark:bg-ink-900 dark:text-ink-100',       iconCls: 'text-rose-600 dark:text-rose-400'    },
    info:    { icon: Info,          cls: 'border-brand-200 bg-white text-ink-800 dark:border-brand-800 dark:bg-ink-900 dark:text-ink-100',     iconCls: 'text-brand-600 dark:text-brand-400'   },
  }[toast.type] || { icon: Info, cls: 'border-ink-200 bg-white text-ink-800 dark:border-ink-700 dark:bg-ink-900 dark:text-ink-100', iconCls: 'text-ink-500 dark:text-ink-400' }

  const Icon = cfg.icon

  return (
    <div className={`pointer-events-auto flex items-start gap-3 rounded-xl border px-4 py-3 shadow-card animate-slide-up ${cfg.cls}`}>
      <Icon className={`shrink-0 mt-0.5 ${cfg.iconCls}`} size={18} />
      <p className="flex-1 text-sm leading-snug">{toast.text}</p>
      <button onClick={onClose} aria-label="Fermer la notification" className="text-ink-400 hover:text-ink-700 dark:text-ink-500 dark:hover:text-ink-200 transition-colors">
        <X size={16} />
      </button>
    </div>
  )
}
