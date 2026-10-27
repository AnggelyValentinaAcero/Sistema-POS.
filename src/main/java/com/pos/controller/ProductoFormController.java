package com.pos.controller;

import com.pos.app.Ventanas;
import com.pos.dto.CategoriaDTO;
import com.pos.dto.ImpuestoDTO;
import com.pos.dto.ProductoDTO;
import com.pos.dto.ProveedorDTO;
import com.pos.service.NegocioException;
import com.pos.service.ProductoService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * Formulario para crear o editar un producto.
 */
public class ProductoFormController {

    private static final String[] UNIDADES = {"UND", "KG", "LT", "MT", "CAJA", "PAQ"};

    @FXML private TextField txtCodigo;
    @FXML private ComboBox<String> cmbUnidad;
    @FXML private TextField txtNombre;
    @FXML private TextField txtDescripcion;
    @FXML private ComboBox<CategoriaDTO> cmbCategoria;
    @FXML private ComboBox<ImpuestoDTO> cmbImpuesto;
    @FXML private ComboBox<ProveedorDTO> cmbProveedor;
    @FXML private TextField txtPrecioCompra;
    @FXML private TextField txtPrecioVenta;
    @FXML private Label lblStock;
    @FXML private TextField txtStock;
    @FXML private TextField txtStockMinimo;
    @FXML private Label lblNotaStock;
    @FXML private Label lblMensaje;
    @FXML private Button btnGuardar;

    private final ProductoService productoService = new ProductoService();
    private ProductoDTO producto;
    private boolean guardado = false;

    @FXML
    private void initialize() {
        cmbUnidad.getItems().addAll(UNIDADES);
        cmbUnidad.setValue("UND");
        try {
            cmbCategoria.getItems().setAll(productoService.listarCategorias());
            cmbImpuesto.getItems().setAll(productoService.listarImpuestos());

            // opción para productos sin proveedor
            ProveedorDTO sinProveedor = new ProveedorDTO();
            sinProveedor.setNombre("(Sin proveedor)");
            cmbProveedor.getItems().add(sinProveedor);
            cmbProveedor.getItems().addAll(productoService.listarProveedores());
            cmbProveedor.setValue(sinProveedor);
        } catch (SQLException e) {
            mostrarError("No se pudieron cargar las listas: " + e.getMessage());
        }
        txtPrecioCompra.setText("0");
        txtStock.setText("0");
        txtStockMinimo.setText("0");
    }

    /** Recibe el producto a editar, o null para crear uno nuevo. */
    public void setProducto(ProductoDTO producto) {
        this.producto = producto;
        if (producto == null) {
            return;
        }

        txtCodigo.setText(producto.getCodigoBarras());
        txtNombre.setText(producto.getNombre());
        txtDescripcion.setText(producto.getDescripcion());
        cmbUnidad.setValue(producto.getUnidadMedida());
        txtPrecioCompra.setText(producto.getPrecioCompra().toPlainString());
        txtPrecioVenta.setText(producto.getPrecioVenta().toPlainString());
        txtStockMinimo.setText(producto.getStockMinimo().stripTrailingZeros().toPlainString());

        // en edición el stock se muestra pero no se puede modificar
        lblStock.setText("Stock actual");
        txtStock.setText(producto.getStock().stripTrailingZeros().toPlainString());
        txtStock.setDisable(true);

        for (CategoriaDTO c : cmbCategoria.getItems()) {
            if (c.getId() == producto.getCategoriaId()) {
                cmbCategoria.setValue(c);
            }
        }
        for (ImpuestoDTO i : cmbImpuesto.getItems()) {
            if (i.getId() == producto.getImpuestoId()) {
                cmbImpuesto.setValue(i);
            }
        }
        for (ProveedorDTO p : cmbProveedor.getItems()) {
            if (producto.getProveedorId() != null && p.getId() == producto.getProveedorId()) {
                cmbProveedor.setValue(p);
            }
        }
    }

    public boolean isGuardado() {
        return guardado;
    }

    @FXML
    private void guardar() {
        try {
            // Se arma un objeto nuevo con los datos del formulario. No se modifica directamente
            // el producto de la tabla, para no mostrar datos que quizá no se lleguen a guardar.
            ProductoDTO p = new ProductoDTO();
            if (producto != null) {
                p.setId(producto.getId());
            }
            p.setCodigoBarras(txtCodigo.getText());
            p.setNombre(txtNombre.getText());
            p.setDescripcion(txtDescripcion.getText());
            p.setUnidadMedida(cmbUnidad.getValue());
            p.setCategoriaId(cmbCategoria.getValue() == null ? 0 : cmbCategoria.getValue().getId());
            p.setImpuestoId(cmbImpuesto.getValue() == null ? 0 : cmbImpuesto.getValue().getId());

            ProveedorDTO proveedor = cmbProveedor.getValue();
            if (proveedor == null || proveedor.getId() == 0) {
                p.setProveedorId(null);
            } else {
                p.setProveedorId(proveedor.getId());
            }

            p.setPrecioCompra(leerNumero(txtPrecioCompra, "precio de compra"));
            p.setPrecioVenta(leerNumero(txtPrecioVenta, "precio de venta"));
            p.setStockMinimo(leerNumero(txtStockMinimo, "stock mínimo"));

            if (producto == null) {
                p.setStock(leerNumero(txtStock, "stock inicial"));
                productoService.crear(p);
            } else {
                productoService.actualizar(p);
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

    /** Convierte el texto de un campo a BigDecimal. Acepta coma o punto como separador decimal. */
    private BigDecimal leerNumero(TextField campo, String nombreCampo) throws NegocioException {
        String texto = campo.getText().trim().replace(",", ".");
        if (texto.isEmpty()) {
            throw new NegocioException("Ingrese el " + nombreCampo + ".");
        }
        try {
            return new BigDecimal(texto);
        } catch (NumberFormatException e) {
            throw new NegocioException("El " + nombreCampo + " debe ser un número.");
        }
    }

    private void mostrarError(String mensaje) {
        lblMensaje.setText(mensaje);
        lblMensaje.setVisible(true);
        lblMensaje.setManaged(true);
        Ventanas.ajustarTamano(lblMensaje);
    }
}
