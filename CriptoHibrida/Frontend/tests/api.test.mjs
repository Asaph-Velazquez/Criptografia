import assert from 'node:assert/strict'
import { test } from 'node:test'
import { transfer } from '../src/lib/api.ts'

test('client refuses an unconfirmed verification and unexpected response types', async () => {
  const original = globalThis.fetch
  try {
    for (const headers of [
      { 'content-type': 'application/octet-stream' },
      { 'content-type': 'application/octet-stream', 'x-integrity-status': 'NOT_REQUESTED' },
      { 'content-type': 'text/html', 'x-integrity-status': 'VERIFIED' },
    ]) {
      globalThis.fetch = async () => new Response(new Uint8Array([0, 255]), { headers })
      await assert.rejects(transfer('receive', new File(['{}'], 'packet.json'), { verify: true }, null, new AbortController().signal))
    }
    globalThis.fetch = async () => new Response('{}', { status: 422 })
    await assert.rejects(transfer('receive', new File(['{}'], 'packet.json'), { verify: true }, null,
      new AbortController().signal), /integridad/)
  } finally { globalThis.fetch = original }
})

test('multipart preserves file bytes and decodes Unicode download names', async () => {
  const original = globalThis.fetch
  try {
    const bytes = new Uint8Array([0, 255, 128, 1])
    globalThis.fetch = async (url, request) => {
      assert.equal(url, '/api/crypto/verify-decrypt')
      assert.deepEqual(new Uint8Array(await request.body.get('package').arrayBuffer()), bytes)
      assert.deepEqual(JSON.parse(await request.body.get('options').text()), { verify: true })
      return new Response(bytes, { headers: { 'content-type': 'application/octet-stream',
        'x-integrity-status': 'VERIFIED', 'content-disposition': "attachment; filename*=UTF-8''m%C3%BAsica.mp3" } })
    }
    const result = await transfer('receive', new File([bytes], 'packet.json'), { verify: true }, null, new AbortController().signal)
    assert.equal(result.filename, 'música.mp3')
    assert.deepEqual(new Uint8Array(await result.blob.arrayBuffer()), bytes)
  } finally { globalThis.fetch = original }
})
