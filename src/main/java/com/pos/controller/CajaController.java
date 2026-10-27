package com.pos.controller;

import com.pos.app.Ventanas;
import com.pos.dto.TurnoCajaDTO;
import com.pos.service.CajaService;
import com.pos.service.NegocioException;
import com.pos.service.ReciboService;
import com.pos.util.Alertas;
import com.pos.util.Textos;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Apertura y cierre de la caja de esta terminal.
 */
public class CajaController {

    @FXML private VBox panelCerrada;
    @FXML private TextField txtMontoInicial;

    @FXML private VBox panelAbierta;
    @FXML private Label lblTerminal;
    @FXML private Label lblUsuario;
    @FXML private Label lblApertura;
    @FXML private Label lblMontoInicial;
    @FXML private Label lblCantidadVentas;
    @FXML private VBox panelTotales;
    @FXML private Label lblEsperado;
    @FXML private TextField txtContado;
    @FXML private TextField txtObservaciones;

    private final CajaService cajaService = new CajaService();
    private final ReciboService reciboService = new ReciboService();

    @FXML
    private void initialize() {
        actualizar();
    }

    /** Consulta si hay un turno abierto y muestra el panel que corresponde. */
    @FXML
    private void actualizar() {
        try {
            TurnoCajaDTO turno = cajaService.buscarTurnoAbierto();
            if (turno == null) {
                mostrarPanel(false);
            } else {
                mostrarPanel(true);
                mostrarResumen(turno);
            }
        } catch (SQLException e) {
            Alertas.error("Error al consultar la caja: " + e.getMessage());
        }
    }

    @FXML
    private void abrirCaja() {
        try {
            BigDecimal monto = leerMonto(txtMontoInicial);
            cajaService.abrirCaja(monto);
            Alertas.info("Caja", "Caja abierta con " + Textos.moneda(monto) + ".");
            actualizar();
        } catch (NegocioException e) {
            Alertas.error(e.getMessage());
        } catch (SQLException e) {
            Alertas.error("Error al abrir la caja: " + e.getMessage());
        }
    }

    @FXML
    private void cerrarCaja() {
        try {
            BigDecimal contado = leerMonto(txtContado);
            if (!Alertas.confirmar("¿Cerrar la caja con " + Textos.moneda(contado) + " en efectivo contado?")) {
                return;
            }
            TurnoCajaDTO turno = cajaService.cerrarCaja(contado, txtObservaciones.getText());

            String resultado;
            int signo = turno.getDiferencia().signum();
            if (signo == 0) {
                resultado = "La caja cuadra exactamente.";
            } else if (signo < 0) {
                resultado = "FALTANTE de " + Textos.moneda(turno.getDiferencia().negate());
            } else {
                resultado = "SOBRANTE de " + Textos.moneda(turno.getDiferencia());
            }
            Alertas.info("Cierre de caja", "Efectivo esperado: " + Textos.moneda(turno.getMontoEsperado())
                    + "\nEfectivo contado: " + Textos.moneda(turno.getMontoContado())
                    + "\n\n" + resultado);

            // recibo/comprobante de cierre para imprimir y firmar
            FXMLLoader loader = Ventanas.cargar("/fxml/recibo_vista.fxml");
            ReciboVistaController vista = loader.getController();
            vista.setLineas(reciboService.reciboCierreCaja(turno));
            Ventanas.mostrarModal(loader, "Recibo de cierre de caja");

            txtContado.clear();
            txtObservaciones.clear();
            actualizar();
        } catch (NegocioException e) {
            Alertas.error(e.getMessage());
        } catch (SQLException e) {
            Alertas.error("Error al cerrar la caja: " + e.getMessage());
        }
    }

    private void mostrarResumen(TurnoCajaDTO turno) throws SQLException {
        lblTerminal.setText(turno.getTerminal());
        lblUsuario.setText(turno.getUsuario());
        lblApertura.setText(turno.getFechaApertura().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        lblMontoInicial.setText(Textos.moneda(turno.getMontoInicial()));
        lblCantidadVentas.setText(String.valueOf(cajaService.contarVentas(turno.getId())));

        Map<String, BigDecimal> totales = cajaService.totalesPorMetodoPago(turno.getId());
        panelTotales.getChildren().clear();
        if (totales.isEmpty()) {
            panelTotales.getChildren().add(new Label("Todavía no hay ventas en este turno."));
        }
        for (String metodo : totales.keySet()) {
            panelTotales.getChildren().add(new Label(metodo + ": " + Textos.moneda(totales.get(metodo))));
        }
        lblEsperado.setText(Textos.moneda(cajaService.calcularEfectivoEsperado(turno, totales)));
    }

    private void mostrarPanel(boolean abierta) {
        panelAbierta.setVisible(abierta);
        panelCerrada.setVisible(!abierta);
    }

    private BigDecimal leerMonto(TextField campo) throws NegocioException {
        String texto = campo.getText().trim().replace(",", ".");
        try {
            return new BigDecimal(texto);
        } catch (NumberFormatException e) {
            throw new NegocioException("Ingrese un monto válido.");
        }
    }
}
