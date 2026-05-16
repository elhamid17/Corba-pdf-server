import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { lazy, Suspense } from 'react'
import Navbar from './components/Navbar'
import Footer from './components/Footer'
import Home   from './pages/Home'
import { ToastProvider } from './components/Toast'
import { Loader2 } from 'lucide-react'

const MergePage        = lazy(() => import('./pages/MergePage'))
const SplitPage        = lazy(() => import('./pages/SplitPage'))
const ExtractPagesPage = lazy(() => import('./pages/ExtractPagesPage'))
const DeletePagesPage  = lazy(() => import('./pages/DeletePagesPage'))
const CompressPage     = lazy(() => import('./pages/CompressPage'))
const RotatePage       = lazy(() => import('./pages/RotatePage'))
const WatermarkPage    = lazy(() => import('./pages/WatermarkPage'))
const ProtectPage      = lazy(() => import('./pages/ProtectPage'))
const SignPage         = lazy(() => import('./pages/SignPage'))
const ConvertPage      = lazy(() => import('./pages/ConvertPage'))
const ExtractTextPage  = lazy(() => import('./pages/ExtractTextPage'))
const OcrPage          = lazy(() => import('./pages/OcrPage'))
const MetadataPage     = lazy(() => import('./pages/MetadataPage'))
const CreatePage       = lazy(() => import('./pages/CreatePage'))
const NotFound         = lazy(() => import('./pages/NotFound'))

function Loading() {
  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] gap-3 text-ink-500">
      <Loader2 className="animate-spin text-brand-600" size={32} />
      <p className="text-sm">Chargement…</p>
    </div>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <ToastProvider>
        <div className="min-h-screen flex flex-col">
          <Navbar />
          <main className="flex-1">
            <Suspense fallback={<Loading />}>
              <Routes>
                <Route path="/"              element={<Home />} />
                <Route path="/merge"         element={<MergePage />} />
                <Route path="/split"         element={<SplitPage />} />
                <Route path="/extract-pages" element={<ExtractPagesPage />} />
                <Route path="/delete-pages"  element={<DeletePagesPage />} />
                <Route path="/compress"      element={<CompressPage />} />
                <Route path="/rotate"        element={<RotatePage />} />
                <Route path="/watermark"     element={<WatermarkPage />} />
                <Route path="/protect"       element={<ProtectPage />} />
                <Route path="/sign"          element={<SignPage />} />
                <Route path="/convert"       element={<ConvertPage />} />
                <Route path="/extract-text"  element={<ExtractTextPage />} />
                <Route path="/ocr"           element={<OcrPage />} />
                <Route path="/metadata"      element={<MetadataPage />} />
                <Route path="/create"        element={<CreatePage />} />
                <Route path="*"              element={<NotFound />} />
              </Routes>
            </Suspense>
          </main>
          <Footer />
        </div>
      </ToastProvider>
    </BrowserRouter>
  )
}
