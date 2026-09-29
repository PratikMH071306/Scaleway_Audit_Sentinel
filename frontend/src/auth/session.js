const STORAGE_KEY = 'secure-monitor.session'

/**
 * Session storage keeps the access token for this browser tab only.
 * The token is still readable by scripts on this origin, so the API
 * remains the authority for every privileged action.
 */

export function readSession() {
  const raw = sessionStorage.getItem(STORAGE_KEY)
  if (!raw) {
    return null
  }
  try {
    const session = JSON.parse(raw)
    if (!session?.accessToken || !session.username || !session.role || !session.expiresAt) {
      sessionStorage.removeItem(STORAGE_KEY)
      return null
    }
    if (Date.now() >= session.expiresAt) {
      sessionStorage.removeItem(STORAGE_KEY)
      return null
    }
    return session
  } catch {
    sessionStorage.removeItem(STORAGE_KEY)
    return null
  }
}

/**
 * @param {{ accessToken: string, expiresIn: number, username: string, role: string }} login
 */
export function storeSession(login) {
  const session = {
    accessToken: login.accessToken,
    username: login.username,
    role: login.role,
    expiresAt: Date.now() + login.expiresIn * 1000,
  }
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session))
  return session
}

export function clearSession() {
  sessionStorage.removeItem(STORAGE_KEY)
}
