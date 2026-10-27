package com.pos.controller;

import com.pos.app.Ventanas;
import com.pos.dto.ProductoDTO;
import com.pos.service.NegocioException;
import com.pos.service.ProductoService;
import com.pos.util.Alertas;
import com.pos.util.Permiso;
import com.pos.util.Sesion;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * Ventana para buscar un producto por nombre cuando no se puede escanear.
 */
public class BuscarProductoController {

    @FXML private TextField txtBuscar;
    @FXML private TableView<ProductoDTO> tabla;
    @FXML private TableColumn<ProductoDTO, String> colCodigo;
    @FXML private TableColumn<ProductoDTO, String> colNombre;
    @FXML private TableColumn<ProductoDTO, BigDecimal> colPrecio;
    @FXML private TableColumn<ProductoDTO, BigDecimal> colStock;
    @FXML private Button btnSeleccionar;

    private final ProductoService productoService = new ProductoService();
    private final ObservableList<ProductoDTO> listaProductos = FXCollections.observableArrayList();
    private ProductoDTO seleccionado;

    @FXML
    private void initialize() {
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigoBarras"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precioVenta"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));

        // Se asigna la lista observable a la tabla desde el inicio
        tabla.setItems(listaProductos);

        // Carga inicial automática condicionada a permisos
        if (Sesion.tienePermiso(Permiso.PRODUCTOS_VER)) {
            buscar();
        }

        Platform.runLater(() -> txtBuscar.requestFocus());
    }

    public ProductoDTO getSeleccionado() {
        return seleccionado;
    }

    @FXML
    private void buscar() {
        try {
            String texto = (txtBuscar != null && txtBuscar.getText() != null) ? txtBuscar.getText().trim() : "";
            List<ProductoDTO> resultados = productoService.buscar(texto, false);
            // Actualiza la lista observable vinculada para refrescar la tabla en el primer clic
            listaProductos.setAll(resultados);
            if (!listaProductos.isEmpty()) {
                tabla.getSelectionModel().selectFirst();
            }
        } catch (NegocioException e) {
            Alertas.error(e.getMessage());
        } catch (SQLException e) {
            Alertas.error("Error al buscar: " + e.getMessage());
        }
    }

    @FXML
    private void clicTabla(MouseEvent evento) {
        if (evento.getClickCount() == 2) {
            seleccionar();
        }
    }

    @FXML
    private void teclaTabla(KeyEvent evento) {
        if (evento.getCode() == KeyCode.ENTER) {
            seleccionar();
        }
    }

    @FXML
    private void seleccionar() {
        ProductoDTO producto = tabla.getSelectionModel().getSelectedItem();
        if (producto == null) {
            Alertas.error("Seleccione un producto.");
            return;
        }
        seleccionado = producto;
        Ventanas.cerrar(btnSeleccionar);
    }

    @FXML
    private void cancelar() {
        seleccionado = null;
        Ventanas.cerrar(btnSeleccionar);
    }
}
