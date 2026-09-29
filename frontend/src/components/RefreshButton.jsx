/**
 * @param {{ label?: string }} props
 */
export default function RefreshButton({ label = 'Refresh', disabled = false, onClick }) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      className="rounded border border-line bg-canvas px-3 py-2 text-sm font-medium text-ink hover:border-signal disabled:cursor-wait disabled:opacity-60"
    >
      {disabled ? 'Refreshing…' : label}
    </button>
  )
}
