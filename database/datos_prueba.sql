-- =====================================================================
--  SISTEMA POS - Datos de prueba para Showcase / Demostración
--  Ejecutar en MySQL Workbench con la base de datos `pos_db`
-- =====================================================================

USE pos_db;

-- ---------------------------------------------------------------------
-- 1. Categorías de prueba
-- ---------------------------------------------------------------------
INSERT INTO categorias (nombre, descripcion, activo) VALUES
    ('Bebidas', 'Refrescos, aguas minerales y jugos', TRUE),
    ('Snacks y Dulces', 'Papas fritas, galletas y golosinas', TRUE),
    ('Lácteos y Embutidos', 'Leche, quesos, yogures y carnes frías', TRUE),
    ('Abarrotes', 'Arroz, aceites, pastas y granos básicos', TRUE),
    ('Limpieza y Hogar', 'Detergentes, jabones y artículos para el hogar', TRUE)
ON DUPLICATE KEY UPDATE descripcion = VALUES(descripcion);

-- ---------------------------------------------------------------------
-- 2. Proveedores de prueba
-- ---------------------------------------------------------------------
INSERT INTO proveedores (nombre, documento, telefono, email, direccion, activo) VALUES
    ('Distribuidora Central S.A.', '900123456-1', '555-0101', 'ventas@distribuidoracentral.com', 'Av. Principal #100, Zona Industrial', TRUE),
    ('Bebidas del Valle Ltda.',    '900654321-2', '555-0202', 'pedidos@bebidasdelvalle.com',    'Calle 45 #20-30', TRUE),
    ('Lácteos La Granja',          '900987654-3', '555-0303', 'contacto@lacteoslagranja.com',   'Carrera 15 #80-12', TRUE)
ON DUPLICATE KEY UPDATE telefono = VALUES(telefono);

-- ---------------------------------------------------------------------
-- 3. Clientes de prueba
-- ---------------------------------------------------------------------
INSERT INTO clientes (tipo_documento, documento, nombre, telefono, email, direccion, activo) VALUES
    ('Cédula / DNI', '1020304050',  'Carlos Mendoza',                '3001234567', 'carlos.mendoza@email.com', 'Calle 10 #5-20', TRUE),
    ('Cédula / DNI', '2030405060',  'María Fernanda Gómez',          '3109876543', 'maria.gomez@email.com',    'Av. Las Palmas #42', TRUE),
    ('NIT / RUC',    '900334455-8', 'Restaurante & Café El Centro',  '6014455660', 'contacto@elcentro.com',   'Cra 7 #32-10 Local 4', TRUE),
    ('Cédula / DNI', '3040506070',  'Juan David Pérez',              '3157788990', 'juan.perez@email.com',     'Barrio El Sol Casa 12', TRUE)
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

-- ---------------------------------------------------------------------
-- 4. Productos de prueba con stock, costo y precio de venta
-- ---------------------------------------------------------------------
-- Nota: Usamos subconsultas para asociar correctamente las categorías,
-- proveedores e impuestos independientemente de los IDs que tengan.

