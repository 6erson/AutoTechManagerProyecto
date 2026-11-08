-- Ejecutar después de 01_clientes_vehiculos.sql. No borra datos existentes.
USE AutoTechManager;
GO
IF OBJECT_ID('Servicios') IS NULL
CREATE TABLE Servicios(ServicioId INT IDENTITY PRIMARY KEY, Codigo NVARCHAR(20) NOT NULL UNIQUE,
 Nombre NVARCHAR(100) NOT NULL, Descripcion NVARCHAR(400) NOT NULL, Nivel INT NOT NULL CHECK(Nivel BETWEEN 1 AND 5),
 Costo DECIMAL(12,2) NOT NULL CHECK(Costo>=0), Horas DECIMAL(8,2) NOT NULL CHECK(Horas>0), Activo BIT NOT NULL DEFAULT 1);
IF OBJECT_ID('Mecanicos') IS NULL
CREATE TABLE Mecanicos(MecanicoId INT IDENTITY PRIMARY KEY, Nombre NVARCHAR(100) NOT NULL,
 Especialidad NVARCHAR(100) NOT NULL, Activo BIT NOT NULL DEFAULT 1);
IF OBJECT_ID('Proveedores') IS NULL
CREATE TABLE Proveedores(ProveedorId INT IDENTITY PRIMARY KEY, Nombre NVARCHAR(100) NOT NULL,
 Telefono NVARCHAR(25) NOT NULL, Activo BIT NOT NULL DEFAULT 1);
IF OBJECT_ID('Repuestos') IS NULL
CREATE TABLE Repuestos(RepuestoId INT IDENTITY PRIMARY KEY, Codigo NVARCHAR(25) NOT NULL UNIQUE,
 Nombre NVARCHAR(100) NOT NULL, ProveedorId INT NOT NULL REFERENCES Proveedores,
 Stock INT NOT NULL DEFAULT 0 CHECK(Stock>=0), Minimo INT NOT NULL CHECK(Minimo>=0),
 Costo DECIMAL(12,2) NOT NULL CHECK(Costo>=0), Precio DECIMAL(12,2) NOT NULL CHECK(Precio>=0), Activo BIT NOT NULL DEFAULT 1);
IF OBJECT_ID('ServicioRepuesto') IS NULL
CREATE TABLE ServicioRepuesto(ServicioId INT REFERENCES Servicios,RepuestoId INT REFERENCES Repuestos,
 Cantidad INT NOT NULL CHECK(Cantidad>0), PRIMARY KEY(ServicioId,RepuestoId));
IF OBJECT_ID('OrdenesTrabajo') IS NULL
CREATE TABLE OrdenesTrabajo(OrdenId INT IDENTITY PRIMARY KEY, ClienteId INT NOT NULL REFERENCES Clientes,
 VehiculoId INT NOT NULL REFERENCES Vehiculos, Fecha DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
 Kilometraje INT NOT NULL CHECK(Kilometraje>=0), Sintomas NVARCHAR(1000) NOT NULL,
 ResponsableId INT NOT NULL REFERENCES Mecanicos, Estado NVARCHAR(35) NOT NULL DEFAULT N'Recibida'
 CHECK(Estado IN(N'Recibida',N'En diagnóstico',N'Pendiente de aprobación',N'En reparación',N'En control de calidad',N'Finalizada y entregada',N'Rechazada',N'Cancelada')),
 FechaEntrega DATETIME2 NULL);
IF OBJECT_ID('OrdenMecanicos') IS NULL
CREATE TABLE OrdenMecanicos(OrdenId INT REFERENCES OrdenesTrabajo,MecanicoId INT REFERENCES Mecanicos,PRIMARY KEY(OrdenId,MecanicoId));
IF OBJECT_ID('DetalleOrden') IS NULL
CREATE TABLE DetalleOrden(DetalleId INT IDENTITY PRIMARY KEY, OrdenId INT NOT NULL REFERENCES OrdenesTrabajo,
 ServicioId INT NOT NULL REFERENCES Servicios, Nombre NVARCHAR(100) NOT NULL, Nivel INT NOT NULL,
 Horas DECIMAL(8,2) NOT NULL CHECK(Horas>0), Importe DECIMAL(12,2) NOT NULL CHECK(Importe>=0), UNIQUE(OrdenId,ServicioId));
