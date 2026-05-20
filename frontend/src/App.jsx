import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { lazy, Suspense } from 'react'
import Navbar from './components/Navbar'
import Footer from './components/Footer'
import Home   from './pages/Home'
import { ToastProvider } from './components/Toast'
import { AuthProvider } from './hooks/useAuth'
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
const PdfToWordPage    = lazy(() => import('./pages/PdfToWordPage'))
const PdfToExcelPage   = lazy(() => import('./pages/PdfToExcelPage'))
const WordToPdfPage    = lazy(() => import('./pages/WordToPdfPage'))
const ImagesToPdfPage  = lazy(() => import('./pages/ImagesToPdfPage'))
const LoginPage        = lazy(() => import('./pages/LoginPage'))
const RegisterPage     = lazy(() => import('./pages/RegisterPage'))
const HistoryPage      = lazy(() => import('./pages/HistoryPage'))
const AdminPage        = lazy(() => import('./pages/AdminPage'))
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
      <AuthProvider>
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
                  <Route path="/pdf-to-word"   element={<PdfToWordPage />} />
                  <Route path="/pdf-to-excel"  element={<PdfToExcelPage />} />
                  <Route path="/word-to-pdf"   element={<WordToPdfPage />} />
                  <Route path="/images-to-pdf" element={<ImagesToPdfPage />} />
                  <Route path="/login"         element={<LoginPage />} />
                  <Route path="/register"      element={<RegisterPage />} />
                  <Route path="/history"       element={<HistoryPage />} />
                  <Route path="/admin"         element={<AdminPage />} />
                  <Route path="*"              element={<NotFound />} />
                </Routes>
              </Suspense>
            </main>
            <Footer />
          </div>
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  )
}
