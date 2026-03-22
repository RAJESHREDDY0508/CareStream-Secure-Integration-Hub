import { useCallback, useState } from 'react'
import { incidentApi } from '../api/incidents'
import { usePolling } from '../hooks/usePolling'
import SeverityBadge from '../components/common/SeverityBadge'
import StatusBadge from '../components/common/StatusBadge'
import LoadingSpinner from '../components/common/LoadingSpinner'
import ErrorBanner from '../components/common/ErrorBanner'
import type { IncidentStatus, IncidentSeverity, ThreatType, Incident } from '../types'

const SEVERITIES: (IncidentSeverity | '')[] = ['', 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW']
const STATUSES: (IncidentStatus | '')[] = ['', 'OPEN', 'INVESTIGATING', 'CONTAINED', 'RESOLVED', 'FALSE_POSITIVE']

function relativeTime(iso: string | null) {
  if (!iso) return '—'
  const diff = Date.now() - new Date(iso).getTime()
  const h = Math.floor(diff / 3_600_000)
  if (h < 1) return `${Math.floor(diff / 60_000)}m ago`
  if (h < 24) return `${h}h ago`
  return `${Math.floor(h / 24)}d ago`
}

type ActionType = 'investigate' | 'contain' | 'resolve' | 'false-positive'

function nextActions(incident: Incident): ActionType[] {
  const map: Record<IncidentStatus, ActionType[]> = {
    OPEN:           ['investigate', 'false-positive'],
    INVESTIGATING:  ['contain', 'resolve', 'false-positive'],
    CONTAINED:      ['resolve'],
    RESOLVED:       [],
    FALSE_POSITIVE: [],
  }
  return map[incident.status] ?? []
}

const actionLabels: Record<ActionType, string> = {
  'investigate':    'Investigate',
  'contain':        'Contain',
  'resolve':        'Resolve',
  'false-positive': 'False +',
}

const actionColors: Record<ActionType, string> = {
  'investigate':    'bg-yellow-900/40 border-yellow-800 text-yellow-400 hover:bg-yellow-900/70',
  'contain':        'bg-orange-900/40 border-orange-800 text-orange-400 hover:bg-orange-900/70',
  'resolve':        'bg-green-900/40 border-green-800 text-green-400 hover:bg-green-900/70',
  'false-positive': 'bg-gray-800 border-gray-700 text-gray-400 hover:bg-gray-700',
}

export default function IncidentsPage() {
  const [severity, setSeverity] = useState('')
  const [status,   setStatus]   = useState('')
  const [page,     setPage]     = useState(0)
  const [action,   setAction]   = useState<{ type: ActionType; incident: Incident } | null>(null)
  const [note,     setNote]     = useState('')
  const [working,  setWorking]  = useState(false)
  const [toast,    setToast]    = useState<string | null>(null)

  const fetcher = useCallback(
    () => incidentApi.list({ severity: severity || undefined, status: status || undefined, page, size: 20 }),
    [severity, status, page],
  )

  const { data, loading, error, refresh } = usePolling(fetcher, { intervalMs: 30_000 })

  const showToast = (msg: string) => { setToast(msg); setTimeout(() => setToast(null), 3500) }

  async function handleAction() {
    if (!action) return
    setWorking(true)
    try {
      const id = action.incident.incidentId
      if (action.type === 'investigate')    await incidentApi.investigate(id)
      if (action.type === 'contain')        await incidentApi.contain(id, note || undefined)
      if (action.type === 'resolve')        await incidentApi.resolve(id, note || 'Resolved')
      if (action.type === 'false-positive') await incidentApi.markFalsePositive(id, note || 'False positive')
      showToast(`${actionLabels[action.type]} applied to ${id}`)
      setAction(null)
      setNote('')
      refresh()
    } catch (e) {
      showToast(`Error: ${e instanceof Error ? e.message : 'Unknown'}`)
    } finally {
      setWorking(false)
    }
  }

  const incidents = data?.content ?? []

  return (
    <div className="p-6 space-y-5">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">Incident Response</h1>
          <p className="text-sm text-gray-500 mt-0.5">
            {data ? `${data.totalElements} incidents` : '—'} · page {page + 1} of {data?.totalPages ?? 1}
          </p>
        </div>
        <button onClick={refresh} className="soc-btn-secondary text-xs">↻ Refresh</button>
      </div>

      {error && <ErrorBanner error={error} onRetry={refresh} />}

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
        {(severity || status) && (
          <button
            onClick={() => { setSeverity(''); setStatus(''); setPage(0) }}
            className="text-xs text-gray-500 hover:text-soc-accent underline"
          >
            Clear
          </button>
        )}
      </div>

      {/* Table */}
      <div className="soc-card p-0 overflow-hidden">
        {loading && incidents.length === 0 ? (
          <LoadingSpinner />
        ) : (
          <div className="overflow-x-auto">
            <table className="soc-table">
              <thead>
                <tr>
                  <th>Incident ID</th>
                  <th>Title</th>
                  <th>Severity</th>
                  <th>Status</th>
                  <th>Threat Type</th>
                  <th>Source</th>
                  <th>Detected</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {incidents.length === 0 ? (
                  <tr><td colSpan={8} className="text-center text-gray-600 py-12">No incidents found</td></tr>
                ) : (
                  incidents.map((inc) => (
                    <tr key={inc.incidentId}>
                      <td className="mono text-soc-accent text-xs">{inc.incidentId}</td>
                      <td className="max-w-[240px]">
                        <p className="text-xs text-gray-200 truncate" title={inc.title}>{inc.title}</p>
                        {inc.affectedResource && (
                          <p className="text-xs text-gray-500 mono truncate">{inc.affectedResource}</p>
                        )}
                      </td>
                      <td><SeverityBadge severity={inc.severity} /></td>
                      <td><StatusBadge status={inc.status} variant="incident" /></td>
                      <td className="text-xs text-gray-400">{inc.threatType.replace(/_/g, ' ')}</td>
                      <td className="text-xs text-gray-500">{inc.sourceService ?? '—'}</td>
                      <td className="text-xs text-gray-500">{relativeTime(inc.detectedAt)}</td>
                      <td>
                        <div className="flex gap-1">
                          {nextActions(inc).map((act) => (
                            <button
                              key={act}
                              onClick={() => { setAction({ type: act, incident: inc }); setNote('') }}
                              className={`text-xs px-2 py-1 rounded border transition-colors ${actionColors[act]}`}
                            >
                              {actionLabels[act]}
                            </button>
                          ))}
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
          <button disabled={page === 0} onClick={() => setPage((p) => p - 1)} className="soc-btn-secondary text-xs disabled:opacity-30">← Prev</button>
          <span className="text-xs text-gray-500 self-center">Page {page + 1} / {data.totalPages}</span>
          <button disabled={page >= data.totalPages - 1} onClick={() => setPage((p) => p + 1)} className="soc-btn-secondary text-xs disabled:opacity-30">Next →</button>
        </div>
      )}

      {/* Action modal */}
      {action && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center z-50">
          <div className="soc-card w-full max-w-md space-y-4">
            <h3 className="font-semibold text-white">
              {actionLabels[action.type]}: <span className="mono text-soc-accent">{action.incident.incidentId}</span>
            </h3>
            <p className="text-sm text-gray-400">{action.incident.title}</p>
            {action.type !== 'investigate' && (
              <textarea
                value={note}
                onChange={(e) => setNote(e.target.value)}
                placeholder="Notes / resolution details..."
                className="w-full bg-soc-surface border border-soc-border rounded-lg px-3 py-2 text-sm text-gray-300 focus:outline-none focus:border-soc-accent resize-none h-24"
              />
            )}
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
