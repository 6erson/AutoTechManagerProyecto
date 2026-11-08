USE AutoTechManager;
GO
IF COL_LENGTH('dbo.Repuestos','Unidad') IS NULL
    ALTER TABLE dbo.Repuestos ADD Unidad NVARCHAR(16) NOT NULL CONSTRAINT DF_Repuestos_Unidad DEFAULT N'Unidad';
GO
IF NOT EXISTS(SELECT 1 FROM sys.check_constraints WHERE name='CK_Repuestos_Unidad')
    ALTER TABLE dbo.Repuestos ADD CONSTRAINT CK_Repuestos_Unidad CHECK(Unidad IN(N'Unidad',N'Litro',N'Juego'));
GO
UPDATE dbo.Repuestos SET Unidad=N'Litro' WHERE Codigo='R001' AND Nombre LIKE N'%aceite%';
GO
