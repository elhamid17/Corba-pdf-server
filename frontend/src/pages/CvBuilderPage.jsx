import { useState } from 'react'
import { UserCircle2, Plus, X } from 'lucide-react'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import OutputFilenameField from '../components/OutputFilenameField'
import { useToast } from '../components/Toast'
import { useProgress } from '../hooks/useProgress'
import { generateCv } from '../api/pdfApi'

export default function CvBuilderPage() {
  const [name, setName] = useState('Jean Dupont')
  const [title, setTitle] = useState('Développeur Full-Stack')
  const [email, setEmail] = useState('jean@exemple.com')
  const [phone, setPhone] = useState('+221 77 000 00 00')
  const [address, setAddress] = useState('Dakar, Sénégal')
  const [summary, setSummary] = useState('Passionné de développement web et de design distribué.')
  const [experiences, setExperiences] = useState([
    { period: '2023 - présent', role: 'Développeur', company: 'CORBA PDF Suite', description: 'Développement d\'une plateforme PDF distribuée.' },
  ])
  const [education, setEducation] = useState([
    { period: '2024 - 2026', diploma: 'Licence AgroTIC', school: 'USSEIN', description: '' },
  ])
  const [skills, setSkills] = useState('Java, React, CORBA, Docker, PDFBox')
  const [languages, setLanguages] = useState('Français (natif), Anglais (B2), Wolof (natif)')
  const [outputName, setOutputName] = useState('')
  const [loading, setLoading] = useState(false)
  const toast = useToast()
  const { progress, onProgress, reset } = useProgress()

  async function handleSubmit() {
    if (!name.trim()) return toast.error('Le nom est obligatoire.')
    setLoading(true)
    try {
      const cvData = {
        name: name.trim(), title, email, phone, address, summary,
        experiences: experiences.filter(e => e.role || e.company),
        education: education.filter(e => e.diploma || e.school),
        skills: skills.split(',').map(s => s.trim()).filter(Boolean),
        languages: languages.split(',').map(s => s.trim()).filter(Boolean),
      }
      await generateCv(cvData, outputName, onProgress)
      toast.success('CV généré.')
    } catch (e) {
      toast.error(e.message)
    } finally {
      setLoading(false)
      reset()
    }
  }

  return (
    <ToolPage
      icon={UserCircle2}
      title="Générateur de CV"
      subtitle="Remplissez le formulaire — un CV PDF propre et mis en page est généré automatiquement."
    >
      <div className="space-y-5">
        <Section title="Identité">
          <div className="grid sm:grid-cols-2 gap-3">
            <Field label="Nom complet" value={name} onChange={setName} placeholder="Jean Dupont" />
            <Field label="Titre / fonction" value={title} onChange={setTitle} placeholder="Développeur" />
            <Field label="Email" value={email} onChange={setEmail} placeholder="jean@exemple.com" />
            <Field label="Téléphone" value={phone} onChange={setPhone} placeholder="+221..." />
            <div className="sm:col-span-2">
              <Field label="Adresse" value={address} onChange={setAddress} placeholder="Ville, Pays" />
            </div>
          </div>
        </Section>

        <Section title="Profil">
          <textarea value={summary} onChange={e => setSummary(e.target.value)}
                    className="field min-h-20 text-sm" placeholder="Quelques lignes sur vous…" />
        </Section>

        <Section title="Expérience">
          {experiences.map((exp, i) => (
            <ListRow key={i} onRemove={() => setExperiences(experiences.filter((_, j) => j !== i))}>
              <Field label="Période" value={exp.period} onChange={v => updateAt(experiences, setExperiences, i, 'period', v)} placeholder="2023 - présent" />
              <Field label="Poste" value={exp.role} onChange={v => updateAt(experiences, setExperiences, i, 'role', v)} placeholder="Développeur" />
              <Field label="Entreprise" value={exp.company} onChange={v => updateAt(experiences, setExperiences, i, 'company', v)} placeholder="USSEIN" />
              <Field label="Description" value={exp.description} onChange={v => updateAt(experiences, setExperiences, i, 'description', v)} placeholder="Vos réalisations…" textarea />
            </ListRow>
          ))}
          <button type="button" onClick={() => setExperiences([...experiences, { period: '', role: '', company: '', description: '' }])}
                  className="btn-ghost h-8 px-3 text-xs"><Plus size={12} /> Ajouter une expérience</button>
        </Section>

        <Section title="Formation">
          {education.map((ed, i) => (
            <ListRow key={i} onRemove={() => setEducation(education.filter((_, j) => j !== i))}>
              <Field label="Période" value={ed.period} onChange={v => updateAt(education, setEducation, i, 'period', v)} placeholder="2024 - 2026" />
              <Field label="Diplôme" value={ed.diploma} onChange={v => updateAt(education, setEducation, i, 'diploma', v)} placeholder="Licence" />
              <Field label="Établissement" value={ed.school} onChange={v => updateAt(education, setEducation, i, 'school', v)} placeholder="USSEIN" />
              <Field label="Détails" value={ed.description} onChange={v => updateAt(education, setEducation, i, 'description', v)} placeholder="Mention, spécialité…" textarea />
            </ListRow>
          ))}
          <button type="button" onClick={() => setEducation([...education, { period: '', diploma: '', school: '', description: '' }])}
                  className="btn-ghost h-8 px-3 text-xs"><Plus size={12} /> Ajouter une formation</button>
        </Section>

        <Section title="Compétences (séparées par des virgules)">
          <input value={skills} onChange={e => setSkills(e.target.value)} className="field" placeholder="Java, React, Docker…" />
        </Section>

        <Section title="Langues (séparées par des virgules)">
          <input value={languages} onChange={e => setLanguages(e.target.value)} className="field" placeholder="Français, Anglais B2…" />
        </Section>

        <OutputFilenameField value={outputName} onChange={setOutputName} placeholder="mon-cv" extension=".pdf" />
      </div>

      <div className="mt-6">
        <SubmitButton loading={loading} progress={progress} onClick={handleSubmit} disabled={!name.trim()}>
          Générer le CV
        </SubmitButton>
      </div>
    </ToolPage>
  )
}

function updateAt(arr, setter, i, key, value) {
  setter(arr.map((item, j) => j === i ? { ...item, [key]: value } : item))
}

function Section({ title, children }) {
  return (
    <div className="space-y-2">
      <h3 className="text-xs font-semibold uppercase tracking-wider text-ink-500 dark:text-ink-400">{title}</h3>
      {children}
    </div>
  )
}

function Field({ label, value, onChange, placeholder, textarea }) {
  return (
    <div>
      <label className="text-xs font-medium text-ink-600 dark:text-ink-400">{label}</label>
      {textarea
        ? <textarea value={value} onChange={e => onChange(e.target.value)} className="field min-h-16 text-sm" placeholder={placeholder} />
        : <input value={value} onChange={e => onChange(e.target.value)} className="field text-sm" placeholder={placeholder} />}
    </div>
  )
}

function ListRow({ children, onRemove }) {
  return (
    <div className="relative rounded-xl border border-ink-200 dark:border-ink-700 p-3 space-y-2">
      <button type="button" onClick={onRemove} className="absolute top-2 right-2 text-ink-400 hover:text-rose-600 dark:hover:text-rose-400" title="Retirer">
        <X size={14} />
      </button>
      <div className="grid sm:grid-cols-2 gap-3">{children}</div>
    </div>
  )
}
