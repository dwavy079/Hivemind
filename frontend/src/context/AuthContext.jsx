import { createContext, useContext, useMemo, useState } from 'react'
import { api, saveSession, clearSession, currentSession } from '../api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [session, setSession] = useState(() => currentSession())

  const login = async (email, password) => {
    const { data } = await api.post('/auth/authenticate', { email, password })
    saveSession(data)
    setSession(currentSession())
  }

  const register = async (fullName, email, password) => {
    const { data } = await api.post('/auth/register', { fullName, email, password })
    saveSession(data)
    setSession(currentSession())
  }

  const logout = async () => {
    try {
      await api.post('/auth/logout')
    } catch {
      // Best-effort: clear locally regardless of whether the server call succeeded.
    }
    clearSession()
    setSession(null)
  }

  const value = useMemo(() => ({ session, login, register, logout }), [session])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider')
  return ctx
}
