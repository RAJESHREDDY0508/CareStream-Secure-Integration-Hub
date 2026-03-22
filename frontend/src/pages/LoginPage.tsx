import { useState, FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

interface DemoCred {
  username: string
  password: string
  role:     string
  color:    string
}

const DEMO_CREDS: DemoCred[] = [
  { username: 'admin',    password: 'Admin@CareStream1!',   role: 'ADMIN',   color: 'text-soc-accent' },
  { username: 'dr.smith', password: 'Doctor@CareStream1!',  role: 'DOCTOR',  color: 'text-yellow-400' },
  { username: 'svc-user', password: 'Service@CareStream1!', role: 'SERVICE', color: 'text-blue-400'   },
]

export default function LoginPage() {
  const { login }    = useAuth()
  const navigate     = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error,    setError]    = useState<string | null>(null)
  const [loading,  setLoading]  = useState(false)

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await login(username, password)
      navigate('/', { replace: true })
    } catch {
      setError('Invalid username or password. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  const fillCred = (cred: DemoCred) => {
    setUsername(cred.username)
    setPassword(cred.password)
    setError(null)
  }

  return (
    <div className="min-h-screen bg-soc-bg flex items-center justify-center px-4">
      <div className="w-full max-w-md">

        {/* Logo */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-soc-accent/10 border border-soc-accent/30 mb-4">
            <span className="text-soc-accent text-3xl">⬡</span>
          </div>
          <h1 className="text-white text-2xl font-bold">CareStream</h1>
          <p className="text-gray-500 text-sm mono mt-1">SOC Platform · Secure Access</p>
        </div>

        {/* Login card */}
        <div className="soc-card mb-4">
          <h2 className="text-white font-semibold text-base mb-5">Sign in to your account</h2>

          {error && (
            <div className="bg-red-500/10 border border-red-500/30 rounded-lg px-4 py-3 mb-5 text-red-400 text-sm flex items-center gap-2">
              <span>⚠</span>
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs text-gray-400 mb-1.5 uppercase tracking-wider font-medium">
                Username
              </label>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="w-full bg-soc-bg border border-soc-border rounded-lg px-4 py-2.5 text-white text-sm
                           placeholder-gray-600 focus:outline-none focus:border-soc-accent transition-colors"
                placeholder="Enter username"
                required
                autoFocus
                autoComplete="username"
              />
            </div>

            <div>
              <label className="block text-xs text-gray-400 mb-1.5 uppercase tracking-wider font-medium">
                Password
              </label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full bg-soc-bg border border-soc-border rounded-lg px-4 py-2.5 text-white text-sm
                           placeholder-gray-600 focus:outline-none focus:border-soc-accent transition-colors"
                placeholder="Enter password"
                required
                autoComplete="current-password"
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-2.5 mt-1 rounded-lg bg-soc-accent text-white font-semibold text-sm
                         hover:bg-soc-accent/90 active:scale-[0.98] transition-all disabled:opacity-60
                         disabled:cursor-not-allowed flex items-center justify-center gap-2"
            >
              {loading ? (
                <>
                  <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  Signing in...
                </>
              ) : (
                'Sign In'
              )}
            </button>
          </form>
        </div>

        {/* Demo credentials */}
        <div className="soc-card">
          <p className="text-xs text-gray-400 font-medium uppercase tracking-wider mb-3">
            Demo Credentials — click to fill
          </p>
          <div className="space-y-2">
            {DEMO_CREDS.map((cred) => (
              <button
                key={cred.username}
                type="button"
                onClick={() => fillCred(cred)}
                className="w-full flex items-center justify-between px-3 py-2 rounded-lg
                           bg-soc-bg border border-soc-border hover:border-soc-accent/50
                           hover:bg-soc-accent/5 transition-all group"
              >
                <span className="text-sm text-gray-300 mono group-hover:text-white transition-colors">
                  {cred.username}
                </span>
                <span className={`text-xs font-semibold ${cred.color}`}>
                  {cred.role}
                </span>
              </button>
            ))}
          </div>
          <p className="text-xs text-gray-600 mt-3">
            Click a row to auto-fill credentials, then click Sign In.
          </p>
        </div>

      </div>
    </div>
  )
}
