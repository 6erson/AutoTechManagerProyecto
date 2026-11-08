package com.autotech;

import com.autotech.datos.*;
import com.autotech.negocio.*;
import java.sql.*;
import java.nio.file.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

public class PruebasIntegracion {
    private static String url, user, password, bd;
    private static TallerService taller;
    private static int aprobadas;

    @FunctionalInterface
    interface Prueba {
        void ejecutar() throws Exception;
    }

    private static String dato(String n) throws Exception {
        Field f = ConexionDB.class.getDeclaredField(n);
        f.setAccessible(true);
        return (String) f.get(null);
    }

    private static Connection abrir() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    private static void ok(boolean c, String msg) {
        if (!c) throw new AssertionError(msg);
        aprobadas++;
        System.out.println("OK: " + msg);
    }

    private static void falla(Prueba p, String msg) throws Exception {
        try {
            p.ejecutar();
        } catch (SQLException | IllegalArgumentException e) {
            ok(true, msg);
            return;
        }
        throw new AssertionError("No se bloqueó: " + msg);
    }

    private static int id(String sql, Object... a) throws Exception {
        try (Connection c = abrir()) {
            return ((Number) Sql.escalar(c, sql, a)).intValue();
        }
    }

    private static void sql(String sql, Object... a) throws Exception {
        try (Connection c = abrir()) {
            Sql.ejecutar(c, sql, a);
        }
    }

    private static void script(Connection c, String archivo) throws Exception {
        for (String lote :
                Files.readString(Path.of(archivo)).replace("AutoTechManager", bd).split("(?im)^GO\s*$"))
            if (!lote.isBlank())
                try (Statement s = c.createStatement()) {
                    s.execute(lote);
                }
    }

    private static int vehiculo(int cliente, String placa, String modelo) throws Exception {
        try (Connection c = abrir()) {
            return Sql.insertar(
                    c,
                    "INSERT Vehiculos(ClienteId,Placa,VIN,Marca,Modelo,Anio,Color,Kilometraje,Combustible)"
                        + " VALUES(?,?,?,N'Toyota',?,2015,N'Gris',100000,N'Diésel')",
                    cliente,
                    placa,
                    "VIN-" + placa,
                    modelo);
        }
    }

