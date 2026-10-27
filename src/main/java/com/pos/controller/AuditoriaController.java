package com.pos.controller;

import com.pos.dto.AuditoriaDTO;
import com.pos.dto.UsuarioDTO;
import com.pos.service.AuditoriaService;
import com.pos.service.NegocioException;
import com.pos.util.Alertas;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Visor del registro de auditoría con filtros por fecha, usuario, acción y texto.
 */
public class AuditoriaController {

    private static final String TODAS = "(Todas)";

    @FXML private Label lblResultado;
    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private ComboBox<UsuarioDTO> cmbUsuario;
    @FXML private ComboBox<String> cmbAccion;
    @FXML private TextField txtTexto;
    @FXML private TableView<AuditoriaDTO> tabla;
    @FXML private TableColumn<AuditoriaDTO, String> colFecha;
    @FXML private TableColumn<AuditoriaDTO, String> colUsuario;
    @FXML private TableColumn<AuditoriaDTO, String> colTerminal;
    @FXML private TableColumn<AuditoriaDTO, String> colAccion;
    @FXML private TableColumn<AuditoriaDTO, String> colReferencia;
    @FXML private TableColumn<AuditoriaDTO, String> colDetalle;

    private final AuditoriaService auditoriaService = new AuditoriaService();
    private UsuarioDTO todosLosUsuarios;

    @FXML
    private void initialize() {
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaTexto"));
        colUsuario.setCellValueFactory(new PropertyValueFactory<>("usuario"));
        colTerminal.setCellValueFactory(new PropertyValueFactory<>("terminal"));
        colAccion.setCellValueFactory(new PropertyValueFactory<>("accion"));
        colReferencia.setCellValueFactory(new PropertyValueFactory<>("referencia"));
        colDetalle.setCellValueFactory(new PropertyValueFactory<>("detalle"));

        // opción "(Todos)" del filtro de usuario: un usuario ficticio con id 0
        todosLosUsuarios = new UsuarioDTO();
        todosLosUsuarios.setNombreUsuario("(Todos)");

        try {
            cmbUsuario.getItems().add(todosLosUsuarios);
            cmbUsuario.getItems().addAll(auditoriaService.listarUsuarios());
            cmbAccion.getItems().add(TODAS);
            cmbAccion.getItems().addAll(auditoriaService.listarAcciones());
        } catch (NegocioException e) {
            Alertas.error(e.getMessage());
            return;
        } catch (SQLException e) {
            Alertas.error("Error al cargar los filtros: " + e.getMessage());
            return;
        }
        limpiar();
    }

    @FXML
    private void limpiar() {
        dpDesde.setValue(LocalDate.now().minusDays(7));
        dpHasta.setValue(LocalDate.now());
        cmbUsuario.setValue(todosLosUsuarios);
        cmbAccion.setValue(TODAS);
        txtTexto.clear();
        buscar();
    }

    @FXML
    private void buscar() {
        Integer usuarioId = null;
        if (cmbUsuario.getValue() != null) {
            usuarioId = cmbUsuario.getValue().getId(); // 0 = todos
        }
        String accion = cmbAccion.getValue();
        if (TODAS.equals(accion)) {
            accion = null;
        }

        try {
            List<AuditoriaDTO> registros = auditoriaService.buscar(dpDesde.getValue(), dpHasta.getValue(),
                    usuarioId, accion, txtTexto.getText());
            tabla.getItems().setAll(registros);
            String texto = registros.size() + " registros";
            if (registros.size() == AuditoriaService.MAXIMO_REGISTROS) {
                texto += " (se muestran los " + AuditoriaService.MAXIMO_REGISTROS + " más recientes; use los filtros)";
            }
            lblResultado.setText(texto);
        } catch (NegocioException e) {
            Alertas.error(e.getMessage());
        } catch (SQLException e) {
            Alertas.error("Error al consultar la auditoría: " + e.getMessage());
        }
    }
}
