import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import LoginPage from '../pages/LoginPage'

const mockLogin = vi.fn()
const mockNavigate = vi.fn()
const mockToastError = vi.fn()
const mockToastSuccess = vi.fn()

vi.mock('../hooks/useAuth', () => ({
  useAuth: () => ({ login: mockLogin }),
}))

vi.mock('../components/Toast', () => ({
  useToast: () => ({
    error: mockToastError,
    success: mockToastSuccess,
  }),
}))

vi.mock('../components/ToolPage', () => ({
  default: ({ children, title }) => (
    <div>
      <h1>{title}</h1>
      {children}
    </div>
  ),
}))

vi.mock('../components/SubmitButton', () => ({
  default: ({ children, loading, ...props }) => (
    <button type="submit" disabled={loading} {...props}>
      {children}
    </button>
  ),
}))

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useLocation: () => ({ state: {} }),
  }
})

function renderLogin() {
  return render(
    <MemoryRouter>
      <LoginPage />
    </MemoryRouter>
  )
}

function submitButton() {
  return screen.getAllByRole('button', { name: /Se connecter/i })[0]
}

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders login form fields', () => {
    renderLogin()

    expect(screen.getByRole('heading', { name: 'Connexion' })).toBeInTheDocument()
    expect(screen.getByPlaceholderText('vous@exemple.com')).toBeInTheDocument()
    expect(submitButton()).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Creer un compte/i })).toHaveAttribute('href', '/register')
  })

  it('shows error toast when fields are empty', async () => {
    const user = userEvent.setup()
    renderLogin()

    await user.click(submitButton())

    expect(mockToastError).toHaveBeenCalledWith(
      'Renseignez votre identifiant et votre mot de passe.'
    )
    expect(mockLogin).not.toHaveBeenCalled()
  })

  it('calls login and navigates on success', async () => {
    const user = userEvent.setup()
    mockLogin.mockResolvedValue({ username: 'alice' })
    renderLogin()

    const [identifierInput] = screen.getAllByPlaceholderText('vous@exemple.com')
    const [passwordInput] = screen.getAllByPlaceholderText('••••••••')
    await user.type(identifierInput, 'alice')
    await user.type(passwordInput, 'secret123')
    await user.click(submitButton())

    expect(mockLogin).toHaveBeenCalledWith({
      identifier: 'alice',
      password: 'secret123',
    })
    expect(mockToastSuccess).toHaveBeenCalledWith('Bienvenue alice !')
    expect(mockNavigate).toHaveBeenCalledWith('/', { replace: true })
  })
})
