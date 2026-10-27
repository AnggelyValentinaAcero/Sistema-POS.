package com.pos.controller;

import com.pos.app.Ventanas;
import com.pos.dto.ClienteDTO;
import com.pos.service.ClienteService;
import com.pos.service.NegocioException;
import com.pos.util.Alertas;
import com.pos.util.Permiso;
import com.pos.util.Sesion;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;

import java.sql.SQLException;
import java.util.List;

/**
 * Ventana para asociar un cliente a la venta. Si no existe se puede registrar ahí mismo.
 */
public class BuscarClienteController {

    @FXML private TextField txtBuscar;
    @FXML private TableView<ClienteDTO> tabla;
    @FXML private TableColumn<ClienteDTO, String> colNombre;
    @FXML private TableColumn<ClienteDTO, String> colDocumento;
    @FXML private TableColumn<ClienteDTO, String> colTelefono;
    @FXML private Button btnSeleccionar;

    private final ClienteService clienteService = new ClienteService();
    private final ObservableList<ClienteDTO> listaClientes = FXCollections.observableArrayList();
    private ClienteDTO seleccionado;

    @FXML
    private void initialize() {
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colDocumento.setCellValueFactory(new PropertyValueFactory<>("documento"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));

        // Se asigna la lista observable vinculada a la tabla
        tabla.setItems(listaClientes);

        // Carga inicial automática condicionada a permisos
        if (Sesion.tienePermiso(Permiso.CLIENTES_GESTIONAR)) {
            buscar();
        }

        Platform.runLater(() -> txtBuscar.requestFocus());
    }

    public ClienteDTO getSeleccionado() {
        return seleccionado;
    }

    @FXML
    private void buscar() {
        try {
            String texto = (txtBuscar != null && txtBuscar.getText() != null) ? txtBuscar.getText().trim() : "";
            List<ClienteDTO> resultados = clienteService.buscar(texto, false);
            listaClientes.setAll(resultados);
            if (!listaClientes.isEmpty()) {
                tabla.getSelectionModel().selectFirst();
            }
        } catch (NegocioException e) {
            Alertas.error(e.getMessage());
        } catch (SQLException e) {
            Alertas.error("Error al buscar: " + e.getMessage());
        }
    }

    @FXML
    private void nuevoCliente() {
        FXMLLoader loader = Ventanas.cargar("/fxml/cliente_form.fxml");
        ClienteFormController formulario = loader.getController();
        formulario.setCliente(null);
        Ventanas.mostrarModal(loader, "Nuevo cliente");

        if (formulario.isGuardado()) {
            // se busca por el documento o nombre recién ingresado para mostrarlo en la lista
            txtBuscar.setText(formulario.getTextoBusqueda());
            buscar();
        }
    }

    @FXML
    private void clicTabla(MouseEvent evento) {
        if (evento.getClickCount() == 2) {
            seleccionar();
        }
    }

    @FXML
    private void seleccionar() {
        ClienteDTO cliente = tabla.getSelectionModel().getSelectedItem();
        if (cliente == null) {
            Alertas.error("Seleccione un cliente.");
            return;
        }
        seleccionado = cliente;
        Ventanas.cerrar(btnSeleccionar);
    }

    @FXML
    private void cancelar() {
        seleccionado = null;
        Ventanas.cerrar(btnSeleccionar);
    }
}
