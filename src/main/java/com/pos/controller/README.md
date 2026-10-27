# Paquete `controller`: controladores de las pantallas (la "C" de MVC)

Cada pantalla tiene **un archivo FXML** (la vista, en `src/main/resources/fxml/`) y **un
controlador** en esta carpeta. El controlador reacciona a los botones, lee los campos, llama al
service y muestra el resultado. **No tiene reglas de negocio ni SQL.**

```
Vista (usuarios.fxml)  ⇄  Controlador (UsuariosController)  →  Service (UsuarioService)
```

## Archivos

### Inicio y sesión

| Controlador | Vista (FXML) | Qué hace |
|---|---|---|
| `ConexionController` | `conexion.fxml` | Pide los datos de MySQL, prueba la conexión **en segundo plano** (`Task`) y los guarda en `ConfiguracionLocal`. |
| `LoginController` | `login.fxml` | Inicio de sesión (también en segundo plano). Si el usuario debe cambiar la contraseña, abre `cambio_password.fxml`. |
| `CambioPasswordController` | `cambio_password.fxml` | Cambio de contraseña; obligatorio en el primer ingreso. |
| `PrincipalController` | `principal.fxml` | Menú lateral con **solo los módulos que permite el rol** y el área central donde se carga cada módulo. |
| `ConfiguracionController` | `configuracion.fxml` | Cuenta y sesión actual (cambiar contraseña, cerrar sesión), datos del negocio (solo con `CONFIGURACION_GESTIONAR`) e impresora de esta caja. |

### Ventas y caja

| Controlador | Vista (FXML) | Qué hace |
|---|---|---|
| `PuntoVentaController` | `punto_venta.fxml` | Pantalla de venta: escaneo (`3*código` para cantidades), carrito, cliente, totales, F2 buscar y F12 cobrar. |
| `CobroController` | `cobro.fxml` | Método de pago y cálculo del cambio en vivo. Devuelve el `PagoDTO` (o `null` si se cancela). |
| `BuscarProductoController` | `buscar_producto.fxml` | Selector de productos reutilizable (punto de venta, inventario y kardex). |
| `BuscarClienteController` | `buscar_cliente.fxml` | Selector de clientes para la venta; permite registrar uno nuevo. |
| `CajaController` | `caja.fxml` | Abrir caja con monto inicial; resumen por método de pago; cerrar caja con efectivo contado y comprobante. |
| `HistorialVentasController` | `historial_ventas.fxml` | Ventas por rango de fechas con su detalle, ver o reimprimir el recibo y **anular** (con permiso). |
| `ReciboVistaController` | `recibo_vista.fxml` | Vista previa del recibo, con botón para imprimir. |

### Catálogo e inventario

| Controlador | Vista (FXML) | Qué hace |
|---|---|---|
| `ProductosController` | `productos.fxml` | Lista y búsqueda de productos; filas en rojo si hay stock bajo. El cajero solo consulta. |
| `ProductoFormController` | `producto_form.fxml` | Crear y editar productos. El stock solo se ingresa al crear. |
| `CategoriasController` | `categorias.fxml` | Tabla y formulario en la misma pantalla. |
| `ClientesController` / `ClienteFormController` | `clientes.fxml` / `cliente_form.fxml` | Lista y formulario de clientes. |
| `ProveedoresController` / `ProveedorFormController` | `proveedores.fxml` / `proveedor_form.fxml` | Lista y formulario de proveedores. |
| `InventarioController` | `inventario.fxml` | Pestañas "Stock bajo" y "Kardex" (por producto o de todos), y acceso a entradas y ajustes. |
| `EntradaFormController` | `entrada_form.fxml` | Entrada de mercadería (compra a proveedor). |
| `AjusteFormController` | `ajuste_form.fxml` | Ajuste con motivo (merma, dañado…) o conteo físico. |

### Administración

| Controlador | Vista (FXML) | Qué hace |
|---|---|---|
| `UsuariosController` / `UsuarioFormController` | `usuarios.fxml` / `usuario_form.fxml` | Usuarios: crear, editar, activar/desactivar y restablecer contraseña. |
| `ReportesController` | `reportes.fxml` | Dashboard: indicadores, gráfico de ventas por día (`BarChart`), métodos de pago (`PieChart`), top productos y ventas por cajero. |
| `AuditoriaController` | `auditoria.fxml` | Visor de la auditoría con filtros por fecha, usuario, acción y texto. |

## Partes clave del código

### Cómo se conecta el FXML con el controlador

```xml
<!-- usuarios.fxml -->
<VBox fx:controller="com.pos.controller.UsuariosController">
    <Button text="Nuevo" onAction="#nuevo"/>
    <TableView fx:id="tabla" .../>
</VBox>
```

