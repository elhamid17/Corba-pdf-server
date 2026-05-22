import { BrowserRouter, Routes, Route, useLocation, Navigate } from 'react-router-dom'
import { lazy, Suspense } from 'react'
import { motion } from 'framer-motion'
import Navbar from './components/Navbar'
import Footer from './components/Footer'
import Home   from './pages/Home'
import { ToastProvider } from './components/Toast'
import { AuthProvider } from './hooks/useAuth'
import { WorkflowProvider } from './hooks/useWorkflow'
import CommandPalette from './components/CommandPalette'
import OnboardingTour from './components/OnboardingTour'
import { Loader2 } from 'lucide-react'

const MergePage        = lazy(() => import('./pages/MergePage'))
const SplitPage        = lazy(() => import('./pages/SplitPage'))
const SelectPagesPage  = lazy(() => import('./pages/SelectPagesPage'))
const CompressPage     = lazy(() => import('./pages/CompressPage'))
const RotatePage       = lazy(() => import('./pages/RotatePage'))
const MarkingPage      = lazy(() => import('./pages/MarkingPage'))
const ProtectPage      = lazy(() => import('./pages/ProtectPage'))
const SignPage         = lazy(() => import('./pages/SignPage'))
const ConvertPage      = lazy(() => import('./pages/ConvertPage'))
const ExtractTextPage  = lazy(() => import('./pages/ExtractTextPage'))
const OcrPage          = lazy(() => import('./pages/OcrPage'))
const MetadataPage     = lazy(() => import('./pages/MetadataPage'))
const PdfToWordPage    = lazy(() => import('./pages/PdfToWordPage'))
const PdfToExcelPage   = lazy(() => import('./pages/PdfToExcelPage'))
const WordToPdfPage    = lazy(() => import('./pages/WordToPdfPage'))
const ImagesToPdfPage  = lazy(() => import('./pages/ImagesToPdfPage'))
const PageNumberPage   = lazy(() => import('./pages/PageNumberPage'))
const CropPage         = lazy(() => import('./pages/CropPage'))
const CoverPage        = lazy(() => import('./pages/CoverPage'))
const ReorderPage      = lazy(() => import('./pages/ReorderPage'))
const AnonymizePage    = lazy(() => import('./pages/AnonymizePage'))
const SignatureImagePage = lazy(() => import('./pages/SignatureImagePage'))
const RedactPage       = lazy(() => import('./pages/RedactPage'))
const PdfToPptxPage    = lazy(() => import('./pages/PdfToPptxPage'))
const PdfToMarkdownPage = lazy(() => import('./pages/PdfToMarkdownPage'))
const MarkdownToPdfPage = lazy(() => import('./pages/MarkdownToPdfPage'))
const HtmlToPdfPage    = lazy(() => import('./pages/HtmlToPdfPage'))
const ExcelToPdfPage   = lazy(() => import('./pages/ExcelToPdfPage'))
const OdtToPdfPage     = lazy(() => import('./pages/OdtToPdfPage'))
const UnlockPage       = lazy(() => import('./pages/UnlockPage'))
const VerifySignaturePage = lazy(() => import('./pages/VerifySignaturePage'))
const ComparePage      = lazy(() => import('./pages/ComparePage'))
const CodePage         = lazy(() => import('./pages/CodePage'))
const CvBuilderPage    = lazy(() => import('./pages/CvBuilderPage'))
const ScanPage         = lazy(() => import('./pages/ScanPage'))
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

/**
 * Wrapper qui fait un fade-in subtil a chaque navigation.
 * On utilise une motion.div keyee sur le pathname : React remonte le
 * composant a chaque changement de route, declenchant l'animation initial->animate.
 * Pas d'AnimatePresence : exit-animation est complexe avec react-router-dom
 * (necessiterait Routes location={...}) et le fade-in seul suffit pour l'effet.
 */
