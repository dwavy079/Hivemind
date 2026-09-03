import { useState } from 'react'

export default function NewFolderModal({ onCreate, onClose }) {
  const [name, setName] = useState('')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setSaving(true)
    try {
      await onCreate(name.trim())
      onClose()
    } catch (err) {
      setError(err.response?.data?.message || 'Could not create the folder.')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2>New folder</h2>
        <form onSubmit={submit}>
          <div className="field">
            <label htmlFor="folderName">Folder name</label>
            <input
              id="folderName"
              autoFocus
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
            />
          </div>
          {error && <p className="error-text">{error}</p>}
          <div className="modal-actions">
            <button type="button" className="btn" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn btn-primary" disabled={saving || !name.trim()}>
              {saving ? 'Creating…' : 'Create'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
