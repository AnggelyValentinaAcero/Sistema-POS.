package com.pos.controller;

import com.pos.app.Ventanas;
import com.pos.dto.ClienteDTO;
import com.pos.service.ClienteService;
import com.pos.service.NegocioException;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.sql.SQLException;

public class ClienteFormController {

    @FXML private TextField txtNombre;
    @FXML private ComboBox<String> cmbTipoDocumento;
    @FXML private TextField txtDocumento;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtEmail;
    @FXML private TextField txtDireccion;
    @FXML private Label lblMensaje;
    @FXML private Button btnGuardar;

    private final ClienteService clienteService = new ClienteService();
    private ClienteDTO cliente;
    private boolean guardado = false;

    @FXML
    private void initialize() {
        cmbTipoDocumento.getItems().addAll(ClienteService.TIPOS_DOCUMENTO);
        cmbTipoDocumento.setValue(ClienteService.TIPOS_DOCUMENTO[0]);
    }

    /** Recibe el cliente a editar, o null para crear uno nuevo. */
    public void setCliente(ClienteDTO cliente) {
        this.cliente = cliente;
        if (cliente != null) {
            txtNombre.setText(cliente.getNombre());
            if (cliente.getTipoDocumento() != null) {
                cmbTipoDocumento.setValue(cliente.getTipoDocumento());
            }
            txtDocumento.setText(cliente.getDocumento());
            txtTelefono.setText(cliente.getTelefono());
            txtEmail.setText(cliente.getEmail());
            txtDireccion.setText(cliente.getDireccion());
        }
    }

    public boolean isGuardado() {
        return guardado;
    }

    /** Documento (o nombre si no tiene) del cliente guardado, para buscarlo después. */
    public String getTextoBusqueda() {
        if (!txtDocumento.getText().isBlank()) {
            return txtDocumento.getText().trim();
        }
        return txtNombre.getText().trim();
    }

    @FXML
    private void guardar() {
        ClienteDTO c = new ClienteDTO();
        if (cliente != null) {
            c.setId(cliente.getId());
        }
        c.setNombre(txtNombre.getText());
        c.setTipoDocumento(cmbTipoDocumento.getValue());
        c.setDocumento(txtDocumento.getText());
        c.setTelefono(txtTelefono.getText());
        c.setEmail(txtEmail.getText());
        c.setDireccion(txtDireccion.getText());
        // El estado se asume Activo por defecto para no romper la inserción
        c.setActivo(cliente != null ? cliente.isActivo() : true);

        try {
            clienteService.guardar(c);
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

    private void mostrarError(String mensaje) {
        lblMensaje.setText(mensaje);
        lblMensaje.setVisible(true);
        lblMensaje.setManaged(true);
        Ventanas.ajustarTamano(lblMensaje);
    }
}
