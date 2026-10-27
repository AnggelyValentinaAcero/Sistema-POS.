package com.pos.controller;

import com.pos.app.Ventanas;
import com.pos.dto.ClienteDTO;
import com.pos.service.ClienteService;
import com.pos.service.NegocioException;
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

public class ClientesController {

    @FXML private TextField txtBuscar;
    @FXML private CheckBox chkInactivos;
    @FXML private TableView<ClienteDTO> tabla;
    @FXML private TableColumn<ClienteDTO, String> colNombre;
    @FXML private TableColumn<ClienteDTO, String> colTipoDocumento;
    @FXML private TableColumn<ClienteDTO, String> colDocumento;
    @FXML private TableColumn<ClienteDTO, String> colTelefono;
    @FXML private TableColumn<ClienteDTO, String> colEmail;
    @FXML private TableColumn<ClienteDTO, String> colEstado;

    private final ClienteService clienteService = new ClienteService();

    @FXML
    private void initialize() {
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colTipoDocumento.setCellValueFactory(new PropertyValueFactory<>("tipoDocumento"));
        colDocumento.setCellValueFactory(new PropertyValueFactory<>("documento"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        buscar();
    }

    @FXML
    private void buscar() {
        try {
            tabla.getItems().setAll(clienteService.buscar(txtBuscar.getText(), chkInactivos.isSelected()));
        } catch (NegocioException e) {
            Alertas.error(e.getMessage());
        } catch (SQLException e) {
            Alertas.error("Error al buscar clientes: " + e.getMessage());
        }
    }

    @FXML
    private void nuevo() {
        abrirFormulario(null);
    }

    @FXML
    private void editar() {
        ClienteDTO seleccionado = tabla.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            Alertas.error("Seleccione un cliente de la tabla.");
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

    private void abrirFormulario(ClienteDTO cliente) {
        FXMLLoader loader = Ventanas.cargar("/fxml/cliente_form.fxml");
        ClienteFormController formulario = loader.getController();
        formulario.setCliente(cliente);

        Ventanas.mostrarModal(loader, cliente == null ? "Nuevo cliente" : "Editar cliente");

        if (formulario.isGuardado()) {
            buscar();
        }
    }
}
