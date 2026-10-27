-- =====================================================================
--  SISTEMA POS - Script de creación de la base de datos
--  Ejecutar una sola vez en MySQL Workbench (o en la consola de mysql)
--  con un usuario administrador (root).
-- =====================================================================

CREATE DATABASE IF NOT EXISTS pos_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE pos_db;

-- Usuario que usará la aplicación para conectarse (cambiar la contraseña).
-- CREATE USER 'pos_app'@'localhost' IDENTIFIED BY 'cambiar_esta_clave';
-- GRANT ALL PRIVILEGES ON pos_db.* TO 'pos_app'@'localhost';

-- ---------------------------------------------------------------------
-- Seguridad: roles, permisos y usuarios del personal
-- ---------------------------------------------------------------------
CREATE TABLE roles (
    id          INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(50)  NOT NULL,
    descripcion VARCHAR(255) NULL,
    CONSTRAINT uq_roles_nombre UNIQUE (nombre)
) ENGINE = InnoDB;

CREATE TABLE permisos (
    id          INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    codigo      VARCHAR(50)  NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    CONSTRAINT uq_permisos_codigo UNIQUE (codigo)
) ENGINE = InnoDB;

CREATE TABLE rol_permiso (
    rol_id     INT UNSIGNED NOT NULL,
    permiso_id INT UNSIGNED NOT NULL,
    PRIMARY KEY (rol_id, permiso_id),
    CONSTRAINT fk_rol_permiso_rol     FOREIGN KEY (rol_id)     REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_rol_permiso_permiso FOREIGN KEY (permiso_id) REFERENCES permisos (id) ON DELETE CASCADE
) ENGINE = InnoDB;

CREATE TABLE usuarios (
    id                    INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nombre_usuario        VARCHAR(50)  NOT NULL,
    password_hash         VARCHAR(100) NOT NULL,
    nombre_completo       VARCHAR(150) NOT NULL,
    rol_id                INT UNSIGNED NOT NULL,
    activo                BOOLEAN      NOT NULL DEFAULT TRUE,
    debe_cambiar_password BOOLEAN      NOT NULL DEFAULT FALSE,
    ultimo_acceso         DATETIME     NULL,
    creado_en             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_usuarios_nombre_usuario UNIQUE (nombre_usuario),
    CONSTRAINT fk_usuarios_rol FOREIGN KEY (rol_id) REFERENCES roles (id)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Catálogo
-- ---------------------------------------------------------------------
CREATE TABLE categorias (
    id          INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255) NULL,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_categorias_nombre UNIQUE (nombre)
) ENGINE = InnoDB;

CREATE TABLE proveedores (
    id        INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nombre    VARCHAR(150) NOT NULL,
    documento VARCHAR(30)  NULL,
    telefono  VARCHAR(30)  NULL,
    email     VARCHAR(150) NULL,
    direccion VARCHAR(255) NULL,
    activo    BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_proveedores_documento UNIQUE (documento)
) ENGINE = InnoDB;

CREATE TABLE impuestos (
    id         INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nombre     VARCHAR(50)  NOT NULL,
    porcentaje DECIMAL(5,2) NOT NULL,
    activo     BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_impuestos_nombre UNIQUE (nombre),
    CONSTRAINT ck_impuestos_porcentaje CHECK (porcentaje >= 0 AND porcentaje <= 100)
) ENGINE = InnoDB;

