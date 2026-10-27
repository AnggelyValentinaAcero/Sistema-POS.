package com.pos.controller;

import com.pos.app.Ventanas;
import com.pos.dto.ClienteDTO;
import com.pos.dto.DetalleVentaDTO;
import com.pos.dto.PagoDTO;
import com.pos.dto.ProductoDTO;
import com.pos.dto.TurnoCajaDTO;
import com.pos.dto.VentaDTO;
import com.pos.service.CajaService;
import com.pos.service.NegocioException;
import com.pos.service.ReciboService;
import com.pos.service.VentaService;
import com.pos.util.Alertas;
import com.pos.util.Textos;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Pantalla de venta: se escanean los productos, se arma la lista (carrito)
 * y al cobrar se registra la venta en la base de datos.
 */
public class PuntoVentaController {

    @FXML private Label lblTurno;
    @FXML private Label lblCajaCerrada;
    @FXML private HBox panelCodigo;
    @FXML private TextField txtCodigo;
    @FXML private TableView<DetalleVentaDTO> tabla;
    @FXML private TableColumn<DetalleVentaDTO, String> colCodigo;
    @FXML private TableColumn<DetalleVentaDTO, String> colProducto;
    @FXML private TableColumn<DetalleVentaDTO, String> colCantidad;
    @FXML private TableColumn<DetalleVentaDTO, BigDecimal> colPrecio;
    @FXML private TableColumn<DetalleVentaDTO, BigDecimal> colSubtotal;
    @FXML private VBox panelDerecho;
    @FXML private Label lblCliente;
    @FXML private Label lblSubtotal;
    @FXML private Label lblImpuesto;
    @FXML private Label lblTotal;

    private final VentaService ventaService = new VentaService();
    private final CajaService cajaService = new CajaService();
    private final ReciboService reciboService = new ReciboService();

    private VentaDTO venta;        // la venta que se está armando
    private ClienteDTO cliente;    // null = consumidor final

