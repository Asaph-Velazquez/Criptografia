# Práctica 2 — Función Criptográfica

**Equipo:** 3 personas  
**Objetivo:** Implementar una función criptográfica asimétrica (**RSA**) capaz de **cifrar y descifrar archivos completos** (por ejemplo `.txt`, `.bmp`, etc.).

---

## Flujo General del Sistema

```text
 ____________________      ____________________      ____________________      ____________________      ____________________
|                    |    |                    |    |                    |    |                    |    |                   |
|       P.txt        | -> |       Encrypt      | -> |       p-e.txt      | -> |       Decrypt      | -> |     p-e-d.txt     |
|   (Archivo Base)   |    |       (Llave)      |    |     (Encrypted)    |    |       (Llave)      |    |    (Decrypted)    |
|____________________|    |____________________|    |____________________|    |____________________|    |___________________|
```

El sistema cuenta con una **interfaz gráfica (GUI) responsiva** que permite:

* Generar un par de **llaves criptográficas RSA**
* Seleccionar el **archivo a procesar** y la **llave correspondiente**
* Elegir la operación a realizar mediante un menú sencillo (**Cifrar / Descifrar**)

---

# Librerías utilizadas en Java

## Lógica Criptográfica

Se utilizan las siguientes librerías para implementar RSA:

* `javax.crypto.*`  
  Inicialización de cifradores mediante la clase `Cipher`.

* `javax.security.*`  
  Generación y manejo del `KeyPair` para RSA.

* `java.security.spec.*`  
  Manejo de codificación de llaves en formato **PKCS8** y **X509**.

---

## Interfaz Gráfica (GUI)

Para la interfaz se utilizan componentes de **Swing** y herramientas de layout:

* `javax.swing.*`  
  Componentes visuales como:

  * `JFrame`
  * `JButton`
  * `JLabel`
  * `JFileChooser`

* `java.awt.*`  
  Manejo de layouts y estilos visuales:

  * `GridBagLayout`
  * `BoxLayout`
  * manejo de colores y fuentes

* `java.awt.event.*`  
  Manejo de eventos de interfaz como:

  * clicks en botones
  * redimensionamiento dinámico de la ventana

---

# Estado del Proyecto

## Backend — Lógica de Cifrado y Descifrado

✔ **Generación de llaves**

Se genera un par de llaves RSA de **2048 bits** utilizando `KeyPairGenerator`.

---

✔ **Almacenamiento seguro**

Las llaves se guardan en un archivo `.key` binario que contiene:

* llave privada en formato **PKCS8**
* llave pública en formato **X509**

---

✔ **Persistencia**

El sistema permite **cargar llaves desde un archivo `.key` existente**, lo que permite reutilizarlas en futuras ejecuciones.

---

✔ **Cifrado por bloques**

Los datos se cifran utilizando la llave pública en bloques de:

```
245 bytes
```

con el algoritmo:

```
RSA/ECB/PKCS1Padding
```

---

✔ **Descifrado por bloques**

Los datos cifrados se descifran con la llave privada en bloques de:

```
256 bytes
```

---

✔ **Soporte multiformato**

El sistema trabaja **directamente a nivel de bytes**, por lo que puede procesar cualquier tipo de archivo, por ejemplo:

* `.txt`
* `.bmp`
* `.pdf`
* `.png`
* etc.

---

# Interfaz Gráfica (UX / UI)

✔ **Diseño Responsivo**

Se utiliza `GridBagLayout` para mantener la tarjeta principal centrada aunque la ventana cambie de tamaño, simulando un comportamiento similar a **Flexbox**.

---

✔ **Escalado dinámico**

Se implementa un `ComponentListener` que calcula proporciones para **aumentar o reducir el tamaño del texto automáticamente** según el tamaño de la ventana.

---

✔ **Paleta de colores tipo Dark Mode**

Se utilizan colores inspirados en **Tailwind CSS**:

