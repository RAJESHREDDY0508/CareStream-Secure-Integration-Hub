interface Props {
  label: string
  value: string | number
  sub?: string
  color?: 'default' | 'red' | 'orange' | 'yellow' | 'green' | 'cyan'
  pulse?: boolean
}

const colorMap: Record<string, string> = {
  default: 'text-white',
  red:     'text-red-400',
  orange:  'text-orange-400',
  yellow:  'text-yellow-400',
  green:   'text-green-400',
  cyan:    'text-soc-accent',
}

export default function StatCard({ label, value, sub, color = 'default', pulse }: Props) {
  return (
    <div className="soc-card flex flex-col gap-1">
      <p className="text-xs text-gray-400 uppercase tracking-widest">{label}</p>
      <p className={`text-3xl font-bold mono ${colorMap[color]} ${pulse ? 'animate-pulse' : ''}`}>
        {value}
      </p>
      {sub && <p className="text-xs text-gray-500 mt-1">{sub}</p>}
    </div>
  )
}
