import { useState } from 'react'
import { FileCode2 } from 'lucide-react'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import OutputFilenameField from '../components/OutputFilenameField'
import { useToast } from '../components/Toast'
import { useProgress } from '../hooks/useProgress'
import { markdownToPdf } from '../api/pdfApi'

const SAMPLE = `# Mon document

Voici un **paragraphe** avec un peu de _markdown_.

## Listes

- item 1
- item 2
- item 3

## Code

\`\`\`
const greeting = "Bonjour";
console.log(greeting);
\`\`\`

> Une citation pour finir.
`

export default function MarkdownToPdfPage() {
  const [title, setTitle] = useState('Mon document')
  const [markdown, setMarkdown] = useState(SAMPLE)
  const [outputName, setOutputName] = useState('')
  const [loading, setLoading] = useState(false)
  const toast = useToast()
  const { progress, onProgress, reset } = useProgress()

  async function handleSubmit() {
    if (!markdown.trim()) return toast.error('Saisissez le contenu Markdown.')
    setLoading(true)
    try {
      await markdownToPdf(markdown, title.trim() || 'Document', outputName, onProgress)
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
      icon={FileCode2}
      title="Markdown → PDF"
      subtitle="Conversion Markdown vers PDF avec mise en forme (titres, listes, code, citations)."
    >
      <div className="space-y-5">
        <div>
          <label className="field-label">Titre du document</label>
          <input value={title} onChange={e => setTitle(e.target.value)} className="field" />
        </div>
        <div>
          <label className="field-label">Contenu Markdown — {markdown.length} caractères</label>
          <textarea
            value={markdown}
            onChange={e => setMarkdown(e.target.value)}
            className="field min-h-72 font-mono text-sm"
            placeholder="# Titre&#10;&#10;Votre contenu en markdown..."
          />
        </div>
        <OutputFilenameField value={outputName} onChange={setOutputName} placeholder="mon-document" extension=".pdf" />
      </div>
      <div className="mt-6">
        <SubmitButton loading={loading} progress={progress} onClick={handleSubmit} disabled={!markdown.trim()}>
          Générer le PDF
        </SubmitButton>
      </div>
    </ToolPage>
  )
}
