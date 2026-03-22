import { useState, useEffect, useCallback } from 'react'

interface PollingOptions {
  intervalMs?: number
  enabled?: boolean
}

interface PollingState<T> {
  data: T | null
  loading: boolean
  error: string | null
  refresh: () => void
  lastUpdated: Date | null
}

export function usePolling<T>(
  fetcher: () => Promise<T>,
  { intervalMs = 30_000, enabled = true }: PollingOptions = {},
): PollingState<T> {
  const [data, setData]             = useState<T | null>(null)
  const [loading, setLoading]       = useState(true)
  const [error, setError]           = useState<string | null>(null)
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null)

  const fetch = useCallback(async () => {
    try {
      const result = await fetcher()
      setData(result)
      setError(null)
      setLastUpdated(new Date())
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Fetch failed')
    } finally {
      setLoading(false)
    }
  }, [fetcher])

  useEffect(() => {
    if (!enabled) return
    fetch()
    const id = setInterval(fetch, intervalMs)
    return () => clearInterval(id)
  }, [fetch, intervalMs, enabled])

  return { data, loading, error, refresh: fetch, lastUpdated }
}