* `Slate-900`
* `Slate-800`
* `Emerald-500`
* `Blue-500`

Esto da una apariencia moderna a la aplicación.

---

✔ **Feedback en tiempo real**

En la parte inferior de la interfaz se encuentra un `statusLabel` que informa al usuario:

* qué archivo se cargó
* si ocurrió algún error
* estado actual del sistema

Esto evita saturar la interfaz con ventanas emergentes.

---

✔ **Flujo inteligente**

Los botones:

* **Subir Llave**
* **Subir Archivo**
* **Ejecutar**

permanecen **ocultos hasta que el usuario selecciona el modo de operación** (Cifrar o Descifrar).

---

✔ **Nombrado automático de archivos**

Al guardar el resultado, el sistema detecta la extensión original y genera automáticamente un nombre de salida.

Ejemplo:

```
foto.bmp → foto_cifrado.bmp
```

---

# Flujo de Trabajo de la GUI

```text
 ____________________      ____________________      ____________________      ____________________
|                    |    |                    |    |                    |    |                    |
| 1. Generar Llaves  | -> | 2. Seleccionar Modo| -> | 3. Cargar Archivos | -> | 4. Ejecutar Acción |
|    (Archivo .key)  |    | (Cifrar/Descifrar) |    |   (Llave + Doc)    |    | (Guardar y Avisar) |
|____________________|    |____________________|    |____________________|    |____________________|
```

---

### 1. Preparación

El botón **"Generar Nuevas Llaves RSA"** abre un `JFileChooser` para guardar el par de llaves.

Ejemplo:

```
mi_llave_secreta.key
```

---

### 2. Configuración

El usuario selecciona el modo de operación mediante un `RadioButton`:

* **Cifrar Archivo**
* **Descifrar Archivo**

Esto revela dinámicamente los botones necesarios.

---

### 3. Carga de Datos

El usuario carga:

1. La llave (`.key`)
2. El archivo objetivo (`.txt`, `.bmp`, etc.)

---

### 4. Procesamiento

El botón **Ejecutar**:

* lee el archivo en memoria
* procesa los bloques con RSA
* abre un diálogo para guardar el resultado

El sistema maneja errores utilizando bloques `try-catch` y los muestra en la interfaz.

---

# Estructura del Proyecto

```
P2 -- Funcion Criptografica/
└── encrypt_library/
    ├── pom.xml
    └── src/
        └── main/
            ├── java/
            │   └── encrypt/
            │       ├── App.java
            │       │   Punto de entrada que lanza la GUI
            │
            │       ├── Encrypt.java
            │       │   Lógica RSA: generación, carga de llaves y cifrado/descifrado
            │
            │       ├── FileData.java
            │       │   Manejo de lectura y escritura de archivos a nivel de bytes
            │
            │       └── GUI.java
            │           Interfaz gráfica adaptativa con Swing
            │
            └── resources/
                ├── BaseTXT/
                │   └── p.txt
                │       Archivo de prueba
                │
                ├── EncryptTXT/
                │       Carpeta sugerida para archivos cifrados
                │
                ├── DecryptTXT/
                │       Carpeta sugerida para archivos descifrados
                │
                └── keys/
                        Carpeta sugerida para almacenar llaves .key
```

---

# Cómo Ejecutar el Proyecto

## Requisitos Previos

Instalar:

* **Java 21+**
* **Maven 3.6+**

---

# Compilación

Desde la carpeta del proyecto ejecutar:

```bash
cd encrypt_library
mvn clean compile
```

---

# Ejecución

Para iniciar la aplicación y abrir la interfaz gráfica:

```bash
mvn exec:java -Dexec.mainClass="encrypt.App"
```

---

💡 **Nota:**  
También se puede ejecutar directamente desde el IDE (por ejemplo **NetBeans o IntelliJ**) usando el botón **Run** en el archivo:

```
App.java
```