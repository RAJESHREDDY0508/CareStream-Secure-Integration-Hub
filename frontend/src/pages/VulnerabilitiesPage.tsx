import { useCallback, useState } from 'react'
import { vulnApi } from '../api/vulnerabilities'
import { usePolling } from '../hooks/usePolling'
import SeverityBadge from '../components/common/SeverityBadge'
import StatusBadge from '../components/common/StatusBadge'
import LoadingSpinner from '../components/common/LoadingSpinner'
import ErrorBanner from '../components/common/ErrorBanner'
import type { Severity, VulnStatus } from '../types'

const SEVERITIES: (Severity | '')[] = ['', 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW']
const STATUSES: (VulnStatus | '')[]  = ['', 'OPEN', 'IN_PROGRESS', 'REMEDIATED', 'ACCEPTED', 'OVERDUE']
const SERVICES = ['', 'api-gateway', 'auth-service', 'ingestion-service', 'patient-service', 'audit-service', 'vulnerability-service']

function relativeTime(iso: string) {
  const diff = Date.now() - new Date(iso).getTime()
  const h = Math.floor(diff / 3_600_000)
  if (h < 1) return `${Math.floor(diff / 60_000)}m ago`
  if (h < 24) return `${h}h ago`
  return `${Math.floor(h / 24)}d ago`
}

export default function VulnerabilitiesPage() {
  const [severity, setSeverity] = useState('')
  const [status,   setStatus]   = useState('')
  const [service,  setService]  = useState('')
  const [page,     setPage]     = useState(0)
  const [action,   setAction]   = useState<{ type: string; findingId: string } | null>(null)
  const [actionNote, setActionNote] = useState('')
  const [working,  setWorking]  = useState(false)
  const [toast,    setToast]    = useState<string | null>(null)

  const fetcher = useCallback(
    () => vulnApi.list({ severity: severity || undefined, status: status || undefined, service: service || undefined, page, size: 20 }),
    [severity, status, service, page],
  )

  const { data, loading, error, refresh } = usePolling(fetcher, { intervalMs: 30_000 })

  const showToast = (msg: string) => {
    setToast(msg)
    setTimeout(() => setToast(null), 3500)
  }

  async function handleAction() {
    if (!action) return
    setWorking(true)
    try {
      if (action.type === 'remediate') await vulnApi.remediate(action.findingId, actionNote || 'Remediated')
      if (action.type === 'accept')    await vulnApi.accept(action.findingId, actionNote || 'Risk accepted')
      if (action.type === 'assign')    await vulnApi.assign(action.findingId, actionNote || 'demo-admin')
      showToast(`${action.type} applied to ${action.findingId}`)
      setAction(null)
      setActionNote('')
      refresh()
    } catch (e) {
      showToast(`Error: ${e instanceof Error ? e.message : 'Unknown'}`)
    } finally {
      setWorking(false)
    }
  }

  const vulns = data?.content ?? []

  return (
    <div className="p-6 space-y-5">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">Vulnerability Management</h1>
          <p className="text-sm text-gray-500 mt-0.5">
            {data ? `${data.totalElements} findings` : '—'} · page {page + 1} of {data?.totalPages ?? 1}
          </p>
        </div>
        <button onClick={refresh} className="soc-btn-secondary text-xs">↻ Refresh</button>
      </div>

      {error && <ErrorBanner error={error} onRetry={refresh} />}

      {/* Toast */}
      {toast && (
        <div className="fixed top-4 right-4 z-50 bg-soc-accent text-soc-bg text-sm px-4 py-3 rounded-lg shadow-lg mono">
          {toast}
        </div>
      )}

      {/* Filters */}
      <div className="soc-card flex flex-wrap gap-3 items-center">
        <span className="text-xs text-gray-500 uppercase tracking-widest">Filters</span>
        <select
          value={severity} onChange={(e) => { setSeverity(e.target.value); setPage(0) }}
          className="bg-soc-surface border border-soc-border text-gray-300 text-xs rounded-lg px-3 py-1.5 focus:outline-none focus:border-soc-accent"
        >
          {SEVERITIES.map((s) => <option key={s} value={s}>{s || 'All Severities'}</option>)}
        </select>
        <select
          value={status} onChange={(e) => { setStatus(e.target.value); setPage(0) }}
          className="bg-soc-surface border border-soc-border text-gray-300 text-xs rounded-lg px-3 py-1.5 focus:outline-none focus:border-soc-accent"
        >
          {STATUSES.map((s) => <option key={s} value={s}>{s || 'All Statuses'}</option>)}
        </select>
        <select
          value={service} onChange={(e) => { setService(e.target.value); setPage(0) }}
          className="bg-soc-surface border border-soc-border text-gray-300 text-xs rounded-lg px-3 py-1.5 focus:outline-none focus:border-soc-accent"
        >
          {SERVICES.map((s) => <option key={s} value={s}>{s || 'All Services'}</option>)}
        </select>
        {(severity || status || service) && (
          <button
            onClick={() => { setSeverity(''); setStatus(''); setService(''); setPage(0) }}
            className="text-xs text-gray-500 hover:text-soc-accent underline"
          >
            Clear
          </button>
        )}
      </div>

      {/* Table */}
      <div className="soc-card p-0 overflow-hidden">
        {loading && vulns.length === 0 ? (
          <LoadingSpinner />
        ) : (
          <div className="overflow-x-auto">
            <table className="soc-table">
              <thead>
                <tr>
                  <th>Finding ID</th>
                  <th>CVE</th>
                  <th>Severity</th>
                  <th>Component</th>
                  <th>Service</th>
                  <th>Status</th>
                  <th>SLA</th>
                  <th>CVSS</th>
                  <th>Detected</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {vulns.length === 0 ? (
                  <tr><td colSpan={10} className="text-center text-gray-600 py-12">No vulnerabilities found</td></tr>
                ) : (
                  vulns.map((v) => (
                    <tr key={v.findingId}>
                      <td className="mono text-soc-accent text-xs">{v.findingId}</td>
                      <td className="mono text-xs text-gray-400">{v.cveId || '—'}</td>
                      <td><SeverityBadge severity={v.severity} /></td>
                      <td className="max-w-[160px] truncate text-xs">{v.affectedComponent}</td>
                      <td className="text-xs text-gray-400">{v.affectedService}</td>
                      <td><StatusBadge status={v.status} variant="vuln" /></td>
                      <td><StatusBadge status={v.slaStatus} variant="sla" /></td>
                      <td className={`mono text-xs font-semibold ${v.cvssScore >= 9 ? 'text-red-400' : v.cvssScore >= 7 ? 'text-orange-400' : 'text-yellow-400'}`}>
                        {v.cvssScore?.toFixed(1) ?? '—'}
                      </td>
                      <td className="text-xs text-gray-500">{relativeTime(v.detectedAt)}</td>
                      <td>
                        <div className="flex gap-1">
                          {v.status === 'OPEN' && (
                            <button
                              onClick={() => { setAction({ type: 'assign', findingId: v.findingId }); setActionNote('') }}
                              className="text-xs px-2 py-1 rounded bg-soc-surface border border-soc-border text-gray-400 hover:text-soc-accent hover:border-soc-accent transition-colors"
                            >
                              Assign
                            </button>
                          )}
                          {(v.status === 'OPEN' || v.status === 'IN_PROGRESS') && (
                            <button
                              onClick={() => { setAction({ type: 'remediate', findingId: v.findingId }); setActionNote('') }}
                              className="text-xs px-2 py-1 rounded bg-green-900/40 border border-green-800 text-green-400 hover:bg-green-900/70 transition-colors"
                            >
                              Remediate
                            </button>
                          )}
                          {v.status === 'OPEN' && (
                            <button
                              onClick={() => { setAction({ type: 'accept', findingId: v.findingId }); setActionNote('') }}
                              className="text-xs px-2 py-1 rounded bg-purple-900/40 border border-purple-800 text-purple-400 hover:bg-purple-900/70 transition-colors"
                            >
                              Accept
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div className="flex justify-center gap-2">
          <button
            disabled={page === 0}
            onClick={() => setPage((p) => p - 1)}
            className="soc-btn-secondary text-xs disabled:opacity-30"
          >
            ← Prev
          </button>
          <span className="text-xs text-gray-500 self-center">
            Page {page + 1} / {data.totalPages}
          </span>
          <button
            disabled={page >= data.totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
            className="soc-btn-secondary text-xs disabled:opacity-30"
          >
            Next →
          </button>
        </div>
      )}

      {/* Action modal */}
      {action && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50">
          <div className="soc-card w-full max-w-md space-y-4">
            <h3 className="font-semibold text-white capitalize">{action.type}: <span className="mono text-soc-accent">{action.findingId}</span></h3>
            <textarea
              value={actionNote}
              onChange={(e) => setActionNote(e.target.value)}
              placeholder={action.type === 'assign' ? 'Assignee ID (e.g. sec-analyst-1)' : 'Notes / reason...'}
              className="w-full bg-soc-surface border border-soc-border rounded-lg px-3 py-2 text-sm text-gray-300 focus:outline-none focus:border-soc-accent resize-none h-20"
            />
            <div className="flex gap-2 justify-end">
              <button onClick={() => setAction(null)} className="soc-btn-secondary text-sm">Cancel</button>
              <button onClick={handleAction} disabled={working} className="soc-btn-primary text-sm disabled:opacity-50">
                {working ? 'Applying...' : 'Confirm'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
