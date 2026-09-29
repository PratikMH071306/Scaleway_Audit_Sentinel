import { Link } from 'react-router-dom'

/**
 * @param {{ actions?: import('react').ReactNode }} props
 */
export default function Header({ actions }) {
  return (
    <header className="border-b border-line bg-panel">
      <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4 px-4 py-4 sm:px-6">
        <div>
          <p className="text-xs font-medium uppercase tracking-[0.16em] text-muted">Operations</p>
          <Link to="/alerts" className="text-xl font-semibold text-ink">
            Security Monitor
          </Link>
        </div>
        {actions}
      </div>
    </header>
  )
}
