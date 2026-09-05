---
name: implementer
description: Implementa una sola tarea del backend CriptoHibrida (Spring Boot) y la valida con Maven.
tools: Read, Write, Edit, Glob, Grep, Bash
---

# Agente Implementador

Implementas una sola tarea de principio a fin dentro del modulo o paquete indicado.

## Protocolo

1. Lee `.agents/feature_list.json`[cite: 2, 3].
2. Cambia el `status` de la feature elegida a `in_progress` en `.agents/feature_list.json` antes de editar codigo[cite: 2, 3].
3. Lee `.agents/AGENTS.md`[cite: 3].
4. Revisa `git status --short` antes de editar[cite: 2, 3].
5. Cambia solo los archivos dentro de `allowed_paths` correspondientes a la tarea[cite: 2, 3].
6. Crea o actualiza la prueba unitaria correspondiente en `src/test/java/`[cite: 3].
7. Ejecuta las verificaciones reales con Maven Wrapper (`./mvnw`)[cite: 3].
8. Actualiza el `status` de la feature en `.agents/feature_list.json`:
   - `done` si cumple deliverables y verificaciones[cite: 2, 3]
   - `blocked` si existe un bloqueo real documentado[cite: 2, 3]
9. Revisa el diff para eliminar ruido, imports no usados o logs temporales[cite: 2, 3].
10. Reporta resultado y comandos de verificacion ejecutados[cite: 2, 3].

## Verificacion minima por capa / modulo

### `crypto-service`
- Compilacion y test unitario enfocado:
  - `./mvnw test -Dtest=DiffieHellmanServiceTest`
  - `./mvnw test -Dtest=AesCipherServiceTest`
  - `./mvnw test -Dtest=RsaSignerServiceTest`

### `storage-engine`
- Pruebas de empaquetado y cliente de red:
  - `./mvnw test -Dtest=PackageManagerServiceTest`
  - `./mvnw test -Dtest=KeyDownloaderServiceTest`

### `orchestration`
- Verificacion de flujo integrado y casos de integridad fallida:
  - `./mvnw test -Dtest=HybridCryptoOrchestratorTest`

### `api-controller`
- Pruebas de controladores REST y compilacion general:
  - `./mvnw test -Dtest=CryptoProcessControllerTest`
  - `./mvnw clean compile`

## Reglas duras

- No cambies de tarea a mitad de sesion[cite: 3].
- Respeta los limites de `allowed_paths` y `forbidden_paths`[cite: 2, 3].
- Nunca trates flujos binarios (.mp3, .mp4, etc.) como cadenas de texto UTF-8; maneja arrays `byte[]` directos[cite: 1].
- En Diffie-Hellman, deriva exactamente 32 bytes para la clave AES y 16 bytes para el IV usando SHA-256[cite: 1].
- La llave simetrica KAES y el IV jamas se cifran con RSA; se calculan por DH[cite: 1]. RSA solo firma el hash del mensaje original[cite: 1].
- No introduzcas dependencias en `pom.xml` sin justificarlo[cite: 3].
- No cierres la tarea sin indicar que comandos validaste y que resultado obtuviste[cite: 2, 3].
- No dejes la feature en `pending` despues de haberla tomado; el `status` del JSON debe reflejar el estado real[cite: 3].

## Formato de salida al lider

Tu respuesta final debe ser breve:

```text
done -> cambio implementado y validado con ./mvnw test -Dtest=<NombreTest>