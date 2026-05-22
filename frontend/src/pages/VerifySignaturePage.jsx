import { useState } from 'react'
import { ShieldCheck, CheckCircle2, XCircle, Search } from 'lucide-react'
import DropZone from '../components/DropZone'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import { useToast } from '../components/Toast'
import { useProgress } from '../hooks/useProgress'
import { verifySignature } from '../api/pdfApi'

export default function VerifySignaturePage() {
  const [files, setFiles] = useState([])
  const [loading, setLoading] = useState(false)
  const [result, setResult] = useState(null)
  const toast = useToast()
  const { progress, onProgress, reset } = useProgress()

  async function handleSubmit() {
    if (!files[0]) return toast.error('Sélectionnez un PDF.')
    setLoading(true)
    setResult(null)
    try {
      const data = await verifySignature(files[0], onProgress)
      setResult(data)
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
      reset()
    }
  }

  return (
    <ToolPage
      icon={ShieldCheck}
      title="Vérification de signature"
      subtitle="Détecte et liste les signatures numériques d'un PDF avec le signataire, la date et la validité cryptographique."
    >
      <DropZone onFiles={setFiles} label="Déposez le PDF signé" />

      <div className="mt-6">
        <SubmitButton loading={loading} progress={progress} onClick={handleSubmit} disabled={!files[0]} icon={Search}>
          Analyser les signatures
        </SubmitButton>
      </div>

      {result && (
        <div className="mt-6">
          {result.count === 0 && (
            <div className="rounded-xl border border-amber-200 dark:border-amber-800 bg-amber-50/60 dark:bg-amber-950/30 p-4 text-sm text-ink-700 dark:text-ink-200">
              Aucune signature numérique détectée dans ce document.
            </div>
          )}
          {result.count > 0 && (
            <div className="space-y-3">
              <p className="text-sm text-ink-500">{result.count} signature(s) trouvée(s)</p>
              {result.signatures.map((sig, i) => (
                <div key={i} className={`rounded-xl border p-4 ${
                  sig.valid
                    ? 'border-emerald-300 dark:border-emerald-700 bg-emerald-50/60 dark:bg-emerald-950/30'
                    : 'border-rose-300 dark:border-rose-700 bg-rose-50/60 dark:bg-rose-950/30'
                }`}>
                  <div className="flex items-start gap-3">
                    {sig.valid
                      ? <CheckCircle2 size={20} className="shrink-0 text-emerald-600 dark:text-emerald-400" />
                      : <XCircle size={20} className="shrink-0 text-rose-600 dark:text-rose-400" />}
                    <div className="flex-1 min-w-0 text-sm">
                      <p className={`font-semibold ${sig.valid ? 'text-emerald-800 dark:text-emerald-200' : 'text-rose-800 dark:text-rose-200'}`}>
                        {sig.valid ? 'Signature valide' : 'Signature invalide ou intégrité non vérifiable'}
                      </p>
                      <dl className="mt-2 grid grid-cols-1 sm:grid-cols-[120px_1fr] gap-x-3 gap-y-1 text-ink-700 dark:text-ink-200">
                        {sig.signer && <><dt className="text-ink-500">Signataire :</dt><dd className="truncate">{sig.signer}</dd></>}
                        {sig.name && <><dt className="text-ink-500">Champ :</dt><dd>{sig.name}</dd></>}
                        {sig.date && <><dt className="text-ink-500">Date :</dt><dd>{sig.date}</dd></>}
                        {sig.reason && <><dt className="text-ink-500">Motif :</dt><dd>{sig.reason}</dd></>}
                        {sig.location && <><dt className="text-ink-500">Lieu :</dt><dd>{sig.location}</dd></>}
                      </dl>
                    </div>
                  </div>
                </div>
              ))}
              <p className="text-xs text-ink-400 dark:text-ink-500">
                Note : la validité ici signifie que le PKCS#7 embarqué est cohérent et que le PDF n'a pas été modifié après signature.
                La chaîne de confiance du certificat (CA racine) n'est pas vérifiée.
              </p>
            </div>
          )}
        </div>
      )}
    </ToolPage>
  )
}
