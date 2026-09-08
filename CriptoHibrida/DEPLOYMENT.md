# Nexo en Docker y EC2

La imagen contiene la interfaz React compilada y la API Spring Boot con Java 21. Spring Boot sirve ambos en el puerto 8080, por lo que el navegador usa `/api` en el mismo origen. No se necesita ejecutar Vite en producción ni configurar `VITE_API_BASE_URL`.

Validación del 7 de septiembre de 2026: lint y build del frontend, 6 pruebas de Node, 67 pruebas Maven, revisión visual en escritorio y móvil, y transferencia binaria con los tres modos contra Spring Boot sirviendo la interfaz compilada. También se verificó el intercambio con cabeceras de proxy HTTPS. `docker build --check .` y `docker compose config --quiet` pasaron. La construcción completa de la imagen y su ejecución en contenedor siguen pendientes: se interrumpieron las descargas lentas de imágenes base y plugins Maven. No se ha publicado una imagen ni creado recursos AWS.

## Construir y probar localmente

Inicia Docker Desktop en modo Linux y ejecuta desde la raíz del proyecto:

```sh
docker build -t nexo:local .
docker compose up -d --no-build
docker compose ps
docker compose logs --tail=100 nexo
```

Abre [Nexo local](http://localhost:8080). El Dockerfile ejecuta lint, pruebas y build de React, además de las pruebas y el empaquetado Maven. Solo copia el JAR final a la imagen de ejecución; no incluye Node ni Maven. El contenedor usa un usuario sin privilegios y comprueba la respuesta de la página de inicio cada 30 segundos. El primer arranque puede tardar un minuto.

Compose limita la memoria a 1 GiB, monta `/tmp` en memoria para las cargas multipart, configura rotación de logs y reinicia el servicio tras reiniciar el host. Por defecto publica solo en la interfaz local. Si 8080 está ocupado, establece `NEXO_PORT=8081` en el entorno antes de ejecutar Compose.

Para comprobar el intercambio completo contra el contenedor, desde `Frontend`:

```powershell
$env:TEST_BASE_URL = 'http://localhost:8080'
npm run test:integration
```

En Bash: `TEST_BASE_URL=http://localhost:8080 npm run test:integration`.

Para detenerlo: `docker compose down`.

## Publicar en Docker Hub

Reemplaza `TU_USUARIO` por tu cuenta y crea el repositorio `nexo` en Docker Hub. Autentícate de forma interactiva; no pongas contraseñas en el Dockerfile.

```sh
docker login
docker tag nexo:local TU_USUARIO/nexo:1.0.0
docker push TU_USUARIO/nexo:1.0.0
```

La arquitectura de la imagen debe coincidir con EC2. Para una instancia x86_64 puedes construir expresamente con `docker build --platform linux/amd64 -t TU_USUARIO/nexo:1.0.0 .`. Si vas a usar tanto x86_64 como Graviton (ARM64), publica una imagen para ambas arquitecturas:

```sh
docker buildx build --platform linux/amd64,linux/arm64 -t TU_USUARIO/nexo:1.0.0 --push .
```

No es necesario ejecutar ambas formas de publicación. Utiliza una etiqueta nueva en cada versión para poder regresar a una anterior.

## Ejecutar en EC2 con Amazon Linux 2023

Prepara una instancia de la arquitectura elegida con al menos 2 GiB de RAM para dejar espacio al sistema y al contenedor. Construye la imagen en tu equipo; EC2 solo la descarga y ejecuta. Accede por SSH y ejecuta:

```sh
sudo dnf install -y docker
sudo systemctl enable --now docker
sudo docker pull TU_USUARIO/nexo:1.0.0
sudo docker run -d --name nexo --restart unless-stopped --init \
  --memory=1g --read-only --tmpfs /tmp:rw,size=256m,mode=1777 \
  --cap-drop=ALL --security-opt=no-new-privileges:true \
  --log-opt max-size=10m --log-opt max-file=3 \
  -p 8080:8080 TU_USUARIO/nexo:1.0.0
sudo docker inspect --format='{{.State.Health.Status}}' nexo
curl -I http://127.0.0.1:8080/
```

Para un repositorio privado, ejecuta primero `sudo docker login`. No necesitas Compose en EC2 para esta opción.

### HTTPS y acceso público

La generación de llaves RSA usa Web Crypto: funciona en `localhost` durante desarrollo y requiere HTTPS para acceder desde Internet. Configura un Application Load Balancer (ALB):

1. Crea un certificado ACM para tu dominio y valida su propiedad.
2. Crea un target group HTTP en el puerto 8080, registra la instancia y configura el health check `GET /`, con código esperado 200.
3. Configura el listener HTTPS 443 del ALB con el certificado y el target group. En HTTP 80, configura redirección a HTTPS.
4. Permite 443 y 80 en el security group del ALB. En el de EC2 permite 8080 **solo desde el security group del ALB**, y restringe SSH a tu IP. No abras 8080 al público.
5. Apunta tu dominio al ALB. Ajusta su idle timeout a 180 segundos para permitir las operaciones de hasta dos minutos de la interfaz.
6. Accede a `https://tu-dominio` y verifica transferencia, generación de llaves y descarga. La imagen configura `SERVER_FORWARD_HEADERS_STRATEGY=framework` para reconocer el HTTPS original que comunica el ALB; así evita rechazos CORS al terminar TLS en el balanceador. No se necesita añadir el dominio a CORS al servir interfaz y API desde el mismo origen. Por esa confianza en las cabeceras del proxy, conserva la restricción de acceso al puerto 8080 únicamente desde el ALB.

El health check comprueba que la aplicación sirve la interfaz; el test de integración comprueba el procesamiento criptográfico. El despliegue requiere tu cuenta Docker Hub, una instancia EC2, dominio y certificado; los archivos de este repositorio no crean ni publican esos recursos automáticamente.

### Actualizar o regresar de versión

Descarga primero la nueva etiqueta. Después detén y elimina únicamente el contenedor `nexo`, y repite `docker run` con la nueva etiqueta:

```sh
sudo docker pull TU_USUARIO/nexo:1.0.1
sudo docker stop --time 30 nexo
sudo docker rm nexo
```

Este cambio tiene una breve interrupción. Para regresar, usa la etiqueta anterior con el mismo comando `docker run`. El servicio no guarda archivos ni llaves como historial; los usuarios deben descargar los resultados y respaldos antes de cerrar su sesión.

## Referencias

- [Docker: compilación por etapas](https://docs.docker.com/build/building/multi-stage/).
- [Docker: construir, etiquetar y publicar](https://docs.docker.com/get-started/docker-concepts/building-images/build-tag-and-publish-an-image/).
- [Docker: múltiples arquitecturas](https://docs.docker.com/build/building/multi-platform/).
- [AWS: crear un Application Load Balancer](https://docs.aws.amazon.com/elasticloadbalancing/latest/application/create-application-load-balancer.html).
- [AWS: TLS en Amazon Linux 2023 y terminación en el balanceador](https://docs.aws.amazon.com/linux/al2023/ug/SSL-on-amazon-linux-2023.html).
- [Spring Security: HTTPS y cabeceras de proxy](https://www.springframework.org/security/reference/features/exploits/http.html).
