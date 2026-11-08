-- Catálogos de demostración. No crea clientes ni órdenes.
USE AutoTechManager;
GO
IF NOT EXISTS(SELECT 1 FROM Proveedores WHERE Nombre=N'AutoRepuestos Demo')
 INSERT Proveedores(Nombre,Telefono) VALUES(N'AutoRepuestos Demo','9999-0000');
IF NOT EXISTS(SELECT 1 FROM Mecanicos WHERE Nombre=N'Carlos Mejía') INSERT Mecanicos(Nombre,Especialidad) VALUES(N'Carlos Mejía',N'Mantenimiento y frenos');
IF NOT EXISTS(SELECT 1 FROM Mecanicos WHERE Nombre=N'Ana López') INSERT Mecanicos(Nombre,Especialidad) VALUES(N'Ana López',N'Ajustes y sustitución de motores');
IF NOT EXISTS(SELECT 1 FROM Mecanicos WHERE Nombre=N'Luis Pérez') INSERT Mecanicos(Nombre,Especialidad) VALUES(N'Luis Pérez',N'Diagnóstico eléctrico y apoyo');
IF NOT EXISTS(SELECT 1 FROM Servicios WHERE Codigo='S001') INSERT Servicios(Codigo,Nombre,Descripcion,Nivel,Costo,Horas) VALUES('S001',N'Cambio de aceite',N'Drenaje, filtro y revisión de fugas',1,350,1);
IF NOT EXISTS(SELECT 1 FROM Servicios WHERE Codigo='S002') INSERT Servicios(Codigo,Nombre,Descripcion,Nivel,Costo,Horas) VALUES('S002',N'Mantenimiento de frenos',N'Inspección y ajuste preventivo',2,800,2);
IF NOT EXISTS(SELECT 1 FROM Servicios WHERE Codigo='S003') INSERT Servicios(Codigo,Nombre,Descripcion,Nivel,Costo,Horas) VALUES('S003',N'Reparación de radiador',N'Reparación y prueba de presión',3,1500,4);
IF NOT EXISTS(SELECT 1 FROM Servicios WHERE Codigo='S004') INSERT Servicios(Codigo,Nombre,Descripcion,Nivel,Costo,Horas) VALUES('S004',N'Reparación de transmisión',N'Diagnóstico y reparación avanzada',4,4500,12);
IF NOT EXISTS(SELECT 1 FROM Servicios WHERE Codigo='S005') INSERT Servicios(Codigo,Nombre,Descripcion,Nivel,Costo,Horas) VALUES('S005',N'Sustitución de motor',N'Desmontaje, instalación y pruebas del motor',5,12000,24);
DECLARE @Proveedor INT=(SELECT MIN(ProveedorId) FROM Proveedores WHERE Nombre=N'AutoRepuestos Demo');
IF NOT EXISTS(SELECT 1 FROM Repuestos WHERE Codigo='R001') INSERT Repuestos(Codigo,Nombre,ProveedorId,Minimo,Costo,Precio) VALUES('R001',N'Aceite diésel 1 litro',@Proveedor,5,150,220);
IF NOT EXISTS(SELECT 1 FROM Repuestos WHERE Codigo='R002') INSERT Repuestos(Codigo,Nombre,ProveedorId,Minimo,Costo,Precio) VALUES('R002',N'Filtro de aceite',@Proveedor,3,100,180);
IF NOT EXISTS(SELECT 1 FROM Repuestos WHERE Codigo='R003') INSERT Repuestos(Codigo,Nombre,ProveedorId,Minimo,Costo,Precio) VALUES('R003',N'Motor Toyota Hilux diésel',@Proveedor,1,45000,58000);
IF NOT EXISTS(SELECT 1 FROM Motores WHERE Serie='DEMO-HILUX-001')
 INSERT Motores(RepuestoId,Serie,Marca,Modelo,AnioDesde,AnioHasta,Combustible)
 SELECT RepuestoId,'DEMO-HILUX-001',N'Toyota',N'Hilux',2012,2016,N'Diésel' FROM Repuestos WHERE Codigo='R003';
-- No se inventan existencias: registrar una Entrada de inventario antes de usar repuestos/motores.
IF NOT EXISTS(SELECT 1 FROM ServicioRepuesto sr JOIN Servicios s ON s.ServicioId=sr.ServicioId JOIN Repuestos r ON r.RepuestoId=sr.RepuestoId WHERE s.Codigo='S001' AND r.Codigo='R001')
 INSERT ServicioRepuesto SELECT s.ServicioId,r.RepuestoId,6 FROM Servicios s CROSS JOIN Repuestos r WHERE s.Codigo='S001' AND r.Codigo='R001';
IF NOT EXISTS(SELECT 1 FROM ServicioRepuesto sr JOIN Servicios s ON s.ServicioId=sr.ServicioId JOIN Repuestos r ON r.RepuestoId=sr.RepuestoId WHERE s.Codigo='S001' AND r.Codigo='R002')
 INSERT ServicioRepuesto SELECT s.ServicioId,r.RepuestoId,1 FROM Servicios s CROSS JOIN Repuestos r WHERE s.Codigo='S001' AND r.Codigo='R002';
GO
