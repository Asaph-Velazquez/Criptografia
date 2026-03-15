# Practica 2 -- Funcion Criptografica

En equipos de 3 personas  
Implementar cualquier función criptografica con archivos .txt o .bmp

```text
 ____________________      ____________________      ____________________      ____________________     ____________________
|                    |    |                    |    |                    |    |                    |    |                   |
|       P.txt        | -> |        Encypt      | -> |       p-e.txt      | -> |       Decrypt      | -> |     p-e-d.txt     |
|                    |    |       (llave)      |    |     (Encrypted)    |    |       (llave)      |    |    (Decrypted)    |
|____________________|    |____________________|    |____________________|    |____________________|    |___________________|
```

Su programa debe tener una interfaz que:

1. Seleccione archivo y llave  
2. Menú para cifrado/decifrado  

Además se implementó una **interfaz gráfica (GUI)** que permite:

- Generar un par de **llaves RSA**
- Seleccionar **archivo y llave** usando explorador de archivos
- Elegir **Cifrar o Descifrar**
- Guardar el archivo resultado automáticamente

---

## Librerias en Java

* `javax.crypto`
* `javax.security`
* `javax.swing` (para la interfaz gráfica)

---

## Estado del proyecto

### ✅ Lo que ya hace

- Genera un par de llaves RSA de 2048 bits (`KeyPairGenerator`)
- **Guarda las llaves** en un archivo `.key` binario (llave privada en PKCS8 + llave pública en X.509)
- **Carga llaves** desde un archivo `.key` existente para reutilizarlas entre sesiones
- Cifra datos con la llave pública en bloques de **245 bytes** (`RSA/ECB/PKCS1Padding`)
- Descifra datos con la llave privada en bloques de **256 bytes**
- Soporta cualquier tipo de archivo: `.txt`, `.bmp`, etc.

### Interfaz gráfica (GUI)

La GUI fue implementada con **Java Swing** y permite:

- Botón **Generar Llaves** para crear el archivo `.key`
- Botón **Subir Llave** para cargar la llave
- Botón **Subir Archivo** para seleccionar el archivo
- Selección **Cifrar / Descifrar**
- Botón **Ejecutar** para realizar la operación y guardar el resultado

---

## Estructura de archivos

```
P2 -- Funcion Criptografica/
└── encrypt_library/
    ├── pom.xml
    └── src/
        └── main/
            ├── java/
            │   └── encrypt/
            │       ├── App.java          ← punto de entrada, lanza la GUI
            │       ├── Encrypt.java      ← RSA: genera, guarda, carga llaves y cifra/descifra
            │       ├── FileData.java     ← maneja lectura y escritura de archivos
            │       └── GUI.java          ← interfaz gráfica (Swing)
            └── resources/
                ├── BaseTXT/
                │   └── p.txt             ← archivo de ejemplo
                ├── EncryptTXT/           ← carpeta sugerida para resultados cifrados
                ├── DecryptTXT/           ← carpeta sugerida para resultados descifrados
                └── keys/                 ← carpeta sugerida para guardar archivos .key
```

---

## Cómo correrlo

### Requisitos

- Java 21+
- Maven 3.6+

### Compilar

```bash
cd encrypt_library
mvn clean compile
```

### Ejecutar

```bash
mvn exec:java -Dexec.mainClass="encrypt.App"
```

O desde la raíz del workspace con VS Code, usando el botón **Run** en `App.java`.

---

### Resultado esperado

Se abre la ventana de la **GUI**.  
Desde ahí puedes generar un `.key`, seleccionarlo, cargar un archivo y cifrarlo o descifrarlo.  
El archivo resultante se guarda donde el usuario indique.