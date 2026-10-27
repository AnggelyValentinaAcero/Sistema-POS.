package com.pos.controller;

import com.pos.app.Ventanas;
import com.pos.dto.MovimientoDTO;
import com.pos.dto.ProductoDTO;
import com.pos.service.InventarioService;
import com.pos.service.NegocioException;
import com.pos.util.Alertas;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Módulo de inventario: lista de productos con stock bajo, kardex por producto,
 * y acceso a los formularios de entrada y ajuste.
 */
public class InventarioController {

    @FXML private TabPane pestanas;
    @FXML private Tab tabKardex;

    @FXML private Label lblStockBajo;
    @FXML private TableView<ProductoDTO> tablaStockBajo;
    @FXML private TableColumn<ProductoDTO, String> colSbCodigo;
    @FXML private TableColumn<ProductoDTO, String> colSbNombre;
    @FXML private TableColumn<ProductoDTO, BigDecimal> colSbStock;
    @FXML private TableColumn<ProductoDTO, BigDecimal> colSbMinimo;
    @FXML private TableColumn<ProductoDTO, String> colSbUnidad;
    @FXML private TableColumn<ProductoDTO, String> colSbProveedor;

    @FXML private Label lblProductoKardex;
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private Label lblResumenKardex;
    @FXML private TableView<MovimientoDTO> tablaKardex;
    @FXML private TableColumn<MovimientoDTO, String> colKFecha;
    @FXML private TableColumn<MovimientoDTO, String> colKProducto;
    @FXML private TableColumn<MovimientoDTO, String> colKTipo;
    @FXML private TableColumn<MovimientoDTO, String> colKCantidad;
    @FXML private TableColumn<MovimientoDTO, String> colKStock;
    @FXML private TableColumn<MovimientoDTO, String> colKUsuario;
    @FXML private TableColumn<MovimientoDTO, String> colKDetalle;

    private final InventarioService inventarioService = new InventarioService();
    private ProductoDTO productoKardex; // producto elegido en la pestaña Kardex (null = todos)

    @FXML
    private void initialize() {
        colSbCodigo.setCellValueFactory(new PropertyValueFactory<>("codigoBarras"));
        colSbNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colSbStock.setCellValueFactory(new PropertyValueFactory<>("stock"));
        colSbMinimo.setCellValueFactory(new PropertyValueFactory<>("stockMinimo"));
        colSbUnidad.setCellValueFactory(new PropertyValueFactory<>("unidadMedida"));
        colSbProveedor.setCellValueFactory(new PropertyValueFactory<>("proveedor"));

        colKFecha.setCellValueFactory(new PropertyValueFactory<>("fechaTexto"));
        colKProducto.setCellValueFactory(new PropertyValueFactory<>("producto"));
        colKTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colKCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidadTexto"));
        colKStock.setCellValueFactory(new PropertyValueFactory<>("stockResultanteTexto"));
        colKUsuario.setCellValueFactory(new PropertyValueFactory<>("usuario"));
        colKDetalle.setCellValueFactory(new PropertyValueFactory<>("detalle"));

        // en el kardex las entradas se pintan en verde y las salidas en rojo
        tablaKardex.setRowFactory(t -> new TableRow<MovimientoDTO>() {
            @Override
            protected void updateItem(MovimientoDTO mov, boolean vacia) {
                super.updateItem(mov, vacia);
                getStyleClass().removeAll("fila-entrada", "fila-salida");
                if (!vacia && mov != null) {
                    getStyleClass().add(mov.getCantidad().signum() > 0 ? "fila-entrada" : "fila-salida");
                }
            }
        });

        dpDesde.setValue(LocalDate.now().withDayOfMonth(1)); // desde el inicio del mes
        dpHasta.setValue(LocalDate.now());
        cargarStockBajo();
        consultarKardex(); // Carga predeterminada automática de todos los movimientos
    }

    @FXML
    private void cargarStockBajo() {
        try {
            List<ProductoDTO> productos = inventarioService.listarStockBajo();
            tablaStockBajo.getItems().setAll(productos);
            lblStockBajo.setText(productos.size() + " productos para reponer");
        } catch (NegocioException e) {
            Alertas.error(e.getMessage());
        } catch (SQLException e) {
            Alertas.error("Error al cargar el inventario: " + e.getMessage());
        }
    }

