import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import Layout from './components/layout/Layout'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import VulnerabilitiesPage from './pages/VulnerabilitiesPage'
import IncidentsPage from './pages/IncidentsPage'
import DemoControlPage from './pages/DemoControlPage'

// ── Protected layout — redirects to /login if not authenticated ───────────────
function ProtectedLayout() {
  const { isAuthenticated } = useAuth()
  if (!isAuthenticated) return <Navigate to="/login" replace />
  return <Layout />
}

// ── Routes — needs to be inside AuthProvider & BrowserRouter ─────────────────
function AppRoutes() {
  const { isAuthenticated } = useAuth()

  return (
    <Routes>
      {/* Public */}
      <Route
        path="/login"
        element={isAuthenticated ? <Navigate to="/" replace /> : <LoginPage />}
      />

      {/* Protected — all use the sidebar Layout */}
      <Route element={<ProtectedLayout />}>
        <Route index path="/"                element={<DashboardPage />} />
        <Route path="/vulnerabilities"       element={<VulnerabilitiesPage />} />
        <Route path="/incidents"             element={<IncidentsPage />} />
        <Route path="/demo"                  element={<DemoControlPage />} />
      </Route>

      {/* Catch-all */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </AuthProvider>
  )
}
