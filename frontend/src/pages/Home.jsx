import { useEffect, useState } from 'react'
import { ping } from '../api/pdfApi'
import ServiceCard from '../components/ServiceCard'
import {
  Layers, Scissors, Archive, RotateCw,
  Droplets, Lock, Image, FileText, ScanText, Plus
} from 'lucide-react'

const services = [
  { icon: Layers,   title: 'Fusion',      description: 'Fusionnez plusieurs PDFs en un seul',      to: '/merge'        },
  { icon: Scissors, title: 'Découpage',   description: 'Découpez un PDF en segments',               to: '/split'        },
  { icon: Archive,  title: 'Compression', description: 'Réduisez la taille de vos PDFs',            to: '/compress'     },
  { icon: RotateCw, title: 'Rotation',    description: 'Pivotez les pages de votre PDF',            to: '/rotate'       },
  { icon: Droplets, title: 'Filigrane',   description: 'Ajoutez un filigrane texte',                to: '/watermark'    },
  { icon: Lock,     title: 'Protection',  description: 'Protégez votre PDF par mot de passe',       to: '/protect'      },
  { icon: Image,    title: 'Images',      description: 'Convertissez chaque page en image',         to: '/convert'      },
  { icon: FileText, title: 'Texte',       description: 'Extrayez le texte brut du PDF',             to: '/extract-text' },
  { icon: ScanText, title: 'OCR',         description: 'Reconnaissance de texte sur PDF scanné',    to: '/ocr'          },
  { icon: Plus,     title: 'Création',    description: 'Créez un PDF depuis du texte',              to: '/create'       },
]

export default function Home() {
  const [status, setStatus] = useState(null)

  useEffect(() => {
    ping()
      .then(d => setStatus(d.status))
      .catch(() => setStatus('ERROR'))
  }, [])

  return (
    <div className="max-w-7xl mx-auto px-4 py-10">

      {/* Hero */}
      <div className="text-center mb-12">
        <h1 className="text-4xl font-bold text-gray-900 mb-3">
          CORBA PDF Server
        </h1>
        <p className="text-lg text-gray-500 max-w-xl mx-auto">
          Plateforme distribuée de traitement PDF — propulsée par CORBA, JacORB et Apache PDFBox
        </p>
        <div className="mt-4 inline-flex items-center gap-2 text-sm">
          <span className={`w-2.5 h-2.5 rounded-full ${
            status === 'OK' ? 'bg-green-400' :
            status === 'ERROR' ? 'bg-red-400' : 'bg-yellow-400'
          }`}/>
          <span className="text-gray-500">
            Serveur : {status ?? 'vérification...'}
          </span>
        </div>
      </div>

      {/* Grille des services */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
        {services.map(s => (
          <ServiceCard key={s.to} {...s} />
        ))}
      </div>

      {/* Footer */}
      <p className="text-center text-xs text-gray-400 mt-16">
        USSEIN — L2 AgroTIC · Projet académique CORBA
      </p>
    </div>
  )
}