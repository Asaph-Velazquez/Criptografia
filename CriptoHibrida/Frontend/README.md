# Nexo · Frontend React + Vite

Interfaz en español para el backend de criptografía híbrida. Incluye envío y recepción, generación local de llaves RSA/DH, carga de llaves existentes y Thinking Orb durante el procesamiento. Las llaves y los resultados se mantienen en memoria; recargar o cerrar la página termina la sesión.

## Ejecutar en desarrollo

Desde `Frontend`:

```sh
npm ci
npm run dev
```

Abre http://localhost:5173. Vite reenvía `/api` al backend en `http://127.0.0.1:8080`. Si el puerto 5173 está ocupado por este proyecto, usa el servidor existente.

Desde la raíz del backend, en otra terminal:

```sh
./mvnw spring-boot:run -Djava.version=23
```

En PowerShell también puedes usar `./mvnw.cmd`. El POM está configurado para Java 25; el argumento permite trabajar con el JDK 23 disponible en este entorno. Con JDK 25 puedes omitirlo.

## Primer intercambio

1. El destinatario crea llaves de intercambio en **Mis llaves**, descarga ambos JSON y comparte únicamente `intercambio-publico.json`.
2. El remitente prepara sus llaves de firma RSA y comparte `firma-publica.pem` con el destinatario. También se aceptan llaves PEM existentes: privada PKCS#8 y pública X.509.
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

```sh
npm run lint
npm run build
npm test
```

Con Vite y Spring Boot activos:

```sh
npm run test:integration
```

La prueba de integración usa el mismo cliente multipart de la interfaz. Comprueba los tres modos, igualdad exacta de bytes, nombres de descarga y rechazo de firmas alteradas. Las pruebas no usan llaves ni archivos personales.

## Publicación

`npm run build` genera `dist`. Sirve esa carpeta con una ruta `/api` hacia Spring Boot o define `VITE_API_BASE_URL` al compilar. Si frontend y backend están en orígenes distintos, configura `crypto.cors.allowed-origins` en Spring Boot; sus valores locales predeterminados incluyen los puertos 5173 y 3000. Las APIs de generación de llaves requieren un contexto seguro (HTTPS o localhost).

Thinking Orb respeta `prefers-reduced-motion`. Las cargas admiten selección con teclado y arrastrar archivos. El diseño se adapta a escritorio y móvil.
