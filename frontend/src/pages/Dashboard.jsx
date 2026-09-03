import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { api } from '../api/client'
import { useAuth } from '../context/AuthContext'
import { formatBytes, formatDate } from '../utils/format'
import NewFolderModal from '../components/NewFolderModal'
import ShareModal from '../components/ShareModal'
import VersionsModal from '../components/VersionsModal'

export default function Dashboard() {
  const { session, logout } = useAuth()
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const folderId = searchParams.get('folder') ? Number(searchParams.get('folder')) : null

  const [contents, setContents] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [showNewFolder, setShowNewFolder] = useState(false)
  const [shareTarget, setShareTarget] = useState(null)
  const [versionsTarget, setVersionsTarget] = useState(null)
  const [uploading, setUploading] = useState(false)
  const fileInputRef = useRef(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const { data } = await api.get('/files', { params: folderId ? { folderId } : {} })
      setContents(data)
    } catch (err) {
      setError(err.response?.data?.message || 'Could not load your files.')
    } finally {
      setLoading(false)
    }
  }, [folderId])

  useEffect(() => {
    load()
  }, [load])

  const openFolder = (id) => {
    if (id) setSearchParams({ folder: String(id) })
    else setSearchParams({})
  }

  const createFolder = async (name) => {
    await api.post('/files/folders', { name, parentId: folderId })
    await load()
  }

  const uploadFiles = async (fileList) => {
    if (!fileList || fileList.length === 0) return
    setUploading(true)
    try {
      for (const file of fileList) {
        const formData = new FormData()
        formData.append('file', file)
        if (folderId) formData.append('parentId', folderId)
        await api.post('/files/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
      }
      await load()
    } catch (err) {
      setError(err.response?.data?.message || 'Upload failed.')
    } finally {
      setUploading(false)
    }
  }

  const onFileInputChange = (e) => {
    uploadFiles(Array.from(e.target.files || []))
    e.target.value = ''
  }

  const onDrop = (e) => {
    e.preventDefault()
    uploadFiles(Array.from(e.dataTransfer.files || []))
  }

  const downloadFile = (item) => {
    window.location.href = `${api.defaults.baseURL}/files/${item.id}/download`
  }

  const renameItem = async (item) => {
    const newName = window.prompt('Rename to:', item.name)
    if (!newName || newName === item.name) return
    await api.patch(`/files/${item.id}/rename`, { newName })
    await load()
  }

  const deleteItem = async (item) => {
    const kind = item.type === 'FOLDER' ? 'folder (and everything inside it)' : 'file'
    if (!window.confirm(`Delete this ${kind}: "${item.name}"?`)) return
    await api.delete(`/files/${item.id}`)
    await load()
  }

  const onLogout = async () => {
    await logout()
    navigate('/login')
  }

  return (
    <div className="app-shell" onDragOver={(e) => e.preventDefault()} onDrop={onDrop}>
      <div className="topbar">
        <div className="brand">📁 FileShare</div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <span style={{ fontSize: 13, color: 'var(--text-muted)' }}>{session?.fullName}</span>
          <button className="btn" onClick={onLogout}>Log out</button>
        </div>
      </div>

      <div className="main">
        <div className="toolbar">
          <button className="btn btn-primary" onClick={() => fileInputRef.current?.click()} disabled={uploading}>
            {uploading ? 'Uploading…' : 'Upload files'}
          </button>
          <input ref={fileInputRef} type="file" multiple style={{ display: 'none' }} onChange={onFileInputChange} />
          <button className="btn" onClick={() => setShowNewFolder(true)}>New folder</button>
          <span style={{ fontSize: 12, color: 'var(--text-muted)', marginLeft: 8 }}>
            or drag &amp; drop files anywhere on this page
          </span>
        </div>

        {contents && (
          <div className="breadcrumb">
            <button onClick={() => openFolder(null)}>My Files</button>
            {contents.breadcrumb.map((b) => (
              <span key={b.id}>
                {' / '}
                <button onClick={() => openFolder(b.id)}>{b.name}</button>
              </span>
            ))}
          </div>
        )}

        {error && <p className="error-text">{error}</p>}

        {loading && <p className="subtitle">Loading…</p>}

        {!loading && contents && contents.items.length === 0 && (
          <div className="empty-state">Nothing here yet. Upload a file or create a folder to get started.</div>
        )}

        {!loading && contents && contents.items.length > 0 && (
          <table className="file-table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Size</th>
                <th>Updated</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {contents.items.map((item) => (
                <tr key={item.id}>
                  <td>
                    <button
                      className="file-name-btn"
                      onClick={() => (item.type === 'FOLDER' ? openFolder(item.id) : downloadFile(item))}
                    >
                      <span>{item.type === 'FOLDER' ? '📁' : '📄'}</span>
                      {item.name}
                      {item.type === 'FILE' && item.versionCount > 1 && (
                        <span className="badge">v{item.versionCount}</span>
                      )}
                    </button>
                  </td>
                  <td>{item.type === 'FILE' ? formatBytes(item.sizeBytes) : '—'}</td>
                  <td>{formatDate(item.updatedAt)}</td>
                  <td>
                    <div className="row-actions">
                      {item.type === 'FILE' && (
                        <>
                          <button onClick={() => setShareTarget(item)}>Share</button>
                          <button onClick={() => setVersionsTarget(item)}>Versions</button>
                        </>
                      )}
                      <button onClick={() => renameItem(item)}>Rename</button>
                      <button style={{ color: 'var(--danger)' }} onClick={() => deleteItem(item)}>Delete</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {showNewFolder && <NewFolderModal onCreate={createFolder} onClose={() => setShowNewFolder(false)} />}
      {shareTarget && <ShareModal file={shareTarget} onClose={() => setShareTarget(null)} />}
      {versionsTarget && (
        <VersionsModal file={versionsTarget} onClose={() => setVersionsTarget(null)} onUploadedNewVersion={load} />
      )}
    </div>
  )
}