IF OBJECT_ID('Diagnosticos') IS NULL
CREATE TABLE Diagnosticos(DiagnosticoId INT IDENTITY PRIMARY KEY,OrdenId INT NOT NULL UNIQUE REFERENCES OrdenesTrabajo,
 Evaluacion NVARCHAR(2000) NOT NULL,Evidencias NVARCHAR(2000) NOT NULL,Fecha DATETIME2 NOT NULL DEFAULT SYSDATETIME());
IF OBJECT_ID('ConsumosOrden') IS NULL
CREATE TABLE ConsumosOrden(ConsumoId INT IDENTITY PRIMARY KEY,OrdenId INT NOT NULL REFERENCES OrdenesTrabajo,
 RepuestoId INT NOT NULL REFERENCES Repuestos,Cantidad INT NOT NULL CHECK(Cantidad>0),
 Precio DECIMAL(12,2) NOT NULL CHECK(Precio>=0),Procesado BIT NOT NULL DEFAULT 0,UNIQUE(OrdenId,RepuestoId));
IF OBJECT_ID('MovimientosInventario') IS NULL
CREATE TABLE MovimientosInventario(MovimientoId INT IDENTITY PRIMARY KEY,RepuestoId INT NOT NULL REFERENCES Repuestos,
 OrdenId INT NULL REFERENCES OrdenesTrabajo,Tipo NVARCHAR(10) NOT NULL CHECK(Tipo IN('Entrada','Salida')),
 Cantidad INT NOT NULL CHECK(Cantidad>0),Fecha DATETIME2 NOT NULL DEFAULT SYSDATETIME(),Motivo NVARCHAR(300) NOT NULL);
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE name='UX_SalidaOrden' AND object_id=OBJECT_ID('MovimientosInventario'))
CREATE UNIQUE INDEX UX_SalidaOrden ON MovimientosInventario(OrdenId,RepuestoId) WHERE Tipo='Salida' AND OrdenId IS NOT NULL;
IF OBJECT_ID('Presupuestos') IS NULL
CREATE TABLE Presupuestos(PresupuestoId INT IDENTITY PRIMARY KEY,OrdenId INT NOT NULL UNIQUE REFERENCES OrdenesTrabajo,
 ManoObra DECIMAL(12,2) NOT NULL CHECK(ManoObra>=0),Repuestos DECIMAL(12,2) NOT NULL CHECK(Repuestos>=0),
 Descuento DECIMAL(12,2) NOT NULL CHECK(Descuento>=0),AutorizoDescuento NVARCHAR(100) NOT NULL,
 Impuesto DECIMAL(5,2) NOT NULL CHECK(Impuesto BETWEEN 0 AND 100),
 Total DECIMAL(12,2) NOT NULL CHECK(Total>=0),Decision NVARCHAR(15) NOT NULL DEFAULT 'Pendiente' CHECK(Decision IN('Pendiente','Aprobado','Rechazado')),
 Autorizacion NVARCHAR(200) NOT NULL DEFAULT '',Fecha DATETIME2 NOT NULL DEFAULT SYSDATETIME());
IF OBJECT_ID('Pagos') IS NULL
CREATE TABLE Pagos(PagoId INT IDENTITY PRIMARY KEY,OrdenId INT NOT NULL REFERENCES OrdenesTrabajo,
 Fecha DATETIME2 NOT NULL DEFAULT SYSDATETIME(),Monto DECIMAL(12,2) NOT NULL CHECK(Monto>0),
 Metodo NVARCHAR(30) NOT NULL,Referencia NVARCHAR(100) NOT NULL);
IF OBJECT_ID('Motores') IS NULL
CREATE TABLE Motores(MotorId INT IDENTITY PRIMARY KEY,RepuestoId INT NOT NULL REFERENCES Repuestos,
 Serie NVARCHAR(60) NOT NULL UNIQUE,Marca NVARCHAR(60) NOT NULL,Modelo NVARCHAR(60) NOT NULL,
 AnioDesde INT NOT NULL,AnioHasta INT NOT NULL,Combustible NVARCHAR(15) NOT NULL,
 Estado NVARCHAR(15) NOT NULL DEFAULT 'Disponible' CHECK(Estado IN('Disponible','Reservado','Instalado')),
 CHECK(AnioDesde BETWEEN 1900 AND 2100 AND AnioHasta BETWEEN AnioDesde AND 2100));
