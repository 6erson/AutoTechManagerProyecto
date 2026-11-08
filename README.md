# AutoTech Manager

Sistema de taller mecánico para el examen de Programación Avanzada II. Java 26, Swing, JDBC y SQL Server, con tema oscuro carbón, azul y verde.

## Funciones

- Clientes y vehículos, con desactivación e historiales separados.
- Servicios de niveles 1 a 5, mecánicos, proveedores, repuestos y motores por serie.
- Órdenes con diagnóstico, responsables, actividades y flujo de aprobación/reparación/calidad/entrega.
- Presupuestos con precios históricos, descuentos autorizados e impuesto configurable.
- Inventario transaccional con protección contra consumos duplicados.
- Sustitución de motor con compatibilidad, reserva, serie anterior/nueva, pruebas y garantía.
- Pagos parciales/totales sin sobrepagos y comprobante interno imprimible.
- Reportes administrativos, historial por vehículo y auditoría.

## Ejecutar en NetBeans

1. Abrir esta carpeta como proyecto Maven y usar JDK 26.
2. Ejecutar en SQL Server Management Studio los scripts `sql/01_clientes_vehiculos.sql`, `sql/02_taller_completo.sql` y, si deseas catálogos de ejemplo, `sql/03_catalogos_iniciales.sql`.
3. En `src/main/java/com/autotech/datos/ConexionDB.java`, completar URL, USER y PASSWORD con los datos locales. La base creada por los scripts es `AutoTechManager`. El repositorio no incluye una contraseña real.
4. Ejecutar `com.autotech.AutoTechManager`.
5. Registrar entradas de inventario antes de consumir repuestos. El catálogo de ejemplo comienza con stock cero.

## Ejecutable y pruebas

`mvn clean package` genera un jar con dependencias en target. Como alternativa, `powershell -ExecutionPolicy Bypass -File compilar.ps1` genera dist/AutoTechManager.jar y su driver en dist/lib. `iniciar.bat` ejecuta esa distribución. Requiere Java 26 y acceso a SQL Server.

`powershell -ExecutionPolicy Bypass -File probar.ps1` compila y ejecuta las pruebas de integración en una base de datos aislada. Requiere permisos SQL para crear bases de pruebas. No inserta datos en la base real.

## Documentación de entrega

- [Informe y capturas](docs/Informe.md)
- [Manual técnico y de uso](docs/Manual-tecnico.md)
- [Diagrama entidad-relación y UML](docs/Diagramas.md)
- [Guion de defensa: servicio básico y cambio de motor](docs/Guion-defensa.md)
- [Resultados de pruebas](docs/Pruebas.md)

Repositorio: https://github.com/6erson/AutoTechManager

La aplicación usa comprobantes internos, sin JasperReports. Las evidencias técnicas y autorizaciones se registran como texto. Para mantener disponibilidad simple, cada mecánico atiende una orden abierta a la vez y cada vehículo admite una orden abierta.
