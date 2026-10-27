package com.pos.controller;

import com.pos.app.Ventanas;
import com.pos.config.ConfiguracionLocal;
import com.pos.dto.NegocioDTO;
import com.pos.dto.UsuarioDTO;
import com.pos.print.ImpresoraSistema;
import com.pos.print.ReciboTexto;
import com.pos.service.AuthService;
import com.pos.service.ConfiguracionService;
import com.pos.service.NegocioException;
import com.pos.service.ReciboService;
import com.pos.util.Alertas;
import com.pos.util.Textos;
import com.pos.util.Permiso;
import com.pos.util.Sesion;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Configuración de los datos del negocio y de la impresora de esta terminal,
 * además de la gestión de la cuenta y sesión actual.
 */
public class ConfiguracionController {

    // Textos que se muestran en el ComboBox de tipo de impresora
    private static final String TEXTO_NINGUNA = "Sin impresora";
    private static final String TEXTO_SISTEMA = "Impresora del sistema (Windows / PDF)";
    private static final String TEXTO_ESCPOS = "Impresora térmica de recibos (ESC/POS)";

    @FXML private Label lblUsuarioActual;
    @FXML private Label lblRolActual;
    @FXML private Label lblTerminalActual;
    @FXML private VBox panelNegocio;
    @FXML private Button btnGuardarNegocio;

    @FXML private TextField txtNombre;
    @FXML private TextField txtDocumento;
    @FXML private TextField txtDireccion;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtSimbolo;
    @FXML private TextField txtMensaje;

    @FXML private Label lblTerminal;
    @FXML private ComboBox<String> cmbTipo;
    @FXML private ComboBox<String> cmbImpresora;
    @FXML private ComboBox<String> cmbAncho;
    @FXML private CheckBox chkAutomatico;
    @FXML private CheckBox chkCajon;

    private final AuthService authService = new AuthService();
    private final ConfiguracionService configuracionService = new ConfiguracionService();
    private final ReciboService reciboService = new ReciboService();

    @FXML
    private void initialize() {
        cargarSesion();
        cargarNegocio();
        cargarImpresora();
    }

    private void cargarSesion() {
        UsuarioDTO usuario = Sesion.getUsuario();
        if (usuario != null) {
            lblUsuarioActual.setText(usuario.getNombreCompleto() + " (" + usuario.getNombreUsuario() + ")");
            lblRolActual.setText(usuario.getRol() != null ? usuario.getRol() : "-");
        }
        lblTerminalActual.setText(Sesion.getTerminal());

        if (!Sesion.tienePermiso(Permiso.CONFIGURACION_GESTIONAR)) {
            panelNegocio.setDisable(true);
        }
    }

    @FXML
    private void cambiarPassword() {
        FXMLLoader loader = Ventanas.cargar("/fxml/cambio_password.fxml");
        Ventanas.mostrarModal(loader, "Cambiar contraseña");
    }

    @FXML
    private void cerrarSesion() {
        if (Alertas.confirmar("¿Está seguro de que desea cerrar la sesión actual?")) {
            authService.cerrarSesion();
            Ventanas.cambiarVista("/fxml/login.fxml");
        }
    }

    private void cargarNegocio() {
        try {
            NegocioDTO negocio = configuracionService.obtenerNegocio();
            txtNombre.setText(negocio.getNombre());
            txtDocumento.setText(negocio.getDocumento());
            txtDireccion.setText(negocio.getDireccion());
            txtTelefono.setText(negocio.getTelefono());
            txtSimbolo.setText(negocio.getSimboloMoneda());
            txtMensaje.setText(negocio.getMensajePie());
        } catch (SQLException e) {
            Alertas.error("Error al cargar los datos del negocio: " + e.getMessage());
        }
    }