IF OBJECT_ID('CambiosMotor') IS NULL
CREATE TABLE CambiosMotor(CambioId INT IDENTITY PRIMARY KEY,OrdenId INT NOT NULL UNIQUE REFERENCES OrdenesTrabajo,
 MotorId INT NOT NULL UNIQUE REFERENCES Motores,SerieAnterior NVARCHAR(60) NOT NULL,
 DestinoRetirado NVARCHAR(300) NOT NULL,Pruebas NVARCHAR(2000) NOT NULL DEFAULT '',
 Garantia NVARCHAR(500) NOT NULL DEFAULT '');
IF OBJECT_ID('ActividadesTrabajo') IS NULL
CREATE TABLE ActividadesTrabajo(ActividadId INT IDENTITY PRIMARY KEY,OrdenId INT NOT NULL REFERENCES OrdenesTrabajo,
 MecanicoId INT NOT NULL REFERENCES Mecanicos,Descripcion NVARCHAR(500) NOT NULL,Completada BIT NOT NULL DEFAULT 0);
IF OBJECT_ID('ControlesCalidad') IS NULL
CREATE TABLE ControlesCalidad(ControlId INT IDENTITY PRIMARY KEY,OrdenId INT NOT NULL REFERENCES OrdenesTrabajo,
 Descripcion NVARCHAR(500) NOT NULL,Obligatorio BIT NOT NULL DEFAULT 1,Aprobado BIT NOT NULL DEFAULT 0,
 Resultado NVARCHAR(1000) NOT NULL DEFAULT '');
IF OBJECT_ID('AuditoriaOrdenes') IS NULL
CREATE TABLE AuditoriaOrdenes(AuditoriaId INT IDENTITY PRIMARY KEY,OrdenId INT NOT NULL,
 EstadoAnterior NVARCHAR(35),EstadoNuevo NVARCHAR(35),Fecha DATETIME2 NOT NULL DEFAULT SYSDATETIME(),Usuario NVARCHAR(128) NOT NULL DEFAULT SUSER_SNAME());
GO
CREATE OR ALTER TRIGGER tr_AuditoriaOrdenes ON OrdenesTrabajo AFTER INSERT,UPDATE AS
BEGIN
 SET NOCOUNT ON;
 INSERT INTO AuditoriaOrdenes(OrdenId,EstadoAnterior,EstadoNuevo)
 SELECT i.OrdenId,d.Estado,i.Estado FROM inserted i LEFT JOIN deleted d ON d.OrdenId=i.OrdenId
 WHERE d.OrdenId IS NULL OR d.Estado<>i.Estado;
END;
GO
CREATE OR ALTER VIEW vw_SaldosOrdenes AS
SELECT o.OrdenId,o.Fecha,c.Nombre Cliente,v.Placa,o.Estado,CASE WHEN o.Estado IN(N'Rechazada',N'Cancelada') THEN 0 ELSE COALESCE(p.Total,0) END Total,
 COALESCE(a.Abonado,0) Abonado,CASE WHEN o.Estado IN(N'Rechazada',N'Cancelada') THEN 0 ELSE COALESCE(p.Total,0)-COALESCE(a.Abonado,0) END Saldo
