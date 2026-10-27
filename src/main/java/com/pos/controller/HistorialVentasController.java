package com.pos.controller;

import com.pos.app.Ventanas;
import com.pos.dto.DetalleVentaDTO;
import com.pos.dto.VentaDTO;
import com.pos.service.NegocioException;
import com.pos.service.ReciboService;
import com.pos.service.VentaService;
import com.pos.util.Alertas;
import com.pos.util.Textos;
import com.pos.util.Permiso;
import com.pos.util.Sesion;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Consulta de ventas por rango de fechas, con el detalle de la venta seleccionada.
 */
public class HistorialVentasController {

    @FXML private Button btnAnular;
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private Label lblResumen;
    @FXML private Label lblSoloPropias;
    @FXML private TableView<VentaDTO> tablaVentas;
    @FXML private TableColumn<VentaDTO, Long> colNumero;
    @FXML private TableColumn<VentaDTO, String> colFecha;
    @FXML private TableColumn<VentaDTO, String> colCajero;
    @FXML private TableColumn<VentaDTO, String> colCliente;
    @FXML private TableColumn<VentaDTO, String> colMetodo;
    @FXML private TableColumn<VentaDTO, BigDecimal> colTotal;
    @FXML private TableColumn<VentaDTO, String> colEstado;
    @FXML private Label lblDetalle;
    @FXML private TableView<DetalleVentaDTO> tablaDetalle;
    @FXML private TableColumn<DetalleVentaDTO, String> colDetCodigo;
    @FXML private TableColumn<DetalleVentaDTO, String> colDetProducto;
    @FXML private TableColumn<DetalleVentaDTO, String> colDetCantidad;
    @FXML private TableColumn<DetalleVentaDTO, BigDecimal> colDetPrecio;
    @FXML private TableColumn<DetalleVentaDTO, BigDecimal> colDetSubtotal;

    private final VentaService ventaService = new VentaService();
    private final ReciboService reciboService = new ReciboService();

