import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { UserCircle2, Plus, X, Trash2, Upload, FileText, Loader2, Layout, Palette } from 'lucide-react'
import ToolPage from '../components/ToolPage'
import SubmitButton from '../components/SubmitButton'
import OutputFilenameField from '../components/OutputFilenameField'
import PdfPreview from '../components/PdfPreview'
import { useToast } from '../components/Toast'
import { useProgress } from '../hooks/useProgress'
import { API_BASE, getToken } from '../api/client'
import { PDF } from '../api/routes'
import { generateCv } from '../api/pdfApi'

const DRAFT_KEY = 'corba_pdf_cv_draft'

const TEMPLATES = [
  { id: 'classique', label: 'Classique', desc: 'Sobre, conservateur, mono-colonne' },
  { id: 'moderne',   label: 'Moderne',   desc: 'Sidebar colorée, photo, élégant' },
  { id: 'compact',   label: 'Compact',   desc: '2 colonnes, dense, idéal étudiant' },
]

const ACCENT_COLORS = [
  { id: '#6366f1', label: 'Indigo'  },
  { id: '#0891b2', label: 'Cyan'    },
  { id: '#10b981', label: 'Vert'    },
  { id: '#f59e0b', label: 'Orange'  },
  { id: '#e11d48', label: 'Rose'    },
  { id: '#7c3aed', label: 'Violet'  },
  { id: '#475569', label: 'Ardoise' },
]

const DEFAULT_DATA = {
  template: 'classique',
  accentColor: '#6366f1',
  photo: '',  // data URL base64
  name: 'Jean Dupont',
  title: 'Développeur Full-Stack',
  email: 'jean@exemple.com',
  phone: '+221 77 000 00 00',
  address: 'Dakar, Sénégal',
  summary: 'Passionné de développement web et de design distribué.',
  experiences: [
    { period: '2023 - présent', role: 'Développeur', company: 'CORBA PDF Suite', description: 'Développement d\'une plateforme PDF distribuée.' },
  ],
  education: [
    { period: '2024 - 2026', diploma: 'Licence AgroTIC', school: 'USSEIN', description: '' },
  ],
  skills: 'Java, React, CORBA, Docker, PDFBox',
  languages: 'Français (natif), Anglais (B2), Wolof (natif)',
}

function loadDraft() {
  try {
    const raw = localStorage.getItem(DRAFT_KEY)
    if (!raw) return null
    return JSON.parse(raw)
  } catch { return null }
}

function saveDraft(data) {
  try { localStorage.setItem(DRAFT_KEY, JSON.stringify(data)) } catch {}
}

