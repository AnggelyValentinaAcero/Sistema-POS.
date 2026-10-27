package com.pos.controller;

import com.pos.app.Ventanas;
import com.pos.dto.ProveedorDTO;
import com.pos.service.NegocioException;
import com.pos.service.ProveedorService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.sql.SQLException;

public class ProveedorFormController {

    @FXML private TextField txtNombre;
    @FXML private TextField txtDocumento;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtEmail;
    @FXML private TextField txtDireccion;
    @FXML private Label lblMensaje;
    @FXML private Button btnGuardar;

    private final ProveedorService proveedorService = new ProveedorService();
    private ProveedorDTO proveedor;
    private boolean guardado = false;

    /** Recibe el proveedor a editar, o null para crear uno nuevo. */
    public void setProveedor(ProveedorDTO proveedor) {
        this.proveedor = proveedor;
        if (proveedor != null) {
            txtNombre.setText(proveedor.getNombre());
            txtDocumento.setText(proveedor.getDocumento());
            txtTelefono.setText(proveedor.getTelefono());
            txtEmail.setText(proveedor.getEmail());
            txtDireccion.setText(proveedor.getDireccion());
        }
    }

    public boolean isGuardado() {
        return guardado;
    }

    @FXML
    private void guardar() {
        ProveedorDTO p = new ProveedorDTO();
        if (proveedor != null) {
            p.setId(proveedor.getId());
        }
        p.setNombre(txtNombre.getText());
        p.setDocumento(txtDocumento.getText());
        p.setTelefono(txtTelefono.getText());
        p.setEmail(txtEmail.getText());
        p.setDireccion(txtDireccion.getText());
        // El estado se asume Activo por defecto para no romper la inserción
        p.setActivo(proveedor != null ? proveedor.isActivo() : true);

        try {
            proveedorService.guardar(p);
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
