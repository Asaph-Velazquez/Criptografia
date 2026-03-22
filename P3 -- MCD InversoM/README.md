# MCDInversoM - Inverso Multiplicativo Modular

Calculadora del **Inverso Multiplicativo Modular** usando el Algoritmo de Euclides Extendido con interfaz gráfica para cifrado afín.

## 📚 Concepto

El **inverso multiplicativo modular** de un número `α` módulo `n` es el número `α⁻¹` tal que:

```
α × α⁻¹ ≡ 1 (mod n)
```

Este inverso existe **solo si** `MCD(α, n) = 1` (es decir, α y n son coprimos).

## 🔧 Algoritmos Implementados

### 1. Algoritmo de Euclides
Calcula el Máximo Común Divisor y guarda el rastro de cada división:
```
dividendo = divisor × cociente + residuo
```

### 2. Sustitución hacia atrás
A partir del MCD = 1, despeja hacia atrás para encontrar los coeficientes de Bézout y finalmente el inverso.

## 🖥️ Uso

### Interfaz Gráfica
```bash
mvn compile exec:java -Dexec.mainClass="com.mycompany.mcdinversom.GUI"
```

Ingresa:
- **α (Alfa)**: El valor del cual quieres el inverso
- **β (Beta)**: Factor de traslación del cifrado afín
- **n (Módulo)**: El módulo de trabajo

La GUI muestra:
- El inverso multiplicativo `α⁻¹`
- Fórmula de cifrado: `C = (αP + β) mod n`
- Fórmula de descifrado: `P = α⁻¹(C + (n - β)) mod n`

### Consola
```bash
mvn exec:java 
```

Ingresa los valores cuando se solicite. Muestra todo el procedimiento paso a paso.

## 📁 Estructura del Proyecto

```
MCDInversoM/
├── pom.xml                     # Configuración Maven
├── nbactions.xml               # Configuración NetBeans
└── src/main/java/com/mycompany/mcdinversom/
    ├── MCDInversoM.java        # Lógica de los algoritmos
    └── GUI.java               # Interfaz gráfica Swing
```

## 🧪 Ejemplo

**Entrada:**
- α = 7
- n = 26

**Salida:**
- MCD(7, 26) = 1 ✓
- α⁻¹ = 15 (porque 7 × 15 = 105 ≡ 1 mod 26)

## 🛠️ Requisitos

- Java 8+
- Maven 3.x

## 📦 Compilación y Ejecución

```bash
# Compilar
mvn compile

# Ejecutar GUI
mvn exec:java "-Dexec.mainClass=com.mycompany.mcdinversom.GUI"

# Ejecutar consola
mvn exec:java "-Dexec.mainClass=com.mycompany.mcdinversom.MCDInversoM"
```

---