```java
public class UsuariosController {
    @FXML private TableView<UsuarioDTO> tabla;   // mismo nombre que fx:id

    @FXML
    private void nuevo() { ... }                 // mismo nombre que onAction="#nuevo"

    @FXML
    private void initialize() { ... }            // JavaFX lo llama solo al cargar la vista
}
```

### Llenar una tabla con `PropertyValueFactory`

```java
// PropertyValueFactory busca el getter con ese nombre en UsuarioDTO
// (por ejemplo "nombreUsuario" -> getNombreUsuario())
colUsuario.setCellValueFactory(new PropertyValueFactory<>("nombreUsuario"));
colRol.setCellValueFactory(new PropertyValueFactory<>("rol"));
colEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));   // getEstado(): "Activo"/"Inactivo"
```

### Manejo de errores en todos los controladores

```java
try {
    tabla.getItems().setAll(usuarioService.listar());
} catch (NegocioException e) {
    Alertas.error(e.getMessage());                    // error de negocio: el mensaje es para el usuario
} catch (SQLException e) {
    Alertas.error("Error al cargar los usuarios: " + e.getMessage());   // error de base de datos
}
```

### Menú según el rol (`PrincipalController`)

```java
agregarOpcion("Punto de venta", Permiso.VENTAS_REALIZAR, "/fxml/punto_venta.fxml", null);
agregarOpcion("Usuarios", Permiso.USUARIOS_GESTIONAR, "/fxml/usuarios.fxml", null);
...

private void agregarOpcion(String titulo, Permiso permiso, String fxml, String fase) {
    if (permiso != null && !Sesion.tienePermiso(permiso)) {
        return;                                   // sin permiso: el botón no se crea
    }
    Button boton = new Button(titulo);
    boton.getStyleClass().add("boton-menu");
    boton.setMaxWidth(Double.MAX_VALUE);
    boton.setOnAction(evento -> {
        marcarSeleccionado(boton);
        if (fxml != null) {
            FXMLLoader loader = Ventanas.cargar(fxml);
            Parent vista = loader.getRoot();
            contenido.getChildren().setAll(vista);   // carga el módulo en el área central
        } else {
            mostrarMensaje(titulo, "Este módulo se implementará en la " + fase + ".");
        }
    });
    menu.getChildren().add(boton);
}
```

### Lector de código de barras (`PuntoVentaController.agregarPorCodigo`)

Los lectores de código de barras funcionan como un teclado: escriben el código y presionan
Enter. Por eso basta con un `TextField` con `onAction="#agregarPorCodigo"`:

```java
String texto = txtCodigo.getText().trim();
BigDecimal cantidad = BigDecimal.ONE;
String codigo = texto;
if (texto.contains("*")) {                       // formato "3*7750123" = 3 unidades
    String[] partes = texto.split("\\*");
    cantidad = new BigDecimal(partes[0].trim().replace(",", "."));
    codigo = partes[1].trim();
}
ProductoDTO producto = ventaService.buscarProductoPorCodigo(codigo);
agregarProducto(producto, cantidad);             // si ya estaba en la lista, suma la cantidad
```

### Tareas en segundo plano (`ConexionController`, `LoginController`)

Probar la conexión puede tardar varios segundos si el servidor no responde. Para que la ventana
no se congele se usa un `Task` de JavaFX:

```java
Task<String> tarea = new Task<>() {
    @Override
    protected String call() throws Exception {
        return Conexion.probar(config);          // corre en otro hilo
    }
};
tarea.setOnSucceeded(evento -> ...);             // vuelve al hilo de la pantalla
tarea.setOnFailed(evento -> ...);
new Thread(tarea).start();
```

### Pintar filas según su contenido (`setRowFactory`)

Se usa para el stock bajo en rojo (Productos), las entradas y salidas del kardex en verde y
rojo (Inventario) y las ventas anuladas tachadas (Historial):

```java
tabla.setRowFactory(t -> new TableRow<ProductoDTO>() {
    @Override
    protected void updateItem(ProductoDTO producto, boolean vacia) {
        super.updateItem(producto, vacia);
        getStyleClass().remove("fila-stock-bajo");
        if (!vacia && producto != null && producto.isActivo() && producto.isStockBajo()) {
            getStyleClass().add("fila-stock-bajo");   // el color está en css/estilos.css
        }
    }
});
```

### Formularios: no modificar el objeto de la tabla

```java
// ProductoFormController.guardar(): se arma un DTO NUEVO con los datos del formulario
ProductoDTO p = new ProductoDTO();
if (producto != null) {
    p.setId(producto.getId());   // solo se copia el id para saber qué registro actualizar
}
p.setNombre(txtNombre.getText());
...
```

Si se modificara directamente el producto de la tabla y el guardado fallara, la tabla mostraría
datos que no se guardaron.