FROM OrdenesTrabajo o INNER JOIN Clientes c ON c.ClienteId=o.ClienteId
INNER JOIN Vehiculos v ON v.VehiculoId=o.VehiculoId LEFT JOIN Presupuestos p ON p.OrdenId=o.OrdenId
LEFT JOIN(SELECT OrdenId,SUM(Monto) Abonado FROM Pagos GROUP BY OrdenId) a ON a.OrdenId=o.OrdenId;
GO
CREATE OR ALTER VIEW vw_StockBajo AS
SELECT r.RepuestoId,r.Codigo,r.Nombre,r.Stock,r.Minimo,p.Nombre Proveedor
FROM Repuestos r INNER JOIN Proveedores p ON p.ProveedorId=r.ProveedorId
WHERE r.Activo=1 AND r.Stock<=r.Minimo;
GO
CREATE OR ALTER PROCEDURE sp_ConsumirRepuestos @OrdenId INT AS
BEGIN
 SET NOCOUNT ON; SET XACT_ABORT ON;
 BEGIN TRY
 BEGIN TRAN;
 DECLARE @Estado NVARCHAR(35);
 SELECT @Estado=Estado FROM OrdenesTrabajo WITH(UPDLOCK,HOLDLOCK) WHERE OrdenId=@OrdenId;
 IF @Estado IS NULL OR @Estado<>N'En reparación' THROW 50001,N'Solo se consumen repuestos durante reparación.',1;
 IF NOT EXISTS(SELECT 1 FROM Presupuestos WHERE OrdenId=@OrdenId AND Decision='Aprobado') THROW 50002,N'Falta presupuesto aprobado.',1;
 IF NOT EXISTS(SELECT 1 FROM ConsumosOrden WHERE OrdenId=@OrdenId AND Procesado=0) THROW 50003,N'No hay consumos pendientes. No se descontará dos veces.',1;
 DECLARE @Pendientes INT=(SELECT COUNT(*) FROM ConsumosOrden WHERE OrdenId=@OrdenId AND Procesado=0);
 UPDATE r WITH(UPDLOCK) SET Stock=Stock-c.Cantidad FROM Repuestos r
 INNER JOIN ConsumosOrden c ON c.RepuestoId=r.RepuestoId
 WHERE c.OrdenId=@OrdenId AND c.Procesado=0 AND r.Stock>=c.Cantidad AND r.Activo=1;
 IF @@ROWCOUNT<>@Pendientes THROW 50004,N'Stock insuficiente o repuesto inactivo. Operación revertida.',1;
 INSERT INTO MovimientosInventario(RepuestoId,OrdenId,Tipo,Cantidad,Motivo)
 SELECT RepuestoId,@OrdenId,'Salida',Cantidad,N'Consumo de orden' FROM ConsumosOrden WHERE OrdenId=@OrdenId AND Procesado=0;
 UPDATE ConsumosOrden SET Procesado=1 WHERE OrdenId=@OrdenId AND Procesado=0;
 COMMIT;
 END TRY BEGIN CATCH IF @@TRANCOUNT>0 ROLLBACK; THROW; END CATCH
END;
GO
CREATE OR ALTER PROCEDURE sp_RegistrarPago @OrdenId INT,@Monto DECIMAL(12,2),@Metodo NVARCHAR(30),@Referencia NVARCHAR(100) AS
BEGIN
 SET NOCOUNT ON; SET XACT_ABORT ON;
 BEGIN TRY
 BEGIN TRAN;
 DECLARE @Estado NVARCHAR(35),@Total DECIMAL(12,2),@Pagado DECIMAL(12,2);
 SELECT @Estado=Estado FROM OrdenesTrabajo WITH(UPDLOCK,HOLDLOCK) WHERE OrdenId=@OrdenId;
 SELECT @Total=Total FROM Presupuestos WHERE OrdenId=@OrdenId AND Decision='Aprobado';
 SELECT @Pagado=COALESCE(SUM(Monto),0) FROM Pagos WHERE OrdenId=@OrdenId;
 IF @Estado IS NULL OR @Estado IN(N'Cancelada',N'Rechazada') OR @Total IS NULL THROW 50005,N'La orden no admite pagos.',1;
 IF @Monto<=0 OR @Monto>@Total-@Pagado THROW 50006,N'El pago debe ser positivo y no superar el saldo.',1;
 INSERT INTO Pagos(OrdenId,Monto,Metodo,Referencia) VALUES(@OrdenId,@Monto,@Metodo,@Referencia);
 COMMIT;
 END TRY BEGIN CATCH IF @@TRANCOUNT>0 ROLLBACK; THROW; END CATCH
