import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './context/AuthContext'
import Landing from './pages/Landing'
import Login from './pages/Login'
import Register from './pages/Register'
import Dashboard from './pages/Dashboard'
import SharedFile from './pages/SharedFile'

function PrivateRoute({ children }) {
  const { session } = useAuth()
  return session ? children : <Navigate to="/login" replace />
}

export default function App() {
  const { session } = useAuth()

  return (
    <Routes>
      <Route path="/" element={<Landing />} />
      <Route path="/login" element={session ? <Navigate to="/app" replace /> : <Login />} />
      <Route path="/register" element={session ? <Navigate to="/app" replace /> : <Register />} />
      <Route path="/s/:token" element={<SharedFile />} />
      <Route
        path="/app/*"
        element={
          <PrivateRoute>
            <Dashboard />
          </PrivateRoute>
        }
      />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
