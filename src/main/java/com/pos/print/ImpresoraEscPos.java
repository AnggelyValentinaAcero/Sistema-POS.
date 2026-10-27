package com.pos.print;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.SimpleDoc;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Impresión en impresoras térmicas de recibos usando comandos ESC/POS.
 * ESC/POS es el "idioma" de casi todas las impresoras de recibos (Epson, Xprinter, Bixolon...):
 * se le envían bytes con el texto y algunos comandos especiales que empiezan con ESC (27) o GS (29).
 */
public class ImpresoraEscPos {

    private static final byte ESC = 27;
    private static final byte GS = 29;

    private static final byte[] INICIALIZAR = {ESC, '@'};                   // reinicia la impresora
    private static final byte[] TABLA_CARACTERES_850 = {ESC, 't', 2};       // tabla con tildes y ñ (PC850)
    private static final byte[] AVANZAR_5_LINEAS = {ESC, 'd', 5};           // deja espacio antes del corte
    private static final byte[] CORTAR_PAPEL = {GS, 'V', 1};                // corte parcial del papel
    private static final byte[] ABRIR_CAJON = {ESC, 'p', 0, 25, (byte) 250}; // pulso al cajón de dinero

    public static void imprimir(String nombreImpresora, List<String> lineas, boolean abrirCajon)
            throws PrintException, IOException {
        PrintService impresora = ImpresoraSistema.buscar(nombreImpresora);
        if (impresora == null) {
            throw new PrintException("No se encontró la impresora '" + nombreImpresora + "'.");
        }

        // Se arma todo el contenido en un arreglo de bytes
        ByteArrayOutputStream datos = new ByteArrayOutputStream();
        datos.write(INICIALIZAR);
        datos.write(TABLA_CARACTERES_850);
        for (String linea : lineas) {
            datos.write(linea.getBytes(Charset.forName("Cp850")));
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
    }
}
