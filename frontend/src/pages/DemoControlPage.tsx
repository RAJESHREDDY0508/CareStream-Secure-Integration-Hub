import { useState } from 'react'
import { vulnApi } from '../api/vulnerabilities'
import { incidentApi } from '../api/incidents'
import type { AttackScenario, Severity } from '../types'

interface ResultPanel {
  type: 'success' | 'error'
  title: string
  body: string
}

const ATTACK_SCENARIOS: { value: AttackScenario; label: string; desc: string; color: string }[] = [
  {
    value: 'BRUTE_FORCE',
    label: 'Brute Force Attack',
    desc: 'Simulates 15+ failed login attempts → account lockout → successful auth from new IP.',
    color: 'border-orange-700 hover:border-orange-500',
  },
  {
    value: 'PRIVILEGE_ESCALATION',
    label: 'Privilege Escalation',
    desc: 'Service account accesses admin-only endpoints and escalates from SERVICE to ADMIN role.',
    color: 'border-yellow-700 hover:border-yellow-500',
  },
  {
    value: 'DATA_EXFILTRATION',
    label: 'Data Exfiltration',
    desc: 'Bulk patient record export at 2 AM, 3.2 GB transfer to external IP, PHI data stream.',
    color: 'border-red-700 hover:border-red-500',
  },
  {
    value: 'RANSOMWARE_PREP',
    label: 'Ransomware Prep',
    desc: 'File share enumeration → bulk encrypted writes → shadow copy deletion attempt.',
    color: 'border-red-800 hover:border-red-600',
  },
  {
    value: 'INSIDER_THREAT',
    label: 'Insider Threat',
    desc: 'Employee accessing records outside department at 11 PM + mass download before departure.',
    color: 'border-purple-700 hover:border-purple-500',
  },
  {
    value: 'FULL_ATTACK_CHAIN',
    label: 'Full Attack Chain',
    desc: 'Runs Brute Force → Privilege Escalation → Data Exfiltration as a combined APT simulation.',
    color: 'border-red-600 hover:border-red-400',
  },
  {
    value: 'RANDOM',
    label: 'Random Threat',
    desc: 'Generates 2–4 random threat events across a random service.',
    color: 'border-soc-border hover:border-soc-accent',
  },
]

const SERVICES = ['', 'api-gateway', 'auth-service', 'ingestion-service', 'patient-service', 'audit-service']

function ResultCard({ result }: { result: ResultPanel }) {
  return (
    <div className={`soc-card border-l-4 ${result.type === 'success' ? 'border-l-green-500' : 'border-l-red-500'}`}>
      <p className={`text-sm font-semibold ${result.type === 'success' ? 'text-green-400' : 'text-red-400'}`}>
        {result.title}
      </p>
      <pre className="text-xs text-gray-400 mt-2 whitespace-pre-wrap font-mono leading-relaxed">
        {result.body}
      </pre>
    </div>
  )
}

