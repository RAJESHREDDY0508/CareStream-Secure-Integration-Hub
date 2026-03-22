import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'

interface NavItem {
  path:  string
  label: string
  icon:  string
}

const navItems: NavItem[] = [
  { path: '/',                label: 'Dashboard',      icon: '⬡' },
  { path: '/vulnerabilities', label: 'Vulnerabilities', icon: '⚡' },
  { path: '/incidents',       label: 'Incidents',       icon: '🔥' },
  { path: '/demo',            label: 'Demo Controls',   icon: '⚙' },
]

const ROLE_COLOR: Record<string, string> = {
  ADMIN:   'text-soc-accent',
  DOCTOR:  'text-yellow-400',
  SERVICE: 'text-blue-400',
}

export default function Sidebar() {
  const { user, logout } = useAuth()
  const navigate         = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <aside className="w-56 shrink-0 bg-soc-surface border-r border-soc-border flex flex-col h-screen sticky top-0">

      {/* Logo */}
      <div className="px-5 py-5 border-b border-soc-border">
        <div className="flex items-center gap-2">
          <span className="text-soc-accent text-2xl">⬡</span>
          <div>
            <p className="text-white font-bold text-sm leading-tight">CareStream</p>
            <p className="text-gray-500 text-xs mono">SOC Platform</p>
          </div>
        </div>
      </div>

      {/* Nav */}
      <nav className="flex-1 px-3 py-4 flex flex-col gap-1">
        {navItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            end={item.path === '/'}
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all ${
                isActive
                  ? 'bg-soc-accent/10 text-soc-accent border border-soc-accent/30'
                  : 'text-gray-400 hover:text-white hover:bg-soc-card'
              }`
            }
          >
            <span className="text-base w-5 text-center">{item.icon}</span>
            {item.label}
          </NavLink>
        ))}
      </nav>

      {/* User + Logout */}
      <div className="px-4 py-4 border-t border-soc-border space-y-3">
        {/* User info */}
        {user && (
          <div className="flex items-center gap-2.5">
            <div className="w-7 h-7 rounded-full bg-soc-accent/20 border border-soc-accent/40
                            flex items-center justify-center text-soc-accent text-xs font-bold shrink-0">
              {user.username[0].toUpperCase()}
            </div>
            <div className="min-w-0">
              <p className="text-white text-xs font-medium truncate mono">{user.username}</p>
              <p className={`text-xs font-semibold ${ROLE_COLOR[user.role] ?? 'text-gray-400'}`}>
                {user.role}
              </p>
            </div>
          </div>
        )}

        {/* Logout */}
        <button
          onClick={handleLogout}
          className="w-full flex items-center gap-2 px-3 py-2 rounded-lg text-xs text-gray-400
                     hover:text-red-400 hover:bg-red-500/10 border border-transparent
                     hover:border-red-500/20 transition-all font-medium"
        >
          <span className="text-base">⎋</span>
          Sign Out
        </button>

        {/* Version */}
        <p className="text-xs text-gray-700 mono">v1.0.0 · Phase 7</p>
      </div>

    </aside>
  )
}