    @FXML
    private void initialize() {
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigoBarras"));
        colProducto.setCellValueFactory(new PropertyValueFactory<>("nombreProducto"));
        colCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidadTexto"));
        colPrecio.setCellValueFactory(new PropertyValueFactory<>("precioUnitario"));
        colSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));

        verificarCaja();
        nuevaVenta();
    }

    /** Si la caja está cerrada se bloquea la pantalla. */
    private void verificarCaja() {
        try {
            TurnoCajaDTO turno = cajaService.buscarTurnoAbierto();
            boolean abierta = (turno != null);
            lblCajaCerrada.setVisible(!abierta);
            lblCajaCerrada.setManaged(!abierta);
            panelCodigo.setDisable(!abierta);
            panelDerecho.setDisable(!abierta);
            if (abierta) {
                lblTurno.setText("Turno #" + turno.getId() + " · " + turno.getTerminal());
            }
        } catch (SQLException e) {
            Alertas.error("Error al consultar la caja: " + e.getMessage());
        }
    }

    private void nuevaVenta() {
        venta = new VentaDTO();
        cliente = null;
        lblCliente.setText("Consumidor final");
        tabla.getItems().clear();
        actualizarTotales();
        // el cursor vuelve al campo de código, listo para el siguiente escaneo
        Platform.runLater(() -> txtCodigo.requestFocus());
    }

    /** Atajos de teclado: F2 buscar producto, F12 cobrar. */
    @FXML
    private void teclaPresionada(KeyEvent evento) {
        if (evento.getCode() == KeyCode.F2) {
            buscarProducto();
            evento.consume();
        } else if (evento.getCode() == KeyCode.F12) {
            cobrar();
            evento.consume();
        }
    }

    /**
     * Se ejecuta al presionar Enter en el campo de código. Los lectores de código de
     * barras funcionan como un teclado: escriben el código y presionan Enter.
     * Acepta el formato "cantidad*codigo" (ej: 3*7750123) para vender varias unidades.
     */
    @FXML
    private void agregarPorCodigo() {
        String texto = txtCodigo.getText().trim();
        txtCodigo.clear();
        if (texto.isEmpty()) {
            return;
        }

        BigDecimal cantidad = BigDecimal.ONE;
        String codigo = texto;
        if (texto.contains("*")) {
            String[] partes = texto.split("\\*");
            try {
                cantidad = new BigDecimal(partes[0].trim().replace(",", "."));
                codigo = partes[1].trim();
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                Alertas.error("Formato inválido. Use cantidad*código, por ejemplo 3*7750123.");
                return;
            }
        }

        try {
            ProductoDTO producto = ventaService.buscarProductoPorCodigo(codigo);
            agregarProducto(producto, cantidad);
        } catch (NegocioException e) {
            Alertas.error(e.getMessage());
        } catch (SQLException e) {
            Alertas.error("Error al buscar el producto: " + e.getMessage());
        }
        txtCodigo.requestFocus();
    }

    @FXML
    private void buscarProducto() {
        FXMLLoader loader = Ventanas.cargar("/fxml/buscar_producto.fxml");
        BuscarProductoController buscador = loader.getController();
        Ventanas.mostrarModal(loader, "Buscar producto");

        if (buscador.getSeleccionado() != null) {
            agregarProducto(buscador.getSeleccionado(), BigDecimal.ONE);
        }
        txtCodigo.requestFocus();
    }

    /** Agrega el producto a la venta. Si ya estaba en la lista, suma la cantidad. */
    private void agregarProducto(ProductoDTO producto, BigDecimal cantidad) {
        if (cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            Alertas.error("La cantidad debe ser mayor a 0.");
            return;
        }

        DetalleVentaDTO linea = buscarLinea(producto.getId());
        BigDecimal cantidadTotal = cantidad;
        if (linea != null) {
            cantidadTotal = linea.getCantidad().add(cantidad);
        }

        // aviso temprano; la validación definitiva la hace VentaService al cobrar
        if (cantidadTotal.compareTo(producto.getStock()) > 0) {
            Alertas.error("Stock insuficiente de " + producto.getNombre() + ". Disponible: "
                    + producto.getStock().stripTrailingZeros().toPlainString());
            return;
        }

        if (linea == null) {
            linea = new DetalleVentaDTO();
            linea.setProductoId(producto.getId());
            linea.setCodigoBarras(producto.getCodigoBarras());
            linea.setNombreProducto(producto.getNombre());
            linea.setUnidadMedida(producto.getUnidadMedida());
            linea.setPrecioUnitario(producto.getPrecioVenta());
            linea.setPorcentajeImpuesto(producto.getPorcentajeImpuesto());
            tabla.getItems().add(linea);
        }
        linea.setCantidad(cantidadTotal);

        tabla.getSelectionModel().select(linea);
        actualizarTotales();
    }

    @FXML
    private void cambiarCantidad() {
        DetalleVentaDTO linea = tabla.getSelectionModel().getSelectedItem();
        if (linea == null) {
            Alertas.error("Seleccione un producto de la lista.");
            return;
        }
        TextInputDialog dialogo = new TextInputDialog(linea.getCantidadTexto());
        dialogo.setTitle("Cambiar cantidad");
        dialogo.setHeaderText(linea.getNombreProducto());
        dialogo.setContentText("Nueva cantidad:");
        Optional<String> respuesta = dialogo.showAndWait();

        if (respuesta.isPresent()) {
            try {
                BigDecimal cantidad = new BigDecimal(respuesta.get().trim().replace(",", "."));
                if (cantidad.compareTo(BigDecimal.ZERO) <= 0) {
                    Alertas.error("La cantidad debe ser mayor a 0.");
                    return;
                }
                linea.setCantidad(cantidad);
                actualizarTotales();
            } catch (NumberFormatException e) {
                Alertas.error("Ingrese un número válido.");
            }
        }
    }

    @FXML
    private void quitarProducto() {
        DetalleVentaDTO linea = tabla.getSelectionModel().getSelectedItem();
        if (linea == null) {
            Alertas.error("Seleccione un producto de la lista.");
            return;
        }
        tabla.getItems().remove(linea);
        actualizarTotales();
    }

    @FXML
    private void cancelarVenta() {
        if (tabla.getItems().isEmpty() || Alertas.confirmar("¿Cancelar la venta actual?")) {
            nuevaVenta();
        }
    }

    @FXML
    private void elegirCliente() {
        FXMLLoader loader = Ventanas.cargar("/fxml/buscar_cliente.fxml");
        BuscarClienteController buscador = loader.getController();
        Ventanas.mostrarModal(loader, "Elegir cliente");

        if (buscador.getSeleccionado() != null) {
            cliente = buscador.getSeleccionado();
            lblCliente.setText(cliente.getNombre());
        }
    }

    @FXML
    private void quitarCliente() {
        cliente = null;
        lblCliente.setText("Consumidor final");
    }

    @FXML
    private void cobrar() {
        if (panelDerecho.isDisabled()) {
            return; // caja cerrada
        }
        if (tabla.getItems().isEmpty()) {
            Alertas.error("Agregue al menos un producto.");
            return;
        }

        // 1. Ventana de cobro: método de pago y monto recibido
        FXMLLoader loader = Ventanas.cargar("/fxml/cobro.fxml");
        CobroController cobro = loader.getController();
        cobro.setTotal(venta.getTotal());
        Ventanas.mostrarModal(loader, "Cobrar");

        PagoDTO pago = cobro.getPago();
        if (pago == null) {
            return; // el cajero canceló el cobro
        }

        // 2. Se registra la venta (transacción en VentaService)
        venta.setDetalles(tabla.getItems());
        venta.setClienteId(cliente == null ? null : cliente.getId());
        venta.setPago(pago);
        try {
            long numero = ventaService.registrarVenta(venta);

            // 3. Recibo (la venta ya está guardada: si la impresión falla, solo se avisa)
            if (reciboService.imprimirAutomaticamente()) {
                try {
                    reciboService.imprimir(reciboService.reciboVenta(numero, false), true);
                } catch (NegocioException | SQLException e) {
                    Alertas.error("La venta se registró, pero no se pudo imprimir el recibo: " + e.getMessage()
                            + "\nPuede reimprimirlo desde el Historial de ventas.");
                }
            }

            String mensaje = "Venta N° " + numero + " registrada.\nTotal: " + Textos.moneda(venta.getTotal());
            if (pago.getCambio().compareTo(BigDecimal.ZERO) > 0) {
                mensaje += "\n\nCAMBIO: " + Textos.moneda(pago.getCambio());
            }
            Alertas.info("Venta registrada", mensaje);
            nuevaVenta();
        } catch (NegocioException e) {
            // ej: stock insuficiente. La venta no se guardó y el carrito se mantiene
            Alertas.error(e.getMessage());
            tabla.refresh();
        } catch (SQLException e) {
            Alertas.error("No se pudo registrar la venta: " + e.getMessage());
        }
    }

    private DetalleVentaDTO buscarLinea(int productoId) {
        for (DetalleVentaDTO linea : tabla.getItems()) {
            if (linea.getProductoId() == productoId) {
                return linea;
            }
        }
        return null;
    }

    private void actualizarTotales() {
        venta.setDetalles(tabla.getItems());
        ventaService.calcularTotales(venta);
        tabla.refresh(); // para que la tabla muestre las cantidades y subtotales nuevos

        lblSubtotal.setText(Textos.moneda(venta.getSubtotal()));
        lblImpuesto.setText(Textos.moneda(venta.getImpuesto()));
        lblTotal.setText(Textos.moneda(venta.getTotal()));
    }
}