CREATE TABLE productos (
    id             INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    codigo_barras  VARCHAR(50)   NOT NULL,
    nombre         VARCHAR(150)  NOT NULL,
    descripcion    VARCHAR(255)  NULL,
    categoria_id   INT UNSIGNED  NOT NULL,
    proveedor_id   INT UNSIGNED  NULL,
    impuesto_id    INT UNSIGNED  NOT NULL,
    unidad_medida  VARCHAR(10)   NOT NULL DEFAULT 'UND',
    precio_compra  DECIMAL(12,2) NOT NULL DEFAULT 0,
    precio_venta   DECIMAL(12,2) NOT NULL,
    stock          DECIMAL(12,3) NOT NULL DEFAULT 0,
    stock_minimo   DECIMAL(12,3) NOT NULL DEFAULT 0,
    activo         BOOLEAN       NOT NULL DEFAULT TRUE,
    creado_en      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_productos_codigo_barras UNIQUE (codigo_barras),
    CONSTRAINT fk_productos_categoria FOREIGN KEY (categoria_id) REFERENCES categorias (id),
    CONSTRAINT fk_productos_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores (id),
    CONSTRAINT fk_productos_impuesto  FOREIGN KEY (impuesto_id)  REFERENCES impuestos (id),
    CONSTRAINT ck_productos_precios CHECK (precio_compra >= 0 AND precio_venta >= 0),
    CONSTRAINT ck_productos_stock   CHECK (stock >= 0 AND stock_minimo >= 0)
) ENGINE = InnoDB;

CREATE INDEX ix_productos_nombre ON productos (nombre);

-- ---------------------------------------------------------------------
-- Clientes (no inician sesión; solo se asocian a ventas)
-- ---------------------------------------------------------------------
CREATE TABLE clientes (
    id             INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    tipo_documento VARCHAR(20)  NULL,
    documento      VARCHAR(30)  NULL,
    nombre         VARCHAR(150) NOT NULL,
    telefono       VARCHAR(30)  NULL,
    email          VARCHAR(150) NULL,
    direccion      VARCHAR(255) NULL,
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_clientes_documento UNIQUE (documento)
) ENGINE = InnoDB;

CREATE INDEX ix_clientes_nombre ON clientes (nombre);

-- ---------------------------------------------------------------------
-- Caja y ventas (Maestro-Detalle)
-- ---------------------------------------------------------------------
CREATE TABLE metodos_pago (
    id     INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    activo BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_metodos_pago_nombre UNIQUE (nombre)
) ENGINE = InnoDB;

CREATE TABLE turnos_caja (
    id             INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    terminal       VARCHAR(50)   NOT NULL,
    usuario_id     INT UNSIGNED  NOT NULL,
    fecha_apertura DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    monto_inicial  DECIMAL(12,2) NOT NULL DEFAULT 0,
    fecha_cierre   DATETIME      NULL,
    monto_esperado DECIMAL(12,2) NULL,
    monto_contado  DECIMAL(12,2) NULL,
    diferencia     DECIMAL(12,2) NULL,
    estado         ENUM('ABIERTO', 'CERRADO') NOT NULL DEFAULT 'ABIERTO',
    observaciones  VARCHAR(255)  NULL,
    CONSTRAINT fk_turnos_caja_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
) ENGINE = InnoDB;

CREATE INDEX ix_turnos_caja_estado ON turnos_caja (terminal, estado);

-- Maestro: metadatos de la transacción
CREATE TABLE ventas (
    id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    turno_id         INT UNSIGNED  NOT NULL,
    usuario_id       INT UNSIGNED  NOT NULL,
    cliente_id       INT UNSIGNED  NULL,
    fecha            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    subtotal         DECIMAL(12,2) NOT NULL,
    descuento        DECIMAL(12,2) NOT NULL DEFAULT 0,
    impuesto         DECIMAL(12,2) NOT NULL DEFAULT 0,
    total            DECIMAL(12,2) NOT NULL,
    estado           ENUM('COMPLETADA', 'ANULADA') NOT NULL DEFAULT 'COMPLETADA',
    anulada_por      INT UNSIGNED  NULL,
    fecha_anulacion  DATETIME      NULL,
    motivo_anulacion VARCHAR(255)  NULL,
    CONSTRAINT fk_ventas_turno       FOREIGN KEY (turno_id)    REFERENCES turnos_caja (id),
    CONSTRAINT fk_ventas_usuario     FOREIGN KEY (usuario_id)  REFERENCES usuarios (id),
    CONSTRAINT fk_ventas_cliente     FOREIGN KEY (cliente_id)  REFERENCES clientes (id),
    CONSTRAINT fk_ventas_anulada_por FOREIGN KEY (anulada_por) REFERENCES usuarios (id),
    CONSTRAINT ck_ventas_montos CHECK (subtotal >= 0 AND descuento >= 0 AND impuesto >= 0 AND total >= 0)
) ENGINE = InnoDB;

