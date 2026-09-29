import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { apiPost, errorMessage } from '../api/http.js'
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
          </article>
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
