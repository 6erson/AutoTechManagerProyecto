-- Ejecutar en SQL Server Management Studio. Primera etapa del proyecto.
USE master;
GO
IF DB_ID(N'AutoTechManager') IS NULL CREATE DATABASE AutoTechManager;
GO
USE AutoTechManager;
GO
IF OBJECT_ID(N'dbo.Clientes',N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Clientes (
        ClienteId INT IDENTITY PRIMARY KEY,
        Nombre NVARCHAR(120) NOT NULL CHECK (LEN(LTRIM(RTRIM(Nombre))) > 0),
        Telefono NVARCHAR(25) NOT NULL,
        Correo NVARCHAR(120) NOT NULL DEFAULT '',
        Activo BIT NOT NULL DEFAULT 1
    );
END;
GO
IF OBJECT_ID(N'dbo.Vehiculos',N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Vehiculos (
        VehiculoId INT IDENTITY PRIMARY KEY,
        ClienteId INT NOT NULL REFERENCES dbo.Clientes(ClienteId),
        Placa NVARCHAR(15) NOT NULL UNIQUE CHECK (LEN(LTRIM(RTRIM(Placa))) > 0),
        VIN NVARCHAR(30) NOT NULL UNIQUE CHECK (LEN(LTRIM(RTRIM(VIN))) > 0),
        Marca NVARCHAR(60) NOT NULL,
        Modelo NVARCHAR(60) NOT NULL,
        Anio INT NOT NULL CHECK (Anio BETWEEN 1900 AND 2100),
        Color NVARCHAR(40) NOT NULL,
        Kilometraje INT NOT NULL CHECK (Kilometraje >= 0),
        Combustible NVARCHAR(15) NOT NULL CHECK (Combustible IN (N'Gasolina',N'Diésel',N'Híbrido',N'Eléctrico',N'GLP'))
    );
END;
GO
