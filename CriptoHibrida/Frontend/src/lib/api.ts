export interface TransferResult { blob: Blob; filename: string; integrity: string; direction: 'send' | 'receive' }

export async function transfer(direction: 'send' | 'receive', file: File, options: Record<string, unknown>,
  pem: File | null, signal: AbortSignal): Promise<TransferResult> {
  const body = new FormData()
  body.append(direction === 'send' ? 'file' : 'package', file)
  body.append('options', new Blob([JSON.stringify(options)], { type: 'application/json' }))
  if (pem) body.append(direction === 'send' ? 'privateKey' : 'publicKey', pem)
  const base = (import.meta.env?.VITE_API_BASE_URL ?? '').replace(/\/$/, '')
  const response = await fetch(`${base}/api/crypto/${direction === 'send' ? 'process' : 'verify-decrypt'}`, {
    method: 'POST', body, signal, credentials: 'omit',
  })
  if (!response.ok) {
    if (response.status === 422) throw new Error('La integridad no pudo verificarse. No se descargó ningún archivo. Comprueba la llave pública y solicita el paquete original al remitente.')
    if (response.status === 413) throw new Error('El archivo supera el límite: 10 MB para enviar o 16 MB para un paquete recibido.')
    if (response.status >= 500) throw new Error('No se pudo conectar con el servicio criptográfico. Comprueba que Spring Boot esté activo en el puerto 8080 y vuelve a intentar.')
    throw new Error('El servicio rechazó la solicitud. Revisa que las llaves correspondan al archivo y a las operaciones elegidas.')
  }
  const blob = await response.blob()
  if (direction === 'send' && !response.headers.get('content-type')?.includes('application/json'))
    throw new Error('La respuesta no es un paquete JSON. Revisa la dirección del backend.')
  if (direction === 'receive') {
    if (!response.headers.get('content-type')?.includes('application/octet-stream'))
      throw new Error('El servicio no devolvió un archivo binario válido.')
    if (options.verify && response.headers.get('x-integrity-status') !== 'VERIFIED')
      throw new Error('El servicio no confirmó la integridad. No se habilitó la descarga.')
  }
  const disposition = response.headers.get('content-disposition') ?? ''
  const encoded = /filename\*=UTF-8''([^;]+)/i.exec(disposition)?.[1]
  const plain = /filename="([^"]+)"/i.exec(disposition)?.[1]
  let filename = direction === 'send' ? 'paquete-protegido.json' : 'archivo-recuperado.bin'
  try { filename = encoded ? decodeURIComponent(encoded) : plain && !plain.startsWith('=?') ? plain : filename } catch { /* keep safe default */ }
  filename = Array.from(filename.replaceAll('\\', '/').split('/').pop() || 'archivo.bin').filter(char => char.charCodeAt(0) > 31 && char.charCodeAt(0) !== 127).join('') || 'archivo.bin'
  return { blob, filename, integrity: response.headers.get('x-integrity-status') ?? 'NOT_REQUESTED', direction }
}

export function sizeLabel(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}
