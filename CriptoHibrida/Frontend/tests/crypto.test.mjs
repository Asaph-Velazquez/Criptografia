import assert from 'node:assert/strict'
import { test } from 'node:test'
import { DH_N, generateDh, generateRsa, generateEcdsa, publicDh, readDh, readSigningKey } from '../src/lib/keys.ts'

test('DH uses a 2048-bit modulus and independent random exchanges', () => {
  assert.equal(BigInt(DH_N).toString(2).length, 2048)
  const first = generateDh()
  const second = generateDh()
  assert.notEqual(first.privateK, first.privateIv)
  assert.notEqual(first.publicK, second.publicK)
  assert.notEqual(first.publicIv, second.publicIv)
  assert.deepEqual(Object.keys(publicDh(first)).sort(), ['g', 'n', 'publicIv', 'publicK'])
})

test('public and private JSON files preserve integer precision', async () => {
  const keys = generateDh()
  assert.deepEqual(await readDh(new File([JSON.stringify(keys)], 'private.json'), true), keys)
  assert.deepEqual(await readDh(new File([JSON.stringify(keys)], 'public.json'), false), publicDh(keys))
})

test('rejects invalid or missing private keys and oversized files', async () => {
  const keys = generateDh()
  await assert.rejects(readDh(new File([JSON.stringify(publicDh(keys))], 'public.json'), true))
  for (const value of [null, {}, { ...keys, n: '23' }, { ...keys, publicK: keys.n }, { ...keys, privateK: '-1' }])
    await assert.rejects(readDh(new File([JSON.stringify(value)], 'bad.json'), true))
  await assert.rejects(readDh(new File([new Uint8Array(32769)], 'large.json'), false))
})

test('generated RSA PEM keys sign and verify raw binary bytes', async () => {
  const files = await generateRsa()
  assert.equal(await readSigningKey(files.privateKey, true), 'RSA')
  assert.equal(await readSigningKey(files.publicKey, false), 'RSA')
  const decode = async file => Buffer.from((await file.text()).replace(/-----[^\n]+-----/g, '').replace(/\s/g, ''), 'base64')
  const algorithm = { name: 'RSASSA-PKCS1-v1_5', hash: 'SHA-256' }
  const privateKey = await crypto.subtle.importKey('pkcs8', await decode(files.privateKey), algorithm, false, ['sign'])
  const publicKey = await crypto.subtle.importKey('spki', await decode(files.publicKey), algorithm, false, ['verify'])
  const bytes = Uint8Array.from({ length: 256 }, (_, index) => index)
  const signature = await crypto.subtle.sign(algorithm, privateKey, bytes)
  assert.equal(await crypto.subtle.verify(algorithm, publicKey, signature, bytes), true)
  bytes[0] ^= 1
  assert.equal(await crypto.subtle.verify(algorithm, publicKey, signature, bytes), false)
})

test('generated ECDSA P-256 PEM keys can be read, sign and verify binary bytes', async () => {
  const files = await generateEcdsa()
  assert.equal(await readSigningKey(files.privateKey, true), 'ECDSA')
  assert.equal(await readSigningKey(files.publicKey, false), 'ECDSA')
  const decode = async file => Buffer.from((await file.text()).replace(/-----[^\n]+-----/g, '').replace(/\s/g, ''), 'base64')
  const privateKey = await crypto.subtle.importKey('pkcs8', await decode(files.privateKey), { name: 'ECDSA', namedCurve: 'P-256' }, false, ['sign'])
  const publicKey = await crypto.subtle.importKey('spki', await decode(files.publicKey), { name: 'ECDSA', namedCurve: 'P-256' }, false, ['verify'])
  const bytes = Uint8Array.from({ length: 256 }, (_, index) => index)
  const algorithm = { name: 'ECDSA', hash: 'SHA-256' }
  const signature = await crypto.subtle.sign(algorithm, privateKey, bytes)
  assert.equal(await crypto.subtle.verify(algorithm, publicKey, signature, bytes), true)
  bytes[0] ^= 1
  assert.equal(await crypto.subtle.verify(algorithm, publicKey, signature, bytes), false)
})

test('PEM reading rejects wrong roles, invalid data, weak RSA and unsupported curves', async () => {
  const files = await generateEcdsa()
  await assert.rejects(readSigningKey(files.privateKey, false))
  await assert.rejects(readSigningKey(files.publicKey, true))
  for (const text of ['', 'not PEM', await files.publicKey.text() + await files.publicKey.text(),
    '-----BEGIN PUBLIC KEY-----\nAAAA\n-----END PUBLIC KEY-----'])
    await assert.rejects(readSigningKey(new File([text], 'bad.pem'), false))
  await assert.rejects(readSigningKey(new File([new Uint8Array(65537)], 'large.pem'), false))
  for (const algorithm of [
    { name: 'ECDSA', namedCurve: 'P-384' },
    { name: 'RSASSA-PKCS1-v1_5', modulusLength: 1024, publicExponent: new Uint8Array([1, 0, 1]), hash: 'SHA-256' },
  ]) {
    const keys = await crypto.subtle.generateKey(algorithm, true, ['sign', 'verify'])
    const body = Buffer.from(await crypto.subtle.exportKey('spki', keys.publicKey)).toString('base64')
    await assert.rejects(readSigningKey(new File([`-----BEGIN PUBLIC KEY-----\n${body}\n-----END PUBLIC KEY-----`], 'unsupported.pem'), false))
  }
})
