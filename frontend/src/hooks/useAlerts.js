import { useCallback, useEffect, useState } from 'react'
import { getAlerts } from '../api/alertApi.js'
import { errorMessage } from '../api/http.js'

export function useAlerts() {
  const [alerts, setAlerts] = useState(null)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)

  const refresh = useCallback(async () => {
    setLoading(true)
    try {
      setAlerts(await getAlerts())
      setError(null)
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    let cancelled = false
    getAlerts()
      .then((next) => {
        if (cancelled) {
          return
        }
        setAlerts(next)
        setError(null)
      })
      .catch((cause) => {
        if (!cancelled) {
          setError(errorMessage(cause))
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false)
        }
      })
    return () => {
      cancelled = true
    }
  }, [])

  return { alerts, error, loading, refresh }
}