    @FXML
    private void initialize() {
        colNumero.setCellValueFactory(new PropertyValueFactory<>("id"));
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaTexto"));
        colCajero.setCellValueFactory(new PropertyValueFactory<>("usuario"));
        colCliente.setCellValueFactory(new PropertyValueFactory<>("clienteTexto"));
        colMetodo.setCellValueFactory(new PropertyValueFactory<>("metodoPago"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));

        colDetCodigo.setCellValueFactory(new PropertyValueFactory<>("codigoBarras"));
        colDetProducto.setCellValueFactory(new PropertyValueFactory<>("nombreProducto"));
        colDetCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidadTexto"));
        colDetPrecio.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        colDetSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));

        // al seleccionar una venta se carga su detalle
        tablaVentas.getSelectionModel().selectedItemProperty().addListener((obs, anterior, venta) -> {
            if (venta != null) {
                mostrarDetalle(venta);
            }
        });

        // las ventas anuladas se muestran en gris y tachadas
        tablaVentas.setRowFactory(t -> new TableRow<VentaDTO>() {
            @Override
            protected void updateItem(VentaDTO venta, boolean vacia) {
                super.updateItem(venta, vacia);
                getStyleClass().remove("fila-anulada");
                if (!vacia && venta != null && venta.isAnulada()) {
                    getStyleClass().add("fila-anulada");
                }
            }
        });

        // solo quien tiene permiso de anular ve el botón
        btnAnular.setVisible(Sesion.tienePermiso(Permiso.VENTAS_ANULAR));
        btnAnular.setManaged(Sesion.tienePermiso(Permiso.VENTAS_ANULAR));

        // el cajero (sin permiso de reportes) solo ve sus propias ventas
        boolean soloPropias = !Sesion.tienePermiso(Permiso.REPORTES_VER);
        lblSoloPropias.setVisible(soloPropias);
        lblSoloPropias.setManaged(soloPropias);

        dpDesde.setValue(LocalDate.now());
        dpHasta.setValue(LocalDate.now());
        buscar();
    }

    @FXML
    private void buscar() {
        tablaDetalle.getItems().clear();
        lblDetalle.setText("Seleccione una venta para ver su detalle");
        try {
            List<VentaDTO> ventas = ventaService.listarVentas(dpDesde.getValue(), dpHasta.getValue());
            tablaVentas.getItems().setAll(ventas);

            // total del período (solo ventas completadas)
            BigDecimal total = BigDecimal.ZERO;
            int cantidad = 0;
            for (VentaDTO v : ventas) {
                if ("COMPLETADA".equals(v.getEstado())) {
                    total = total.add(v.getTotal());
                    cantidad++;
                }
            }
            lblResumen.setText(cantidad + " ventas · Total: " + Textos.moneda(total));
        } catch (NegocioException e) {
            Alertas.error(e.getMessage());
        } catch (SQLException e) {
            Alertas.error("Error al consultar las ventas: " + e.getMessage());
        }
    }

    @FXML
    private void anularVenta() {
        VentaDTO venta = tablaVentas.getSelectionModel().getSelectedItem();
        if (venta == null) {
            Alertas.error("Seleccione una venta de la tabla.");
            return;
        }
        if (venta.isAnulada()) {
            Alertas.error("La venta ya está anulada.");
            return;
        }

        TextInputDialog dialogo = new TextInputDialog();
        dialogo.setTitle("Anular venta");
        dialogo.setHeaderText("Anular la venta N° " + venta.getId() + " por " + Textos.moneda(venta.getTotal())
                + ".\nLos productos volverán al stock. Esta acción no se puede deshacer.");
        dialogo.setContentText("Motivo:");
        Optional<String> motivo = dialogo.showAndWait();
        if (motivo.isEmpty()) {
            return; // canceló
        }

        try {
            ventaService.anularVenta(venta.getId(), motivo.get());
            Alertas.info("Venta anulada", "La venta N° " + venta.getId() + " fue anulada y el stock devuelto."
                    + "\nSi fue en efectivo, entregue " + Textos.moneda(venta.getTotal()) + " al cliente.");
            buscar();
        } catch (NegocioException e) {
            Alertas.error(e.getMessage());
        } catch (SQLException e) {
            Alertas.error("Error al anular la venta: " + e.getMessage());
        }
    }

    @FXML
    private void verRecibo() {
        VentaDTO venta = tablaVentas.getSelectionModel().getSelectedItem();
        if (venta == null) {
            Alertas.error("Seleccione una venta de la tabla.");
            return;
        }
        try {
            List<String> lineas = reciboService.reciboVenta(venta.getId(), true);
            FXMLLoader loader = Ventanas.cargar("/fxml/recibo_vista.fxml");
            ReciboVistaController vista = loader.getController();
            vista.setLineas(lineas);
            Ventanas.mostrarModal(loader, "Recibo venta N° " + venta.getId());
        } catch (NegocioException e) {
            Alertas.error(e.getMessage());
        } catch (SQLException e) {
            Alertas.error("Error al generar el recibo: " + e.getMessage());
        }
    }

    @FXML
    private void verTicket() {
        verRecibo();
    }

    private void mostrarDetalle(VentaDTO venta) {
        try {
            tablaDetalle.getItems().setAll(ventaService.listarDetalles(venta.getId()));
            String texto = "Venta N° " + venta.getId() + " · " + venta.getFechaTexto()
                    + " · Subtotal " + Textos.moneda(venta.getSubtotal())
                    + " · Impuestos " + Textos.moneda(venta.getImpuesto())
                    + " · Total " + Textos.moneda(venta.getTotal());
            if (venta.isAnulada()) {
                texto += "   |   ANULADA: " + venta.getMotivoAnulacion();
            }
            lblDetalle.setText(texto);
        } catch (SQLException e) {
            Alertas.error("Error al cargar el detalle: " + e.getMessage());
        }
    }
}
