package com.pos.print;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.List;

/**
 * Impresión en cualquier impresora instalada en Windows (láser, tinta, o
 * "Microsoft Print to PDF"). Se "dibuja" el texto en la página con la API
 * java.awt.print, usando letra monoespaciada para conservar la alineación.
 * También tiene los métodos para consultar las impresoras instaladas (Java Print Service).
 */
public class ImpresoraSistema {

    /** Nombres de las impresoras instaladas en Windows, para elegir una en Configuración. */
    public static List<String> listarNombres() {
        List<String> nombres = new ArrayList<>();
        for (PrintService impresora : PrintServiceLookup.lookupPrintServices(null, null)) {
            nombres.add(impresora.getName());
        }
        return nombres;
    }

    /** Busca una impresora instalada por su nombre. Devuelve null si no existe. */
    public static PrintService buscar(String nombre) {
        for (PrintService impresora : PrintServiceLookup.lookupPrintServices(null, null)) {
            if (impresora.getName().equals(nombre)) {
                return impresora;
            }
        }
        return null;
    }

    public static void imprimir(String nombreImpresora, List<String> lineas) throws PrinterException {
        PrintService impresora = buscar(nombreImpresora);
        if (impresora == null) {
            throw new PrinterException("No se encontró la impresora '" + nombreImpresora + "'.");
        }
        PrinterJob trabajo = PrinterJob.getPrinterJob();
        trabajo.setPrintService(impresora);
        trabajo.setJobName("Recibo POS");
        trabajo.setPrintable(new ReciboImprimible(lineas));
        trabajo.print();
    }

    /**
     * Printable es la interfaz que Java llama una vez por cada página a imprimir.
     */
    private static class ReciboImprimible implements Printable {

        private final List<String> lineas;

        ReciboImprimible(List<String> lineas) {
            this.lineas = lineas;
        }

        @Override
        public int print(Graphics graphics, PageFormat pagina, int numeroPagina) {
            Graphics2D g = (Graphics2D) graphics;
            // se mueve el origen al área imprimible (sin los márgenes de la impresora)
            g.translate(pagina.getImageableX(), pagina.getImageableY());

            // tamaño de letra para que la línea más larga entre en el ancho del papel
            // (en letra monoespaciada cada carácter mide aprox. 0.6 veces el tamaño de la letra)
            int caracteres = 1;
            for (String linea : lineas) {
                caracteres = Math.max(caracteres, linea.length());
            }
            float tamano = (float) Math.min(10, pagina.getImageableWidth() / (caracteres * 0.6));
            g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, Math.round(tamano)));

            FontMetrics medidas = g.getFontMetrics();
            int altoLinea = medidas.getHeight();
            int lineasPorPagina = (int) (pagina.getImageableHeight() / altoLinea);

            int primera = numeroPagina * lineasPorPagina;
            if (primera >= lineas.size()) {
                return NO_SUCH_PAGE; // no hay más páginas
            }
            int ultima = Math.min(primera + lineasPorPagina, lineas.size());

            int y = medidas.getAscent();
            for (int i = primera; i < ultima; i++) {
                g.drawString(lineas.get(i), 0, y);
                y += altoLinea;
            }
            return PAGE_EXISTS;
        }
    }
}
