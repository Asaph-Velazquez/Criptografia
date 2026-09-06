import { useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { ThinkingOrb } from 'thinking-orbs'
import { Icon } from './components/Icon'
import { FilePicker } from './components/FilePicker'
import { KeyManager } from './components/KeyManager'
import { download, readDh } from './lib/keys'
import type { DhKeys } from './lib/keys'
import { sizeLabel, transfer } from './lib/api'
import type { TransferResult } from './lib/api'
import './App.css'

function App() {
  const [page, setPage] = useState<'transfer' | 'keys' | 'guide'>('transfer')
  const [direction, setDirection] = useState<'send' | 'receive'>('send')
  const [file, setFile] = useState<File | null>(null)
  const [cipher, setCipher] = useState(true)
  const [signature, setSignature] = useState(true)
  const [sender, setSender] = useState('')
  const [sendPem, setSendPem] = useState<File | null>(null)
  const [receivePem, setReceivePem] = useState<File | null>(null)
  const [sendDh, setSendDh] = useState<DhKeys | null>(null)
  const [receiveDh, setReceiveDh] = useState<DhKeys | null>(null)
  const [sendDhFile, setSendDhFile] = useState<File | null>(null)
  const [receiveDhFile, setReceiveDhFile] = useState<File | null>(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [loadingKey, setLoadingKey] = useState(false)
  const [result, setResult] = useState<TransferResult | null>(null)
  const controller = useRef<AbortController | null>(null)
  const keyRevision = useRef(0)
  const sending = direction === 'send'
  const pem = sending ? sendPem : receivePem
  const dh = sending ? sendDh : receiveDh
  const keysReady = (!cipher || dh !== null) && (!signature || pem !== null)

  function switchDirection(next: 'send' | 'receive') {
    if (next === direction) return
    keyRevision.current++
    setLoadingKey(false); setDirection(next); setFile(null); setResult(null); setError(''); setCipher(true); setSignature(true)
  }
  async function setDhFile(next: File | null) {
    const revision = ++keyRevision.current
    setError(''); setResult(null)
    if (sending) { setSendDh(null); setSendDhFile(next) } else { setReceiveDh(null); setReceiveDhFile(next) }
    if (!next) { setLoadingKey(false); return }
    setLoadingKey(true)
    try {
      const keys = await readDh(next, !sending)
      if (revision === keyRevision.current) { if (sending) setSendDh(keys); else setReceiveDh(keys) }
    } catch (cause) { if (revision === keyRevision.current) setError(cause instanceof Error ? cause.message : 'La llave JSON no es válida.') }
    finally { if (revision === keyRevision.current) setLoadingKey(false) }
  }
  function setPem(next: File | null) {
    setError(''); setResult(null)
    if (next && next.size > 65536) { setError('La llave PEM no debe superar 64 KB.'); return }
    if (sending) setSendPem(next); else setReceivePem(next)
  }
  function chooseFile(next: File | null) {
    setError(''); setResult(null)
    if (next && next.size > (sending ? 10 : 16) * 1024 * 1024) { setError(`El límite es de ${sending ? 10 : 16} MB. Elige un archivo más pequeño.`); return }
    setFile(next)
  }
  async function submit(event: FormEvent) {
    event.preventDefault(); setError(''); setResult(null)
    if (!file) { setError('Elige el archivo que quieres procesar.'); return }
    if (!cipher && !signature) { setError('Activa al menos una operación.'); return }
    if (sending && !sender.trim()) { setError('Escribe el nombre del remitente.'); return }
    if (!keysReady) { setError('Carga las llaves necesarias para las operaciones seleccionadas.'); return }
    setBusy(true)
    const abort = new AbortController()
    controller.current = abort
    const timeout = setTimeout(() => abort.abort('timeout'), 120000)
    try {
      const options: Record<string, unknown> = sending ? { encrypt: cipher, sign: signature, remitente: sender.trim() } : { decrypt: cipher, verify: signature }
      if (cipher && dh) Object.assign(options, sending ? { g: dh.g, n: dh.n, publicK: dh.publicK, publicIv: dh.publicIv } : { g: dh.g, n: dh.n, privateK: dh.privateK, privateIv: dh.privateIv })
      setResult(await transfer(direction, file, options, signature ? pem : null, abort.signal))
    } catch (cause) {
      setError(abort.signal.aborted ? abort.signal.reason === 'timeout' ? 'La operación superó dos minutos. Comprueba el backend y vuelve a intentar.' : 'Solicitud cancelada. No se descargó ningún resultado.' : cause instanceof TypeError ? 'No hay conexión con el backend. Inicia Spring Boot en el puerto 8080 y vuelve a intentar.' : cause instanceof Error ? cause.message : 'No se pudo procesar el archivo.')
    } finally { clearTimeout(timeout); setBusy(false); controller.current = null }
  }
  const action = sending ? cipher && signature ? 'Cifrar y firmar archivo' : cipher ? 'Cifrar archivo' : 'Firmar archivo' : cipher && signature ? 'Descifrar y verificar' : cipher ? 'Descifrar archivo' : 'Verificar firma'

  return <div className="app-shell">
    <aside className="sidebar">
      <a className="brand" href="#" onClick={event => { event.preventDefault(); if (!busy) setPage('transfer') }} aria-label="Nexo, inicio"><span className="brand-mark"><i /><i /></span><span>Nexo<span className="brand-caption">Criptografía híbrida</span></span></a>
      <div className="workspace-label">Tu espacio de trabajo</div>
      <nav aria-label="Navegación principal">
        <button className={page === 'transfer' ? 'nav-item active' : 'nav-item'} disabled={busy} onClick={() => setPage('transfer')}><Icon name="transfer" />Transferir archivos<Icon name="chevron" size={15} /></button>
        <button className={page === 'keys' ? 'nav-item active' : 'nav-item'} disabled={busy} onClick={() => setPage('keys')}><Icon name="key" />Mis llaves</button>
        <button className={page === 'guide' ? 'nav-item active' : 'nav-item'} disabled={busy} onClick={() => setPage('guide')}><Icon name="help" />Cómo funciona</button>
      </nav>
      <div className="sidebar-bottom"><div className="sidebar-note"><span className="mini-lock"><Icon name="lock" size={18} /></span><h3>La privacidad empieza<br />con un buen intercambio.</h3><p>Comparte tus llaves públicas.<br />Conserva las privadas.</p><button className="text-button" disabled={busy} onClick={() => setPage('guide')}>Conocer el proceso <Icon name="arrow" size={15} /></button></div><div className="session"><span className="avatar">N</span><span>Sesión de trabajo<small>Sin historial guardado</small></span></div></div>
    </aside>
    <div className="main-shell">
      <header className="topbar"><div className="breadcrumb">Espacio personal <Icon name="chevron" size={14} /><strong>{page === 'keys' ? 'Mis llaves' : page === 'guide' ? 'Cómo funciona' : 'Transferir archivos'}</strong></div><button className="help-button" disabled={busy} onClick={() => setPage('guide')}><Icon name="help" size={17} /><span>Guía rápida</span></button></header>
      <main>
        <div hidden={page !== 'keys'}><KeyManager onUseDh={keys => { setReceiveDh(keys); setReceiveDhFile(new File([JSON.stringify(keys)], 'respaldo-privado-dh.json')); setResult(null) }} onUseRsa={keys => { setSendPem(keys.privateKey); setReceivePem(keys.publicKey); setResult(null) }} /></div>
        {page === 'guide' && <section className="guide-page"><div className="page-heading"><div><h1>De un archivo a un intercambio seguro.</h1><p>Dos personas. Llaves distintas. Un archivo que llega intacto.</p></div></div><div className="guide-flow">{[
          ['Prepara la recepción', 'La persona que recibirá el archivo crea sus llaves de intercambio en Mis llaves. Envía el JSON público al remitente y guarda el respaldo privado.'],
          ['Protege y comparte', 'El remitente carga el archivo y la llave pública de intercambio del destinatario. Para firmar, carga su propia llave privada PEM. Después comparte el paquete JSON y su llave pública de firma.'],
          ['Recupera y verifica', 'El destinatario abre Recibir paquete, carga el JSON, su respaldo privado DH y la llave pública PEM del remitente. Si la firma no coincide, el servicio no entrega el archivo.'],
        ].map(([title, text], index) => <article key={title}><span className="step-number">{index + 1}</span><div><h2>{title}</h2><p>{text}</p></div></article>)}</div><div className="guide-details"><h2>Elige lo que necesitas</h2><p><strong>Cifrar</strong> protege el contenido con AES-CBC. <strong>Firmar</strong> permite comprobar sus bytes con RSA/SHA-256. Puedes usar uno o ambos; descifrar sin verificar no confirma la integridad.</p><p>El procesamiento de archivos ocurre en el backend. Las llaves PEM que cargas se envían al servicio para la operación; utiliza HTTPS fuera de localhost. Los nombres de archivo y remitente son metadatos, no están cubiertos por la firma.</p></div><button className="primary-button" onClick={() => setPage('transfer')}>Preparar un archivo <Icon name="arrow" size={18} /></button></section>}
        {page === 'transfer' && <>
          <div className="page-heading"><div><h1>Protege lo que compartes.</h1><p>Cifra tus archivos, firma su origen y compártelos con confianza.</p></div><span className="protocol-tag"><Icon name="shield" size={17} /> Esquema híbrido</span></div>
          <div className="transfer-layout"><section className="transfer-workspace" aria-label="Preparar transferencia">
            <div className="direction-tabs" role="group" aria-label="Dirección de transferencia"><button aria-pressed={sending} className={sending ? 'selected' : ''} disabled={busy} onClick={() => switchDirection('send')}><Icon name="upload" size={18} />Enviar archivo</button><button aria-pressed={!sending} className={!sending ? 'selected' : ''} disabled={busy} onClick={() => switchDirection('receive')}><Icon name="download" size={18} />Recibir paquete</button></div>
            <form onSubmit={submit}>
              <fieldset disabled={busy}>
                <section className="form-section"><div className="section-heading"><span className={`step-number ${file ? 'complete' : ''}`}>{file ? <Icon name="check" size={15} /> : '1'}</span><div><h2>{sending ? 'Elige tu archivo' : 'Carga el paquete recibido'}</h2><p>{sending ? 'Cualquier formato. Los mismos bytes, bajo protección.' : 'Selecciona el JSON que compartió el remitente.'}</p></div><span className="limit-label">Máx. {sending ? '10' : '16'} MB</span></div>
                <FilePicker file={file} onChange={chooseFile} label={sending ? 'Arrastra tu archivo aquí' : 'Arrastra tu paquete JSON aquí'} hint="o selecciónalo desde tu equipo" accept={sending ? undefined : '.json,application/json'} disabled={busy} />
                <div className="file-support"><span>Documentos</span><span>Imágenes</span><span>Audio y video</span><span>Y cualquier otro archivo</span></div>
                </section>
                <section className="form-section"><div className="section-heading"><span className="step-number">2</span><div><h2>{sending ? 'Elige cómo protegerlo' : 'Elige cómo recuperarlo'}</h2><p>Puedes usar una operación o combinar ambas.</p></div></div>
                  <div className="operation-options"><label className={`operation ${cipher ? 'chosen' : ''}`}><input type="checkbox" checked={cipher} onChange={event => { setCipher(event.target.checked); setResult(null) }} /><Icon name="lock" size={23} /><strong>{sending ? 'Cifrar archivo' : 'Descifrar archivo'}</strong><span>{sending ? 'Solo el destinatario podrá abrirlo.' : 'Recupera los bytes del archivo original.'}</span><small>AES-256 · CBC</small></label><label className={`operation ${signature ? 'chosen' : ''}`}><input type="checkbox" checked={signature} onChange={event => { setSignature(event.target.checked); setResult(null) }} /><Icon name="sign" size={23} /><strong>{sending ? 'Firmar archivo' : 'Verificar firma'}</strong><span>{sending ? 'Permite verificar que no fue alterado.' : 'Comprueba que el contenido no cambió.'}</span><small>RSA · SHA-256</small></label></div>
                  {!signature && <p className="inline-warning"><Icon name="info" size={16} />El cifrado por sí solo no comprueba la integridad.</p>}
                </section>
                <section className="form-section keys-section"><div className="section-heading"><span className={`step-number ${keysReady ? 'complete' : ''}`}>{keysReady ? <Icon name="check" size={15} /> : '3'}</span><div><h2>Prepara tus llaves</h2><p>{sending ? 'Las del destinatario para cifrar. Las tuyas para firmar.' : 'Tu respaldo para descifrar. La pública del remitente para verificar.'}</p></div><button type="button" className="text-button" onClick={() => setPage('keys')}>Mis llaves <Icon name="chevron" size={14} /></button></div>
                  {sending && <label className="text-field">Nombre del remitente<input value={sender} onChange={event => { setSender(event.target.value); setResult(null) }} placeholder="¿Quién envía este archivo?" maxLength={120} autoComplete="name" /></label>}
                  {cipher && <div className="key-input"><label>{sending ? 'Intercambio público del destinatario' : 'Tu respaldo privado de intercambio'}<span>JSON</span></label><FilePicker compact file={sending ? sendDhFile : receiveDhFile} onChange={setDhFile} label={sending ? 'Cargar llave de intercambio' : 'Cargar respaldo privado DH'} hint={sending ? 'Archivo .json que te compartió el destinatario' : 'Archivo .json que guardaste al crear las llaves'} accept=".json,application/json" disabled={busy} />{loadingKey && <span className="reading-key" role="status"><ThinkingOrb state="searching" size={20} theme="light" />Leyendo parámetros…</span>}</div>}
                  {signature && <div className="key-input"><label>{sending ? 'Tu llave privada de firma' : 'Llave pública de firma del remitente'}<span>PEM</span></label><FilePicker compact file={pem} onChange={setPem} label={sending ? 'Cargar llave privada' : 'Cargar llave pública'} hint={sending ? 'Formato PKCS#8 · archivo .pem' : 'Formato X.509 · archivo .pem'} accept=".pem,.key" disabled={busy} /></div>}
                </section>
              </fieldset>
              <div className="form-actions">{error && <div className="error-message" role="alert"><Icon name="info" size={19} /><span>{error}</span></div>}<button className="primary-button submit-button" type="submit" disabled={busy || loadingKey || !file || (!cipher && !signature)}>{busy ? <><ThinkingOrb state={sending ? 'weaving' : 'solving'} size={20} theme="dark" />Procesando archivo…</> : <>{action}<Icon name="arrow" size={19} /></>}</button><p className="action-note"><Icon name="lock" size={13} />{sending ? 'El resultado se descarga como un paquete JSON.' : 'La descarga conserva el formato del archivo original.'}</p></div>
            </form>
          </section>
          <aside className="transfer-summary">
            <div className="envelope-scene" aria-hidden="true"><span className="orbit orbit-one" /><span className="orbit orbit-two" /><span className="orbit-point" /><div className="floating-card"><span /><span /><span /></div><div className="envelope"><div className="envelope-fold" /><div className="envelope-seal"><Icon name="lock" size={25} /></div></div><span className="scene-tag"><Icon name="shield" size={13} />Contenido protegido</span></div>
            <div className="summary-body"><h2>{sending ? 'Un intercambio, bien protegido.' : 'Tu archivo, de vuelta.'}</h2><p>{sending ? 'Cada archivo tiene su destino. Tú eliges cómo proteger el camino.' : 'Recupera el contenido y comprueba su integridad antes de guardarlo.'}</p><div className="summary-list"><div><Icon name="file" size={17} /><span>Archivo</span><strong title={file?.name}>{file ? sizeLabel(file.size) : 'Sin seleccionar'}</strong></div><div><Icon name="lock" size={17} /><span>{sending ? 'Cifrado' : 'Descifrado'}</span><strong>{cipher ? 'AES-256' : 'Desactivado'}</strong></div><div><Icon name="sign" size={17} /><span>{sending ? 'Firma digital' : 'Verificación'}</span><strong>{signature ? 'RSA + SHA-256' : 'Desactivada'}</strong></div><div><Icon name="key" size={17} /><span>Llaves</span><strong className={keysReady ? 'ready-text' : ''}>{keysReady ? 'Preparadas' : 'Por cargar'}</strong></div></div>
            {busy ? <div className="result-panel working" role="status" aria-live="polite"><ThinkingOrb state={sending ? 'weaving' : 'solving'} size={64} theme="light" /><h3>{sending ? 'Protegiendo tu archivo' : 'Recuperando tu archivo'}</h3><p>Esperando el resultado del servicio.</p><button type="button" className="text-button" onClick={() => controller.current?.abort()}>Cancelar solicitud</button></div> : result ? <div className="result-panel success" role="status"><span className="result-check"><Icon name="check" size={22} /></span><h3>{result.direction === 'send' ? 'Tu paquete está listo' : result.integrity === 'VERIFIED' ? 'Firma verificada' : 'Archivo recuperado'}</h3><p>{result.direction === 'send' ? 'Descárgalo y compártelo con el destinatario.' : result.integrity === 'VERIFIED' ? 'Los bytes coinciden con la firma del remitente.' : 'No se solicitó verificar la integridad.'}</p><button className="download-button" onClick={() => download(result.blob, result.filename)}><Icon name="download" size={17} />{result.direction === 'send' ? 'Descargar paquete' : 'Descargar archivo'}</button></div> : <div className="result-placeholder"><span className="result-dot" /><p>Tu resultado aparecerá aquí<br />cuando completes el proceso.</p></div>}
            </div>
            <div className="summary-tip"><Icon name="info" size={18} /><p>{sending ? '¿Es tu primer intercambio? Pide al destinatario su llave pública antes de empezar.' : 'Usa las mismas llaves de intercambio con las que se preparó el envío.'}<button className="text-button" disabled={busy} onClick={() => setPage('guide')}>Ver cómo funciona</button></p></div>
          </aside></div>
          <footer className="workspace-footer"><span>Nexo · Criptografía aplicada</span><span>Diseñado para conservar cada byte.</span></footer>
        </>}
      </main>
    </div>
  </div>
}

export default App
