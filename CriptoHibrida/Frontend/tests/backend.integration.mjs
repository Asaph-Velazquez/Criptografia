import assert from 'node:assert/strict'
import { generateDh, generateRsa, generateEcdsa } from '../src/lib/keys.ts'
import { transfer } from '../src/lib/api.ts'

// Exercise the browser's multipart client against the real application (Docker or Vite).
const nativeFetch = globalThis.fetch
globalThis.fetch = (url, options) => nativeFetch(new URL(url, process.env.TEST_BASE_URL ?? 'http://localhost:5173'), options)
const dh = generateDh()
const bytes = Uint8Array.from({ length: 4097 }, (_, index) => index % 256)
const original = new File([bytes], 'prueba-binaria.mp4', { type: 'application/octet-stream' })
for (const signatureAlgorithm of ['RSA', 'ECDSA']) {
  const keys = await (signatureAlgorithm === 'RSA' ? generateRsa() : generateEcdsa())
  const wrong = await (signatureAlgorithm === 'RSA' ? generateRsa() : generateEcdsa())
  for (const [encrypt, sign] of [[true, true], [true, false], [false, true]]) {
    const options = { encrypt, sign, signatureAlgorithm, remitente: 'Prueba Docker', g: dh.g, n: dh.n, publicK: dh.publicK, publicIv: dh.publicIv }
    const emission = await transfer('send', original, options, sign ? keys.privateKey : null, AbortSignal.timeout(30000))
    const packet = new File([emission.blob], 'paquete.json', { type: 'application/json' })
    const receive = { decrypt: encrypt, verify: sign, g: dh.g, n: dh.n, privateK: dh.privateK, privateIv: dh.privateIv }
    const result = await transfer('receive', packet, receive, sign ? keys.publicKey : null, AbortSignal.timeout(30000))
    assert.deepEqual(new Uint8Array(await result.blob.arrayBuffer()), bytes)
    assert.equal(result.integrity, sign ? 'VERIFIED' : 'NOT_REQUESTED')
    assert.equal(result.filename, original.name)
    console.log(`PASS ${signatureAlgorithm} encrypt=${encrypt}, sign=${sign}: exact binary round trip`)
    if (sign) {
      await assert.rejects(transfer('receive', packet, receive, wrong.publicKey, AbortSignal.timeout(30000)), /integridad/)
      const altered = JSON.parse(await emission.blob.text())
      altered.firma = Buffer.alloc(256).toString('base64')
      await assert.rejects(transfer('receive', new File([JSON.stringify(altered)], 'alterado.json'), receive,
        keys.publicKey, AbortSignal.timeout(30000)), /integridad/)
      console.log('PASS tampered signature and wrong public key rejected without download')
    }
  }
}
