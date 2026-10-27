# Pruebas: `print`

Pruebas unitarias con **JUnit 5** (sin base de datos ni impresora). Se ejecutan con
`.\mvnw.cmd test`.

## Archivos

| Archivo | Qué prueba |
|---|---|
| `ReciboTextoTest.java` | Que `ReciboTexto` arme bien las líneas de ancho fijo del recibo. |

## Pruebas

| Prueba | Caso | Resultado esperado |
|---|---|---|
| `centraElTextoConEspacios` | Centrar "HOLA" en un ancho de 20 | 8 espacios + "HOLA" |
| `alineaColumnasALosExtremos` | Columnas "A" y "B" en un ancho de 10 | "A" a la izquierda y "B" a la derecha, separadas por 8 espacios |
| `divideTextoLargoPorEspacios` | Un texto más largo que el ancho | Se divide en varias líneas cortando en los espacios: "uno dos" / "tres" / "cuatro" |
| `cortaPalabraMasLargaQueElAncho` | Una palabra más larga que la línea | Se corta donde sea: "abcde" / "fghij" |
| `separadorLlenaElAncho` | Separador en un ancho de 16 | 16 guiones |

## Parte clave del código

```java
@Test
void centraElTextoConEspacios() {
    ReciboTexto t = new ReciboTexto(20);
    t.centrado("HOLA");
    List<String> lineas = t.getLineas();
    assertEquals(1, lineas.size());
    assertEquals("        HOLA", lineas.get(0)); // 8 espacios + 4 letras = 12 (centrado en 20)
}
```

Como el recibo es solo una lista de textos, se puede probar sin imprimir nada: basta con comparar
las líneas generadas con las esperadas.
