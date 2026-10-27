package com.pos.controller;

import com.pos.service.NegocioException;
import com.pos.service.ReciboService;
import com.pos.util.Alertas;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.util.List;

/**
 * Muestra un recibo en pantalla tal como saldrá impreso, con la opción de imprimirlo.
 */
public class ReciboVistaController {

    @FXML private TextArea txtRecibo;
    @FXML private Button btnCerrar;

    private final ReciboService reciboService = new ReciboService();
    private List<String> lineas;

    public void setLineas(List<String> lineas) {
        this.lineas = lineas;
        txtRecibo.setText(String.join("\n", lineas));

        // Ajusta el ancho del TextArea a la línea más larga para que no sobre espacio a los lados
        int caracteres = 0;
        for (String linea : lineas) {
            caracteres = Math.max(caracteres, linea.length());
        }
        txtRecibo.setPrefColumnCount(caracteres + 2);
    }

    @FXML
    private void imprimir() {
        try {
            reciboService.imprimir(lineas, false);
            cerrar();
        } catch (NegocioException e) {
            Alertas.error(e.getMessage());
        }
    }

    @FXML
    private void cerrar() {
        ((Stage) btnCerrar.getScene().getWindow()).close();
    }
}
