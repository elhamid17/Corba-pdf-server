import { useState } from 'react'
import { PenTool } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import OutputFilenameField from '../components/OutputFilenameField'
import { useToast } from '../components/Toast'
import { useProgress } from '../hooks/useProgress'
import { signPDF } from '../api/pdfApi'

export default function SignPage() {
  const [pdfFiles, setPdfFiles] = useState([])
  const [certFiles, setCertFiles] = useState([])
  const [password, setPassword] = useState('')
  const [reason, setReason] = useState('Signature numérique')
  const [location, setLocation] = useState('Sénégal')
  const [outputName, setOutputName] = useState('')
  const [loading, setLoading] = useState(false)
  const toast = useToast()
  const { progress, onProgress, reset } = useProgress()

  async function handleSubmit() {
    if (!pdfFiles[0])  return toast.error('Sélectionnez un PDF à signer.')
    if (!certFiles[0]) return toast.error('Fournissez un certificat (.p12).')
    if (!password)     return toast.error('Saisissez le mot de passe du certificat.')
    setLoading(true)
    try {
      await signPDF(pdfFiles[0], certFiles[0], password, reason, location, outputName, onProgress)
      toast.success('Signature appliquée.')
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
      reset()
    }
  }

  return (
    <ToolPage
      icon={PenTool}
      title="Signature numérique"
      subtitle="Signez un PDF à l’aide d’un certificat PKCS#12 (.p12). Conforme aux standards PKI."
      footer="Le certificat doit contenir une clé privée + une chaîne X.509."
    >
      <div className="grid gap-5">
        <div>
          <label className="field-label">Document à signer</label>
          <DropZone onFiles={setPdfFiles} label="Déposez le PDF" />
        </div>

        <div>
          <label className="field-label">Certificat PKCS#12</label>
          <DropZone
            onFiles={setCertFiles}
            accept={{ 'application/x-pkcs12': ['.p12', '.pfx'] }}
            label="Déposez votre .p12 ou .pfx"
            hint="Fichier de certificat avec clé privée"
          />
        </div>

        <div>
          <label className="field-label">Mot de passe du certificat</label>
          <input type="password" value={password} onChange={e => setPassword(e.target.value)} className="field" autoComplete="off" />
        </div>

        <div className="grid sm:grid-cols-2 gap-4">
          <div>
            <label className="field-label">Motif</label>
            <input value={reason} onChange={e => setReason(e.target.value)} className="field" />
          </div>
          <div>
            <label className="field-label">Lieu</label>
            <input value={location} onChange={e => setLocation(e.target.value)} className="field" />
          </div>
        </div>

        <OutputFilenameField value={outputName} onChange={setOutputName} placeholder="document-signe" extension=".pdf" />
      </div>

      <div className="mt-6">
        <SubmitButton loading={loading} progress={progress} onClick={handleSubmit} disabled={!pdfFiles[0] || !certFiles[0] || !password}>
          Signer le PDF
        </SubmitButton>
      </div>
    </ToolPage>
  )
}
