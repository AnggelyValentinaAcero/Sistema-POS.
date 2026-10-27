package com.pos.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * Configuración propia de ESTA computadora (terminal / caja):
 * - la conexión a MySQL y el nombre de la terminal;
 * - la impresora de recibos (cada caja puede tener una distinta).
 * Se guarda en el archivo sistema-pos.properties, en la carpeta del usuario de Windows
 * (ej: C:/Users/Juan). No se guarda junto al programa porque, una vez instalado en
 * "Archivos de programa", Windows no permite escribir en esa carpeta.
 * Lo que es común a todas las cajas (datos del negocio) está en la base de datos.
 */
public class ConfiguracionLocal {

    public static final String ARCHIVO = System.getProperty("user.home") + File.separator + "sistema-pos.properties";

    // Ubicación usada en versiones anteriores (carpeta desde donde se ejecutaba el programa)
    private static final String ARCHIVO_ANTERIOR = "pos.properties";

    // Tipos de impresora
    public static final String NINGUNA = "NINGUNA";
    public static final String SISTEMA = "SISTEMA"; // cualquier impresora de Windows (o PDF)
    public static final String ESCPOS = "ESCPOS";   // impresora térmica de recibos

    // Conexión a MySQL
    private boolean conexionGuardada; // false = primera vez que se ejecuta el sistema
    private String host;
    private int puerto;
    private String baseDatos;
    private String usuario;
    private String password;
    private String terminal;

    // Impresora de recibos
    private String tipoImpresora;
    private String nombreImpresora;
    private int anchoMm;                  // 58 u 80
    private boolean imprimirAutomatico;   // imprimir el recibo al terminar cada venta
    private boolean abrirCajon;           // abrir el cajón de dinero (solo ESC/POS)

    public ConfiguracionLocal() {
        // valores por defecto para una instalación local
        this.host = "localhost";
        this.puerto = 3306;
        this.baseDatos = "pos_db";
        this.usuario = "";
        this.password = "";
        this.terminal = nombreDelEquipo();
        this.tipoImpresora = NINGUNA;
        this.nombreImpresora = "";
        this.anchoMm = 80;
    }

    public ConfiguracionLocal(String host, int puerto, String baseDatos, String usuario, String password) {
        this();
        this.host = host;
        this.puerto = puerto;
        this.baseDatos = baseDatos;
        this.usuario = usuario;
        this.password = password;
    }

    /**
     * Lee la configuración desde el archivo. Si el archivo no existe devuelve los valores
     * por defecto con isConexionGuardada() = false (primera ejecución).
     */
    public static ConfiguracionLocal cargar() {
        Properties props = leerArchivo();
        ConfiguracionLocal config = new ConfiguracionLocal();

        config.setConexionGuardada(props.getProperty("db.host") != null);
        config.setHost(props.getProperty("db.host", "localhost"));
        config.setPuerto(Integer.parseInt(props.getProperty("db.puerto", "3306")));
        config.setBaseDatos(props.getProperty("db.nombre", "pos_db"));
        config.setUsuario(props.getProperty("db.usuario", ""));
        config.setPassword(props.getProperty("db.password", ""));
        config.setTerminal(props.getProperty("terminal", nombreDelEquipo()));

        config.setTipoImpresora(props.getProperty("impresora.tipo", NINGUNA));
        config.setNombreImpresora(props.getProperty("impresora.nombre", ""));
        config.setAnchoMm(Integer.parseInt(props.getProperty("impresora.ancho_mm", "80")));
        config.setImprimirAutomatico(Boolean.parseBoolean(props.getProperty("impresora.automatico", "false")));
        config.setAbrirCajon(Boolean.parseBoolean(props.getProperty("impresora.abrir_cajon", "false")));
        return config;
    }

    /**
     * Guarda SOLO los datos de conexión. Se parte del archivo existente para no
     * borrar la configuración de la impresora.
     */
    public void guardarConexion() throws IOException {
        Properties props = leerArchivo();
        props.setProperty("db.host", host);
        props.setProperty("db.puerto", String.valueOf(puerto));
        props.setProperty("db.nombre", baseDatos);
        props.setProperty("db.usuario", usuario);
        props.setProperty("db.password", password);
        props.setProperty("terminal", terminal);
        escribirArchivo(props);
        conexionGuardada = true;
    }

