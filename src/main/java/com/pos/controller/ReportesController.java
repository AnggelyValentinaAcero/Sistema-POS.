package com.pos.controller;

import com.pos.dto.ItemReporteDTO;
import com.pos.dto.ResumenVentasDTO;
import com.pos.service.NegocioException;
import com.pos.service.ReporteService;
import com.pos.util.Alertas;
import com.pos.util.Textos;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Dashboard de reportes: indicadores principales, ventas por día,
 * productos más vendidos, métodos de pago y ventas por cajero.
 */
public class ReportesController {

    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;

    @FXML private Label lblCantidad;
    @FXML private Label lblTotal;
    @FXML private Label lblImpuestos;
    @FXML private Label lblReciboPromedio;
    @FXML private Label lblGanancia;
    @FXML private Label lblMargen;

    @FXML private BarChart<String, Number> graficoDias;
    @FXML private PieChart graficoMetodos;

    @FXML private TableView<ItemReporteDTO> tablaProductos;
    @FXML private TableColumn<ItemReporteDTO, String> colProducto;
    @FXML private TableColumn<ItemReporteDTO, String> colUnidades;
    @FXML private TableColumn<ItemReporteDTO, BigDecimal> colTotalProducto;
    @FXML private TableColumn<ItemReporteDTO, BigDecimal> colGananciaProducto;

    @FXML private TableView<ItemReporteDTO> tablaCajeros;
    @FXML private TableColumn<ItemReporteDTO, String> colCajero;
    @FXML private TableColumn<ItemReporteDTO, String> colVentasCajero;
    @FXML private TableColumn<ItemReporteDTO, BigDecimal> colTotalCajero;

    private final ReporteService reporteService = new ReporteService();

    @FXML
    private void initialize() {
        colProducto.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colUnidades.setCellValueFactory(new PropertyValueFactory<>("cantidadTexto"));
        colTotalProducto.setCellValueFactory(new PropertyValueFactory<>("total"));
        colGananciaProducto.setCellValueFactory(new PropertyValueFactory<>("ganancia"));

        colCajero.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colVentasCajero.setCellValueFactory(new PropertyValueFactory<>("cantidadTexto"));
        colTotalCajero.setCellValueFactory(new PropertyValueFactory<>("total"));

        esteMes();
    }

    // ---- Atajos de fechas ----

    @FXML
    private void hoy() {
        cambiarRango(LocalDate.now(), LocalDate.now());
    }

    @FXML
    private void ultimos7Dias() {
        cambiarRango(LocalDate.now().minusDays(6), LocalDate.now());
    }

    @FXML
    private void esteMes() {
        cambiarRango(LocalDate.now().withDayOfMonth(1), LocalDate.now());
    }

    @FXML
    private void mesAnterior() {
        LocalDate inicio = LocalDate.now().minusMonths(1).withDayOfMonth(1);
        LocalDate fin = inicio.withDayOfMonth(inicio.lengthOfMonth()); // último día de ese mes
        cambiarRango(inicio, fin);
    }

    private void cambiarRango(LocalDate desde, LocalDate hasta) {
        dpDesde.setValue(desde);
        dpHasta.setValue(hasta);
        consultar();
    }

    @FXML
    private void consultar() {
        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();
        try {
            mostrarResumen(reporteService.resumen(desde, hasta));
            mostrarVentasPorDia(reporteService.ventasPorDia(desde, hasta));
            mostrarMetodosPago(reporteService.ventasPorMetodoPago(desde, hasta));
            tablaProductos.getItems().setAll(reporteService.productosMasVendidos(desde, hasta));
            tablaCajeros.getItems().setAll(reporteService.ventasPorCajero(desde, hasta));
        } catch (NegocioException e) {
            Alertas.error(e.getMessage());
        } catch (SQLException e) {
            Alertas.error("Error al generar los reportes: " + e.getMessage());
        }
    }

    private void mostrarResumen(ResumenVentasDTO r) {
        lblCantidad.setText(String.valueOf(r.getCantidadVentas()));
        lblTotal.setText(Textos.moneda(r.getTotalVendido()));
        lblImpuestos.setText("Impuestos: " + Textos.moneda(r.getTotalImpuestos()));
        lblReciboPromedio.setText(Textos.moneda(r.getReciboPromedio()));
        lblGanancia.setText(Textos.moneda(r.getGanancia()));
        lblMargen.setText("Margen: " + r.getMargenPorcentaje() + " % sobre la venta neta");
    }

    private void mostrarVentasPorDia(List<ItemReporteDTO> dias) {
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM");
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        for (ItemReporteDTO dia : dias) {
            String etiqueta = LocalDate.parse(dia.getNombre()).format(formato);
            serie.getData().add(new XYChart.Data<>(etiqueta, dia.getTotal()));
        }
        graficoDias.getData().clear();
        graficoDias.getData().add(serie);
    }

    private void mostrarMetodosPago(List<ItemReporteDTO> metodos) {
        graficoMetodos.getData().clear();
        for (ItemReporteDTO metodo : metodos) {
            String etiqueta = metodo.getNombre() + "\n" + Textos.moneda(metodo.getTotal());
            graficoMetodos.getData().add(new PieChart.Data(etiqueta, metodo.getTotal().doubleValue()));
        }
    }
}