    @FXML
    private void clicStockBajo(MouseEvent evento) {
        ProductoDTO producto = tablaStockBajo.getSelectionModel().getSelectedItem();
        if (evento.getClickCount() == 2 && producto != null) {
            abrirEntrada(producto);
        }
    }

    @FXML
    private void verKardexSeleccionado() {
        ProductoDTO producto = tablaStockBajo.getSelectionModel().getSelectedItem();
        if (producto == null) {
            Alertas.error("Seleccione un producto de la tabla.");
            return;
        }
        mostrarKardex(producto);
    }

    @FXML
    private void registrarEntrada() {
        abrirEntrada(null);
    }

    @FXML
    private void ajustarInventario() {
        FXMLLoader loader = Ventanas.cargar("/fxml/ajuste_form.fxml");
        AjusteFormController form = loader.getController();
        form.setProducto(productoSeleccionado());
        Ventanas.mostrarModal(loader, "Ajuste de inventario");
        if (form.isGuardado()) {
            refrescar();
        }
    }

    @FXML
    private void elegirProductoKardex() {
        FXMLLoader loader = Ventanas.cargar("/fxml/buscar_producto.fxml");
        BuscarProductoController buscador = loader.getController();
        Ventanas.mostrarModal(loader, "Elegir producto");
        if (buscador.getSeleccionado() != null) {
            mostrarKardex(buscador.getSeleccionado());
        }
    }

    @FXML
    private void consultarKardex() {
        Integer prodId = (productoKardex != null) ? productoKardex.getId() : null;
        try {
            List<MovimientoDTO> movimientos = inventarioService.kardex(prodId,
                    dpDesde.getValue(), dpHasta.getValue());
            tablaKardex.getItems().setAll(movimientos);

            // totales del período
            BigDecimal entradas = BigDecimal.ZERO;
            BigDecimal salidas = BigDecimal.ZERO;
            for (MovimientoDTO m : movimientos) {
                if (m.getCantidad().signum() > 0) {
                    entradas = entradas.add(m.getCantidad());
                } else {
                    salidas = salidas.add(m.getCantidad().negate());
                }
            }
            if (productoKardex != null) {
                ProductoDTO actualizado = inventarioService.buscarProducto(productoKardex.getId());
                String stockActual = (actualizado != null && actualizado.getStock() != null)
                        ? actualizado.getStock().stripTrailingZeros().toPlainString()
                        : "-";
                lblResumenKardex.setText("Entradas: " + entradas.stripTrailingZeros().toPlainString()
                        + " · Salidas: " + salidas.stripTrailingZeros().toPlainString()
                        + " · Stock actual: " + stockActual);
            } else {
                lblProductoKardex.setText("(todos los productos)");
                lblResumenKardex.setText("Total movimientos: " + movimientos.size()
                        + " · Entradas: " + entradas.stripTrailingZeros().toPlainString()
                        + " · Salidas: " + salidas.stripTrailingZeros().toPlainString());
            }
        } catch (NegocioException e) {
            Alertas.error(e.getMessage());
        } catch (SQLException e) {
            Alertas.error("Error al consultar el kardex: " + e.getMessage());
        }
    }

    @FXML
    private void verTodosKardex() {
        productoKardex = null;
        lblProductoKardex.setText("(todos los productos)");
        consultarKardex();
    }

    private void mostrarKardex(ProductoDTO producto) {
        productoKardex = producto;
        lblProductoKardex.setText(producto.getCodigoBarras() + " - " + producto.getNombre());
        pestanas.getSelectionModel().select(tabKardex);
        consultarKardex();
    }

    private void abrirEntrada(ProductoDTO producto) {
        FXMLLoader loader = Ventanas.cargar("/fxml/entrada_form.fxml");
        EntradaFormController form = loader.getController();
        form.setProducto(producto != null ? producto : productoSeleccionado());
        Ventanas.mostrarModal(loader, "Entrada de mercadería");
        if (form.isGuardado()) {
            refrescar();
        }
    }

    /** Producto con el que se está trabajando: el del kardex o el seleccionado en stock bajo. */
    private ProductoDTO productoSeleccionado() {
        if (pestanas.getSelectionModel().getSelectedItem() == tabKardex) {
            return productoKardex;
        }
        return tablaStockBajo.getSelectionModel().getSelectedItem();
    }

    private void refrescar() {
        cargarStockBajo();
        consultarKardex();
    }
}
