import { createContext, useContext, useState, ReactNode } from 'react'
import axios from 'axios'

// ── Types ────────────────────────────────────────────────────────────────────

export interface AuthUser {
  username: string
  role: string
}

interface AuthContextValue {
  user:            AuthUser | null
  token:           string | null
  login:           (username: string, password: string) => Promise<void>
  logout:          () => void
  isAuthenticated: boolean
}

// ── Storage keys ─────────────────────────────────────────────────────────────

export const TOKEN_KEY = 'cs_access_token'
export const USER_KEY  = 'cs_user'

// ── Context ───────────────────────────────────────────────────────────────────

const AuthContext = createContext<AuthContextValue | null>(null)

// ── Provider ──────────────────────────────────────────────────────────────────

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(
    () => localStorage.getItem(TOKEN_KEY)
  )
  const [user, setUser] = useState<AuthUser | null>(() => {
    const stored = localStorage.getItem(USER_KEY)
    try { return stored ? JSON.parse(stored) : null } catch { return null }
  })

  const login = async (username: string, password: string): Promise<void> => {
    // Use plain axios (not the client instance) so no circular dep with interceptors
    const res = await axios.post('/api/v1/auth/login', { username, password })
    const { accessToken, role, username: uname } = res.data as {
      accessToken: string
      role: string
      username: string
    }
    const authUser: AuthUser = { username: uname, role }
    localStorage.setItem(TOKEN_KEY, accessToken)
    localStorage.setItem(USER_KEY,  JSON.stringify(authUser))
    setToken(accessToken)
    setUser(authUser)
  }

  const logout = (): void => {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    setToken(null)
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, token, login, logout, isAuthenticated: !!token }}>
      {children}
    </AuthContext.Provider>
  )
}

// ── Hook ──────────────────────────────────────────────────────────────────────

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>')
  return ctx
}
