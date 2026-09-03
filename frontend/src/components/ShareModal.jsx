import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { formatDate } from '../utils/format'

export default function ShareModal({ file, onClose }) {
  const [links, setLinks] = useState([])
  const [loading, setLoading] = useState(true)
  const [expiresInHours, setExpiresInHours] = useState('')
  const [creating, setCreating] = useState(false)
  const [error, setError] = useState('')
  const [copiedToken, setCopiedToken] = useState('')

  const load = async () => {
    setLoading(true)
    try {
      const { data } = await api.get(`/files/${file.id}/share-links`)
      setLinks(data)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [file.id])

  const createLink = async () => {
    setError('')
    setCreating(true)
    try {
      await api.post('/files/share-links', {
        fileItemId: file.id,
        expiresInHours: expiresInHours ? Number(expiresInHours) : null,
      })
      setExpiresInHours('')
      await load()
    } catch (err) {
      setError(err.response?.data?.message || 'Could not create a share link.')
    } finally {
      setCreating(false)
    }
  }

  const revoke = async (id) => {
    await api.delete(`/files/share-links/${id}`)
    await load()
  }

  const copy = async (url, token) => {
    try {
      await navigator.clipboard.writeText(url)
      setCopiedToken(token)
      setTimeout(() => setCopiedToken(''), 1500)
    } catch {
      // Clipboard API unavailable — the link is still selectable in the input field.
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2>Share “{file.name}”</h2>

        <div className="field" style={{ display: 'flex', gap: 8, alignItems: 'flex-end' }}>
          <div style={{ flex: 1 }}>
            <label htmlFor="expiresIn">Expires in (hours, optional)</label>
            <input
              id="expiresIn"
              type="number"
              min="1"
              placeholder="Never expires"
              value={expiresInHours}
              onChange={(e) => setExpiresInHours(e.target.value)}
            />
          </div>
          <button className="btn btn-primary" onClick={createLink} disabled={creating}>
            {creating ? 'Creating…' : 'New link'}
          </button>
        </div>
        {error && <p className="error-text">{error}</p>}

        <div style={{ marginTop: 12 }}>
          {loading && <p className="subtitle">Loading links…</p>}
          {!loading && links.length === 0 && <p className="subtitle">No share links yet.</p>}
          {links.map((link) => (
            <div key={link.id} style={{ borderBottom: '1px solid var(--border)', padding: '8px 0' }}>
              <div className="share-link-row" style={{ border: 'none', padding: 0 }}>
                <input readOnly value={link.url} onFocus={(e) => e.target.select()} />
                <button className="btn" onClick={() => copy(link.url, link.token)}>
                  {copiedToken === link.token ? 'Copied' : 'Copy'}
                </button>
                {!link.revoked && (
                  <button className="btn btn-danger" onClick={() => revoke(link.id)}>
                    Revoke
                  </button>
                )}
                {link.revoked && <span className="badge">Revoked</span>}
              </div>
              <div style={{ fontSize: 11, color: 'var(--text-muted)', marginTop: 4 }}>
                {link.downloadCount} download{link.downloadCount === 1 ? '' : 's'}
                {link.expiresAt ? ` · expires ${formatDate(link.expiresAt)}` : ' · never expires'}
                {' · created '}{formatDate(link.createdAt)}
              </div>
            </div>
          ))}
        </div>

        <div className="modal-actions">
          <button className="btn" onClick={onClose}>Close</button>
        </div>
      </div>
    </div>
  )
}
