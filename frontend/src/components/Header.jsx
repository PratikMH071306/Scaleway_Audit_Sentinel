import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext.jsx'

/**
 * @param {{ actions?: import('react').ReactNode }} props
 */
export default function Header({ actions }) {
  const { session, logout } = useAuth()

  return (
    <header className="border-b border-line bg-panel">
      <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4 px-4 py-4 sm:px-6">
        <div>
          <p className="text-xs font-medium uppercase tracking-[0.16em] text-muted">Operations</p>
          <Link to="/alerts" className="text-xl font-semibold text-ink">
            Security Monitor
          </Link>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          {session ? (
            <p className="text-sm text-muted">
              {session.username}
              <span className="ml-2 rounded border border-line px-2 py-0.5 text-xs uppercase tracking-wide">{session.role}</span>
            </p>
          ) : null}
          {actions}
          {session ? (
            <button
              type="button"
              className="rounded-md border border-line px-3 py-2 text-sm text-ink"
              onClick={logout}
            >
              Sign out
            </button>
          ) : null}
        </div>
      </div>
    </header>
  )
}
