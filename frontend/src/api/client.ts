import axios from 'axios'
import { TOKEN_KEY, USER_KEY } from '../context/AuthContext'

const client = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15_000,
})

// ── Request interceptor — attach JWT from localStorage ────────────────────────
client.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  return config
})

// ── Response interceptor — handle errors & 401 redirect ──────────────────────
client.interceptors.response.use(
  (res) => res,
  (err) => {
    const msg = err.response?.data?.message ?? err.message ?? 'Unknown error'
    console.error('[API Error]', err.response?.status, msg)

    if (err.response?.status === 401) {
      // Token expired or invalid — clear storage and send to login
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      window.location.href = '/login'
    }

    return Promise.reject(new Error(msg))
  },
)

export default client
