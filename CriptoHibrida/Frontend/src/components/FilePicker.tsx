import { useId, useState } from 'react'
import { Icon } from './Icon'
import { sizeLabel } from '../lib/api'

export function FilePicker({ file, onChange, label, hint, accept, compact = false, disabled = false }:
  { file: File | null; onChange: (file: File | null) => void; label: string; hint: string; accept?: string; compact?: boolean; disabled?: boolean }) {
  const id = useId()
  const [dragging, setDragging] = useState(false)
  return <div className={`file-picker ${compact ? 'compact' : ''} ${file ? 'has-file' : ''} ${dragging ? 'dragging' : ''}`}
    onDragOver={event => { event.preventDefault(); if (!disabled) setDragging(true) }}
    onDragLeave={() => setDragging(false)}
    onDrop={event => { event.preventDefault(); setDragging(false); if (!disabled && event.dataTransfer.files[0]) onChange(event.dataTransfer.files[0]) }}>
    <input id={id} aria-label={label} type="file" accept={accept} disabled={disabled} onChange={event => { onChange(event.target.files?.[0] ?? null); event.target.value = '' }} />
    <label htmlFor={id} className="file-picker-label">
      <span className="upload-icon"><Icon name={file ? 'file' : compact ? 'key' : 'upload'} size={compact ? 20 : 27} /></span>
      <span className="upload-copy"><strong>{file ? file.name : label}</strong><span>{file ? sizeLabel(file.size) : hint}</span></span>
      {!compact && !file && <span className="browse-button">Elegir archivo <Icon name="plus" size={16} /></span>}
      {compact && !file && <Icon name="plus" size={17} />}
    </label>
    {file && <button type="button" className="remove-file icon-button" aria-label={`Quitar ${file.name}`} disabled={disabled} onClick={() => onChange(null)}><Icon name="x" size={17} /></button>}
  </div>
}
