package com.pos.util;

/**
 * Permisos del sistema. Cada valor tiene el mismo nombre que la columna
 * "codigo" de la tabla permisos.
 */
public enum Permiso {
    VENTAS_REALIZAR,
    VENTAS_HISTORIAL,
    VENTAS_ANULAR,
    CAJA_OPERAR,
    PRODUCTOS_VER,
    PRODUCTOS_GESTIONAR,
    INVENTARIO_GESTIONAR,
    CLIENTES_GESTIONAR,
    PROVEEDORES_GESTIONAR,
    REPORTES_VER,
    USUARIOS_GESTIONAR,
    CONFIGURACION_GESTIONAR,
    AUDITORIA_VER
}
