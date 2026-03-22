interface Props {
  error: string
  onRetry?: () => void
}

export default function ErrorBanner({ error, onRetry }: Props) {
  return (
    <div className="flex items-center gap-4 bg-red-950 border border-red-800 rounded-xl px-5 py-4 text-sm text-red-300">
      <span className="text-red-400 text-lg">⚠</span>
      <span className="flex-1">{error}</span>
      {onRetry && (
        <button onClick={onRetry} className="soc-btn-secondary text-xs">
          Retry
        </button>
      )}
    </div>
  )
}