CREATE INDEX ix_ventas_fecha ON ventas (fecha);

-- Detalle: instantánea de cada línea con precio y costo históricos
CREATE TABLE detalle_ventas (
    id                  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    venta_id            BIGINT UNSIGNED NOT NULL,
    producto_id         INT UNSIGNED    NOT NULL,
    cantidad            DECIMAL(12,3)   NOT NULL,
    precio_unitario     DECIMAL(12,2)   NOT NULL,
    costo_unitario      DECIMAL(12,2)   NOT NULL,
    porcentaje_impuesto DECIMAL(5,2)    NOT NULL DEFAULT 0,
    descuento           DECIMAL(12,2)   NOT NULL DEFAULT 0,
    subtotal            DECIMAL(12,2)   NOT NULL,
    CONSTRAINT fk_detalle_ventas_venta    FOREIGN KEY (venta_id)    REFERENCES ventas (id),
    CONSTRAINT fk_detalle_ventas_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
    CONSTRAINT ck_detalle_ventas_cantidad CHECK (cantidad > 0)
) ENGINE = InnoDB;

CREATE TABLE pagos (
    id             BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    venta_id       BIGINT UNSIGNED NOT NULL,
    metodo_pago_id INT UNSIGNED    NOT NULL,
    monto          DECIMAL(12,2)   NOT NULL,
    monto_recibido DECIMAL(12,2)   NULL,
    cambio         DECIMAL(12,2)   NULL,
    referencia     VARCHAR(100)    NULL,
    CONSTRAINT fk_pagos_venta       FOREIGN KEY (venta_id)       REFERENCES ventas (id),
    CONSTRAINT fk_pagos_metodo_pago FOREIGN KEY (metodo_pago_id) REFERENCES metodos_pago (id),
    CONSTRAINT ck_pagos_monto CHECK (monto > 0)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- Inventario (kardex) y auditoría
-- ---------------------------------------------------------------------
CREATE TABLE movimientos_inventario (
    id               BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    producto_id      INT UNSIGNED    NOT NULL,
    tipo             ENUM('VENTA', 'ANULACION', 'ENTRADA', 'AJUSTE') NOT NULL,
    cantidad         DECIMAL(12,3)   NOT NULL COMMENT 'Positiva = entra, negativa = sale',
    stock_resultante DECIMAL(12,3)   NOT NULL,
    costo_unitario   DECIMAL(12,2)   NULL,
    venta_id         BIGINT UNSIGNED NULL,
    usuario_id       INT UNSIGNED    NOT NULL,
    motivo           VARCHAR(255)    NULL,
    fecha            DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_movimientos_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
    CONSTRAINT fk_movimientos_venta    FOREIGN KEY (venta_id)    REFERENCES ventas (id),
    CONSTRAINT fk_movimientos_usuario  FOREIGN KEY (usuario_id)  REFERENCES usuarios (id)
) ENGINE = InnoDB;

CREATE INDEX ix_movimientos_producto_fecha ON movimientos_inventario (producto_id, fecha);

CREATE TABLE auditoria (
    id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT UNSIGNED NULL,
    terminal   VARCHAR(50)  NULL,
    accion     VARCHAR(50)  NOT NULL,
    entidad    VARCHAR(50)  NULL,
    entidad_id VARCHAR(50)  NULL,
    detalle    TEXT         NULL,
    fecha      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_auditoria_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
) ENGINE = InnoDB;

CREATE INDEX ix_auditoria_fecha ON auditoria (fecha);

-- ---------------------------------------------------------------------
-- Configuración compartida por todas las terminales (datos del negocio,
-- recibo). La impresora es propia de cada equipo y va en pos.properties.
-- ---------------------------------------------------------------------
CREATE TABLE configuracion (
    clave       VARCHAR(100) PRIMARY KEY,
    valor       TEXT         NULL,
    descripcion VARCHAR(255) NULL
) ENGINE = InnoDB;

-- =====================================================================
--  DATOS INICIALES
-- =====================================================================

INSERT INTO roles (nombre, descripcion) VALUES
    ('ADMINISTRADOR', 'Acceso total al sistema'),
    ('SUPERVISOR',    'Reportes, inventario y anulaciones'),
    ('CAJERO',        'Ventas y operación de su caja');

INSERT INTO permisos (codigo, descripcion) VALUES
    ('VENTAS_REALIZAR',         'Registrar ventas'),
    ('VENTAS_HISTORIAL',        'Consultar historial y reimprimir recibos'),
    ('VENTAS_ANULAR',           'Anular ventas'),
    ('CAJA_OPERAR',             'Abrir y cerrar turno de caja'),
    ('PRODUCTOS_VER',           'Consultar productos y precios'),
    ('PRODUCTOS_GESTIONAR',     'Crear y modificar productos y categorías'),
    ('INVENTARIO_GESTIONAR',    'Entradas, ajustes y kardex de inventario'),
    ('CLIENTES_GESTIONAR',      'Crear y modificar clientes'),
    ('PROVEEDORES_GESTIONAR',   'Crear y modificar proveedores'),
    ('REPORTES_VER',            'Dashboard y reportes'),
    ('USUARIOS_GESTIONAR',      'Administrar usuarios y roles'),
    ('CONFIGURACION_GESTIONAR', 'Configuración del negocio e impresora'),
    ('AUDITORIA_VER',           'Consultar el registro de auditoría');

-- ADMINISTRADOR: todos los permisos
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permisos p
WHERE r.nombre = 'ADMINISTRADOR';

-- SUPERVISOR: todo excepto usuarios, configuración y auditoría
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permisos p
WHERE r.nombre = 'SUPERVISOR'
  AND p.codigo NOT IN ('USUARIOS_GESTIONAR', 'CONFIGURACION_GESTIONAR', 'AUDITORIA_VER');

-- CAJERO: operación de venta
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permisos p
WHERE r.nombre = 'CAJERO'
  AND p.codigo IN ('VENTAS_REALIZAR', 'VENTAS_HISTORIAL', 'CAJA_OPERAR', 'PRODUCTOS_VER', 'CLIENTES_GESTIONAR');

-- Usuario inicial: admin / admin (se exige cambiar la contraseña en el primer ingreso)
INSERT INTO usuarios (nombre_usuario, password_hash, nombre_completo, rol_id, debe_cambiar_password)
SELECT 'admin', '$2a$12$EdBP8TtxUN9w2uAe9Uk1Ju5oOiDdZYoc6uBHdzVXtV0bhd1WetccK', 'Administrador', id, TRUE
FROM roles WHERE nombre = 'ADMINISTRADOR';

INSERT INTO impuestos (nombre, porcentaje) VALUES
    ('Exento', 0.00),
    ('IVA',    16.00);

INSERT INTO metodos_pago (nombre) VALUES
    ('Efectivo'),
    ('Tarjeta'),
    ('Transferencia');

INSERT INTO categorias (nombre, descripcion) VALUES
    ('General', 'Categoría por defecto');

INSERT INTO configuracion (clave, valor, descripcion) VALUES
    ('negocio.nombre',     'Mi Negocio', 'Nombre comercial que aparece en el recibo'),
    ('negocio.documento',  '',           'Identificación fiscal (RUC/NIT/RFC)'),
    ('negocio.direccion',  '',           'Dirección del negocio'),
    ('negocio.telefono',   '',           'Teléfono de contacto'),
    ('moneda.simbolo',     '$',          'Símbolo de moneda'),
    ('recibo.mensaje_pie', '¡Gracias por su compra!', 'Mensaje al final del recibo');
