import assert from 'node:assert/strict'
import { generateDh, generateRsa } from '../src/lib/keys.ts'
import { transfer } from '../src/lib/api.ts'

// Exercise the same client and Vite proxy used by the browser, against the real backend.
const nativeFetch = globalThis.fetch
globalThis.fetch = (url, options) => nativeFetch(new URL(url, 'http://localhost:5173'), options)
const dh = generateDh()
const rsa = await generateRsa()
const bytes = Uint8Array.from({ length: 4097 }, (_, index) => index % 256)
const original = new File([bytes], 'prueba-binaria.mp4', { type: 'application/octet-stream' })
for (const [encrypt, sign] of [[true, true], [true, false], [false, true]]) {
  const options = { encrypt, sign, remitente: 'Prueba local', g: dh.g, n: dh.n, publicK: dh.publicK, publicIv: dh.publicIv }
  const emission = await transfer('send', original, options, sign ? rsa.privateKey : null, AbortSignal.timeout(30000))
  const packet = new File([emission.blob], 'paquete.json', { type: 'application/json' })
  const receive = { decrypt: encrypt, verify: sign, g: dh.g, n: dh.n, privateK: dh.privateK, privateIv: dh.privateIv }
  const result = await transfer('receive', packet, receive, sign ? rsa.publicKey : null, AbortSignal.timeout(30000))
  assert.deepEqual(new Uint8Array(await result.blob.arrayBuffer()), bytes)
  assert.equal(result.integrity, sign ? 'VERIFIED' : 'NOT_REQUESTED')
  assert.equal(result.filename, original.name)
  console.log(`PASS encrypt=${encrypt}, sign=${sign}: exact binary round trip`)
  if (sign) {
    const altered = JSON.parse(await emission.blob.text())
    altered.firma = Buffer.alloc(256).toString('base64')
    await assert.rejects(transfer('receive', new File([JSON.stringify(altered)], 'alterado.json'), receive,
      rsa.publicKey, AbortSignal.timeout(30000)), /integridad/)
    console.log('PASS tampered signature rejected without download')
  }
}