    /** Guarda SOLO la configuración de la impresora, sin tocar la conexión. */
    public void guardarImpresora() throws IOException {
        Properties props = leerArchivo();
        props.setProperty("impresora.tipo", tipoImpresora);
        props.setProperty("impresora.nombre", nombreImpresora == null ? "" : nombreImpresora);
        props.setProperty("impresora.ancho_mm", String.valueOf(anchoMm));
        props.setProperty("impresora.automatico", String.valueOf(imprimirAutomatico));
        props.setProperty("impresora.abrir_cajon", String.valueOf(abrirCajon));
        escribirArchivo(props);
    }

    /**
     * Lee el archivo de configuración. Si no existe pero hay uno de una versión anterior
     * (pos.properties), lee ese; al guardar se creará el nuevo. Si no hay ninguno,
     * devuelve un Properties vacío (primera ejecución).
     */
    private static Properties leerArchivo() {
        String ruta = ARCHIVO;
        if (!new File(ARCHIVO).exists() && new File(ARCHIVO_ANTERIOR).exists()) {
            ruta = ARCHIVO_ANTERIOR;
        }
        Properties props = new Properties();
        try (InputStream entrada = new FileInputStream(ruta)) {
            props.load(entrada);
        } catch (IOException e) {
            // el archivo todavía no existe: primera ejecución
        }
        return props;
    }

    private static void escribirArchivo(Properties props) throws IOException {
        try (OutputStream salida = new FileOutputStream(ARCHIVO)) {
            props.store(salida, "Configuracion del Sistema POS");
        }
    }

    public String getUrl() {
        return "jdbc:mysql://" + host + ":" + puerto + "/" + baseDatos
                + "?characterEncoding=UTF-8&allowPublicKeyRetrieval=true";
    }

    /**
     * Cantidad de caracteres por línea del recibo. Con letra de tamaño normal,
     * una impresora de 58 mm imprime 32 caracteres y una de 80 mm, 48.
     */
    public int getCaracteresPorLinea() {
        return anchoMm == 58 ? 32 : 48;
    }

    private static String nombreDelEquipo() {
        String nombre = System.getenv("COMPUTERNAME");
        if (nombre == null || nombre.isEmpty()) {
            nombre = "CAJA-1";
        }
        return nombre;
    }

    // ---- Getters y setters: conexión ----

    public boolean isConexionGuardada() {
        return conexionGuardada;
    }

    public void setConexionGuardada(boolean conexionGuardada) {
        this.conexionGuardada = conexionGuardada;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPuerto() {
        return puerto;
    }

    public void setPuerto(int puerto) {
        this.puerto = puerto;
    }

    public String getBaseDatos() {
        return baseDatos;
    }

    public void setBaseDatos(String baseDatos) {
        this.baseDatos = baseDatos;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTerminal() {
        return terminal;
    }

    public void setTerminal(String terminal) {
        this.terminal = terminal;
    }

    // ---- Getters y setters: impresora ----

    public String getTipoImpresora() {
        return tipoImpresora;
    }

    public void setTipoImpresora(String tipoImpresora) {
        this.tipoImpresora = tipoImpresora;
    }

    public String getNombreImpresora() {
        return nombreImpresora;
    }

    public void setNombreImpresora(String nombreImpresora) {
        this.nombreImpresora = nombreImpresora;
    }

    public int getAnchoMm() {
        return anchoMm;
    }

    public void setAnchoMm(int anchoMm) {
        this.anchoMm = anchoMm;
    }

    public boolean isImprimirAutomatico() {
        return imprimirAutomatico;
    }

    public void setImprimirAutomatico(boolean imprimirAutomatico) {
        this.imprimirAutomatico = imprimirAutomatico;
    }

    public boolean isAbrirCajon() {
        return abrirCajon;
    }

    public void setAbrirCajon(boolean abrirCajon) {
        this.abrirCajon = abrirCajon;
    }
}
