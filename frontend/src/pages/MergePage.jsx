import { useState } from 'react'
import DropZone from '../components/DropZone'
import { mergePDFs } from '../api/pdfApi'
import { Layers, Loader2 } from 'lucide-react'

export default function MergePage() {
  const [files,   setFiles]   = useState([])
  const [loading, setLoading] = useState(false)
  const [msg,     setMsg]     = useState(null)

  async function handleMerge() {
    if (files.length < 2) {
      setMsg({ type: 'error', text: 'Sélectionnez au moins 2 PDFs' })
      return
    }
    setLoading(true)
    setMsg(null)
    try {
      await mergePDFs(files)
      setMsg({ type: 'success', text: 'Fusion réussie — téléchargement en cours' })
    } catch(e) {
      setMsg({ type: 'error', text: e.message })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-2xl mx-auto px-4 py-10">
      <div className="flex items-center gap-3 mb-8">
        <Layers className="text-primary-600" size={28} />
        <h1 className="text-2xl font-bold text-gray-900">Fusion de PDFs</h1>
      </div>

      <DropZone
        onFiles={setFiles}
        multiple={true}
        label="Déposez vos PDFs à fusionner (dans l'ordre souhaité)"
      />

      {files.length > 0 && (
        <p className="text-sm text-gray-500 mt-2">
          {files.length} fichier(s) sélectionné(s)
        </p>
      )}

      <button
        onClick={handleMerge}
        disabled={loading}
        className="mt-6 w-full bg-primary-600 hover:bg-primary-700 disabled:opacity-50
                   text-white font-semibold py-3 rounded-xl transition-colors
                   flex items-center justify-center gap-2"
      >
        {loading
          ? <><Loader2 className="animate-spin" size={18}/> Fusion en cours...</>
          : 'Fusionner les PDFs'
        }
      </button>

      {msg && (
        <div className={`mt-4 p-4 rounded-xl text-sm font-medium
          ${msg.type === 'success'
            ? 'bg-green-50 text-green-700 border border-green-200'
            : 'bg-red-50 text-red-700 border border-red-200'
          }`}>
          {msg.text}
        </div>
      )}
    </div>
  )
}