package com.pos.controller;

import com.pos.app.Ventanas;
import com.pos.dto.ProductoDTO;
import com.pos.dto.ProveedorDTO;
import com.pos.service.InventarioService;
import com.pos.service.NegocioException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * Formulario de entrada de mercadería (compra a proveedor).
 */
public class EntradaFormController {

    @FXML private Label lblProducto;
    @FXML private Label lblStockActual;
    @FXML private TextField txtCantidad;
    @FXML private TextField txtCosto;
    @FXML private CheckBox chkActualizarCosto;
    @FXML private ComboBox<ProveedorDTO> cmbProveedor;
    @FXML private TextField txtObservacion;
    @FXML private Label lblMensaje;
    @FXML private Button btnGuardar;

    private final InventarioService inventarioService = new InventarioService();
    private ProductoDTO producto;
    private boolean guardado = false;

    @FXML
    private void initialize() {
        ProveedorDTO sinProveedor = new ProveedorDTO();
        sinProveedor.setNombre("(Sin proveedor)");
        cmbProveedor.getItems().add(sinProveedor);
        try {
            cmbProveedor.getItems().addAll(inventarioService.listarProveedores());
        } catch (SQLException e) {
            mostrarError("No se pudieron cargar los proveedores: " + e.getMessage());
        }
        cmbProveedor.setValue(sinProveedor);
    }

    /** Permite abrir el formulario con un producto ya elegido (ej: desde la lista de stock bajo). */
    public void setProducto(ProductoDTO producto) {
        this.producto = producto;
        if (producto == null) {
            return;
        }
        lblProducto.setText(producto.getCodigoBarras() + " - " + producto.getNombre());
        lblStockActual.setText("Stock actual: " + producto.getStock().stripTrailingZeros().toPlainString()
                + " " + producto.getUnidadMedida() + " · Mínimo: " + producto.getStockMinimo().stripTrailingZeros().toPlainString());
        txtCosto.setText(producto.getPrecioCompra().toPlainString());

        // si el producto tiene proveedor, se selecciona por defecto
        for (ProveedorDTO p : cmbProveedor.getItems()) {
            if (producto.getProveedorId() != null && p.getId() == producto.getProveedorId()) {
                cmbProveedor.setValue(p);
            }
        }
        txtCantidad.requestFocus();
    }

    public boolean isGuardado() {
        return guardado;
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
        try {
            BigDecimal cantidad = leerNumero(txtCantidad, "la cantidad");
            BigDecimal costo = leerNumero(txtCosto, "el costo unitario");
            inventarioService.registrarEntrada(producto.getId(), cantidad, costo, cmbProveedor.getValue(),
                    chkActualizarCosto.isSelected(), txtObservacion.getText());
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

    private BigDecimal leerNumero(TextField campo, String nombre) throws NegocioException {
        try {
            return new BigDecimal(campo.getText().trim().replace(",", "."));
        } catch (NumberFormatException e) {
            throw new NegocioException("Ingrese " + nombre + " (un número).");
        }
    }

    private void mostrarError(String mensaje) {
        lblMensaje.setText(mensaje);
        lblMensaje.setVisible(true);
        lblMensaje.setManaged(true);
        Ventanas.ajustarTamano(lblMensaje);
    }
}
