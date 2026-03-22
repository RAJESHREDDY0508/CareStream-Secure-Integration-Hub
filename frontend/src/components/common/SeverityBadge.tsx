import type { Severity, IncidentSeverity } from '../../types'

const styles: Record<string, string> = {
  CRITICAL: 'bg-red-900/60 text-red-300 border border-red-700',
  HIGH:     'bg-orange-900/60 text-orange-300 border border-orange-700',
  MEDIUM:   'bg-yellow-900/60 text-yellow-300 border border-yellow-700',
  LOW:      'bg-blue-900/60 text-blue-300 border border-blue-700',
}

interface Props {
  severity: Severity | IncidentSeverity
  size?: 'sm' | 'md'
}

export default function SeverityBadge({ severity, size = 'sm' }: Props) {
  const base = size === 'sm' ? 'text-xs px-2 py-0.5' : 'text-sm px-3 py-1'
  return (
    <span className={`${base} rounded-full font-semibold mono ${styles[severity] ?? 'bg-gray-800 text-gray-400'}`}>
      {severity}
    </span>
  )
}
