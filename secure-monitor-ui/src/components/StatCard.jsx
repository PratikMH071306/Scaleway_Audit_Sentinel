/**
 * @param {{ label: string, value: number | string, hint?: string, tone?: 'signal' | 'high' | 'ink' }} props
 */
export default function StatCard({ label, value, hint, tone = 'ink' }) {
  const valueColor = {
    signal: 'text-signal',
    high: 'text-high',
    ink: 'text-ink',
  }[tone]

  return (
    <section className="rounded-lg border border-line bg-panel px-5 py-4">
      <h2 className="text-sm font-medium text-muted">{label}</h2>
      <p className={`mt-2 font-mono text-4xl font-semibold tracking-tight ${valueColor}`}>{value}</p>
      {hint ? <p className="mt-2 text-xs text-muted">{hint}</p> : null}
    </section>
  )
}
