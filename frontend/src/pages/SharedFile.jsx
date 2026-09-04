import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api/client'
import { formatBytes } from '../utils/format'
import Logo from '../components/Logo'

export default function SharedFile() {
  const { token } = useParams()
  const [info, setInfo] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api
      .get(`/share/${token}`)
      .then(({ data }) => setInfo(data))
      .catch((err) => setError(err.response?.data?.message || 'This link is not available.'))
  }, [token])

  const downloadUrl = `${api.defaults.baseURL}/share/${token}/download`

  return (
    <div className="auth-shell">
      <div className="auth-card" style={{ width: 420 }}>
        <Link to="/" className="auth-logo" style={{ display: 'inline-flex' }}>
          <Logo size={20} />
        </Link>
        <h1>Shared file</h1>
        {error && <p className="error-text">{error}</p>}
        {!error && !info && <p className="subtitle">Loading…</p>}
        {info && (
          <>
            <p className="subtitle">Someone shared this file with you.</p>
            <div style={{ border: '1px solid var(--border)', borderRadius: 8, padding: 14, marginBottom: 18 }}>
              <div style={{ fontWeight: 600 }}>{info.fileName}</div>
              <div style={{ fontSize: 13, color: 'var(--text-muted)' }}>{formatBytes(info.sizeBytes)}</div>
            </div>
            <a className="btn btn-primary" style={{ display: 'block', textAlign: 'center' }} href={downloadUrl}>
              Download
            </a>
          </>
        )}
      </div>
    </div>
  )
}
