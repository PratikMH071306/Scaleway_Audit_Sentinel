import { useEffect, useMemo, useState } from 'react'
import { filterAlerts, ruleOptions, sortAlerts, summarizeAlerts } from '../alerts/view.js'
import AlertFilters from '../components/AlertFilters.jsx'
import AlertTable from '../components/AlertTable.jsx'
import ErrorNotice from '../components/ErrorNotice.jsx'
import Header from '../components/Header.jsx'
import RefreshButton from '../components/RefreshButton.jsx'
import StatCard from '../components/StatCard.jsx'
import { useAlerts } from '../hooks/useAlerts.js'

export default function DashboardPage() {
  const { alerts, error, loading, refresh } = useAlerts()
  const [severity, setSeverity] = useState('ALL')
  const [status, setStatus] = useState('ALL')
  const [rule, setRule] = useState('ALL')
  const [sort, setSort] = useState('newest')

  useEffect(() => {
    document.title = 'Security Monitor'
  }, [])

  const rules = useMemo(() => (alerts ? ruleOptions(alerts) : []), [alerts])
  const selectedRule = rules.includes(rule) ? rule : 'ALL'

  const summary = alerts ? summarizeAlerts(alerts) : null
  const visible = alerts ? sortAlerts(filterAlerts(alerts, { severity, status, rule: selectedRule }), sort) : []

  return (
    <div className="min-h-screen">
      <Header actions={<RefreshButton disabled={loading} onClick={refresh} />} />
      <main className="mx-auto flex max-w-6xl flex-col gap-6 px-4 py-6 sm:px-6">
        {loading && !alerts ? <p className="text-sm text-muted">Loading alerts…</p> : null}

        {error && !alerts ? <ErrorNotice message={error} onRetry={refresh} /> : null}

        {error && alerts ? <ErrorNotice message={error} onRetry={refresh} /> : null}

        {alerts && alerts.length === 0 ? (
          <section className="rounded-lg border border-dashed border-line bg-panel px-6 py-10 text-center">
            <h2 className="text-lg font-semibold">No security alerts</h2>
            <p className="mt-2 text-sm text-muted">No suspicious activity has been detected.</p>
          </section>
        ) : null}

        {summary && alerts && alerts.length > 0 ? (
          <>
            <section className="grid gap-4 sm:grid-cols-3" aria-label="Alert summary">
              <StatCard label="Open Alerts" value={summary.open} hint="Status is OPEN" tone="signal" />
              <StatCard label="High Severity" value={summary.highSeverity} hint="HIGH and CRITICAL" tone="high" />
              <StatCard label="Total Alerts" value={summary.total} hint="Stored alerts" />
            </section>

            <section className="overflow-hidden rounded-lg border border-line bg-panel">
              <div className="flex flex-col gap-4 border-b border-line px-4 py-4 sm:px-5">
                <div>
                  <h2 className="text-base font-semibold">Recent security alerts</h2>
                  <p className="mt-1 text-sm text-muted">
                    Showing {visible.length} of {alerts.length}
                  </p>
                </div>
                <AlertFilters
                  severity={severity}
                  status={status}
                  rule={selectedRule}
                  sort={sort}
                  rules={rules}
                  onSeverity={setSeverity}
                  onStatus={setStatus}
                  onRule={setRule}
                  onSort={setSort}
                />
              </div>
              {visible.length === 0 ? (
                <div className="px-5 py-10 text-center">
                  <h3 className="font-semibold">No alerts match these filters</h3>
                  <p className="mt-2 text-sm text-muted">Change the severity, status, or rule to see more alerts.</p>
                  <button
                    type="button"
                    onClick={() => {
                      setSeverity('ALL')
                      setStatus('ALL')
                      setRule('ALL')
                    }}
                    className="mt-4 rounded border border-line px-3 py-1.5 text-sm hover:border-signal"
                  >
                    Clear filters
                  </button>
                </div>
              ) : (
                <AlertTable alerts={visible} />
              )}
            </section>
          </>
        ) : null}
      </main>
    </div>
  )
}
