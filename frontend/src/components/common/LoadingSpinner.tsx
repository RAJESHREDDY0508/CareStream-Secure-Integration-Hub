export default function LoadingSpinner({ message = 'Loading...' }: { message?: string }) {
  return (
    <div className="flex flex-col items-center justify-center h-48 gap-4">
      <div className="w-10 h-10 border-2 border-soc-border border-t-soc-accent rounded-full animate-spin" />
      <p className="text-sm text-gray-500 mono">{message}</p>
    </div>
  )
}
