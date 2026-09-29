const MARKS = {
  CRITICAL: '■■',
  HIGH: '■',
  MEDIUM: '▬',
  LOW: '·',
}

const BORDERS = {
  CRITICAL: 'border-critical',
  HIGH: 'border-high',
  MEDIUM: 'border-medium',
  LOW: 'border-low',
}

/**
 * @param {{ severity: import('../model/alert.js').AlertSeverity }} props
 */
export default function SeverityBadge({ severity }) {
  return (
    <span
      className={`inline-flex items-center gap-2 rounded border border-line border-l-4 px-2 py-1 text-xs font-semibold tracking-wide text-ink ${BORDERS[severity]}`}
    >
      <span aria-hidden="true" className="font-mono">
        {MARKS[severity]}
      </span>
      <span>{severity}</span>
    </span>
  )
}