export default function CvBuilderPage() {
  const draft = useMemo(loadDraft, [])
  const initial = draft || DEFAULT_DATA

  const [template, setTemplate] = useState(initial.template || 'classique')
  const [accentColor, setAccentColor] = useState(initial.accentColor || '#6366f1')
  const [photo, setPhoto] = useState(initial.photo || '')
  const [name, setName] = useState(initial.name || '')
  const [title, setTitle] = useState(initial.title || '')
  const [email, setEmail] = useState(initial.email || '')
  const [phone, setPhone] = useState(initial.phone || '')
  const [address, setAddress] = useState(initial.address || '')
  const [summary, setSummary] = useState(initial.summary || '')
  const [experiences, setExperiences] = useState(initial.experiences || [])
  const [education, setEducation] = useState(initial.education || [])
  const [skills, setSkills] = useState(initial.skills || '')
  const [languages, setLanguages] = useState(initial.languages || '')
  const [outputName, setOutputName] = useState('')

  const [loading, setLoading] = useState(false)
  const { progress, onProgress, reset } = useProgress()
  const toast = useToast()

  // Live preview state
  const [previewBlob, setPreviewBlob] = useState(null)
  const [previewLoading, setPreviewLoading] = useState(false)
  const debounceTimer = useRef(null)
  const previewSeqRef = useRef(0)

  const cvData = useMemo(() => ({
    template, accentColor, photo,
    name: name.trim(), title, email, phone, address, summary,
    experiences: experiences.filter(e => e.role || e.company),
    education:   education.filter(e => e.diploma || e.school),
    skills:    skills.split(',').map(s => s.trim()).filter(Boolean),
    languages: languages.split(',').map(s => s.trim()).filter(Boolean),
  }), [template, accentColor, photo, name, title, email, phone, address, summary, experiences, education, skills, languages])

  // Sauvegarde brouillon a chaque modif
  useEffect(() => {
    saveDraft({
      template, accentColor, photo,
      name, title, email, phone, address, summary,
      experiences, education, skills, languages,
    })
  }, [template, accentColor, photo, name, title, email, phone, address, summary, experiences, education, skills, languages])

  // Live preview : debounce 700ms, charge un blob PDF a chaque modif
  useEffect(() => {
    if (!cvData.name) {
      setPreviewBlob(null)
      return
    }
    if (debounceTimer.current) clearTimeout(debounceTimer.current)
    debounceTimer.current = setTimeout(async () => {
      const seq = ++previewSeqRef.current
      setPreviewLoading(true)
      try {
        const blob = await fetchCvBlob(cvData)
        if (seq !== previewSeqRef.current) return  // requete obsolete
        setPreviewBlob(blob)
      } catch (e) {
        if (seq === previewSeqRef.current) setPreviewBlob(null)
      } finally {
        if (seq === previewSeqRef.current) setPreviewLoading(false)
      }
    }, 700)
    return () => clearTimeout(debounceTimer.current)
  }, [cvData])

  function clearDraft() {
    if (!window.confirm('Effacer toutes les données du formulaire ?')) return
    localStorage.removeItem(DRAFT_KEY)
    window.location.reload()
  }

  async function handlePhotoUpload(e) {
    const file = e.target.files?.[0]
    if (!file) return
    if (file.size > 5 * 1024 * 1024) {
      toast.error('Photo trop volumineuse (max 5 Mo).')
      return
    }
    try {
      // Downscale + recompression pour limiter le poids transmis (base64 +33%)
      const dataUrl = await downscaleImage(file, 600, 0.85)
      setPhoto(dataUrl)
    } catch (err) {
      toast.error('Lecture de la photo échouée : ' + err.message)
    }
  }

  async function handleSubmit() {
    if (!name.trim()) return toast.error('Le nom est obligatoire.')
    setLoading(true)
    try {
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
      subtitle="Remplissez le formulaire — votre CV se construit en temps réel à droite."
    >
      <div className="grid lg:grid-cols-[1fr_320px] gap-6">
        {/* Colonne formulaire */}
        <div className="space-y-5 min-w-0">
          <Section title="Modèle visuel" icon={Layout}>
            <div className="grid grid-cols-3 gap-2">
              {TEMPLATES.map(t => (
                <button key={t.id} type="button" onClick={() => setTemplate(t.id)}
                        className={`text-left p-2.5 rounded-lg border transition-all ${
                          template === t.id
                            ? 'border-brand-500 bg-brand-50/70 dark:bg-brand-950/40 shadow-sm'
                            : 'border-ink-200 hover:border-ink-300 dark:border-ink-700 dark:hover:border-ink-600'
                        }`}>
                  <p className="text-sm font-semibold">{t.label}</p>
                  <p className="text-[10px] text-ink-500 mt-0.5 leading-tight">{t.desc}</p>
                </button>
              ))}
            </div>
          </Section>

          <Section title="Couleur d'accent" icon={Palette}>
            <div className="flex flex-wrap gap-2">
              {ACCENT_COLORS.map(c => (
                <button key={c.id} type="button" onClick={() => setAccentColor(c.id)}
                        title={c.label}
                        className={`h-9 w-9 rounded-full border-2 transition-all ${
                          accentColor === c.id ? 'border-ink-900 dark:border-ink-100 scale-110 shadow-md' : 'border-transparent hover:scale-105'
                        }`}
                        style={{ backgroundColor: c.id }} />
              ))}
              <span className="text-xs text-ink-500 self-center ml-2">
                {ACCENT_COLORS.find(c => c.id === accentColor)?.label || accentColor}
              </span>
            </div>
            <p className="mt-1 text-xs text-ink-400">Utilisée sur les templates <strong>Moderne</strong> et <strong>Compact</strong>.</p>
          </Section>

          <Section title="Identité">
            <div className="grid sm:grid-cols-2 gap-3">
              <Field label="Nom complet" value={name} onChange={setName} placeholder="Jean Dupont" required />
              <Field label="Titre / fonction" value={title} onChange={setTitle} placeholder="Développeur" />
              <Field label="Email" value={email} onChange={setEmail} placeholder="jean@exemple.com" type="email" />
              <Field label="Téléphone" value={phone} onChange={setPhone} placeholder="+221..." type="tel" />
              <div className="sm:col-span-2">
                <Field label="Adresse" value={address} onChange={setAddress} placeholder="Ville, Pays" />
              </div>
            </div>
          </Section>

          <Section title="Photo (optionnelle)">
            <div className="flex items-center gap-3">
              {photo ? (
                <img src={photo} alt="Aperçu" className="h-16 w-16 rounded-lg object-cover ring-2 ring-ink-200 dark:ring-ink-700" />
              ) : (
                <div className="h-16 w-16 rounded-lg bg-ink-100 dark:bg-ink-800 grid place-items-center text-ink-400">
                  <UserCircle2 size={28} />
                </div>
              )}
              <div className="flex-1">
                <label className="btn-ghost h-9 px-3 text-sm cursor-pointer inline-flex">
                  <Upload size={14} /> Choisir une photo
                  <input type="file" accept="image/png,image/jpeg" onChange={handlePhotoUpload} className="hidden" />
                </label>
                {photo && (
                  <button type="button" onClick={() => setPhoto('')}
                          className="ml-2 btn-ghost h-9 px-3 text-sm text-rose-600 dark:text-rose-400">
                    <Trash2 size={14} /> Retirer
                  </button>
                )}
                <p className="mt-1 text-xs text-ink-400">Visible uniquement sur le template <strong>Moderne</strong>. La photo est redimensionnée automatiquement.</p>
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

          <div className="flex items-center justify-between pt-2">
            <button type="button" onClick={clearDraft}
                    className="text-xs font-medium text-ink-500 hover:text-rose-600 dark:hover:text-rose-400 transition-colors">
              Effacer le brouillon
            </button>
            <span className="text-[10px] text-ink-400">Sauvegarde auto en local</span>
          </div>

          <div className="mt-3">
            <SubmitButton loading={loading} progress={progress} onClick={handleSubmit} disabled={!name.trim()}>
              Télécharger le CV
            </SubmitButton>
          </div>
        </div>

        {/* Colonne preview (desktop) / panneau collant en haut (mobile) */}
        <div className="lg:sticky lg:top-20 lg:self-start">
          <div className="rounded-xl border border-ink-200 dark:border-ink-700 bg-white dark:bg-ink-900 overflow-hidden">
            <div className="flex items-center justify-between px-3 py-2 border-b border-ink-100 dark:border-ink-800">
              <div className="flex items-center gap-2 text-xs font-semibold text-ink-700 dark:text-ink-200">
                <FileText size={12} /> Aperçu temps réel
              </div>
              {previewLoading && <Loader2 size={12} className="animate-spin text-ink-400" />}
            </div>
            {previewBlob ? (
              <div className="p-2">
                <PdfPreview file={previewBlob} maxPages={2} className="border-0" />
              </div>
            ) : (
              <div className="aspect-[3/4] grid place-items-center text-ink-400 p-6 text-center text-xs">
                {previewLoading ? 'Génération de l\'aperçu…' : 'Remplissez les champs pour voir l\'aperçu.'}
              </div>
            )}
          </div>
        </div>
      </div>
    </ToolPage>
  )
}

async function fetchCvBlob(cvData) {
  const token = getToken()
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`
  const res = await fetch(`${API_BASE}${PDF}/generate-cv`, {
    method: 'POST',
    credentials: 'include',
    headers,
    body: JSON.stringify(cvData),
  })
  if (!res.ok) throw new Error('Erreur preview')
  return res.blob()
}

function updateAt(arr, setter, i, key, value) {
  setter(arr.map((item, j) => j === i ? { ...item, [key]: value } : item))
}

function Section({ title, icon: Icon, children }) {
  return (
    <div className="space-y-2">
      <h3 className="flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-ink-500 dark:text-ink-400">
        {Icon && <Icon size={12} />} {title}
      </h3>
      {children}
    </div>
  )
}

function Field({ label, value, onChange, placeholder, textarea, type = 'text', required }) {
  return (
    <div>
      <label className="text-xs font-medium text-ink-600 dark:text-ink-400">
        {label}{required && <span className="text-rose-500"> *</span>}
      </label>
      {textarea
        ? <textarea value={value} onChange={e => onChange(e.target.value)} className="field min-h-16 text-sm" placeholder={placeholder} />
        : <input type={type} value={value} onChange={e => onChange(e.target.value)} className="field text-sm" placeholder={placeholder} />}
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

/**
 * Redimensionne une image (file) cote client via canvas pour respecter
 * une largeur max + re-encode en JPEG qualite donnee. Retourne un data URL.
 * Evite d'envoyer des fichiers 5 Mo qui exploseraient le payload JSON
 * (~6.7 Mo en base64) et la limite Tomcat.
 */
async function downscaleImage(file, maxWidth = 600, quality = 0.85) {
  const url = URL.createObjectURL(file)
  try {
    const img = await new Promise((resolve, reject) => {
      const i = new Image()
      i.onload = () => resolve(i)
      i.onerror = reject
      i.src = url
    })
    const ratio = Math.min(1, maxWidth / img.naturalWidth)
    const w = Math.round(img.naturalWidth * ratio)
    const h = Math.round(img.naturalHeight * ratio)
    const canvas = document.createElement('canvas')
    canvas.width = w
    canvas.height = h
    const ctx = canvas.getContext('2d')
    ctx.drawImage(img, 0, 0, w, h)
    return canvas.toDataURL('image/jpeg', quality)
  } finally {
    URL.revokeObjectURL(url)
  }
}
