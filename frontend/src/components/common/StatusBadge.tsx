const vulnStyles: Record<string, string> = {
  OPEN:        'bg-red-900/50 text-red-300 border border-red-800',
  IN_PROGRESS: 'bg-blue-900/50 text-blue-300 border border-blue-800',
  REMEDIATED:  'bg-green-900/50 text-green-300 border border-green-800',
  ACCEPTED:    'bg-purple-900/50 text-purple-300 border border-purple-800',
  OVERDUE:     'bg-red-950 text-red-200 border border-red-700 animate-pulse',
}

const incidentStyles: Record<string, string> = {
  OPEN:           'bg-red-900/50 text-red-300 border border-red-800',
  INVESTIGATING:  'bg-yellow-900/50 text-yellow-300 border border-yellow-800',
  CONTAINED:      'bg-orange-900/50 text-orange-300 border border-orange-800',
  RESOLVED:       'bg-green-900/50 text-green-300 border border-green-800',
  FALSE_POSITIVE: 'bg-gray-800 text-gray-400 border border-gray-700',
}

const slaStyles: Record<string, string> = {
  ON_TRACK:  'bg-green-900/50 text-green-300 border border-green-800',
  AT_RISK:   'bg-yellow-900/50 text-yellow-300 border border-yellow-800',
  BREACHED:  'bg-red-900/50 text-red-300 border border-red-800 animate-pulse',
  COMPLETED: 'bg-gray-800 text-gray-400 border border-gray-700',
}

interface Props {
  status: string
  variant?: 'vuln' | 'incident' | 'sla'
}

export default function StatusBadge({ status, variant = 'vuln' }: Props) {
  const map = variant === 'incident' ? incidentStyles : variant === 'sla' ? slaStyles : vulnStyles
  return (
    <span className={`text-xs px-2 py-0.5 rounded-full font-medium mono ${map[status] ?? 'bg-gray-800 text-gray-400'}`}>
      {status.replace('_', ' ')}
    </span>
  )
}
