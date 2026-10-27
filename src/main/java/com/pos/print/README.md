# Paquete `print`: armado e impresión de recibos

Genera el texto de los recibos (venta y comprobante de cierre de caja) y lo envía a una
impresora térmica de recibos o a cualquier impresora de Windows.

```
ReciboService  →  GeneradorRecibo (usa ReciboTexto)  →  List<String> con las líneas del recibo
               →  ImpresoraEscPos  (impresora térmica de recibos)
               →  ImpresoraSistema (cualquier impresora de Windows o "Microsoft Print to PDF")
```

## Archivos

| Archivo | Qué hace |
|---|---|
| `ReciboTexto.java` | Herramienta para armar un recibo como líneas de texto de ancho fijo: texto centrado, columnas izquierda/derecha y separadores. |
| `GeneradorRecibo.java` | Arma el contenido completo: el recibo de venta (encabezado del negocio, productos, totales, pago, cambio) y el comprobante de cierre de caja (esperado, contado, faltante o sobrante, firma). |
| `ImpresoraEscPos.java` | Imprime en impresoras térmicas enviando bytes con comandos **ESC/POS** (cortar papel, abrir el cajón de dinero, tildes). |
| `ImpresoraSistema.java` | Imprime en cualquier impresora de Windows "dibujando" el texto con `java.awt.print`. También lista y busca las impresoras instaladas. |

## Partes clave del código

### `ReciboTexto.columnas()`: alinear con espacios

```java
public void columnas(String izquierda, String derecha) {
    int espacioIzquierda = ancho - derecha.length() - 1;
    if (izquierda.length() > espacioIzquierda) {
        izquierda = izquierda.substring(0, espacioIzquierda);
    }
    int espacios = ancho - izquierda.length() - derecha.length();
    lineas.add(izquierda + " ".repeat(espacios) + derecha);
}
```

Las impresoras de recibos usan letra **monoespaciada** (todas las letras miden lo mismo), así
que para alinear basta con agregar espacios. Con ancho 48:

```
TOTAL                                    $ 37.50
```

`texto()` divide los textos largos en varias líneas cortando en el último espacio, para no partir
palabras. Lo prueba `src/test/java/com/pos/print/ReciboTextoTest.java`.

### `ImpresoraEscPos`: los comandos que entiende la impresora

```java
private static final byte ESC = 27;
private static final byte GS = 29;

private static final byte[] INICIALIZAR = {ESC, '@'};                    // reinicia la impresora
private static final byte[] TABLA_CARACTERES_850 = {ESC, 't', 2};        // tabla con tildes y ñ (PC850)
private static final byte[] AVANZAR_5_LINEAS = {ESC, 'd', 5};            // deja espacio antes del corte
private static final byte[] CORTAR_PAPEL = {GS, 'V', 1};                 // corte parcial del papel
private static final byte[] ABRIR_CAJON = {ESC, 'p', 0, 25, (byte) 250}; // pulso al cajón de dinero
```

ESC/POS es el "idioma" de casi todas las impresoras de recibos (Epson, Xprinter, Bixolon…). Se le
envían bytes: el texto, más comandos especiales que empiezan con ESC (27) o GS (29).

```java
ByteArrayOutputStream datos = new ByteArrayOutputStream();
datos.write(INICIALIZAR);
datos.write(TABLA_CARACTERES_850);
for (String linea : lineas) {
    datos.write(linea.getBytes(Charset.forName("Cp850")));   // codificación con tildes y ñ
    datos.write('\n');
}
datos.write(AVANZAR_5_LINEAS);
datos.write(CORTAR_PAPEL);
if (abrirCajon) {
    datos.write(ABRIR_CAJON);
}

// Se envían los bytes "en crudo" a la impresora, sin pasar por el formato de Windows
DocPrintJob trabajo = impresora.createPrintJob();
Doc documento = new SimpleDoc(datos.toByteArray(), DocFlavor.BYTE_ARRAY.AUTOSENSE, null);
trabajo.print(documento, null);
```

### `ImpresoraSistema`: imprimir en cualquier impresora

Usa la interfaz `Printable`. Java llama a su método `print(graphics, pagina, numeroPagina)` una vez
por cada página. El método dibuja las líneas con `drawString` en letra monoespaciada, y cuando no
quedan líneas devuelve `NO_SUCH_PAGE`. Ajusta el tamaño de la letra para que la línea más larga
entre en el ancho del papel.

Con **Microsoft Print to PDF** se puede demostrar la impresión sin tener una impresora de recibos.

### `ImpresoraSistema.buscar()`: encontrar una impresora por nombre

```java
public static PrintService buscar(String nombre) {
    for (PrintService impresora : PrintServiceLookup.lookupPrintServices(null, null)) {
        if (impresora.getName().equals(nombre)) {
            return impresora;
        }
    }
    return null;
}
```

`PrintServiceLookup` (Java Print Service) devuelve las impresoras instaladas en Windows. La
usan las dos clases de impresión y la pantalla de Configuración para llenar la lista.
