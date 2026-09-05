# Skill: Commit standard for CriptoHibrida (Spring Boot)

## Format

type(scope): short description in English

Use Conventional Commits style.

## Allowed types

| Type | Use |
|---|---|
| feat | New functionality or cryptographic capability |
| fix | Bug fix or handling of cryptographic edge cases |
| refactor | Internal restructuring without intended behavior change |
| docs | Documentation, slides, or README updates |
| test | Unit tests added or corrected |
| chore | Build scripts, dependencies, Maven configs, maintenance |
| style | Formatting or visual-only changes without logic changes |
| perf | Performance improvement |
| build | Maven build pipeline, POM updates, or packaging changes |
| ci | CI workflow changes |

## Suggested scopes

| Scope | Use |
|---|---|
| dh | Diffie-Hellman key agreement and parameter derivation |
| aes | AES-CBC symmetric cipher and PKCS padding over binary streams |
| rsa | RSA key generation, PEM parsing, and digital signature routines |
| package | JSON container serialization, deserialization, and Base64 handling |
| net | HTTP/HTTPS client for remote public key fetching |
| orchestrator | Hybrid crypto orchestration ("one of two or two of two" flows) |
| api | Spring Boot REST controllers, DTOs, and multipart endpoints |
| agents | AI agent workflows, backlog, and prompt rules (.agents/) |
| build | Root POM (pom.xml), Maven Wrapper, or workspace configuration |

## Rules

1. Write the commit message in English.
2. Keep one logical change per commit.
3. Describe what changed, not how you changed it.
4. Do not end the subject with a period.
5. Use the dominant cryptographic or architectural scope when one layer clearly owns the change.
6. Use orchestrator or build for cross-cutting integration work.

## Examples

feat(dh): implement modular exponentiation and key derivation
feat(aes): add pkcs5 padding support for raw byte streams
feat(rsa): add sha256 digital signature and pem parser
feat(package): serialize hybrid payload to json container
feat(net): implement webclient for remote public key fetching
feat(api): expose multipart encryption and verification endpoints
test(rsa): add integrity failure test case for corrupted signatures
fix(dh): resolve 16-byte truncation for cbc initialization vector
docs(agents): update backlog status for aes byte stream service
chore(build): bump bouncycastle and jackson dependencies