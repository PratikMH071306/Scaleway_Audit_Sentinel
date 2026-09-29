/**
 * @param {{ message: string, onRetry?: () => void }} props
 */
export default function ErrorNotice({ message, onRetry }) {
  return (
    <div role="alert" className="rounded-lg border border-critical/50 bg-panel px-4 py-4">
      <p className="text-sm font-semibold tracking-wide text-critical">API error</p>
      <p className="mt-1 text-sm text-ink">{message}</p>
      {onRetry ? (
        <button
          type="button"
          onClick={onRetry}
          className="mt-3 rounded border border-line px-3 py-1.5 text-sm text-ink hover:border-signal"
        >
          Try again
        </button>
      ) : null}
    </div>
  )
}
