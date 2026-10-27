package com.pos.service;

import com.pos.config.Conexion;
import com.pos.dao.AuditoriaDAO;
import com.pos.dao.CategoriaDAO;
import com.pos.dao.MovimientoInventarioDAO;
import com.pos.dao.ProductoDAO;
import com.pos.dao.ProveedorDAO;
import com.pos.dto.CategoriaDTO;
import com.pos.dto.ImpuestoDTO;
import com.pos.dto.ProductoDTO;
import com.pos.dto.ProveedorDTO;
import com.pos.util.Permiso;
import com.pos.util.Sesion;
import com.pos.util.Textos;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Lógica de negocio del catálogo: productos y sus categorías.
 */
public class ProductoService {

    private final ProductoDAO productoDAO = new ProductoDAO();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();
    private final ProveedorDAO proveedorDAO = new ProveedorDAO();
    private final MovimientoInventarioDAO movimientoDAO = new MovimientoInventarioDAO();
    private final AuditoriaDAO auditoriaDAO = new AuditoriaDAO();

    public List<ProductoDTO> buscar(String texto, boolean incluirInactivos) throws NegocioException, SQLException {
        if (!Sesion.tienePermiso(Permiso.PRODUCTOS_VER)) {
            throw new NegocioException("No tiene permiso para ver productos.");
        }
        String filtro = (texto == null) ? "" : texto.trim();
        try (Connection con = Conexion.getConexion()) {
            return productoDAO.buscar(con, filtro, incluirInactivos);
        }
    }

    // ---- Datos para llenar los ComboBox del formulario ----

    public List<CategoriaDTO> listarCategorias() throws SQLException {
        try (Connection con = Conexion.getConexion()) {
            return categoriaDAO.listar(con, false);
        }
    }

    public List<ImpuestoDTO> listarImpuestos() throws SQLException {
        try (Connection con = Conexion.getConexion()) {
            return productoDAO.listarImpuestos(con);
        }
    }

    public List<ProveedorDTO> listarProveedores() throws SQLException {
        try (Connection con = Conexion.getConexion()) {
            return proveedorDAO.buscar(con, "", false);
        }
    }

    /**
     * Crea un producto. Si tiene stock inicial, también se registra la entrada
     * en el kardex (movimientos_inventario). Ambas operaciones van en una misma
     * transacción: o se guardan las dos o ninguna.
     */
    public void crear(ProductoDTO producto) throws NegocioException, SQLException {
        verificarPermisoGestion();
        validar(producto);
        if (producto.getStock().compareTo(BigDecimal.ZERO) < 0) {
            throw new NegocioException("El stock inicial no puede ser negativo.");
        }

        Connection con = null;
        try {
            con = Conexion.getConexion();
            con.setAutoCommit(false);

            if (productoDAO.buscarPorCodigo(con, producto.getCodigoBarras()) != null) {
                throw new NegocioException("Ya existe un producto con el código " + producto.getCodigoBarras() + ".");
            }

            int id = productoDAO.insertar(con, producto);
            int usuarioId = Sesion.getUsuario().getId();

            if (producto.getStock().compareTo(BigDecimal.ZERO) > 0) {
                movimientoDAO.registrar(con, id, "ENTRADA", producto.getStock(), producto.getStock(),
                        producto.getPrecioCompra(), null, usuarioId, "Stock inicial");
            }
            auditoriaDAO.registrar(con, usuarioId, Sesion.getTerminal(), "PRODUCTO_CREADO", "productos",
                    String.valueOf(id), "Se creó el producto: " + producto.getNombre() + " (Código: " + producto.getCodigoBarras() + ")");

            con.commit();
        } catch (SQLException | NegocioException e) {
            Conexion.rollback(con);
            throw e;
        } finally {
            Conexion.cerrar(con);
        }
    }

    /**
     * Modifica los datos del producto (no el stock). Los cambios de precio quedan
     * registrados en auditoría con el valor anterior y el nuevo, para poder
     * detectar manipulaciones de precios.
     */
    public void actualizar(ProductoDTO producto) throws NegocioException, SQLException {
        verificarPermisoGestion();
        validar(producto);

        Connection con = null;
        try {
            con = Conexion.getConexion();
            con.setAutoCommit(false);

            ProductoDTO conMismoCodigo = productoDAO.buscarPorCodigo(con, producto.getCodigoBarras());
            if (conMismoCodigo != null && conMismoCodigo.getId() != producto.getId()) {
                throw new NegocioException("Ya existe otro producto con el código " + producto.getCodigoBarras() + ".");
            }

            ProductoDTO anterior = productoDAO.buscarPorId(con, producto.getId());
            productoDAO.actualizar(con, producto);

            int usuarioId = Sesion.getUsuario().getId();
            String id = String.valueOf(producto.getId());
            // compareTo se usa para comparar BigDecimal (equals también compara la escala: 10.0 != 10.00)
            boolean cambioPrecio = anterior.getPrecioVenta().compareTo(producto.getPrecioVenta()) != 0
                    || anterior.getPrecioCompra().compareTo(producto.getPrecioCompra()) != 0;
            if (cambioPrecio) {
                String detalle = "venta: " + anterior.getPrecioVenta() + " -> " + producto.getPrecioVenta()
                        + ", compra: " + anterior.getPrecioCompra() + " -> " + producto.getPrecioCompra();
                auditoriaDAO.registrar(con, usuarioId, Sesion.getTerminal(), "PRECIO_MODIFICADO", "productos", id, detalle);
            }
            auditoriaDAO.registrar(con, usuarioId, Sesion.getTerminal(), "PRODUCTO_MODIFICADO", "productos", id,
                    "Se actualizó el producto ID: " + producto.getId() + " (" + producto.getNombre() + ")");

            con.commit();
        } catch (SQLException | NegocioException e) {
            Conexion.rollback(con);
            throw e;
        } finally {
            Conexion.cerrar(con);
        }
    }