END;
GO
CREATE OR ALTER PROCEDURE sp_CambiarEstado @OrdenId INT,@Nuevo NVARCHAR(35) AS
BEGIN
 SET NOCOUNT ON; SET XACT_ABORT ON;
 BEGIN TRY
 BEGIN TRAN;
 DECLARE @Actual NVARCHAR(35);
 SELECT @Actual=Estado FROM OrdenesTrabajo WITH(UPDLOCK,HOLDLOCK) WHERE OrdenId=@OrdenId;
 IF @Actual IS NULL OR @Actual IN(N'Finalizada y entregada',N'Cancelada',N'Rechazada') THROW 50007,N'Orden inexistente o cerrada.',1;
 IF @Nuevo=N'Cancelada'
 BEGIN
  IF EXISTS(SELECT 1 FROM ConsumosOrden WHERE OrdenId=@OrdenId AND Procesado=1)
     OR EXISTS(SELECT 1 FROM Pagos WHERE OrdenId=@OrdenId)
     THROW 50008,N'No se puede cancelar con consumos o pagos. Requiere gestionar devolución.',1;
  UPDATE Motores SET Estado='Disponible' WHERE MotorId IN(SELECT MotorId FROM CambiosMotor WHERE OrdenId=@OrdenId);
  DELETE FROM CambiosMotor WHERE OrdenId=@OrdenId;
 END
 ELSE IF NOT ((@Actual=N'Recibida' AND @Nuevo=N'En diagnóstico') OR
 (@Actual=N'En diagnóstico' AND @Nuevo=N'Pendiente de aprobación') OR
 (@Actual=N'Pendiente de aprobación' AND @Nuevo IN(N'En reparación',N'Rechazada')) OR
 (@Actual=N'En reparación' AND @Nuevo=N'En control de calidad') OR
 (@Actual=N'En control de calidad' AND @Nuevo=N'Finalizada y entregada')) THROW 50009,N'Transición de estado no permitida.',1;
 IF @Nuevo=N'Pendiente de aprobación' AND
 (NOT EXISTS(SELECT 1 FROM Diagnosticos WHERE OrdenId=@OrdenId) OR NOT EXISTS(SELECT 1 FROM Presupuestos WHERE OrdenId=@OrdenId))
 THROW 50010,N'Registra diagnóstico y presupuesto primero.',1;
 IF @Nuevo=N'En reparación' AND NOT EXISTS(SELECT 1 FROM Presupuestos WHERE OrdenId=@OrdenId AND Decision='Aprobado')
 THROW 50011,N'El cliente debe aprobar el presupuesto.',1;
 IF @Nuevo=N'Rechazada' AND NOT EXISTS(SELECT 1 FROM Presupuestos WHERE OrdenId=@OrdenId AND Decision='Rechazado')
 THROW 50012,N'Registra el rechazo del presupuesto.',1;
 IF @Nuevo=N'Rechazada'
 BEGIN
  UPDATE Motores SET Estado='Disponible' WHERE MotorId IN(SELECT MotorId FROM CambiosMotor WHERE OrdenId=@OrdenId);
  DELETE FROM CambiosMotor WHERE OrdenId=@OrdenId;
 END;
 IF @Nuevo IN(N'En control de calidad',N'Finalizada y entregada')
 BEGIN
  IF NOT EXISTS(SELECT 1 FROM ActividadesTrabajo WHERE OrdenId=@OrdenId) OR EXISTS(SELECT 1 FROM ActividadesTrabajo WHERE OrdenId=@OrdenId AND Completada=0)
    THROW 50013,N'Hay actividades pendientes o no se registró el trabajo.',1;
  IF EXISTS(SELECT 1 FROM ConsumosOrden WHERE OrdenId=@OrdenId AND Procesado=0) THROW 50014,N'Procesa todos los consumos antes de continuar.',1;
  IF EXISTS(SELECT 1 FROM CambiosMotor WHERE OrdenId=@OrdenId AND (LEN(LTRIM(Pruebas))=0 OR LEN(LTRIM(Garantia))=0))
    THROW 50015,N'Completa pruebas y garantía del motor.',1;
  IF EXISTS(SELECT 1 FROM DetalleOrden d WHERE d.OrdenId=@OrdenId AND d.Nivel=5)
   AND NOT EXISTS(SELECT 1 FROM CambiosMotor WHERE OrdenId=@OrdenId) THROW 50016,N'El servicio mayor requiere registrar el cambio de motor.',1;
 END;
 IF @Nuevo=N'Finalizada y entregada'
 BEGIN
  IF NOT EXISTS(SELECT 1 FROM ControlesCalidad WHERE OrdenId=@OrdenId AND Obligatorio=1)
    OR EXISTS(SELECT 1 FROM ControlesCalidad WHERE OrdenId=@OrdenId AND Obligatorio=1 AND Aprobado=0)
    THROW 50017,N'Falta aprobar el control de calidad obligatorio.',1;
  UPDATE Motores SET Estado='Instalado' WHERE MotorId IN(SELECT MotorId FROM CambiosMotor WHERE OrdenId=@OrdenId);
 END;
 UPDATE OrdenesTrabajo SET Estado=@Nuevo,FechaEntrega=CASE WHEN @Nuevo=N'Finalizada y entregada' THEN SYSDATETIME() ELSE NULL END WHERE OrdenId=@OrdenId;
 COMMIT;
 END TRY BEGIN CATCH IF @@TRANCOUNT>0 ROLLBACK; THROW; END CATCH
END;
GO
