import { useCallback, useEffect, useState } from 'react'
import { getAlertById } from '../api/alertApi.js'
import { errorMessage } from '../api/http.js'

/**
 * @param {string | undefined} id
 */
export function useAlert(id) {
  const [alert, setAlert] = useState(null)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)
  const [requestedId, setRequestedId] = useState(id)

  if (requestedId !== id) {
    setRequestedId(id)
    setAlert(null)
    setError(null)
    setLoading(true)
  }

  const refresh = useCallback(async (requestOptions) => {
    if (!id) {
      setAlert(null)
      setError('The requested alert was not found.')
      setLoading(false)
      return
    }

    setLoading(true)
    try {
      setAlert(await getAlertById(id, requestOptions))
      setError(null)
      return true
    } catch (cause) {
      if (!requestOptions?.quiet) {
        setError(errorMessage(cause))
      }
      setAlert((current) => (current?.id === id ? current : null))
      return false
    } finally {
      setLoading(false)
    }
  }, [id])

  useEffect(() => {
    if (!id) {
      return
    }

    let cancelled = false
    getAlertById(id)
      .then((next) => {
        if (cancelled) {
          return
        }
        setAlert(next)
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
  }, [id])

  return { alert, error, loading, refresh }
}
