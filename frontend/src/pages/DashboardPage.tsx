import { useCallback } from 'react'
import {
  ResponsiveContainer, BarChart, Bar, XAxis, YAxis,
  Tooltip, PieChart, Pie, Cell, Legend,
} from 'recharts'
import { vulnApi } from '../api/vulnerabilities'
import { incidentApi } from '../api/incidents'
import { usePolling } from '../hooks/usePolling'
import StatCard from '../components/common/StatCard'
import LoadingSpinner from '../components/common/LoadingSpinner'
import ErrorBanner from '../components/common/ErrorBanner'

const SEVERITY_COLORS: Record<string, string> = {
  CRITICAL: '#ef4444',
  HIGH:     '#f97316',
  MEDIUM:   '#eab308',
  LOW:      '#3b82f6',
}

const POLL_MS = 30_000

function LastUpdated({ date }: { date: Date | null }) {
  if (!date) return null
  return (
    <span className="text-xs text-gray-600 mono">
      Updated {date.toLocaleTimeString()}
    </span>
  )
}

export default function DashboardPage() {
  const vulnStatsFetcher   = useCallback(() => vulnApi.stats(), [])
  const incidentStatsFetcher = useCallback(() => incidentApi.stats(), [])

  const vulnState     = usePolling(vulnStatsFetcher, { intervalMs: POLL_MS })
  const incidentState = usePolling(incidentStatsFetcher, { intervalMs: POLL_MS })

  const vs = vulnState.data
  const is = incidentState.data

  // Build chart data
  const vulnSeverityData = vs
    ? Object.entries(vs.bySeverity).map(([k, v]) => ({ name: k, count: v }))
    : []

  const incidentSeverityData = is
    ? Object.entries(is.bySeverity).map(([k, v]) => ({ name: k, value: v }))
    : []

  const statusData = is
    ? [
        { name: 'Open',          value: is.open,         fill: '#ef4444' },
        { name: 'Investigating', value: is.investigating, fill: '#eab308' },
        { name: 'Contained',     value: is.contained,     fill: '#f97316' },
        { name: 'Resolved',      value: is.resolved,      fill: '#22c55e' },
        { name: 'False Positive',value: is.falsePositive, fill: '#6b7280' },
      ]
    : []

  if ((vulnState.loading && !vs) || (incidentState.loading && !is)) {
    return <LoadingSpinner message="Loading dashboard metrics..." />
  }

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-white">Security Operations Dashboard</h1>
          <p className="text-sm text-gray-500 mt-0.5">Real-time threat and vulnerability overview · auto-refreshes every 30s</p>
        </div>
        <LastUpdated date={vulnState.lastUpdated} />
      </div>

      {(vulnState.error || incidentState.error) && (
        <ErrorBanner
          error={vulnState.error ?? incidentState.error ?? 'API error'}
          onRetry={() => { vulnState.refresh(); incidentState.refresh() }}
        />
      )}

      {/* Vulnerability KPIs */}
      <section>
        <h2 className="text-xs text-gray-400 uppercase tracking-widest mb-3">Vulnerability Management</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <StatCard
            label="Open Vulnerabilities"
            value={vs?.open ?? '—'}
            color={vs && vs.open > 0 ? 'red' : 'green'}
          />
          <StatCard
            label="Critical Open"
            value={vs?.bySeverity?.CRITICAL ?? '—'}
            color={(vs?.bySeverity?.CRITICAL ?? 0) > 0 ? 'red' : 'green'}
            pulse={(vs?.bySeverity?.CRITICAL ?? 0) > 0}
          />
          <StatCard
            label="SLA Breached"
            value={vs?.overdue ?? '—'}
            sub="Overdue findings"
            color={(vs?.overdue ?? 0) > 0 ? 'orange' : 'green'}
            pulse={(vs?.overdue ?? 0) > 0}
          />
          <StatCard
            label="SLA Compliance"
            value={vs ? `${vs.slaCompliancePercent}%` : '—'}
            color={
              !vs ? 'default'
              : vs.slaCompliancePercent >= 95 ? 'green'
              : vs.slaCompliancePercent >= 80 ? 'yellow'
              : 'red'
            }
          />
        </div>
      </section>

      {/* Incident KPIs */}
      <section>
        <h2 className="text-xs text-gray-400 uppercase tracking-widest mb-3">Incident Response</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <StatCard
            label="Active Incidents"
            value={is?.active ?? '—'}
            color={is && is.active > 0 ? 'orange' : 'green'}
          />
          <StatCard
            label="Critical Open"
            value={is?.openCriticalCount ?? '—'}
            color={(is?.openCriticalCount ?? 0) > 0 ? 'red' : 'green'}
            pulse={(is?.openCriticalCount ?? 0) > 0}
          />
          <StatCard
            label="Total Resolved"
            value={is?.resolved ?? '—'}
            color="green"
          />
          <StatCard
            label="Avg MTTR"
            value={is ? `${is.mttrHours.toFixed(1)}h` : '—'}
            sub="Mean time to resolve"
            color={
              !is ? 'default'
              : is.mttrHours <= 4 ? 'green'
              : is.mttrHours <= 24 ? 'yellow'
              : 'red'
            }
          />
        </div>
      </section>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-6">

        {/* Vulnerability severity bar chart */}
        <div className="soc-card col-span-1">
          <h3 className="text-sm font-semibold text-gray-300 mb-4">Vulnerabilities by Severity</h3>
          {vs ? (
            <ResponsiveContainer width="100%" height={200}>
              <BarChart data={vulnSeverityData} margin={{ top: 0, right: 0, left: -20, bottom: 0 }}>
                <XAxis dataKey="name" tick={{ fill: '#9ca3af', fontSize: 11 }} />
                <YAxis tick={{ fill: '#9ca3af', fontSize: 11 }} />
                <Tooltip
                  contentStyle={{ background: '#1a2235', border: '1px solid #1e2d45', borderRadius: 8 }}
                  labelStyle={{ color: '#e5e7eb' }}
                  itemStyle={{ color: '#9ca3af' }}
                />
                <Bar dataKey="count" radius={[4, 4, 0, 0]}>
                  {vulnSeverityData.map((entry) => (
                    <Cell key={entry.name} fill={SEVERITY_COLORS[entry.name] ?? '#6b7280'} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          ) : (
            <div className="h-48 flex items-center justify-center text-gray-600 text-sm">No data</div>
          )}
        </div>

        {/* Incident status pie chart */}
        <div className="soc-card col-span-1">
          <h3 className="text-sm font-semibold text-gray-300 mb-4">Incidents by Status</h3>
          {is ? (
            <ResponsiveContainer width="100%" height={200}>
              <PieChart>
                <Pie
                  data={statusData.filter((d) => d.value > 0)}
                  dataKey="value"
                  nameKey="name"
                  cx="50%"
                  cy="50%"
                  innerRadius={50}
                  outerRadius={80}
                  paddingAngle={3}
                >
                  {statusData.map((entry) => (
                    <Cell key={entry.name} fill={entry.fill} />
                  ))}
                </Pie>
                <Tooltip
                  contentStyle={{ background: '#1a2235', border: '1px solid #1e2d45', borderRadius: 8 }}
                  itemStyle={{ color: '#9ca3af' }}
                />
                <Legend
                  iconSize={8}
                  wrapperStyle={{ fontSize: 11, color: '#9ca3af' }}
                />
              </PieChart>
            </ResponsiveContainer>
          ) : (
            <div className="h-48 flex items-center justify-center text-gray-600 text-sm">No data</div>
          )}
        </div>

        {/* Incident severity pie */}
        <div className="soc-card col-span-1">
          <h3 className="text-sm font-semibold text-gray-300 mb-4">Incidents by Severity</h3>
          {is ? (
            <ResponsiveContainer width="100%" height={200}>
              <PieChart>
                <Pie
                  data={incidentSeverityData.filter((d) => d.value > 0)}
                  dataKey="value"
                  nameKey="name"
                  cx="50%"
                  cy="50%"
                  innerRadius={50}
                  outerRadius={80}
                  paddingAngle={3}
                >
                  {incidentSeverityData.map((entry) => (
                    <Cell key={entry.name} fill={SEVERITY_COLORS[entry.name] ?? '#6b7280'} />
                  ))}
                </Pie>
                <Tooltip
                  contentStyle={{ background: '#1a2235', border: '1px solid #1e2d45', borderRadius: 8 }}
                  itemStyle={{ color: '#9ca3af' }}
                />
                <Legend
                  iconSize={8}
                  wrapperStyle={{ fontSize: 11, color: '#9ca3af' }}
                />
              </PieChart>
            </ResponsiveContainer>
          ) : (
            <div className="h-48 flex items-center justify-center text-gray-600 text-sm">No data</div>
          )}
        </div>

      </div>

      {/* System status strip */}
      <div className="soc-card flex flex-wrap gap-4 items-center">
        <span className="text-xs text-gray-500 uppercase tracking-widest">Services</span>
        {[
          'api-gateway:8080',
          'auth-service:8081',
          'ingestion-service:8082',
          'patient-service:8083',
          'audit-service:8084',
          'vulnerability-service:8085',
          'incident-service:8086',
        ].map((svc) => (
          <div key={svc} className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-green-500 animate-pulse" />
            <span className="text-xs mono text-gray-400">{svc}</span>
          </div>
        ))}
      </div>
    </div>
  )
}
