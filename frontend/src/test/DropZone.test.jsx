import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import DropZone from '../components/DropZone'

vi.mock('../hooks/useWorkflow', () => ({
  useWorkflow: () => ({ lastResult: null, consume: vi.fn() }),
}))

vi.mock('../components/PdfPreview', () => ({
  default: () => null,
}))

vi.mock('react-dropzone', () => ({
  useDropzone: () => ({
    getRootProps: () => ({ 'data-testid': 'dropzone-root' }),
    getInputProps: () => ({ 'data-testid': 'dropzone-input' }),
    isDragActive: false,
    isDragReject: false,
  }),
}))

describe('DropZone', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders default label and hint', () => {
    render(<DropZone onFiles={vi.fn()} />)

    expect(screen.getByText('Déposez votre fichier PDF ici')).toBeInTheDocument()
    expect(screen.getByText('Fichier PDF, 50 Mo max')).toBeInTheDocument()
    expect(screen.getByTestId('dropzone-root')).toBeInTheDocument()
  })

  it('renders custom label for multiple files', () => {
    render(
      <DropZone
        multiple
        label="Glissez vos PDF ici"
        hint="2 fichiers max"
        onFiles={vi.fn()}
      />
    )

    expect(screen.getByText('Glissez vos PDF ici')).toBeInTheDocument()
    expect(screen.getByText('2 fichiers max')).toBeInTheDocument()
  })
})
