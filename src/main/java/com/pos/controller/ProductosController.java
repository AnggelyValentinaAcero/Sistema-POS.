package com.pos.controller;

import com.pos.app.Ventanas;
import com.pos.dto.ProductoDTO;
import com.pos.service.NegocioException;
import com.pos.service.ProductoService;
import com.pos.util.Alertas;
import com.pos.util.Permiso;
import com.pos.util.Sesion;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * Listado y búsqueda de productos. Los cajeros solo pueden consultar;
 * quienes tienen PRODUCTOS_GESTIONAR ven además los botones para modificar.
 */
public class ProductosController {

    @FXML private HBox panelBotones;
    @FXML private TextField txtBuscar;
    @FXML private CheckBox chkInactivos;
    @FXML private TableView<ProductoDTO> tabla;
    @FXML private TableColumn<ProductoDTO, String> colCodigo;
    @FXML private TableColumn<ProductoDTO, String> colNombre;
    @FXML private TableColumn<ProductoDTO, String> colCategoria;
    @FXML private TableColumn<ProductoDTO, BigDecimal> colPrecio;
    @FXML private TableColumn<ProductoDTO, BigDecimal> colStock;
    @FXML private TableColumn<ProductoDTO, BigDecimal> colStockMinimo;
    @FXML private TableColumn<ProductoDTO, String> colUnidad;
    @FXML private TableColumn<ProductoDTO, String> colImpuesto;
    @FXML private TableColumn<ProductoDTO, String> colEstado;

    private final ProductoService productoService = new ProductoService();
    private boolean puedeGestionar;

    @FXML
    private void initialize() {
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigoBarras"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precioVenta"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colStockMinimo.setCellValueFactory(new PropertyValueFactory<>("stockMinimo"));
        colUnidad.setCellValueFactory(new PropertyValueFactory<>("unidadMedida"));
        colImpuesto.setCellValueFactory(new PropertyValueFactory<>("impuesto"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        // Pinta de rojo las filas de productos con stock bajo
        tabla.setRowFactory(t -> new TableRow<ProductoDTO>() {
            @Override
            protected void updateItem(ProductoDTO producto, boolean vacia) {
                super.updateItem(producto, vacia);
                getStyleClass().remove("fila-stock-bajo");
                if (!vacia && producto != null && producto.isActivo() && producto.isStockBajo()) {
                    getStyleClass().add("fila-stock-bajo");
                }
            }
        });

        // Los botones de modificar solo se muestran si el usuario tiene permiso
        puedeGestionar = Sesion.tienePermiso(Permiso.PRODUCTOS_GESTIONAR);
        panelBotones.setVisible(puedeGestionar);
        chkInactivos.setVisible(puedeGestionar);

        buscar();
    }

    @FXML
    private void buscar() {
        try {
            tabla.getItems().setAll(productoService.buscar(txtBuscar.getText(), chkInactivos.isSelected()));
        } catch (NegocioException e) {
            Alertas.error(e.getMessage());
        } catch (SQLException e) {
            Alertas.error("Error al buscar productos: " + e.getMessage());
        }
    }

    @FXML
    private void nuevo() {
        abrirFormulario(null);
    }

    @FXML
    private void editar() {
        ProductoDTO seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            Alertas.error("Seleccione un producto de la tabla.");
            return;
        }
        abrirFormulario(seleccionado);
    }

    @FXML
    private void clicTabla(MouseEvent evento) {
        if (puedeGestionar && evento.getClickCount() == 2 && tabla.getSelectionModel().getSelectedItem() != null) {
            editar();
        }
    }

    @FXML
    private void cambiarEstado() {
        ProductoDTO seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            Alertas.error("Seleccione un producto de la tabla.");
            return;
        }
        boolean activar = !seleccionado.isActivo();
        String accion = activar ? "activar" : "desactivar";
        if (!Alertas.confirmar("¿Desea " + accion + " el producto '" + seleccionado.getNombre() + "'?")) {
            return;
        }
        try {
            productoService.cambiarEstado(seleccionado, activar);
            buscar();
        } catch (NegocioException e) {
            Alertas.error(e.getMessage());
        } catch (SQLException e) {
            Alertas.error("Error al " + accion + " el producto: " + e.getMessage());
        }
    }

    private void abrirFormulario(ProductoDTO producto) {
        FXMLLoader loader = Ventanas.cargar("/fxml/producto_form.fxml");
        ProductoFormController formulario = loader.getController();
        formulario.setProducto(producto);

        Ventanas.mostrarModal(loader, producto == null ? "Nuevo producto" : "Editar producto");

        if (formulario.isGuardado()) {
            buscar();
        }
    }
}
