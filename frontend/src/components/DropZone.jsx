import { useCallback } from 'react'
import { useDropzone } from 'react-dropzone'
import { Upload, FileText } from 'lucide-react'

export default function DropZone({ onFiles, multiple = false, label = 'Déposez votre PDF ici' }) {
  const onDrop = useCallback(accepted => onFiles(accepted), [onFiles])

  const { getRootProps, getInputProps, isDragActive, acceptedFiles } = useDropzone({
    onDrop,
    accept: { 'application/pdf': ['.pdf'] },
    multiple,
  })

  return (
    <div
      {...getRootProps()}
      className={`border-2 border-dashed rounded-xl p-8 text-center cursor-pointer transition-all
        ${isDragActive
          ? 'border-primary-500 bg-primary-50 scale-105'
          : 'border-gray-300 hover:border-primary-400 hover:bg-gray-50'
        }`}
    >
      <input {...getInputProps()} />
      <Upload className="mx-auto mb-3 text-gray-400" size={36} />
      <p className="text-gray-600 font-medium">{label}</p>
      <p className="text-sm text-gray-400 mt-1">ou cliquez pour sélectionner</p>

      {acceptedFiles.length > 0 && (
        <div className="mt-4 space-y-1">
          {acceptedFiles.map(f => (
            <div key={f.name}
              className="flex items-center gap-2 bg-primary-50 text-primary-700
                         rounded-lg px-3 py-1.5 text-sm">
              <FileText size={14} />
              <span className="truncate">{f.name}</span>
              <span className="text-xs text-gray-400 ml-auto">
                {(f.size / 1024).toFixed(0)} Ko
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}