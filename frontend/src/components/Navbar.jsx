import { Link, useLocation } from 'react-router-dom'
import { FileText } from 'lucide-react'

const links = [
  { to: '/',              label: 'Accueil'    },
  { to: '/merge',         label: 'Fusion'     },
  { to: '/split',         label: 'Découpage'  },
  { to: '/compress',      label: 'Compression'},
  { to: '/watermark',     label: 'Filigrane'  },
  { to: '/extract-text',  label: 'Texte'      },
  { to: '/ocr',           label: 'OCR'        },
  { to: '/protect',       label: 'Protection' },
  { to: '/convert',       label: 'Images'     },
]

export default function Navbar() {
  const { pathname } = useLocation()

  return (
    <nav className="bg-white shadow-sm border-b border-gray-200">
      <div className="max-w-7xl mx-auto px-4 py-3 flex items-center gap-6 overflow-x-auto">
        <Link to="/" className="flex items-center gap-2 font-bold text-primary-600 shrink-0">
          <FileText size={22} />
          <span>CORBA PDF</span>
        </Link>
        <div className="flex items-center gap-1 flex-wrap">
          {links.map(l => (
            <Link
              key={l.to}
              to={l.to}
              className={`px-3 py-1.5 rounded-lg text-sm font-medium transition-colors
                ${pathname === l.to
                  ? 'bg-primary-100 text-primary-700'
                  : 'text-gray-600 hover:bg-gray-100'
                }`}
            >
              {l.label}
            </Link>
          ))}
        </div>
      </div>
    </nav>
  )
}