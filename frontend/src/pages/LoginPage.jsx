import { useEffect, useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { errorMessage } from '../api/http.js'
import { useAuth } from '../auth/AuthContext.jsx'

export default function LoginPage() {
  const { session, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    document.title = 'Sign in · Security Monitor'
  }, [])

  if (session) {
    return <Navigate to="/alerts" replace />
  }

  async function onSubmit(event) {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await login(username.trim(), password)
      const destination = location.state?.from || '/alerts'
      navigate(destination, { replace: true })
    } catch (cause) {
      setError(errorMessage(cause))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="mx-auto flex min-h-screen max-w-md flex-col justify-center px-4 py-10">
      <p className="text-xs font-medium uppercase tracking-[0.16em] text-muted">Operations</p>
      <h1 className="mt-2 text-2xl font-semibold">Sign in</h1>
      <p className="mt-2 text-sm text-muted">Use your security monitor account. Access is enforced by the API.</p>

      <form className="mt-6 flex flex-col gap-4" onSubmit={onSubmit}>
        <label className="flex flex-col gap-1 text-sm">
          Username
          <input
            className="rounded-md border border-line bg-panel px-3 py-2 text-ink"
            autoComplete="username"
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            required
          />
        </label>
        <label className="flex flex-col gap-1 text-sm">
          Password
          <input
            className="rounded-md border border-line bg-panel px-3 py-2 text-ink"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />
        </label>
        {error ? <p className="text-sm text-critical">{error}</p> : null}
        <button
          className="rounded-md bg-signal px-4 py-2 text-sm font-semibold text-canvas disabled:opacity-60"
          type="submit"
          disabled={submitting}
        >
          {submitting ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
    </main>
  )
}
