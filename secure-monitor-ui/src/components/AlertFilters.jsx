import { SEVERITIES, STATUSES } from '../model/alert.js'

/**
 * @param {{
 *   severity: string,
 *   status: string,
 *   rule: string,
 *   sort: string,
 *   rules: string[],
 *   onSeverity: (value: string) => void,
 *   onStatus: (value: string) => void,
 *   onRule: (value: string) => void,
 *   onSort: (value: string) => void,
 * }} props
 */
export default function AlertFilters({
  severity,
  status,
  rule,
  sort,
  rules,
  onSeverity,
  onStatus,
  onRule,
  onSort,
}) {
  return (
    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
      <FilterField label="Severity" value={severity} onChange={onSeverity}>
        <option value="ALL">All</option>
        {SEVERITIES.map((item) => (
          <option key={item} value={item}>
            {item}
          </option>
        ))}
      </FilterField>
      <FilterField label="Status" value={status} onChange={onStatus}>
        <option value="ALL">All</option>
        {STATUSES.map((item) => (
          <option key={item} value={item}>
            {item}
          </option>
        ))}
      </FilterField>
      <FilterField label="Rule" value={rule} onChange={onRule}>
        <option value="ALL">All</option>
        {rules.map((item) => (
          <option key={item} value={item}>
            {item}
          </option>
        ))}
      </FilterField>
      <FilterField label="Sort" value={sort} onChange={onSort}>
        <option value="newest">Newest</option>
        <option value="oldest">Oldest</option>
        <option value="severity">Highest severity</option>
      </FilterField>
    </div>
  )
}

/**
 * @param {{ label: string, value: string, onChange: (value: string) => void, children: import('react').ReactNode }} props
 */
function FilterField({ label, value, onChange, children }) {
  const id = `alert-filter-${label.toLowerCase()}`
  return (
    <label htmlFor={id} className="flex flex-col gap-1 text-xs font-medium text-muted">
      {label}
      <select
        id={id}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="rounded border border-line bg-canvas px-2 py-2 text-sm text-ink"
      >
        {children}
      </select>
    </label>
  )
}
