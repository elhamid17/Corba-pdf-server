import { Wand2 } from 'lucide-react'

export default function Footer() {
  return (
    <footer className="mt-12 border-t border-ink-200/70 dark:border-ink-800/70 bg-white/70 dark:bg-ink-900/70 backdrop-blur">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8 py-8 grid gap-4 sm:grid-cols-2 items-center">
        <div className="flex items-center gap-2.5">
          <span className="grid place-items-center h-8 w-8 rounded-lg bg-gradient-to-br from-brand-600 to-accent-500 text-white">
            <Wand2 size={14} strokeWidth={2.5}/>
          </span>
          <div className="text-sm">
            <p className="font-semibold text-ink-800 dark:text-ink-100">CORBA PDF Suite</p>
            <p className="text-ink-500 dark:text-ink-400 text-xs">Traitement PDF distribué — JacORB · PDFBox · Spring Boot</p>
          </div>
        </div>
        <p className="text-xs text-ink-500 dark:text-ink-400 sm:text-right">
          USSEIN · L2 AgroTIC · © {new Date().getFullYear()} — Projet académique
        </p>
      </div>
    </footer>
  )
}
