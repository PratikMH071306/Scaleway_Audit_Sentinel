import { createContext, useContext, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { configureApiAuth } from '../api/http.js'
import { loginRequest } from './authApi.js'
import { clearSession, readSession, storeSession } from './session.js'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const navigate = useNavigate()
  const [session, setSession] = useState(() => readSession())

  const value = useMemo(() => {
    configureApiAuth({
      token: () => readSession()?.accessToken ?? null,
      onUnauthorized: () => {
        clearSession()
        setSession(null)
        navigate('/login', { replace: true })
      },
    })

    return {
      session,
      async login(username, password) {
        const response = await loginRequest(username, password)
        const next = storeSession(response)
        setSession(next)
      },
      logout() {
        clearSession()
        setSession(null)
        navigate('/login', { replace: true })
      },
    }
  }, [navigate, session])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}
