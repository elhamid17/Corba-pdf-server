import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import * as authApi from '../api/authApi'
import { clearToken, getToken, setToken } from '../api/client'

const AuthContext = createContext(null)

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth doit etre utilise dans <AuthProvider>')
  return ctx
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(Boolean(getToken()))

  const refresh = useCallback(async () => {
    if (!getToken()) {
      setUser(null)
      setLoading(false)
      return null
    }
    try {
      const me = await authApi.me()
      setUser(me)
      return me
    } catch (e) {
      if (e.status === 401 || e.status === 403) clearToken()
      setUser(null)
      return null
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { refresh() }, [refresh])

  const login = useCallback(async ({ identifier, password }) => {
    const res = await authApi.login({ identifier, password })
    setToken(res.token)
    const fresh = await authApi.me().catch(() => res.user)
    setUser(fresh)
    return fresh
  }, [])

  const register = useCallback(async ({ email, username, password }) => {
    const res = await authApi.register({ email, username, password })
    setToken(res.token)
    const fresh = await authApi.me().catch(() => res.user)
    setUser(fresh)
    return fresh
  }, [])

  const logout = useCallback(() => {
    clearToken()
    setUser(null)
  }, [])

  const value = {
    user,
    loading,
    isAuthenticated: Boolean(user),
    isAdmin: Boolean(user && Array.isArray(user.roles) && user.roles.includes('ADMIN')),
    login,
    register,
    logout,
    refresh,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
