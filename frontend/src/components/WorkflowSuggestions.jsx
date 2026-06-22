import { useNavigate, useLocation } from 'react-router-dom'
import { AnimatePresence, motion } from 'framer-motion'
import { ArrowRight, Sparkles, X } from 'lucide-react'
import { useWorkflow } from '../hooks/useWorkflow'
import { WORKFLOW_SUGGESTIONS } from './workflowSuggestionsMap'

export default function WorkflowSuggestions() {
  const { lastResult, clear } = useWorkflow()
  const navigate = useNavigate()
  const location = useLocation()

  if (!lastResult || lastResult.sourcePath !== location.pathname) return null

  const isChainablePdf = lastResult.filename?.toLowerCase().endsWith('.pdf')
  const suggestions = WORKFLOW_SUGGESTIONS[location.pathname] || []
  const filteredSuggestions = isChainablePdf ? suggestions : []
  if (filteredSuggestions.length === 0) return null

  function handleClickSuggestion(to) {
    navigate(to)
  }

  return (
    <AnimatePresence>
      <motion.div
        key="wf-suggest"
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        exit={{ opacity: 0 }}
        transition={{ duration: 0.25 }}
        className="mt-8 rounded-2xl border border-brand-200 dark:border-brand-800/60 bg-gradient-to-br from-brand-50 to-accent-50 dark:from-brand-950/40 dark:to-accent-950/40 p-5 relative"
      >
        <button
          type="button"
          onClick={clear}
          className="absolute top-2 right-2 grid place-items-center h-7 w-7 rounded-md text-ink-400 hover:text-ink-700 hover:bg-white/50 dark:hover:bg-ink-800 transition-colors"
          aria-label="Fermer les suggestions"
        >
          <X size={14} />
        </button>
        <div className="flex items-center gap-2 mb-1.5">
          <Sparkles size={14} className="text-brand-600 dark:text-brand-400" />
          <p className="text-sm font-semibold text-ink-900 dark:text-ink-100">
            Et après ?
          </p>
        </div>
        <p className="text-xs text-ink-600 dark:text-ink-400 mb-4">
          Enchaînez avec un outil compatible — votre résultat sera automatiquement chargé.
        </p>
        <div className="flex flex-wrap gap-2">
          {filteredSuggestions.map(s => {
            const Icon = s.icon
            return (
              <button
                key={s.to}
                type="button"
                onClick={() => handleClickSuggestion(s.to)}
                className="inline-flex items-center gap-2 h-10 px-4 rounded-lg bg-white dark:bg-ink-900 border border-ink-200 dark:border-ink-700 text-sm font-semibold text-ink-800 dark:text-ink-100 hover:border-brand-400 hover:shadow-sm transition-all group"
              >
                <Icon size={16} className="text-brand-600 dark:text-brand-400" />
                {s.label}
                <ArrowRight size={14} className="text-ink-400 group-hover:translate-x-0.5 transition-transform" />
              </button>
            )
          })}
        </div>
      </motion.div>
    </AnimatePresence>
  )
}
