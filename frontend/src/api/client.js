import axios from 'axios'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'

export const api = axios.create({ baseURL: BASE_URL })

// Every request picks up whatever access token is currently stored.
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// On a 401, try exactly once to refresh the access token using the refresh token,
// then replay the original request. If that also fails, force a re-login.
let refreshPromise = null

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config
    const isAuthRoute = original?.url?.includes('/auth/')

    if (error.response?.status === 401 && !original._retry && !isAuthRoute) {
      original._retry = true
      const refreshToken = localStorage.getItem('refreshToken')
      if (!refreshToken) {
        clearSession()
        return Promise.reject(error)
      }

      try {
        refreshPromise = refreshPromise || api.post('/auth/refresh', { refreshToken })
        const { data } = await refreshPromise
        refreshPromise = null
        saveSession(data)
        original.headers.Authorization = `Bearer ${data.accessToken}`
        return api(original)
      } catch (refreshError) {
        refreshPromise = null
        clearSession()
        return Promise.reject(refreshError)
      }
    }

    return Promise.reject(error)
  }
)

export function saveSession(auth) {
  localStorage.setItem('accessToken', auth.accessToken)
  localStorage.setItem('refreshToken', auth.refreshToken)
  localStorage.setItem('fullName', auth.fullName)
  localStorage.setItem('email', auth.email)
}

export function clearSession() {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  localStorage.removeItem('fullName')
  localStorage.removeItem('email')
}

export function currentSession() {
  const accessToken = localStorage.getItem('accessToken')
  if (!accessToken) return null
  return {
    accessToken,
    fullName: localStorage.getItem('fullName'),
    email: localStorage.getItem('email'),
  }
}