export default function DemoControlPage() {
  const [attackService,     setAttackService]     = useState('')
  const [vulnCount,         setVulnCount]         = useState(20)
  const [vulnService,       setVulnService]       = useState('')
  const [working,           setWorking]           = useState<string | null>(null)
  const [results,           setResults]           = useState<ResultPanel[]>([])

  function addResult(r: ResultPanel) {
    setResults((prev) => [r, ...prev].slice(0, 10))
  }

  async function runAttack(scenario: AttackScenario) {
    setWorking(scenario)
    try {
      const res = await incidentApi.simulateAttack({
        scenario,
        targetService: attackService || undefined,
      })
      addResult({
        type: 'success',
        title: `✓ Attack: ${scenario} → ${res.incidentsCreated} incident(s) created`,
        body: JSON.stringify(
          { scenario: res.scenario, target: res.targetService, incidents: res.incidents },
          null, 2,
        ),
      })
    } catch (e) {
      addResult({ type: 'error', title: `✗ ${scenario} failed`, body: e instanceof Error ? e.message : String(e) })
    } finally {
      setWorking(null)
    }
  }

  async function runVulnScan() {
    setWorking('vuln-scan')
    try {
      const res = await vulnApi.simulate({
        count: vulnCount,
        targetService: vulnService || undefined,
      })
      addResult({
        type: 'success',
        title: `✓ Vulnerability scan complete — ${res.findingsCount} findings`,
        body: JSON.stringify(
          {
            simulationId: res.simulationId,
            critical: res.criticalCount,
            high: res.highCount,
            medium: res.mediumCount,
            low: res.lowCount,
          },
          null, 2,
        ),
      })
    } catch (e) {
      addResult({ type: 'error', title: '✗ Vulnerability scan failed', body: e instanceof Error ? e.message : String(e) })
    } finally {
      setWorking(null)
    }
  }

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold text-white">Demo Control Panel</h1>
        <p className="text-sm text-gray-500 mt-0.5">
          Trigger simulated attacks and scans to populate the dashboard with realistic data.
          All actions are logged and auditable.
        </p>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-2 gap-6">

        {/* ── Attack Simulations ── */}
        <div className="space-y-4">
          <div className="soc-card">
            <h2 className="text-sm font-semibold text-white mb-3">Attack Simulations</h2>
            <div className="flex items-center gap-3 mb-4">
              <label className="text-xs text-gray-500 whitespace-nowrap">Target Service</label>
              <select
                value={attackService}
                onChange={(e) => setAttackService(e.target.value)}
                className="flex-1 bg-soc-surface border border-soc-border text-gray-300 text-xs rounded-lg px-3 py-1.5 focus:outline-none focus:border-soc-accent"
              >
                {SERVICES.map((s) => <option key={s} value={s}>{s || 'Random'}</option>)}
              </select>
            </div>
            <div className="grid grid-cols-1 gap-2">
              {ATTACK_SCENARIOS.map((sc) => (
                <button
                  key={sc.value}
                  disabled={working !== null}
                  onClick={() => runAttack(sc.value)}
                  className={`text-left p-3 rounded-lg bg-soc-surface border ${sc.color} transition-colors disabled:opacity-40 disabled:cursor-not-allowed`}
                >
                  <div className="flex items-center justify-between">
                    <span className="text-sm font-medium text-gray-200">{sc.label}</span>
                    {working === sc.value && (
                      <span className="text-xs text-soc-accent mono animate-pulse">Running...</span>
                    )}
                  </div>
                  <p className="text-xs text-gray-500 mt-0.5 leading-relaxed">{sc.desc}</p>
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* ── Right column ── */}
        <div className="space-y-4">

          {/* Vulnerability Scanner */}
          <div className="soc-card">
            <h2 className="text-sm font-semibold text-white mb-3">Vulnerability Scanner</h2>
            <div className="space-y-3">
              <div className="flex items-center gap-3">
                <label className="text-xs text-gray-500 whitespace-nowrap">Finding Count</label>
                <input
                  type="number"
                  min={1}
                  max={200}
                  value={vulnCount}
                  onChange={(e) => setVulnCount(Number(e.target.value))}
                  className="w-24 bg-soc-surface border border-soc-border text-gray-300 text-xs rounded-lg px-3 py-1.5 focus:outline-none focus:border-soc-accent"
                />
              </div>
              <div className="flex items-center gap-3">
                <label className="text-xs text-gray-500 whitespace-nowrap">Target Service</label>
                <select
                  value={vulnService}
                  onChange={(e) => setVulnService(e.target.value)}
                  className="flex-1 bg-soc-surface border border-soc-border text-gray-300 text-xs rounded-lg px-3 py-1.5 focus:outline-none focus:border-soc-accent"
                >
                  {SERVICES.map((s) => <option key={s} value={s}>{s || 'All Services'}</option>)}
                </select>
              </div>
              <button
                disabled={working !== null}
                onClick={runVulnScan}
                className="w-full soc-btn-primary text-sm disabled:opacity-40"
              >
                {working === 'vuln-scan' ? '⏳ Running scan...' : '⚡ Run Vulnerability Scan'}
              </button>
            </div>
          </div>

          {/* Quick Reference */}
          <div className="soc-card">
            <h2 className="text-sm font-semibold text-white mb-3">Quick Reference</h2>
            <div className="space-y-2 text-xs text-gray-400">
              <p className="font-semibold text-gray-300">What happens after a simulation:</p>
              <ol className="list-decimal list-inside space-y-1 leading-relaxed">
                <li>Threat events are persisted to <span className="mono text-soc-accent">threat_events</span> table</li>
                <li>Detection engine evaluates rules → creates <span className="mono text-soc-accent">Incident</span></li>
                <li>Alerts fired: EMAIL + SMS + PagerDuty (simulated)</li>
                <li>Incidents appear in Kafka topic <span className="mono text-soc-accent">incident.events</span></li>
                <li>Prometheus metrics update within 2 minutes</li>
                <li>Grafana dashboard reflects changes at next scrape</li>
              </ol>
              <p className="font-semibold text-gray-300 mt-3">SLA windows (NIST SP 800-40):</p>
              <ul className="space-y-0.5 mono">
                <li><span className="text-red-400">CRITICAL</span> — 24 hours</li>
                <li><span className="text-orange-400">HIGH</span> — 7 days</li>
                <li><span className="text-yellow-400">MEDIUM</span> — 30 days</li>
                <li><span className="text-blue-400">LOW</span> — 90 days</li>
              </ul>
            </div>
          </div>

          {/* Results */}
          {results.length > 0 && (
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <h2 className="text-sm font-semibold text-white">Results</h2>
                <button onClick={() => setResults([])} className="text-xs text-gray-600 hover:text-gray-400">Clear</button>
              </div>
              {results.map((r, i) => <ResultCard key={i} result={r} />)}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
