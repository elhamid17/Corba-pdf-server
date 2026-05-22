import { useState } from 'react'
import { Globe } from 'lucide-react'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import OutputFilenameField from '../components/OutputFilenameField'
import { useToast } from '../components/Toast'
import { useProgress } from '../hooks/useProgress'
import { htmlToPdf } from '../api/pdfApi'

const SAMPLE = `<h1>Mon document</h1>
<p>Voici un paragraphe avec du <strong>gras</strong> et de l'<em>italique</em>.</p>

<h2>Liste à puces</h2>
<ul>
  <li>Premier item</li>
  <li>Deuxième item</li>
  <li>Troisième item</li>
</ul>

<blockquote>Une citation inspirante.</blockquote>
`

export default function HtmlToPdfPage() {
  const [title, setTitle] = useState('Mon document')
  const [html, setHtml] = useState(SAMPLE)
  const [outputName, setOutputName] = useState('')
  const [loading, setLoading] = useState(false)
  const toast = useToast()
  const { progress, onProgress, reset } = useProgress()

  async function handleSubmit() {
    if (!html.trim()) return toast.error('Saisissez le code HTML.')
    setLoading(true)
    try {
      await htmlToPdf(html, title.trim() || 'Document', outputName, onProgress)
      toast.success('PDF généré.')
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
      reset()
    }
  }

  return (
    <ToolPage
      icon={Globe}
      title="HTML → PDF"
      subtitle="Conversion de code HTML en PDF. Les balises h1-h6, p, ul/ol/li, blockquote, pre/code sont rendues ; les styles CSS sont ignorés."
    >
      <div className="space-y-5">
        <div>
          <label className="field-label">Titre du document</label>
          <input value={title} onChange={e => setTitle(e.target.value)} className="field" />
        </div>
        <div>
          <label className="field-label">Code HTML — {html.length} caractères</label>
          <textarea
            value={html}
            onChange={e => setHtml(e.target.value)}
            className="field min-h-72 font-mono text-sm"
            placeholder="<h1>Titre</h1>&#10;<p>Paragraphe...</p>"
          />
        </div>
        <OutputFilenameField value={outputName} onChange={setOutputName} placeholder="page-web" extension=".pdf" />
      </div>
      <div className="mt-6">
        <SubmitButton loading={loading} progress={progress} onClick={handleSubmit} disabled={!html.trim()}>
          Générer le PDF
        </SubmitButton>
      </div>
    </ToolPage>
  )
}
