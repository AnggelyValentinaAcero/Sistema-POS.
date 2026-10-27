package com.pos.controller;

import com.pos.app.Ventanas;
import com.pos.dto.ProductoDTO;
import com.pos.service.InventarioService;
import com.pos.service.NegocioException;
import com.pos.util.Alertas;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * Formulario de ajuste de inventario (mermas, productos dañados, conteo físico...).
 */
public class AjusteFormController {

    @FXML private Label lblProducto;
    @FXML private Label lblStockActual;
    @FXML private RadioButton rbCantidad;
    @FXML private RadioButton rbConteo;
    @FXML private Label lblCantidad;
    @FXML private TextField txtCantidad;
    @FXML private ComboBox<String> cmbMotivo;
    @FXML private TextField txtDetalle;
    @FXML private Label lblMensaje;
    @FXML private Button btnGuardar;

    private final InventarioService inventarioService = new InventarioService();
    private ProductoDTO producto;
    private boolean guardado = false;

    @FXML
    private void initialize() {
        cmbMotivo.getItems().addAll(InventarioService.MOTIVOS_AJUSTE);
    }

    public void setProducto(ProductoDTO producto) {
        this.producto = producto;
        if (producto == null) {
            return;
        }
        lblProducto.setText(producto.getCodigoBarras() + " - " + producto.getNombre());
        lblStockActual.setText("Stock en el sistema: " + producto.getStock().stripTrailingZeros().toPlainString()
                + " " + producto.getUnidadMedida());
    }

    public boolean isGuardado() {
        return guardado;
    }

    @FXML
    private void tipoCambiado() {
        if (rbConteo.isSelected()) {
            lblCantidad.setText("Stock contado");
            cmbMotivo.setValue("Conteo físico");
        } else {
            lblCantidad.setText("Cantidad");
        }
    }

    @FXML
    private void elegirProducto() {
        FXMLLoader loader = Ventanas.cargar("/fxml/buscar_producto.fxml");
        BuscarProductoController buscador = loader.getController();
        Ventanas.mostrarModal(loader, "Elegir producto");
        if (buscador.getSeleccionado() != null) {
            setProducto(buscador.getSeleccionado());
        }
    }

    @FXML
    private void guardar() {
        if (producto == null) {
            mostrarError("Elija el producto.");
            return;
        }
        BigDecimal numero;
        try {
            numero = new BigDecimal(txtCantidad.getText().trim().replace(",", "."));
        } catch (NumberFormatException e) {
            mostrarError("Ingrese un número válido.");
            return;
        }

        try {
            if (rbConteo.isSelected()) {
                BigDecimal diferencia = inventarioService.registrarConteo(producto.getId(), numero,
                        cmbMotivo.getValue(), txtDetalle.getText());
                Alertas.info("Conteo físico", "Se ajustó el stock en "
                        + diferencia.stripTrailingZeros().toPlainString() + " " + producto.getUnidadMedida() + ".");
            } else {
                inventarioService.registrarAjuste(producto.getId(), numero, cmbMotivo.getValue(), txtDetalle.getText());
            }
            guardado = true;
            Ventanas.cerrar(btnGuardar);
        } catch (NegocioException e) {
            mostrarError(e.getMessage());
        } catch (SQLException e) {
            mostrarError("Error de base de datos: " + e.getMessage());
        }
    }

    @FXML
    private void cancelar() {
        Ventanas.cerrar(btnGuardar);
    }

    private void mostrarError(String mensaje) {
        lblMensaje.setText(mensaje);
        lblMensaje.setVisible(true);
        lblMensaje.setManaged(true);
        Ventanas.ajustarTamano(lblMensaje);
    }
}
