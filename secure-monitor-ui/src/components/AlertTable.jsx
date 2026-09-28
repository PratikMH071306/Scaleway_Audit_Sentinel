import { Link } from 'react-router-dom'
import { formatAbsoluteTime, formatRelativeTime } from '../format/time.js'
import SeverityBadge from './SeverityBadge.jsx'
import StatusBadge from './StatusBadge.jsx'

/**
 * @param {{ alerts: import('../model/alert.js').Alert[] }} props
 */
export default function AlertTable({ alerts }) {
  return (
    <div className="overflow-x-auto">
      <table className="alert-table w-full border-collapse text-left text-sm">
        <caption className="sr-only">Recent security alerts</caption>
        <thead className="text-xs uppercase tracking-wide text-muted">
          <tr className="border-b border-line">
            <th scope="col" className="px-4 py-3 font-medium">Severity</th>
            <th scope="col" className="px-4 py-3 font-medium">Rule</th>
            <th scope="col" className="px-4 py-3 font-medium">Title</th>
            <th scope="col" className="px-4 py-3 font-medium">Source IP</th>
            <th scope="col" className="px-4 py-3 font-medium">Detected at</th>
            <th scope="col" className="px-4 py-3 font-medium">Status</th>
          </tr>
        </thead>
        <tbody>
          {alerts.map((alert) => (
            <tr key={alert.id} className="border-b border-line/80 last:border-b-0">
              <td data-label="Severity" className="px-4 py-3">
                <SeverityBadge severity={alert.severity} />
              </td>
              <td data-label="Rule" className="px-4 py-3 font-mono text-xs text-muted">{alert.rule}</td>
              <td data-label="Title" className="px-4 py-3">
                <Link
                  to={`/alerts/${alert.id}`}
                  className="font-medium text-ink underline-offset-2 hover:underline"
                >
                  {alert.title || 'Untitled alert'}
                </Link>
              </td>
              <td data-label="Source IP" className="px-4 py-3 font-mono text-xs">{alert.sourceIp || 'Unknown'}</td>
              <td data-label="Detected at" className="px-4 py-3">
                <time dateTime={alert.detectedAt} title={formatAbsoluteTime(alert.detectedAt)}>
                  {formatRelativeTime(alert.detectedAt)}
                </time>
              </td>
              <td data-label="Status" className="px-4 py-3">
                <StatusBadge status={alert.status} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