    /** Activa o desactiva un producto. Los productos no se borran porque las ventas pasadas los referencian. */
    public void cambiarEstado(ProductoDTO producto, boolean activo) throws NegocioException, SQLException {
        verificarPermisoGestion();
        try (Connection con = Conexion.getConexion()) {
            productoDAO.cambiarEstado(con, producto.getId(), activo);
            auditoriaDAO.registrar(con, Sesion.getUsuario().getId(), Sesion.getTerminal(),
                    activo ? "PRODUCTO_ACTIVADO" : "PRODUCTO_DESACTIVADO", "productos",
                    String.valueOf(producto.getId()), "Se " + (activo ? "activó" : "desactivó") + " el producto ID: "
                            + producto.getId() + " (" + producto.getNombre() + ")");
        }
    }

    private void validar(ProductoDTO p) throws NegocioException {
        p.setCodigoBarras(Textos.limpiar(p.getCodigoBarras()));
        p.setNombre(Textos.limpiar(p.getNombre()));
        p.setDescripcion(Textos.limpiar(p.getDescripcion()));

        if (p.getCodigoBarras() == null) {
            throw new NegocioException("Ingrese el código de barras.");
        }
        if (p.getNombre() == null) {
            throw new NegocioException("Ingrese el nombre del producto.");
        }
        if (p.getCategoriaId() <= 0) {
            throw new NegocioException("Seleccione una categoría.");
        }
        if (p.getImpuestoId() <= 0) {
            throw new NegocioException("Seleccione un impuesto.");
        }
        if (p.getPrecioCompra().compareTo(BigDecimal.ZERO) < 0 || p.getPrecioVenta().compareTo(BigDecimal.ZERO) <= 0) {
            throw new NegocioException("El precio de venta debe ser mayor a 0 y el de compra no puede ser negativo.");
        }
        if (p.getPrecioVenta().compareTo(p.getPrecioCompra()) < 0) {
            throw new NegocioException("El precio de venta no puede ser menor al precio de compra.");
        }
        if (p.getStockMinimo().compareTo(BigDecimal.ZERO) < 0) {
            throw new NegocioException("El stock mínimo no puede ser negativo.");
        }
    }

    // ---- Categorías (pantalla "Categorías") ----

    /** Todas las categorías, también las inactivas, para administrarlas. */
    public List<CategoriaDTO> listarTodasLasCategorias() throws NegocioException, SQLException {
        verificarPermisoGestion();
        try (Connection con = Conexion.getConexion()) {
            return categoriaDAO.listar(con, true);
        }
    }

    /** Crea la categoría si es nueva (id = 0) o la actualiza si ya existe. */
    public void guardarCategoria(CategoriaDTO categoria) throws NegocioException, SQLException {
        verificarPermisoGestion();
        categoria.setNombre(Textos.limpiar(categoria.getNombre()));
        categoria.setDescripcion(Textos.limpiar(categoria.getDescripcion()));
        if (categoria.getNombre() == null) {
            throw new NegocioException("Ingrese el nombre de la categoría.");
        }

        Connection con = null;
        try {
            con = Conexion.getConexion();
            con.setAutoCommit(false);

            // no puede haber dos categorías con el mismo nombre
            CategoriaDTO existente = categoriaDAO.buscarPorNombre(con, categoria.getNombre());
            if (existente != null && existente.getId() != categoria.getId()) {
                throw new NegocioException("Ya existe una categoría llamada '" + categoria.getNombre() + "'.");
            }

            Integer usuarioId = Sesion.getUsuario() != null ? Sesion.getUsuario().getId() : null;
            String terminal = Sesion.getTerminal();

            if (categoria.getId() == 0) {
                int id = categoriaDAO.insertar(con, categoria);
                categoria.setId(id);
                auditoriaDAO.registrar(con, usuarioId, terminal, "CATEGORIA_CREADA", "categorias",
                        String.valueOf(id), "Se creó la categoría: " + categoria.getNombre());
            } else {
                categoriaDAO.actualizar(con, categoria);
                auditoriaDAO.registrar(con, usuarioId, terminal, "CATEGORIA_MODIFICADA", "categorias",
                        String.valueOf(categoria.getId()), "Se actualizó la categoría ID: " + categoria.getId()
                                + " (" + categoria.getNombre() + ")");
            }

            con.commit();
        } catch (SQLException | NegocioException e) {
            Conexion.rollback(con);
            throw e;
        } finally {
            Conexion.cerrar(con);
        }
    }

    private void verificarPermisoGestion() throws NegocioException {
        if (!Sesion.tienePermiso(Permiso.PRODUCTOS_GESTIONAR)) {
            throw new NegocioException("No tiene permiso para modificar productos.");
        }
    }
}
