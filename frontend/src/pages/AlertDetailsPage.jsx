import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { apiGet, apiPost, errorMessage } from '../api/http.js'
import { useAuth } from '../auth/AuthContext.jsx'
import ErrorNotice from '../components/ErrorNotice.jsx'
import Header from '../components/Header.jsx'
import RefreshButton from '../components/RefreshButton.jsx'
import SeverityBadge from '../components/SeverityBadge.jsx'
import StatusBadge from '../components/StatusBadge.jsx'
import { formatAbsoluteTime } from '../format/time.js'
import { useAlert } from '../hooks/useAlert.js'

export default function AlertDetailsPage() {
  const { id } = useParams()
  const { session } = useAuth()
  const { alert, error, loading, refresh } = useAlert(id)
  const [actionError, setActionError] = useState('')
  const [resolving, setResolving] = useState(false)
  const [pendingAction, setPendingAction] = useState('')
  const [acting, setActing] = useState(false)
  const [lockMessage, setLockMessage] = useState('')
  const [lockedNow, setLockedNow] = useState(null)
  const [history, setHistory] = useState([])
  const userLocked = lockedNow === null ? alert?.scalewayUserLocked === true : lockedNow

  useEffect(() => {
    if (!id) {
      return undefined
    }
    let cancelled = false
    apiGet(`/api/alerts/${id}/remediations`, { logoutOnUnauthorized: false })
      .then((payload) => {
        if (!cancelled) {
          setHistory(Array.isArray(payload) ? payload : [])
        }
      })
      .catch(() => {
        if (!cancelled) {
          setHistory([])
        }
      })
    return () => {
      cancelled = true
    }
  }, [id, lockMessage])

  async function confirmRemediation() {
    const action = pendingAction
    setActionError('')
    setActing(true)
    try {
      const result = await apiPost(`/api/alerts/${id}/remediate`, { action }, { logoutOnUnauthorized: false })
      setLockMessage(result?.message || (action === 'UNLOCK_USER' ? 'User unlocked successfully' : 'User locked successfully'))
      if (result?.status === 'SUCCESS') {
        setLockedNow(result.afterLocked === true)
      }
      setPendingAction('')
      const reloaded = await refresh({ logoutOnUnauthorized: false, quiet: true })
      if (reloaded) {
        setLockedNow(null)
      }
    } catch (cause) {
      setPendingAction('')
      const reloaded = await refresh({ logoutOnUnauthorized: false, quiet: true })
      if (!reloaded) {
        setActionError(errorMessage(cause))
      }
    } finally {
      setActing(false)
    }
  }

  async function resolveAlert() {
    setActionError('')
    setResolving(true)
    try {
      await apiPost(`/api/alerts/${id}/resolve`)
      await refresh()
    } catch (cause) {
      setActionError(errorMessage(cause))
    } finally {
      setResolving(false)
    }
  }

  useEffect(() => {
    document.title = alert?.title ? `${alert.title} · Security Monitor` : 'Alert · Security Monitor'
  }, [alert])

  return (
    <div className="min-h-screen">
      <Header actions={<RefreshButton disabled={loading} onClick={refresh} />} />
      <main className="mx-auto flex max-w-6xl flex-col gap-6 px-4 py-6 sm:px-6">
        <Link to="/alerts" className="text-sm text-muted underline-offset-2 hover:text-ink hover:underline">
          Back to alerts
        </Link>

        {loading && !alert ? <p className="text-sm text-muted">Loading alert…</p> : null}
        {error ? <ErrorNotice message={error} onRetry={refresh} /> : null}
        {actionError ? <ErrorNotice message={actionError} /> : null}

        {alert ? (
          <article className="rounded-lg border border-line bg-panel">
            <header className="space-y-4 border-b border-line px-5 py-5">
              <div className="flex flex-wrap items-center gap-3">
                <SeverityBadge severity={alert.severity} />
                <StatusBadge status={alert.status} />
                {session?.role === 'ADMIN' && alert.status !== 'RESOLVED' ? (
                  <button
                    type="button"
                    className="rounded-md bg-signal px-3 py-1.5 text-sm font-semibold text-canvas disabled:opacity-60"
                    disabled={resolving}
                    onClick={resolveAlert}
                  >
                    {resolving ? 'Resolving…' : 'Resolve'}
                  </button>
                ) : null}
              </div>
              <div>
                <h1 className="text-2xl font-semibold">{alert.title || 'Untitled alert'}</h1>
                <p className="mt-2 max-w-3xl text-sm leading-6 text-muted">
                  {alert.description || 'No description was stored for this alert.'}
                </p>
              </div>
            </header>

            <dl className="grid gap-4 px-5 py-5 sm:grid-cols-2">
              <Detail label="Rule" value={alert.rule || 'Unknown'} mono />
              <Detail label="User" value={alert.username || 'Unknown'} />
              <Detail label="Source IP" value={alert.sourceIp || 'Unknown'} mono />
              <Detail label="Organization" value={alert.organizationId || 'Unknown'} mono />
              <Detail label="Detected at" value={formatAbsoluteTime(alert.detectedAt)} />
              <Detail label="Event count" value={String(alert.eventCount)} />
              <Detail label="Alert ID" value={alert.id} mono />
            </dl>

            <section className="border-t border-line px-5 py-5">
              <h2 className="text-base font-semibold">Related events</h2>
              <p className="mt-1 text-sm text-muted">
                Event identifiers stored with this alert. Full event records are not included in the alert response.
              </p>
              {alert.relatedEventIds.length === 0 ? (
                <p className="mt-4 text-sm">No related events were stored for this alert.</p>
              ) : (
                <div className="mt-4 overflow-x-auto">
                  <table className="w-full min-w-[280px] text-left text-sm">
                    <caption className="sr-only">Related event identifiers</caption>
                    <thead className="text-xs uppercase tracking-wide text-muted">
                      <tr className="border-b border-line">
                        <th scope="col" className="py-2 pr-4 font-medium">Event ID</th>
                      </tr>
                    </thead>
                    <tbody>
                      {alert.relatedEventIds.map((eventId) => (
                        <tr key={eventId} className="border-b border-line/70 last:border-b-0">
                          <td className="py-2 pr-4 font-mono text-xs break-all">{eventId}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </section>

            <section className="border-t border-line px-5 py-5">
              <h2 className="text-base font-semibold">Remediation</h2>
              <p className="mt-1 text-sm text-muted">
                Lock or unlock the Scaleway IAM member recorded on this alert. This does not resolve the alert.
              </p>
              <dl className="mt-4 grid gap-4 sm:grid-cols-2">
                <Detail label="Target user" value={alert.username || 'Unknown'} />
                <Detail label="Scaleway user ID" value={alert.scalewayUserId || 'Not captured'} mono />
                <Detail label="Scaleway status" value={userLocked ? 'Locked' : 'Unlocked'} />
              </dl>
              {lockMessage ? <p className="mt-4 text-sm text-ink">{lockMessage}</p> : null}
              {session?.role === 'ADMIN' && alert.status === 'OPEN' && (alert.scalewayUserId || alert.username) ? (
                <div className="mt-4 flex gap-2">
                  {userLocked ? (
                    <button
                      type="button"
                      className="rounded-md border border-line px-3 py-1.5 text-sm font-semibold disabled:opacity-60"
                      disabled={acting}
                      onClick={() => setPendingAction('UNLOCK_USER')}
                    >
                      {acting ? 'Unlocking…' : 'Unlock User'}
                    </button>
                  ) : (
                    <button
                      type="button"
                      className="rounded-md border border-line px-3 py-1.5 text-sm font-semibold disabled:opacity-60"
                      disabled={acting}
                      onClick={() => setPendingAction('LOCK_USER')}
                    >
                      {acting ? 'Locking…' : 'Lock User'}
                    </button>
                  )}
                </div>
              ) : null}
              {history.length > 0 ? (
                <ul className="mt-4 space-y-2 text-sm">
                  {history.map((item) => (
                    <li key={item.remediationId} className="rounded-md border border-line px-3 py-2">
                      {item.action} · {item.targetUsername || item.targetUserId} · {item.status}
                      {' · '}
                      {item.beforeLocked ? 'Locked' : 'Unlocked'} → {item.afterLocked ? 'Locked' : 'Unlocked'}
                      {item.message ? ` · ${item.message}` : ''}
                    </li>
                  ))}
                </ul>
              ) : null}
            </section>
          </article>
        ) : null}
        {pendingAction && alert ? (
          <div className="fixed inset-0 z-20 flex items-center justify-center bg-black/40 px-4" role="presentation">
            <div role="dialog" aria-labelledby="lock-title" className="w-full max-w-md rounded-lg border border-line bg-panel p-5">
              <h2 id="lock-title" className="text-lg font-semibold">
                {pendingAction === 'UNLOCK_USER' ? 'Unlock user?' : 'Lock user?'}
              </h2>
              <p className="mt-2 text-sm text-muted">
                {pendingAction === 'UNLOCK_USER'
                  ? `You are about to unlock ${alert.username || 'this user'} (${alert.scalewayUserId || 'id resolved on confirm'}). They will be able to log in and use API keys again.`
                  : `You are about to lock ${alert.username || 'this user'} (${alert.scalewayUserId || 'id resolved on confirm'}). A locked member cannot log in or use API keys.`}
              </p>
              <div className="mt-5 flex justify-end gap-2">
                <button type="button" className="rounded-md border border-line px-3 py-1.5 text-sm" disabled={acting} onClick={() => setPendingAction('')}>
                  Cancel
                </button>
                <button type="button" className="rounded-md bg-signal px-3 py-1.5 text-sm font-semibold text-canvas disabled:opacity-60" disabled={acting} onClick={confirmRemediation}>
                  {acting ? 'Working…' : pendingAction === 'UNLOCK_USER' ? 'Confirm Unlock' : 'Confirm Lock'}
                </button>
              </div>
            </div>
          </div>
        ) : null}
      </main>
    </div>
  )
}

/**
 * @param {{ label: string, value: string, mono?: boolean }} props
 */
function Detail({ label, value, mono = false }) {
  return (
    <div>
      <dt className="text-xs font-medium uppercase tracking-wide text-muted">{label}</dt>
      <dd className={`mt-1 text-sm break-all ${mono ? 'font-mono' : ''}`}>{value}</dd>
    </div>
  )
}
