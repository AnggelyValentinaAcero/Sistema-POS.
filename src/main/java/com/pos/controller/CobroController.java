package com.pos.controller;

import com.pos.app.Ventanas;
import com.pos.dto.MetodoPagoDTO;
import com.pos.dto.PagoDTO;
import com.pos.service.VentaService;
import com.pos.util.Textos;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * Ventana de cobro: elige el método de pago y, si es efectivo, calcula el cambio.
 * Si el cajero cancela, getPago() devuelve null.
 */
public class CobroController {

    @FXML private Label lblTotal;
    @FXML private ComboBox<MetodoPagoDTO> cmbMetodo;
    @FXML private VBox panelEfectivo;
    @FXML private TextField txtRecibido;
    @FXML private Label lblCambio;
    @FXML private VBox panelReferencia;
    @FXML private TextField txtReferencia;
    @FXML private Label lblMensaje;
    @FXML private Button btnConfirmar;

    private final VentaService ventaService = new VentaService();
    private BigDecimal total;
    private PagoDTO pago; // queda en null si se cancela

    @FXML
    private void initialize() {
        try {
            cmbMetodo.getItems().setAll(ventaService.listarMetodosPago());
            if (!cmbMetodo.getItems().isEmpty()) {
                cmbMetodo.setValue(cmbMetodo.getItems().get(0)); // Efectivo
            }
        } catch (SQLException e) {
            mostrarError("No se pudieron cargar los métodos de pago: " + e.getMessage());
        }

        // cada vez que cambia el texto del monto recibido se recalcula el cambio
        txtRecibido.textProperty().addListener((obs, anterior, nuevo) -> calcularCambio());
        metodoCambiado();
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
        lblTotal.setText(Textos.moneda(total));
        txtRecibido.setText(total.toPlainString());
        Platform.runLater(() -> {
            txtRecibido.requestFocus();
            txtRecibido.selectAll();
        });
    }

    public PagoDTO getPago() {
        return pago;
    }

    @FXML
    private void metodoCambiado() {
        boolean efectivo = cmbMetodo.getValue() != null && cmbMetodo.getValue().isEfectivo();
        panelEfectivo.setVisible(efectivo);
        panelEfectivo.setManaged(efectivo);
        panelReferencia.setVisible(!efectivo);
        panelReferencia.setManaged(!efectivo);
        if (panelEfectivo.getScene() != null) {
            panelEfectivo.getScene().getWindow().sizeToScene();
        }
    }

    private void calcularCambio() {
        BigDecimal recibido = leerRecibido();
        if (recibido == null || total == null) {
            lblCambio.setText("-");
        } else if (recibido.compareTo(total) < 0) {
            lblCambio.setText("Faltan " + Textos.moneda(total.subtract(recibido)));
        } else {
            lblCambio.setText(Textos.moneda(recibido.subtract(total)));
        }
    }

    @FXML
    private void confirmar() {
        MetodoPagoDTO metodo = cmbMetodo.getValue();
        if (metodo == null) {
            mostrarError("Seleccione el método de pago.");
            return;
        }

        PagoDTO nuevoPago = new PagoDTO();
        nuevoPago.setMetodoPagoId(metodo.getId());
        nuevoPago.setMetodoPago(metodo.getNombre());
        nuevoPago.setMonto(total);

        if (metodo.isEfectivo()) {
            BigDecimal recibido = leerRecibido();
            if (recibido == null || recibido.compareTo(total) < 0) {
                mostrarError("El efectivo recibido no alcanza para pagar el total.");
                return;
            }
            nuevoPago.setMontoRecibido(recibido);
            nuevoPago.setCambio(recibido.subtract(total));
        } else {
            nuevoPago.setMontoRecibido(total);
            nuevoPago.setCambio(BigDecimal.ZERO);
            nuevoPago.setReferencia(txtReferencia.getText());
        }

        pago = nuevoPago;
        Ventanas.cerrar(btnConfirmar);
    }

    @FXML
    private void cancelar() {
        pago = null;
        Ventanas.cerrar(btnConfirmar);
    }

    /** Devuelve el monto escrito o null si no es un número válido. */
    private BigDecimal leerRecibido() {
        try {
            return new BigDecimal(txtRecibido.getText().trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void mostrarError(String mensaje) {
        lblMensaje.setText(mensaje);
        lblMensaje.setVisible(true);
        lblMensaje.setManaged(true);
    }
}
