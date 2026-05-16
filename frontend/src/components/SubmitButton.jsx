import { Loader2 } from 'lucide-react'

export default function SubmitButton({ loading, disabled, children, loadingText = 'Traitement en cours…', icon: Icon, ...rest }) {
  return (
    <button
      {...rest}
      disabled={loading || disabled}
      className="btn-primary w-full py-3 text-base"
    >
      {loading
        ? <><Loader2 className="animate-spin" size={18}/> {loadingText}</>
        : <>{Icon && <Icon size={18} />} {children}</>}
    </button>
  )
}
