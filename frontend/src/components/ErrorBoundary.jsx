import { Component } from 'react'
import { Link } from 'react-router-dom'
import { AlertTriangle, RefreshCw } from 'lucide-react'

export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { error: null }
  }

  static getDerivedStateFromError(error) {
    return { error }
  }

  componentDidCatch(error, info) {
    console.error('ErrorBoundary:', error, info)
  }

  handleRetry = () => {
    this.setState({ error: null })
  }

  render() {
    if (this.state.error) {
      return (
        <div className="mx-auto max-w-lg px-4 py-20 text-center">
          <div className="inline-flex h-14 w-14 items-center justify-center rounded-2xl bg-rose-100 dark:bg-rose-950/50 text-rose-600 dark:text-rose-400 mb-5">
            <AlertTriangle size={28} />
          </div>
          <h1 className="text-xl font-display font-bold text-ink-900 dark:text-ink-100">
            Une erreur est survenue
          </h1>
          <p className="mt-2 text-sm text-ink-500 dark:text-ink-400 leading-relaxed">
            L'interface a rencontré un problème inattendu. Vous pouvez réessayer ou retourner à l'accueil.
          </p>
          <div className="mt-6 flex flex-wrap items-center justify-center gap-3">
            <button type="button" onClick={this.handleRetry} className="btn-primary">
              <RefreshCw size={16} /> Réessayer
            </button>
            <Link to="/" className="btn-secondary" onClick={this.handleRetry}>
              Accueil
            </Link>
          </div>
        </div>
      )
    }
    return this.props.children
  }
}
