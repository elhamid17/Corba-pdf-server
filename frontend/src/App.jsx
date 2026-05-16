import { BrowserRouter, Routes, Route } from 'react-router-dom'
import Navbar    from './components/Navbar'
import Home      from './pages/Home'
import MergePage from './pages/MergePage'

// Les autres pages suivent le même pattern que MergePage
// On les importe dynamiquement pour garder App.jsx lisible
import { lazy, Suspense } from 'react'

const SplitPage       = lazy(() => import('./pages/SplitPage'))
const CompressPage    = lazy(() => import('./pages/CompressPage'))
const RotatePage      = lazy(() => import('./pages/RotatePage'))
const WatermarkPage   = lazy(() => import('./pages/WatermarkPage'))
const ProtectPage     = lazy(() => import('./pages/ProtectPage'))
const ConvertPage     = lazy(() => import('./pages/ConvertPage'))
const ExtractTextPage = lazy(() => import('./pages/ExtractTextPage'))
const OcrPage         = lazy(() => import('./pages/OcrPage'))
const CreatePage      = lazy(() => import('./pages/CreatePage'))

function Loading() {
  return (
    <div className="flex items-center justify-center min-h-[40vh] text-gray-400">
      Chargement...
    </div>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <Navbar />
      <Suspense fallback={<Loading />}>
        <Routes>
          <Route path="/"             element={<Home />}          />
          <Route path="/merge"        element={<MergePage />}     />
          <Route path="/split"        element={<SplitPage />}     />
          <Route path="/compress"     element={<CompressPage />}  />
          <Route path="/rotate"       element={<RotatePage />}    />
          <Route path="/watermark"    element={<WatermarkPage />} />
          <Route path="/protect"      element={<ProtectPage />}   />
          <Route path="/convert"      element={<ConvertPage />}   />
          <Route path="/extract-text" element={<ExtractTextPage />}/>
          <Route path="/ocr"          element={<OcrPage />}       />
          <Route path="/create"       element={<CreatePage />}    />
        </Routes>
      </Suspense>
    </BrowserRouter>
  )
}