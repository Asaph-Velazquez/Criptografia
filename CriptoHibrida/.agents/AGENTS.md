# AGENTS.md - Mapa operativo para agentes de IA

Este archivo define el contexto minimo para que un agente trabaje en el backend de **Criptografia Hibrida (Esquema 2)** en Java Spring Boot (`com.cripto.CriptoHibrida`) sin asumir modulos ni herramientas ajenas.

---

## 1. Arranque obligatorio

1. Lee `.agents/feature_list.json` y selecciona una sola tarea `pending` o la indicada por el usuario.
2. Antes de implementar codigo, cambia el `status` de esa feature a `in_progress` en `.agents/feature_list.json`.
3. Confirma la capa y el modulo afectado segun sus rutas:
   - `crypto-service`: Matematicas DH con `BigInteger`, AES-CBC sobre `byte[]` y firma/verificacion RSA/SHA-256.
   - `storage-engine`: Serializacion JSON/Base64 (`EncryptedPackage`) y cliente WebClient para descarga de llaves publicas por URL.
   - `orchestration`: Coordinador de emision y recepcion criptografica ("uno de dos o dos de dos" procesos).
   - `api-controller`: Endpoints REST multipart y manejo de excepciones HTTP.
4. Revisa cambios locales existentes con `git status --short`.

---

## 2. Mapa del repositorio

| Ruta | Rol |
|---|---|
| `pom.xml` | Configuracion Maven, Spring Boot y dependencias criptograficas/web |
| `.agents/feature_list.json` | Backlog estricto y estado de features |
| `mvnw` / `mvnw.cmd` | Maven Wrapper para compilacion y ejecucion de tests |
| `src/main/java/com/cripto/CriptoHibrida/crypto/service/` | Servicios criptograficos nucleares (DH, AES, RSA) |
| `src/main/java/com/cripto/CriptoHibrida/crypto/model/` | Modelos DTOs, parametros DH y resultados de verificacion |
| `src/main/java/com/cripto/CriptoHibrida/crypto/controller/` | Controladores REST para procesamiento y descarga de archivos |
| `src/main/java/com/cripto/CriptoHibrida/crypto/util/` | Utilerias para formato PEM y codificacion Base64 |
| `src/test/java/com/cripto/CriptoHibrida/` | Pruebas unitarias de JUnit para cada servicio |

---

## 3. Reglas duras

- **Una sola tarea a la vez:** Prohibido modificar modulos fuera de los `allowed_paths` de la feature activa.
- **Manejo binario estricto:** Nunca conviertas archivos multimedia (.mp3, .mp4, etc.) a `String` o texto UTF-8 antes del cifrado o hashing; opera siempre sobre arreglos `byte[]`.
- **Fidelidad al Esquema 2:**
  - $K_{AES}$ y el $IV$ **no** se cifran con RSA; se derivan exclusivamente del intercambio de claves Diffie-Hellman ($g, n, K_a, K_b, K_c, K_d$).
  - RSA se usa exclusivamente para la **Firma Digital** sobre el hash SHA-256 del archivo original.
- **Validacion de Integridad:** Si el archivo o la firma es manipulado (prueba de corrupcion), el validador debe fallar explicitamente y retornar `false` o lanzar excepcion; jamas arrojar un falso positivo.
- **Verificacion obligatoria:** Toda feature completada debe validarse con su comando Maven asignado (`./mvnw test -Dtest=...`).
- **Limpieza de codigo:** No dejes `System.out.println`, logs de debug ruidosos, secretos quemados ni archivos temporales.
- **Gestion de estado:** Usa `in_progress` al comenzar, `done` solo tras verificar con Maven, y `blocked` si hay un impedimento real documentado.

---

## 4. Convenciones tecnicas y comandos

### Stack

- Java 25 (o compatible con el entorno configurado en `pom.xml`).
- Spring Boot 4.x / Spring WebMvc / Spring WebFlux (WebClient).
- APIs nativas `java.security.*` y `javax.crypto.*`.

### Comandos de verificacion

```bash
# Compilacion sin empaquetar
./mvnw clean compile

# Ejecucion de prueba unitaria especifica
./mvnw test -Dtest=DiffieHellmanServiceTest

# Suite completa de tests
./mvnw test

# Arranque local para validacion de humo
./mvnw spring-boot:run