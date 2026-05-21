import { FileEdit } from 'lucide-react'

/**
 * Input optionnel pour personnaliser le nom du fichier de sortie.
 * Le backend ajoutera automatiquement l'extension si elle est manquante.
 *
 * Usage :
 *   const [outputName, setOutputName] = useState('')
 *   <OutputFilenameField value={outputName} onChange={setOutputName} placeholder="mon-document" />
 *
 *   // puis a l'envoi :
 *   form.append('outputName', outputName)  // optionnel
 */
export default function OutputFilenameField({
  value,
  onChange,
  placeholder = 'nom-personnalise',
  extension,           // ex: ".pdf" — affiche un hint
}) {
  return (
    <div>
      <label className="field-label flex items-center gap-1.5">
        <FileEdit size={14} className="text-ink-400" />
        Nom du fichier <span className="text-ink-400 font-normal">(optionnel)</span>
      </label>
      <input
        type="text"
        value={value}
        onChange={e => onChange(e.target.value)}
        className="field"
        placeholder={placeholder}
        maxLength={100}
      />
      <p className="mt-1 text-xs text-ink-400 dark:text-ink-500">
        {extension
          ? `Si vide, un nom par defaut sera utilise. L'extension ${extension} est ajoutee automatiquement.`
          : "Si vide, un nom par defaut sera utilise."}
      </p>
    </div>
  )
}
