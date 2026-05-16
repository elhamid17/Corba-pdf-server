import { useState } from 'react'
import DropZone from '../components/DropZone'
import { FileText, Loader2 } from 'lucide-react'

export default function WatermarkPage() {
  const [file,    setFile]    = useState(null)
  const [loading, setLoading] = useState(false)
  const [msg,     setMsg]     = useState(null)

  async function handleSubmit() {
    if (!file) { setMsg({ type: 'error', text: 'Sélectionnez un PDF' }); return }
    setLoading(true); setMsg(null)
    try {
      setMsg({ type: 'success', text: 'Opération réussie !' })
    } catch(e) {
      setMsg({ type: 'error', text: e.message })
    } finally { setLoading(false) }
  }

  return (
    <div className="max-w-2xl mx-auto px-4 py-10">
      <div className="flex items-center gap-3 mb-8">
        <FileText className="text-primary-600" size={28} />
        <h1 className="text-2xl font-bold text-gray-900">WatermarkPage</h1>
      </div>
      <DropZone onFiles={f => setFile(f[0])} label="Déposez votre PDF ici" />
      <button onClick={handleSubmit} disabled={loading}
        className="mt-6 w-full bg-primary-600 hover:bg-primary-700 disabled:opacity-50
                   text-white font-semibold py-3 rounded-xl transition-colors
                   flex items-center justify-center gap-2">
        {loading ? <><Loader2 className="animate-spin" size={18}/> Traitement...</> : 'Lancer'}
      </button>
      {msg && (
        <div className={`mt-4 p-4 rounded-xl text-sm font-medium ${
          msg.type === 'success'
            ? 'bg-green-50 text-green-700 border border-green-200'
            : 'bg-red-50 text-red-700 border border-red-200'}`}>
          {msg.text}
        </div>
      )}
    </div>
  )
}
