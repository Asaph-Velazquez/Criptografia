# Nexo · Frontend React + Vite

Interfaz en español para el backend de criptografía híbrida. Incluye envío y recepción, generación de llaves RSA/ECDSA/DH, lectura de PEM existentes y Thinking Orb durante el procesamiento. Las llaves y los resultados se mantienen en memoria; recargar o cerrar la página termina la sesión.

## Ejecutar sin instalaciones locales

Desde la raíz del proyecto, con Docker Desktop activo:

```powershell
.\run.ps1
```

Abre http://localhost:8080. Docker compila React y Java y sirve la interfaz y la API juntas. No necesitas instalar Node.js, npm, Java ni Maven. Consulta [las instrucciones completas](../README.md#ejecución-local).

## Primer intercambio

1. El destinatario crea llaves de intercambio en **Mis llaves**, descarga ambos JSON y comparte únicamente `intercambio-publico.json`.
2. El remitente elige RSA o ECDSA P-256 en **Mis llaves**, genera el par y comparte solo la pública con el destinatario. También se aceptan PEM existentes: privada PKCS#8 sin contraseña y pública X.509 (SPKI). Al cargar una llave se valida y detecta su algoritmo.
3. En **Enviar archivo**, carga el archivo, indica el remitente y elige cifrar, firmar o ambos. Para cifrar carga el JSON público del destinatario; para firmar, su propia privada PEM.
4. Descarga el paquete JSON y compártelo. En **Recibir paquete**, el destinatario carga ese paquete, su respaldo privado DH y la pública PEM del remitente. Selecciona solo las operaciones correspondientes al paquete: firma sin cifrado requiere desactivar **Descifrar archivo**.
5. El resultado permite descargar los bytes originales. Una firma incorrecta bloquea la descarga. Descifrar sin verificar muestra que no se solicitó comprobar la integridad.

**Usar en esta sesión** evita volver a cargar las llaves recién generadas, pero no sustituye descargar y guardar un respaldo. No compartas los archivos privados. Crear un nuevo par no permite recuperar archivos preparados para un par anterior.

El frontend genera las llaves en el navegador con Web Crypto y generación aleatoria segura del navegador (`crypto.getRandomValues`). Las operaciones de archivo ocurren en el backend; las llaves necesarias se envían allí exclusivamente en el cuerpo multipart. Utiliza HTTPS fuera de localhost. La firma cubre los bytes originales, no el nombre del archivo ni los metadatos del remitente.

## Contrato y límites

- `POST /api/crypto/process`: partes `file`, `options` (JSON) y `privateKey` (PEM, si se firma).
- `POST /api/crypto/verify-decrypt`: partes `package`, `options` y `publicKey` (PEM, si se verifica).
- Todos los enteros DH viajan como cadenas decimales para conservar su precisión.
- Archivo de entrada: 10 MiB. Paquete recibido: 16 MiB. PEM: 64 KiB. JSON de llaves: 32 KiB.
- Se informa de errores HTTP 400, 413 y 422, fallos de conexión y esperas superiores a dos minutos. Cancelar detiene la solicitud del navegador; una operación ya recibida puede terminar en el servidor.

## Verificar

```powershell
.\run.ps1 test
```

Ejecuta el comando desde la raíz. Todas las pruebas se ejecutan en Docker. La integración usa el mismo cliente multipart de la interfaz y comprueba RSA y ECDSA, los tres modos, igualdad exacta de bytes, nombres de descarga y rechazo de firmas alteradas y llaves incorrectas. Las pruebas no usan llaves ni archivos personales.

## Publicación

Para empaquetar interfaz y backend en una sola imagen, consulta [Docker y despliegue](../README.md#despliegue).

`npm run build` genera `dist`. Sirve esa carpeta con una ruta `/api` hacia Spring Boot o define `VITE_API_BASE_URL` al compilar. Si frontend y backend están en orígenes distintos, configura `crypto.cors.allowed-origins` en Spring Boot; sus valores locales predeterminados incluyen los puertos 5173 y 3000. Las APIs de generación de llaves requieren un contexto seguro (HTTPS o localhost).

Thinking Orb respeta `prefers-reduced-motion`. Las cargas admiten selección con teclado y arrastrar archivos. El diseño se adapta a escritorio y móvil.