    private void cargarImpresora() {
        lblTerminal.setText("Terminal: " + Sesion.getTerminal());
        cmbTipo.getItems().addAll(TEXTO_NINGUNA, TEXTO_SISTEMA, TEXTO_ESCPOS);
        cmbImpresora.getItems().addAll(ImpresoraSistema.listarNombres());
        cmbAncho.getItems().addAll("58 mm", "80 mm");

        ConfiguracionLocal config = ConfiguracionLocal.cargar();
        if (ConfiguracionLocal.SISTEMA.equals(config.getTipoImpresora())) {
            cmbTipo.setValue(TEXTO_SISTEMA);
        } else if (ConfiguracionLocal.ESCPOS.equals(config.getTipoImpresora())) {
            cmbTipo.setValue(TEXTO_ESCPOS);
        } else {
            cmbTipo.setValue(TEXTO_NINGUNA);
        }
        cmbImpresora.setValue(config.getNombreImpresora());
        cmbAncho.setValue(config.getAnchoMm() + " mm");
        chkAutomatico.setSelected(config.isImprimirAutomatico());
        chkCajon.setSelected(config.isAbrirCajon());
        tipoCambiado();
    }

    @FXML
    private void tipoCambiado() {
        boolean sinImpresora = TEXTO_NINGUNA.equals(cmbTipo.getValue());
        cmbImpresora.setDisable(sinImpresora);
        chkAutomatico.setDisable(sinImpresora);
        chkCajon.setDisable(!TEXTO_ESCPOS.equals(cmbTipo.getValue()));
    }

    @FXML
    private void guardarNegocio() {
        NegocioDTO negocio = new NegocioDTO();
        negocio.setNombre(txtNombre.getText());
        negocio.setDocumento(txtDocumento.getText());
        negocio.setDireccion(txtDireccion.getText());
        negocio.setTelefono(txtTelefono.getText());
        negocio.setSimboloMoneda(txtSimbolo.getText());
        negocio.setMensajePie(txtMensaje.getText());
        try {
            configuracionService.guardarNegocio(negocio);
            Textos.setSimbolo(negocio.getSimboloMoneda().trim());
            Alertas.info("Configuración", "Datos del negocio guardados.");
        } catch (NegocioException e) {
            Alertas.error(e.getMessage());
        } catch (SQLException e) {
            Alertas.error("Error al guardar: " + e.getMessage());
        }
    }

    @FXML
    private void guardarImpresora() {
        try {
            leerImpresora().guardarImpresora();
            Alertas.info("Configuración", "Impresora guardada para esta terminal.");
        } catch (NegocioException e) {
            Alertas.error(e.getMessage());
        } catch (IOException e) {
            Alertas.error("No se pudo guardar la configuración: " + e.getMessage());
        }
    }

    /** Guarda la configuración actual e imprime un recibo corto de prueba. */
    @FXML
    private void imprimirPrueba() {
        try {
            ConfiguracionLocal config = leerImpresora();
            config.guardarImpresora();

            ReciboTexto t = new ReciboTexto(config.getCaracteresPorLinea());
            t.centrado(txtNombre.getText());
            t.separador();
            t.centrado("PRUEBA DE IMPRESION");
            t.texto("Ancho: " + config.getAnchoMm() + " mm (" + config.getCaracteresPorLinea() + " caracteres)");
            t.texto("Acentos: áéíóú ñ Ñ ¿? ¡!");
            t.columnas("Izquierda", "Derecha");
            t.separador();
            reciboService.imprimir(t.getLineas(), true);
        } catch (NegocioException e) {
            Alertas.error(e.getMessage());
        } catch (IOException e) {
            Alertas.error("No se pudo guardar la configuración: " + e.getMessage());
        }
    }

    private ConfiguracionLocal leerImpresora() throws NegocioException {
        ConfiguracionLocal config = new ConfiguracionLocal();
        String tipo = cmbTipo.getValue();
        if (TEXTO_SISTEMA.equals(tipo)) {
            config.setTipoImpresora(ConfiguracionLocal.SISTEMA);
        } else if (TEXTO_ESCPOS.equals(tipo)) {
            config.setTipoImpresora(ConfiguracionLocal.ESCPOS);
        } else {
            config.setTipoImpresora(ConfiguracionLocal.NINGUNA);
        }

        if (!ConfiguracionLocal.NINGUNA.equals(config.getTipoImpresora())
                && (cmbImpresora.getValue() == null || cmbImpresora.getValue().isEmpty())) {
            throw new NegocioException("Seleccione la impresora.");
        }
        config.setNombreImpresora(cmbImpresora.getValue());
        config.setAnchoMm("58 mm".equals(cmbAncho.getValue()) ? 58 : 80);
        config.setImprimirAutomatico(chkAutomatico.isSelected());
        config.setAbrirCajon(chkCajon.isSelected());
        return config;
    }
}