INSERT INTO productos (codigo_barras, nombre, descripcion, categoria_id, proveedor_id, impuesto_id, unidad_medida, precio_compra, precio_venta, stock, stock_minimo, activo)
VALUES
    -- Bebidas
    ('7701001', 'Coca-Cola Original 500ml', 'Gaseosa refrescante personal',
        (SELECT id FROM categorias WHERE nombre = 'Bebidas' LIMIT 1),
        (SELECT id FROM proveedores WHERE nombre = 'Bebidas del Valle Ltda.' LIMIT 1),
        (SELECT id FROM impuestos WHERE nombre = 'IVA' LIMIT 1),
        'UND', 2.00, 3.50, 48.000, 10.000, TRUE),

    ('7701002', 'Agua Mineral Sin Gas 600ml', 'Agua pura de manantial',
        (SELECT id FROM categorias WHERE nombre = 'Bebidas' LIMIT 1),
        (SELECT id FROM proveedores WHERE nombre = 'Bebidas del Valle Ltda.' LIMIT 1),
        (SELECT id FROM impuestos WHERE nombre = 'Exento' LIMIT 1),
        'UND', 1.00, 2.00, 60.000, 15.000, TRUE),

    ('7701003', 'Jugo Natural Naranja 1L', 'Jugo 100% fruta pasteurizado',
        (SELECT id FROM categorias WHERE nombre = 'Bebidas' LIMIT 1),
        (SELECT id FROM proveedores WHERE nombre = 'Bebidas del Valle Ltda.' LIMIT 1),
        (SELECT id FROM impuestos WHERE nombre = 'IVA' LIMIT 1),
        'UND', 3.20, 5.50, 24.000, 8.000, TRUE),

    -- Snacks
    ('7702001', 'Papas Fritas Clásicas 115g', 'Papas crujientes con sal marina',
        (SELECT id FROM categorias WHERE nombre = 'Snacks y Dulces' LIMIT 1),
        (SELECT id FROM proveedores WHERE nombre = 'Distribuidora Central S.A.' LIMIT 1),
        (SELECT id FROM impuestos WHERE nombre = 'IVA' LIMIT 1),
        'UND', 2.10, 3.80, 35.000, 10.000, TRUE),

    ('7702002', 'Galletas Choco-Chips 150g', 'Galletas con chispas de chocolate',
        (SELECT id FROM categorias WHERE nombre = 'Snacks y Dulces' LIMIT 1),
        (SELECT id FROM proveedores WHERE nombre = 'Distribuidora Central S.A.' LIMIT 1),
        (SELECT id FROM impuestos WHERE nombre = 'IVA' LIMIT 1),
        'UND', 1.50, 2.70, 40.000, 12.000, TRUE),

    -- Lácteos
    ('7703001', 'Leche Entera 1 Litro', 'Leche fresca pasteurizada en bolsa',
        (SELECT id FROM categorias WHERE nombre = 'Lácteos y Embutidos' LIMIT 1),
        (SELECT id FROM proveedores WHERE nombre = 'Lácteos La Granja' LIMIT 1),
        (SELECT id FROM impuestos WHERE nombre = 'Exento' LIMIT 1),
        'UND', 2.80, 4.20, 50.000, 15.000, TRUE),

    ('7703002', 'Queso Campesino 500g', 'Queso fresco semiduro artesanal',
        (SELECT id FROM categorias WHERE nombre = 'Lácteos y Embutidos' LIMIT 1),
        (SELECT id FROM proveedores WHERE nombre = 'Lácteos La Granja' LIMIT 1),
        (SELECT id FROM impuestos WHERE nombre = 'IVA' LIMIT 1),
        'UND', 5.50, 8.50, 18.000, 5.000, TRUE),

    -- Abarrotes
    ('7704001', 'Arroz Premium Grano Largo 1kg', 'Arroz seleccionado calidad extra',
        (SELECT id FROM categorias WHERE nombre = 'Abarrotes' LIMIT 1),
        (SELECT id FROM proveedores WHERE nombre = 'Distribuidora Central S.A.' LIMIT 1),
        (SELECT id FROM impuestos WHERE nombre = 'Exento' LIMIT 1),
        'UND', 2.50, 3.90, 80.000, 20.000, TRUE),

    ('7704002', 'Aceite Vegetal 900ml', 'Aceite 100% puro de girasol y soya',
        (SELECT id FROM categorias WHERE nombre = 'Abarrotes' LIMIT 1),
        (SELECT id FROM proveedores WHERE nombre = 'Distribuidora Central S.A.' LIMIT 1),
        (SELECT id FROM impuestos WHERE nombre = 'IVA' LIMIT 1),
        'UND', 6.00, 9.50, 30.000, 8.000, TRUE),

    -- Limpieza (Uno con stock normal y otro bajo mínimo para demostrar alertas en el POS)
    ('7705001', 'Detergente Líquido 1 Litro', 'Detergente concentrado aroma floral',
        (SELECT id FROM categorias WHERE nombre = 'Limpieza y Hogar' LIMIT 1),
        (SELECT id FROM proveedores WHERE nombre = 'Distribuidora Central S.A.' LIMIT 1),
        (SELECT id FROM impuestos WHERE nombre = 'IVA' LIMIT 1),
        'UND', 7.20, 12.00, 25.000, 6.000, TRUE),

    ('7705002', 'Papel Higiénico 4 Rollos', 'Papel doble hoja extra suave (Alerta: Stock Bajo)',
        (SELECT id FROM categorias WHERE nombre = 'Limpieza y Hogar' LIMIT 1),
        (SELECT id FROM proveedores WHERE nombre = 'Distribuidora Central S.A.' LIMIT 1),
        (SELECT id FROM impuestos WHERE nombre = 'IVA' LIMIT 1),
        'UND', 3.00, 5.00, 3.000, 10.000, TRUE)
ON DUPLICATE KEY UPDATE 
    precio_compra = VALUES(precio_compra),
    precio_venta = VALUES(precio_venta),
    stock = VALUES(stock);

-- ---------------------------------------------------------------------
-- 5. Kardex Inicial (movimientos_inventario)
-- Registra el stock inicial de cada producto para que el módulo de
-- Inventario y Kardex muestre datos reales de inmediato.
-- ---------------------------------------------------------------------
INSERT INTO movimientos_inventario (producto_id, tipo, cantidad, stock_resultante, costo_unitario, venta_id, usuario_id, motivo, fecha)
SELECT 
    p.id, 
    'ENTRADA', 
    p.stock, 
    p.stock, 
    p.precio_compra, 
    NULL, 
    (SELECT id FROM usuarios WHERE nombre_usuario = 'admin' LIMIT 1), 
    'Stock inicial de apertura', 
    NOW()
FROM productos p
WHERE p.codigo_barras LIKE '770%'
  AND NOT EXISTS (
      SELECT 1 FROM movimientos_inventario mi WHERE mi.producto_id = p.id
  );
