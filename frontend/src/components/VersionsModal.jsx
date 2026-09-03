import { useEffect, useRef, useState } from 'react'
import { api } from '../api/client'
import { formatBytes, formatDate } from '../utils/format'

export default function VersionsModal({ file, onClose, onUploadedNewVersion }) {
  const [versions, setVersions] = useState([])
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)
  const fileInputRef = useRef(null)

  const load = async () => {
    setLoading(true)
    try {
      const { data } = await api.get(`/files/${file.id}/versions`)
      setVersions(data)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [file.id])

  const downloadVersion = (versionId) => {
    window.location.href = `${api.defaults.baseURL}/files/${file.id}/versions/${versionId}/download`
  }

  const uploadNewVersion = async (e) => {
    const picked = e.target.files?.[0]
    if (!picked) return
    setUploading(true)
    try {
      const formData = new FormData()
      formData.append('file', picked)
      formData.append('name', file.name)
      if (file.parentId) formData.append('parentId', file.parentId)
      await api.post('/files/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
      await load()
      onUploadedNewVersion?.()
    } finally {
      setUploading(false)
      e.target.value = ''
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2>Versions of “{file.name}”</h2>

        {loading && <p className="subtitle">Loading…</p>}
        {!loading &&
          versions.map((v) => (
            <div className="version-row" key={v.id}>
              <div>
                <strong>v{v.versionNumber}</strong> · {formatBytes(v.sizeBytes)}
                <div style={{ color: 'var(--text-muted)', fontSize: 12 }}>
                  {formatDate(v.uploadedAt)} by {v.uploadedByEmail}
                </div>
              </div>
              <button className="btn" onClick={() => downloadVersion(v.id)}>Download</button>
            </div>
          ))}

        <div className="modal-actions" style={{ justifyContent: 'space-between' }}>
          <div>
            <input
              ref={fileInputRef}
              type="file"
              style={{ display: 'none' }}
              onChange={uploadNewVersion}
            />
            <button className="btn" onClick={() => fileInputRef.current?.click()} disabled={uploading}>
              {uploading ? 'Uploading…' : 'Upload new version'}
            </button>
          </div>
          <button className="btn" onClick={onClose}>Close</button>
        </div>
      </div>
    </div>
  )
}
