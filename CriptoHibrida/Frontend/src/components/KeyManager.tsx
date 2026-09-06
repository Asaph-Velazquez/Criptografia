import { useState } from 'react'
import { ThinkingOrb } from 'thinking-orbs'
import { Icon } from './Icon'
import { download, downloadJson, generateDh, generateRsa, publicDh } from '../lib/keys'
import type { DhKeys } from '../lib/keys'

export function KeyManager({ onUseDh, onUseRsa }: { onUseDh: (keys: DhKeys) => void; onUseRsa: (keys: { privateKey: File; publicKey: File }) => void }) {
  const [dh, setDh] = useState<DhKeys | null>(null)
  const [rsa, setRsa] = useState<{ privateKey: File; publicKey: File } | null>(null)
  const [busy, setBusy] = useState<'dh' | 'rsa' | null>(null)
  const [message, setMessage] = useState('')
  async function generate(type: 'dh' | 'rsa') {
    setMessage(''); setBusy(type)
    try {
      await new Promise<void>(resolve => requestAnimationFrame(() => requestAnimationFrame(() => resolve())))
      if (type === 'dh') setDh(generateDh()); else setRsa(await generateRsa())
    } catch { setMessage('No se pudieron crear las llaves. Abre la aplicación en localhost o HTTPS y vuelve a intentar.') }
    finally { setBusy(null) }
  }
  return <section className="keys-page">
    <div className="page-heading"><div><h1>Las llaves de tu intercambio.</h1><p>Prepara lo que compartes. Conserva lo que es solo tuyo.</p></div></div>
    <div className="notice"><Icon name="info" /><p>Las llaves se generan en este navegador. Descarga las privadas antes de cerrar la página; no se conservan entre sesiones.</p></div>
    {message && <div className="notice" role="status">{message}</div>}
    <div className="key-workspaces">
      <article className="key-workspace"><div className="key-heading"><span className="feature-icon"><Icon name="transfer" size={25} /></span><span className="subtle-tag">Para recibir archivos</span></div>
        <h2>Llaves de intercambio</h2><p>Comparte tu llave pública con quien te enviará un archivo. Tu respaldo privado te permite recuperarlo después.</p>
        <ul className="key-facts"><li>Dos acuerdos Diffie-Hellman independientes</li><li>Parámetros de 2048 bits</li><li>Archivos de intercambio en JSON</li></ul>
        <button className="primary-button" disabled={busy !== null} onClick={() => generate('dh')}>{busy === 'dh' ? <ThinkingOrb state="connecting" size={20} theme="dark" /> : <Icon name="plus" size={18} />}{dh ? 'Crear un nuevo par DH' : 'Crear llaves de intercambio'}</button>
        {dh && <div className="generated-keys"><p><Icon name="check" size={16} /> Llaves listas</p><button className="download-row" onClick={() => downloadJson(publicDh(dh), 'intercambio-publico.json')}><span>Pública · compartir</span><Icon name="download" size={18} /></button><button className="download-row" onClick={() => downloadJson(dh, 'respaldo-privado-dh.json')}><span>Privada · conservar</span><Icon name="download" size={18} /></button><button className="text-button" onClick={() => { onUseDh(dh); setMessage('Llaves de intercambio listas en la pestaña Recibir.') }}>Usar para recibir en esta sesión <Icon name="arrow" size={16} /></button></div>}
      </article>
      <article className="key-workspace"><div className="key-heading"><span className="feature-icon"><Icon name="sign" size={25} /></span><span className="subtle-tag">Para firmar archivos</span></div>
        <h2>Llaves de firma</h2><p>Firma con tu llave privada. Quien recibe el archivo usa tu llave pública para comprobar que sus bytes no cambiaron.</p>
        <ul className="key-facts"><li>Firma RSA de 2048 bits con SHA-256</li><li>Privada en formato PKCS#8</li><li>Pública en formato X.509</li></ul>
        <button className="primary-button" disabled={busy !== null} onClick={() => generate('rsa')}>{busy === 'rsa' ? <ThinkingOrb state="solving" size={20} theme="dark" /> : <Icon name="plus" size={18} />}{rsa ? 'Crear un nuevo par RSA' : 'Crear llaves de firma'}</button>
        {rsa && <div className="generated-keys"><p><Icon name="check" size={16} /> Llaves listas</p><button className="download-row" onClick={() => download(rsa.publicKey, rsa.publicKey.name)}><span>Pública · compartir</span><Icon name="download" size={18} /></button><button className="download-row" onClick={() => download(rsa.privateKey, rsa.privateKey.name)}><span>Privada · conservar</span><Icon name="download" size={18} /></button><button className="text-button" onClick={() => { onUseRsa(rsa); setMessage('Llaves de firma listas para enviar y recibir en esta sesión.') }}>Usar en esta sesión <Icon name="arrow" size={16} /></button></div>}
      </article>
    </div>
    <p className="footnote">Si creas otro par, descarga primero el actual. Las llaves nuevas no abren archivos protegidos con las anteriores.</p>
  </section>
}
