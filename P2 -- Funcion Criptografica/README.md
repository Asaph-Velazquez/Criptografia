# Practica 2 -- Funcion Criptografica

En equipos de 3 personas
Implementar cualquier función criptografica con archivos .txt o .bmp

```bash
 ____________________      ____________________      ____________________      ____________________     ____________________
|                    |    |                    |    |                    |    |                    |    |                   |
|       P.txt        | -> |        Encypt      | -> |       p-e.txt      | -> |       Decrypt      | -> |     p-e-d.txt     |
|                    |    |       (llave)      |    |     (Encrypted)    |    |       (llave)      |    |    (Decrypted)    |
|____________________|    |____________________|    |____________________|    |____________________|    |___________________|
```
Su programa debe tener una interfaz que 
1. Seleecione archivo y llave
2. Menú para cifrado/decifrado


## Librerias en Java
*  javax.crypto
*  javax.security

---

## Estado del proyecto

### ✅ Lo que ya hace
- Genera un par de llaves RSA de 2048 bits (`KeyPairGenerator`)
- Lee un archivo `p.txt` desde `src/main/resources/BaseTXT/`
- Cifra el contenido con la llave pública y escribe el resultado en `EncryptTXT/p-e.txt`
- Descifra el archivo cifrado con la llave privada y escribe el resultado en `DecryptTXT/p-e-d.txt`
- Imprime `"Proceso terminado"` al finalizar

### ❌ Lo que falta
- **Interfaz de usuario**: no hay menú ni selección interactiva de archivos o llaves
- **Selección de archivo**: la ruta del archivo de entrada está hardcodeada; el usuario no puede elegirla
- **Selección de llave**: las llaves se generan nuevas en cada ejecución y no se guardan; no es posible cargar una llave existente
- **Menú de cifrado/descifrado**: no existe opción para elegir entre solo cifrar, solo descifrar, o ambas operaciones
- **Soporte para archivos `.bmp`**: actualmente solo se prueba con `.txt`
- **Persistencia de llaves**: las llaves RSA generadas no se exportan ni almacenan para uso posterior

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
            │       ├── App.java          ← punto de entrada, orquesta el flujo
            │       ├── Encrypt.java      ← maneja generación de llaves RSA y cifrado/descifrado
            │       └── FileData.java     ← maneja lectura y escritura de archivos
            └── resources/
                ├── BaseTXT/
                │   └── p.txt             ← archivo de entrada (texto a cifrar)
                ├── EncryptTXT/
                │   └── p-e.txt           ← resultado cifrado (generado al correr)
                └── DecryptTXT/
                    └── p-e-d.txt         ← resultado descifrado (generado al correr)
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

### Resultado esperado
Los archivos `p-e.txt` (cifrado) y `p-e-d.txt` (descifrado) se generarán en sus respectivas carpetas dentro de `src/main/resources/`, y la consola mostrará:

```
Proceso terminado
```