function AnimatedOutlet({ children }) {
  const location = useLocation()
  return (
    <motion.div
      key={location.pathname}
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.2, ease: 'easeOut' }}
    >
      {children}
    </motion.div>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <ToastProvider>
          <WorkflowProvider>
          <CommandPalette />
          <OnboardingTour />
          <div className="min-h-screen flex flex-col">
            <Navbar />
            <main className="flex-1">
              <Suspense fallback={<Loading />}>
                <AnimatedOutlet>
                <Routes>
                  <Route path="/"              element={<Home />} />
                  <Route path="/merge"         element={<MergePage />} />
                  <Route path="/split"         element={<SplitPage />} />
                  <Route path="/select-pages"  element={<SelectPagesPage />} />
                  {/* Redirects pour anciennes URLs (bookmarks, liens externes) */}
                  <Route path="/extract-pages" element={<Navigate to="/select-pages" replace />} />
                  <Route path="/delete-pages"  element={<Navigate to="/select-pages" replace />} />
                  <Route path="/compress"      element={<CompressPage />} />
                  <Route path="/to-pdfa"       element={<Navigate to="/compress" replace />} />
                  <Route path="/rotate"        element={<RotatePage />} />
                  <Route path="/marking"       element={<MarkingPage />} />
                  <Route path="/watermark"     element={<Navigate to="/marking" replace />} />
                  <Route path="/stamp"         element={<Navigate to="/marking" replace />} />
                  <Route path="/protect"       element={<ProtectPage />} />
                  <Route path="/sign"          element={<SignPage />} />
                  <Route path="/convert"       element={<ConvertPage />} />
                  <Route path="/extract-text"  element={<ExtractTextPage />} />
                  <Route path="/ocr"           element={<OcrPage />} />
                  <Route path="/metadata"      element={<MetadataPage />} />
                  <Route path="/create"        element={<Navigate to="/markdown-to-pdf" replace />} />
                  <Route path="/pdf-to-word"   element={<PdfToWordPage />} />
                  <Route path="/pdf-to-excel"  element={<PdfToExcelPage />} />
                  <Route path="/word-to-pdf"   element={<WordToPdfPage />} />
                  <Route path="/images-to-pdf" element={<ImagesToPdfPage />} />
                  <Route path="/reverse"       element={<Navigate to="/reorder" replace />} />
                  <Route path="/page-numbers"  element={<PageNumberPage />} />
                  <Route path="/resize"        element={<Navigate to="/" replace />} />
                  <Route path="/crop"          element={<CropPage />} />
                  <Route path="/cover"         element={<CoverPage />} />
                  <Route path="/reorder"       element={<ReorderPage />} />
                  <Route path="/anonymize"     element={<AnonymizePage />} />
                  <Route path="/sign-image"    element={<SignatureImagePage />} />
                  <Route path="/redact"        element={<RedactPage />} />
                  <Route path="/pdf-to-pptx"   element={<PdfToPptxPage />} />
                  <Route path="/pdf-to-markdown" element={<PdfToMarkdownPage />} />
                  <Route path="/markdown-to-pdf" element={<MarkdownToPdfPage />} />
                  <Route path="/html-to-pdf"   element={<HtmlToPdfPage />} />
                  <Route path="/excel-to-pdf"  element={<ExcelToPdfPage />} />
                  <Route path="/odt-to-pdf"    element={<OdtToPdfPage />} />
                  <Route path="/unlock"        element={<UnlockPage />} />
                  <Route path="/verify-signature" element={<VerifySignaturePage />} />
                  <Route path="/compare"       element={<ComparePage />} />
                  <Route path="/stats"         element={<Navigate to="/" replace />} />
                  <Route path="/code"          element={<CodePage />} />
                  <Route path="/qr"            element={<Navigate to="/code" replace />} />
                  <Route path="/barcode"       element={<Navigate to="/code" replace />} />
                  <Route path="/cv"            element={<CvBuilderPage />} />
                  <Route path="/scan"          element={<ScanPage />} />
                  <Route path="/login"         element={<LoginPage />} />
                  <Route path="/register"      element={<RegisterPage />} />
                  <Route path="/history"       element={<HistoryPage />} />
                  <Route path="/admin"         element={<AdminPage />} />
                  <Route path="*"              element={<NotFound />} />
                </Routes>
                </AnimatedOutlet>
              </Suspense>
            </main>
            <Footer />
          </div>
          </WorkflowProvider>
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
  )
}
