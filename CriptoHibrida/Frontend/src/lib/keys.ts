// RFC 3526, group 14. Only public parameters are fixed; secrets use Web Crypto.
export const DH_G = '2'
export const DH_N = BigInt('0x' +
  'FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E088A67CC74020BBEA63B139B22514A08798E3404DDEF' +
  '9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED' +
  'EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3DC2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F' +
  '83655D23DCA3AD961C62F356208552BB9ED529077096966D670C354E4ABC9804F1746C08CA18217C32905E462E36CE3BE' +
  '39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9DE2BCBF6955817183995497CEA956AE515D2261898FA051015' +
  '728E5A8AACAA68FFFFFFFFFFFFFFFF').toString()

export interface DhKeys { g: string; n: string; publicK: string; publicIv: string; privateK?: string; privateIv?: string }

function modPow(base: bigint, exponent: bigint, modulus: bigint): bigint {
  let value = 1n
  while (exponent > 0n) {
    if (exponent & 1n) value = value * base % modulus
    base = base * base % modulus
    exponent >>= 1n
  }
  return value
}

function secret(modulus: bigint): bigint {
  let value: bigint
  do {
    const bytes = crypto.getRandomValues(new Uint8Array(256))
    value = BigInt('0x' + Array.from(bytes, b => b.toString(16).padStart(2, '0')).join(''))
  } while (value < 2n || value > modulus - 2n)
  return value
}

export function generateDh(): DhKeys {
  const n = BigInt(DH_N)
  const b = secret(n)
  const d = secret(n)
  return { g: DH_G, n: DH_N, privateK: b.toString(), privateIv: d.toString(),
    publicK: modPow(2n, b, n).toString(), publicIv: modPow(2n, d, n).toString() }
}

export function publicDh(keys: DhKeys): DhKeys {
  return { g: keys.g, n: keys.n, publicK: keys.publicK, publicIv: keys.publicIv }
}

export async function readDh(file: File, privateRequired: boolean): Promise<DhKeys> {
  if (file.size > 32_768) throw new Error('La llave de intercambio no debe superar 32 KB.')
  const value = JSON.parse(await file.text()) as Record<string, unknown>
  if (!value || typeof value !== 'object') throw new Error('El archivo no contiene una llave de intercambio válida.')
  const fields = ['g', 'n', 'publicK', 'publicIv', ...(privateRequired ? ['privateK', 'privateIv'] : [])]
  for (const field of fields) {
    if (typeof value[field] !== 'string' || !/^[1-9][0-9]{0,2499}$/.test(value[field] as string))
      throw new Error(`La llave debe incluir ${field} como un entero decimal en texto.`)
  }
  const n = BigInt(value.n as string)
  if (n.toString(2).length < 2048 || n.toString(2).length > 8192) throw new Error('El módulo DH debe tener entre 2048 y 8192 bits.')
  for (const field of fields.filter(field => field !== 'n')) {
    const number = BigInt(value[field] as string)
    if (number < 2n || number > n - 2n) throw new Error(`El parámetro ${field} está fuera de rango.`)
  }
  return Object.fromEntries(fields.map(field => [field, value[field]])) as unknown as DhKeys
}

function pem(bytes: ArrayBuffer, label: string): string {
  const base64 = btoa(Array.from(new Uint8Array(bytes), byte => String.fromCharCode(byte)).join(''))
  return `-----BEGIN ${label}-----\n${base64.match(/.{1,64}/g)?.join('\n')}\n-----END ${label}-----\n`
}

export async function generateRsa(): Promise<{ privateKey: File; publicKey: File }> {
  const keys = await crypto.subtle.generateKey({ name: 'RSASSA-PKCS1-v1_5', modulusLength: 2048,
    publicExponent: new Uint8Array([1, 0, 1]), hash: 'SHA-256' }, true, ['sign', 'verify'])
  return {
    privateKey: new File([pem(await crypto.subtle.exportKey('pkcs8', keys.privateKey), 'PRIVATE KEY')], 'firma-privada.pem', { type: 'application/x-pem-file' }),
    publicKey: new File([pem(await crypto.subtle.exportKey('spki', keys.publicKey), 'PUBLIC KEY')], 'firma-publica.pem', { type: 'application/x-pem-file' }),
  }
}

export function download(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = filename
  anchor.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

export function downloadJson(value: unknown, filename: string) {
  download(new Blob([JSON.stringify(value, null, 2)], { type: 'application/json' }), filename)
}
