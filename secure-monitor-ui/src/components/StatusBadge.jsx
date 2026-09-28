/**
 * @param {{ status: import('../model/alert.js').AlertStatus }} props
 */
export default function StatusBadge({ status }) {
  const open = status === 'OPEN'
  return (
    <span className="inline-flex items-center gap-2 text-xs font-semibold tracking-wide text-ink">
      <span aria-hidden="true" className={open ? 'text-signal' : 'text-muted'}>
        {open ? '○' : '✓'}
      </span>
      <span>{status}</span>
    </span>
  )
}
