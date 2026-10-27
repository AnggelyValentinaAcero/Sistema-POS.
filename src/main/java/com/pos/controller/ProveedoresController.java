package com.pos.controller;

import com.pos.app.Ventanas;
import com.pos.dto.ProveedorDTO;
import com.pos.service.NegocioException;
import com.pos.service.ProveedorService;
import com.pos.util.Alertas;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;

import java.sql.SQLException;

public class ProveedoresController {

    @FXML private TextField txtBuscar;
    @FXML private CheckBox chkInactivos;
    @FXML private TableView<ProveedorDTO> tabla;
    @FXML private TableColumn<ProveedorDTO, String> colNombre;
    @FXML private TableColumn<ProveedorDTO, String> colDocumento;
    @FXML private TableColumn<ProveedorDTO, String> colTelefono;
    @FXML private TableColumn<ProveedorDTO, String> colEmail;
    @FXML private TableColumn<ProveedorDTO, String> colEstado;

    private final ProveedorService proveedorService = new ProveedorService();

    @FXML
    private void initialize() {
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colDocumento.setCellValueFactory(new PropertyValueFactory<>("documento"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        buscar();
    }

    @FXML
    private void buscar() {
        try {
            tabla.getItems().setAll(proveedorService.buscar(txtBuscar.getText(), chkInactivos.isSelected()));
        } catch (NegocioException e) {
            Alertas.error(e.getMessage());
        } catch (SQLException e) {
            Alertas.error("Error al buscar proveedores: " + e.getMessage());
        }
    }

    @FXML
    private void nuevo() {
        abrirFormulario(null);
    }

    @FXML
    private void editar() {
        ProveedorDTO seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            Alertas.error("Seleccione un proveedor de la tabla.");
            return;
        }
        abrirFormulario(seleccionado);
    }

    @FXML
    private void clicTabla(MouseEvent evento) {
        if (evento.getClickCount() == 2 && tabla.getSelectionModel().getSelectedItem() != null) {
            editar();
        }
    }

    private void abrirFormulario(ProveedorDTO proveedor) {
        FXMLLoader loader = Ventanas.cargar("/fxml/proveedor_form.fxml");
        ProveedorFormController formulario = loader.getController();
        formulario.setProveedor(proveedor);

        Ventanas.mostrarModal(loader, proveedor == null ? "Nuevo proveedor" : "Editar proveedor");

        if (formulario.isGuardado()) {
            buscar();
        }
    }
}