    private static void concurrentes(Prueba a, Prueba b, String msg) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch salida = new CountDownLatch(1);
        Callable<Boolean> uno =
                () -> {
                    salida.await();
                    try {
                        a.ejecutar();
                        return true;
                    } catch (SQLException e) {
                        return false;
                    }
                };
        Callable<Boolean> dos =
                () -> {
                    salida.await();
                    try {
                        b.ejecutar();
                        return true;
                    } catch (SQLException e) {
                        return false;
                    }
                };
        try {
            Future<Boolean> f = pool.submit(uno), g = pool.submit(dos);
            salida.countDown();
            ok(f.get(20, TimeUnit.SECONDS) ^ g.get(20, TimeUnit.SECONDS), msg);
        } finally {
            pool.shutdownNow();
        }
    }

    private static void cerrar(int orden) throws Exception {
        var actividades =
                taller.consultar("SELECT ActividadId FROM ActividadesTrabajo WHERE OrdenId=?", orden);
        for (var f : actividades.filas()) taller.completarActividad(orden, (int) f.get(0));
        taller.estado(orden, "En control de calidad");
        falla(() -> taller.estado(orden, "Finalizada y entregada"), "Entrega bloqueada sin calidad aprobada");
        var controles = taller.consultar("SELECT ControlId FROM ControlesCalidad WHERE OrdenId=?", orden);
        for (var f : controles.filas())
            taller.aprobarControl(orden, (int) f.get(0), "Prueba satisfactoria y sin fugas");
        taller.estado(orden, "Finalizada y entregada");
    }

    public static void main(String[] args) throws Exception {
        bd = "AutoTechManager_Pruebas_" + System.currentTimeMillis();
        user = dato("USER");
        password = dato("PASSWORD");
        url = dato("URL").replaceAll("databaseName=[^;]*;", "databaseName=" + bd + ";");
        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        try (Connection c = DriverManager.getConnection(url.replace(bd, "master"), user, password)) {
            script(c, "sql/01_clientes_vehiculos.sql");
            script(c, "sql/02_taller_completo.sql");
            script(c, "sql/03_catalogos_iniciales.sql");
            script(c, "sql/04_unidades_repuestos.sql");
        }
        taller = new TallerService(PruebasIntegracion::abrir);
        try {
            int cliente;
            try (Connection c = abrir()) {
                cliente =
                        Sql.insertar(
                                c,
                                "INSERT Clientes(Nombre,Telefono,Correo) VALUES(N'Cliente de"
                                    + " prueba','99998888','prueba@demo.com')");
            }
            int v1 = vehiculo(cliente, "TEST-01", "Hilux"), v2 = vehiculo(cliente, "TEST-02", "Hilux");
            int m1 = id("SELECT MIN(MecanicoId) FROM Mecanicos"),
                    m2 = id("SELECT MAX(MecanicoId) FROM Mecanicos"),
                    m3 = id("SELECT MecanicoId FROM Mecanicos WHERE Nombre=N'Ana López'");
            int aceite = id("SELECT RepuestoId FROM Repuestos WHERE Codigo='R001'"),
                    filtro = id("SELECT RepuestoId FROM Repuestos WHERE Codigo='R002'"),
                    motor = id("SELECT MotorId FROM Motores WHERE Serie='DEMO-HILUX-001'"),
                    rm = id("SELECT RepuestoId FROM Repuestos WHERE Codigo='R003'");
            int basico = id("SELECT ServicioId FROM Servicios WHERE Codigo='S001'"),
                    mayor = id("SELECT ServicioId FROM Servicios WHERE Codigo='S005'");
            taller.entrada(aceite, 6, "Carga de prueba");
            taller.entrada(filtro, 1, "Carga de prueba");
            taller.entrada(rm, 1, "Motor de prueba");
            int o = taller.crearOrden(v1, m1, 100001, "Cambio de aceite", "Mantenimiento periódico");
            falla(
                    () -> taller.crearOrden(v2, m1, 100001, "Prueba", "Prueba"),
                    "Mecánico ocupado no admite otra orden");
            falla(
                    () -> taller.crearOrden(v1, m2, 100001, "Prueba", "Prueba"),
                    "Vehículo con orden abierta bloqueado");
            taller.estado(o, "En diagnóstico");
            taller.diagnostico(o, "Aceite degradado", "Inspección y medición de nivel");
            taller.agregarServicio(o, basico, BigDecimal.ONE);
            ok(
                    id("SELECT COUNT(*) FROM ConsumosOrden WHERE OrdenId=?", o) == 0,
                    "Agregar servicio no genera cantidades ocultas");
            taller.agregarRepuesto(o, aceite, 6);
            taller.agregarRepuesto(o, filtro, 1);
            taller.agregarRepuesto(o, filtro, 1);
            ok(
                    id("SELECT Cantidad FROM ConsumosOrden WHERE OrdenId=? AND RepuestoId=?", o, filtro) == 1,
                    "Registrar un filtro dos veces mantiene la cantidad total en uno");
            BigDecimal total = taller.presupuesto(o, BigDecimal.ZERO, new BigDecimal("15"), "");
            ok(
                    total.compareTo(new BigDecimal("2127.50")) == 0,
                    "Presupuesto: mano de obra + aceite/filtro + impuesto");
            taller.estado(o, "Pendiente de aprobación");
            taller.revisarPresupuesto(o);
            ok(
                    id("SELECT COUNT(*) FROM Presupuestos WHERE OrdenId=?", o) == 0,
                    "Revisión invalida el presupuesto pendiente");
            taller.presupuesto(o, BigDecimal.ZERO, new BigDecimal("15"), "");
            taller.estado(o, "Pendiente de aprobación");
            taller.decidir(o, true, "Cliente de prueba autoriza");
            falla(() -> taller.revisarPresupuesto(o), "No se revisa un presupuesto ya aprobado");
            falla(() -> taller.agregarRepuesto(o, filtro, 1), "Presupuesto aprobado congela detalles");
            sql("UPDATE Servicios SET Costo=9999 WHERE ServicioId=?", basico);
            ok(
                    id("SELECT CAST(Importe AS INT) FROM DetalleOrden WHERE OrdenId=?", o) == 350,
                    "Cambio de precio no altera historial");
            falla(() -> taller.estado(o, "En control de calidad"), "Actividades pendientes bloquean avance");
            sql("UPDATE Repuestos SET Stock=0 WHERE RepuestoId=?", filtro);
            falla(() -> taller.consumir(o), "Stock insuficiente revierte consumo completo");
            ok(
                    id("SELECT Stock FROM Repuestos WHERE RepuestoId=?", aceite) == 6,
                    "Rollback conserva aceite al faltar filtro");
            taller.entrada(filtro, 1, "Reposición de prueba");
            concurrentes(
                    () -> taller.consumir(o),
                    () -> taller.consumir(o),
                    "Consumo concurrente se ejecuta una sola vez");
            ok(
                    id("SELECT Stock FROM Repuestos WHERE RepuestoId=?", aceite) == 0,
                    "Stock descontado exactamente una vez");
            concurrentes(
                    () -> taller.pagar(o, new BigDecimal("1500"), "Efectivo", "P1"),
                    () -> taller.pagar(o, new BigDecimal("1500"), "Efectivo", "P2"),
                    "Pagos concurrentes no producen sobrepago");
            taller.pagar(o, new BigDecimal("627.50"), "Efectivo", "Saldo");
            falla(() -> taller.pagar(o, BigDecimal.ONE, "Efectivo", "Extra"), "Sobrepago bloqueado");
            cerrar(o);
            ok(taller.comprobanteHtml(o).contains("L 2,127.50"), "Comprobante básico conserva importes");
            falla(() -> taller.diagnostico(o, "Cambiar", "Cambiar"), "Orden finalizada protege su historial");
            int om =
                    taller.crearOrden(
                            v2,
                            m3,
                            100010,
                            "Sobrecalentamiento, pérdida de potencia y daños internos",
                            "Se requiere sustituir el motor");
            taller.estado(om, "En diagnóstico");
            taller.diagnostico(om, "Daño interno irreparable", "Compresión baja y desgaste de cilindros");
            taller.agregarServicio(om, mayor, new BigDecimal("24"));
            falla(() -> taller.reservarMotor(om, motor, "", "Taller"), "Motor exige serie anterior");
            sql("UPDATE Motores SET Modelo=N'Corolla' WHERE MotorId=?", motor);
            falla(
                    () -> taller.reservarMotor(om, motor, "ANT-001", "Reciclaje autorizado"),
                    "Motor incompatible bloqueado");
            sql("UPDATE Motores SET Modelo=N'Hilux' WHERE MotorId=?", motor);
            taller.reservarMotor(om, motor, "ANT-001", "Motor retirado para devolución al cliente");
            falla(
                    () -> taller.reservarMotor(om, motor, "ANT-002", "Taller"),
                    "Motor ya reservado no se duplica");
            int otroMotor;
            try (Connection c = abrir()) {
                otroMotor =
                        Sql.insertar(
                                c,
                                "INSERT"
                                    + " Motores(RepuestoId,Serie,Marca,Modelo,AnioDesde,AnioHasta,Combustible)"
                                    + " VALUES(?,N'DEMO-HILUX-002',N'Toyota',N'Hilux',2012,2016,N'Diésel')",
                                rm);
            }
            int vReserva = vehiculo(cliente, "TEST-RES", "Hilux"),
                    oReserva = taller.crearOrden(vReserva, m1, 100000, "Reserva", "Diagnóstico");
            taller.estado(oReserva, "En diagnóstico");
            taller.agregarServicio(oReserva, mayor, new BigDecimal("24"));
            falla(
                    () -> taller.reservarMotor(oReserva, otroMotor, "ANT-RES", "Taller"),
                    "Reservas de series distintas no exceden stock compartido");
            taller.estado(oReserva, "Cancelada");
            falla(
                    () -> taller.activar(Catalogo.MECANICOS, m3, false),
                    "No se desactiva un mecánico con orden abierta");
            taller.asignar(om, m2);
            taller.presupuesto(om, new BigDecimal("1000"), new BigDecimal("15"), "Jefe de taller");
            taller.estado(om, "Pendiente de aprobación");
            taller.decidir(om, true, "Cliente autoriza sustitución");
            taller.consumir(om);
            for (var f :
                    taller.consultar("SELECT ActividadId FROM ActividadesTrabajo WHERE OrdenId=?", om)
                            .filas()) taller.completarActividad(om, (int) f.get(0));
            falla(() -> taller.estado(om, "En control de calidad"), "Motor exige pruebas y garantía");
            taller.pruebasMotor(
                    om,
                    "Temperatura normal, presión estable y ruta de 10 km sin fugas",
                    "90 días / 5000 km por instalación");
            cerrar(om);
            ok(
                    id("SELECT COUNT(*) FROM Motores WHERE MotorId=? AND Estado='Instalado'", motor) == 1,
                    "Motor instalado conserva trazabilidad");
            ok(
                    id("SELECT COUNT(*) FROM AuditoriaOrdenes WHERE OrdenId=?", om) >= 6,
                    "Trigger audita el flujo completo");
            ok(
                    taller.comprobanteHtml(om).contains("DEMO-HILUX-001"),
                    "Comprobante incluye motor, pruebas y garantía");
            int v3 = vehiculo(cliente, "TEST-03", "Hilux"),
                    oc = taller.crearOrden(v3, m1, 100000, "Prueba cancelación", "Prueba");
            taller.estado(oc, "Cancelada");
            ok(
                    id("SELECT COUNT(*) FROM OrdenesTrabajo WHERE OrdenId=? AND Estado=N'Cancelada'", oc)
                            == 1,
                    "Cancelación sin consumos ni pagos");
            int or = taller.crearOrden(v3, m1, 100001, "Prueba rechazo", "Prueba");
            taller.estado(or, "En diagnóstico");
            taller.agregarServicio(or, basico, BigDecimal.ONE);
            taller.presupuesto(or, BigDecimal.ZERO, BigDecimal.ZERO, "");
            taller.estado(or, "Pendiente de aprobación");
            taller.decidir(or, false, "Cliente no acepta");
            ok(
                    id("SELECT COUNT(*) FROM OrdenesTrabajo WHERE OrdenId=? AND Estado=N'Rechazada'", or)
                            == 1,
                    "Rechazo cierra orden y conserva historial");
            taller.entrada(rm, 1, "Segundo motor disponible");
            int v4 = vehiculo(cliente, "TEST-04", "Hilux"),
                    o4 = taller.crearOrden(v4, m1, 100000, "Motor", "Daño interno");
            taller.estado(o4, "En diagnóstico");
            taller.agregarServicio(o4, mayor, new BigDecimal("24"));
            falla(
                    () -> taller.reservarMotor(o4, motor, "ANT-4", "Taller"),
                    "Motor instalado no se puede usar en otra orden");
            taller.reservarMotor(o4, otroMotor, "ANT-4", "Devolver al cliente");
            taller.presupuesto(o4, BigDecimal.ZERO, BigDecimal.ZERO, "");
            taller.estado(o4, "Pendiente de aprobación");
            taller.decidir(o4, false, "Cliente no acepta motor");
            ok(
                    id("SELECT COUNT(*) FROM Motores WHERE MotorId=? AND Estado='Disponible'", otroMotor)
                            == 1,
                    "Rechazo libera motor reservado");
            ok(
                    id("SELECT Stock FROM Repuestos WHERE RepuestoId=?", rm) == 1,
                    "Rechazo conserva stock no consumido");
            ok(
                    id("SELECT CAST(Saldo AS INT) FROM vw_SaldosOrdenes WHERE OrdenId=?", o4) == 0,
                    "Orden rechazada no genera deuda");
            taller.guardarCatalogo(Catalogo.PROVEEDORES, 0, List.of("Proveedor QA", "9999-1111"));
            int pq = id("SELECT ProveedorId FROM Proveedores WHERE Nombre='Proveedor QA'");
            taller.guardarCatalogo(Catalogo.PROVEEDORES, pq, List.of("Proveedor editado QA", "9999-2222"));
            taller.activar(Catalogo.PROVEEDORES, pq, false);
            ok(
                    id(
                                    "SELECT COUNT(*) FROM Proveedores WHERE ProveedorId=? AND"
                                        + " Nombre='Proveedor editado QA' AND Activo=0",
                                    pq)
                            == 1,
                    "CRUD de catálogos conserva edición y desactivación");
            falla(
                    () ->
                            taller.guardarCatalogo(
                                    Catalogo.SERVICIOS,
                                    0,
                                    List.of("QA", "Servicio QA", "Descripción", "9", "100", "1")),
                    "Catálogo rechaza nivel fuera de 1 a 5");
            falla(
                    () ->
                            taller.guardarCatalogo(
                                    Catalogo.REPUESTOS,
                                    0,
                                    List.of(
                                            "QA",
                                            "Repuesto QA",
                                            String.valueOf(pq),
                                            "Unidad",
                                            "-1",
                                            "100",
                                            "120")),
                    "Catálogo rechaza existencias mínimas negativas");
            System.out.println("RESULTADO: " + aprobadas + " comprobaciones aprobadas. Base aislada: " + bd);
        } finally {
            System.out.println("La base real AutoTechManager no recibió datos de prueba.");
        }
    }
}
