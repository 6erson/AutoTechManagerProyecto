package com.autotech.negocio;

import com.autotech.datos.*;
import com.autotech.modelo.*;
import java.sql.*;
import java.math.*;
import java.util.*;
import static com.autotech.datos.Sql.*;

public class TallerService {
    @FunctionalInterface
    public interface FabricaConexion {
        Connection abrir() throws SQLException;
    }

    @FunctionalInterface
    private interface Operacion<T> {
        T ejecutar(Connection c) throws SQLException;
    }

    private final FabricaConexion fabrica;

    public TallerService() {
        this(ConexionDB::getConexion);
    }

    public TallerService(FabricaConexion fabrica) {
        this.fabrica = fabrica;
    }

    private <T> T tx(Operacion<T> op) throws SQLException {
        try (Connection c = fabrica.abrir()) {
            c.setAutoCommit(false);
            try {
                T resultado = op.ejecutar(c);
                c.commit();
                return resultado;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public Sql.Tabla consultar(String sql, Object... args) throws SQLException {
        try (Connection c = fabrica.abrir()) {
            return Sql.consultar(c, sql, args);
        }
    }

    private void exigir(boolean condicion, String mensaje) throws SQLException {
        if (!condicion) throw new SQLException(mensaje);
    }

    private boolean existe(Connection c, String sql, Object... args) throws SQLException {
        return escalar(c, sql, args) != null;
    }

    private String texto(String s, String campo, int max) {
        return Validaciones.texto(s, campo, max);
    }

    public static BigDecimal dinero(String valor) {
        try {
            BigDecimal b = new BigDecimal(Presentacion.entrada(valor));
            if (b.signum() >= 0 && b.scale() <= 2 && b.compareTo(new BigDecimal("9999999999.99")) <= 0)
                return b;
        } catch (NumberFormatException ignored) {
        }
        throw new IllegalArgumentException(
                "Escribe un importe válido, positivo o cero, con hasta dos decimales (usa punto).");
    }

    private OrdenTrabajo orden(Connection c, int id, String... estados) throws SQLException {
        Sql.Tabla t =
                Sql.consultar(
                        c,
                        "SELECT OrdenId,VehiculoId,ResponsableId,Estado FROM OrdenesTrabajo"
                            + " WITH(UPDLOCK,HOLDLOCK) WHERE OrdenId=?",
                        id);
        exigir(!t.filas().isEmpty(), "La orden no existe.");
        var f = t.filas().get(0);
        OrdenTrabajo o = new OrdenTrabajo((int) f.get(0), (int) f.get(1), (int) f.get(2), (String) f.get(3));
        exigir(!o.cerrada(), "La orden está cerrada; su historial es de solo lectura.");
        if (estados.length > 0)
            exigir(
                    Arrays.asList(estados).contains(o.estado()),
                    "Esta acción no está disponible en el estado actual de la orden.");
        return o;
    }

    private void disponible(Connection c, int mecanico, int orden) throws SQLException {
        var datos =
                Sql.consultar(
                        c,
                        "SELECT MecanicoId,Nombre,Especialidad,Activo FROM Mecanicos WITH(UPDLOCK,HOLDLOCK)"
                            + " WHERE MecanicoId=?",
                        mecanico);
        exigir(!datos.filas().isEmpty(), "El mecánico no existe.");
        var f = datos.filas().get(0);
        Mecanico personal =
                new Mecanico((int) f.get(0), (String) f.get(1), (String) f.get(2), (boolean) f.get(3));
        exigir(personal.activo(), "El mecánico no está activo.");
        exigir(
                !existe(
                        c,
                        "SELECT om.OrdenId FROM OrdenMecanicos om INNER JOIN OrdenesTrabajo o ON"
                            + " o.OrdenId=om.OrdenId WHERE om.MecanicoId=? AND o.OrdenId<>? AND o.Estado NOT"
                            + " IN(N'Finalizada y entregada',N'Cancelada',N'Rechazada')",
                        mecanico,
                        orden),
                "El mecánico tiene otra orden abierta. Selecciona otro disponible.");
    }

    public Sql.Tabla catalogo(Catalogo cat) throws SQLException {
        return consultar("SELECT * FROM " + cat.tabla + " ORDER BY " + cat.id);
    }

    public void guardarCatalogo(Catalogo cat, int id, List<String> valores) throws SQLException {
        List<Object> datos = new ArrayList<>();
        for (int i = 0; i < cat.campos.size(); i++) {
            Catalogo.Campo f = cat.campos.get(i);
            String v = valores.get(i);
            datos.add(
                    switch (f.tipo()) {
                        case "texto" -> texto(v, f.etiqueta(), f.max());
                        case "dinero" -> dinero(v);
                        case "horas" -> {
                            BigDecimal d = dinero(v);
                            if (d.signum() == 0 || d.compareTo(new BigDecimal("999999.99")) > 0)
                                throw new IllegalArgumentException(
                                        "Horas debe ser mayor que cero y menor que un millón.");
                            yield d;
                        }
                        case "nivel" -> Validaciones.numero(v, "Nivel", 1, 5);
                        case "anio" -> Validaciones.numero(v, "Año", 1900, 2100);
                        case "entero" -> Validaciones.numero(v, f.etiqueta(), 0, Integer.MAX_VALUE);
                        case "unidad" -> {
                            exigir(List.of("Unidad", "Litro", "Juego").contains(v), "Unidad invalida.");
                            yield v;
                        }
                        case "combustible" -> {
                            exigir(
                                    List.of("Gasolina", "Diésel", "Híbrido", "Eléctrico", "GLP").contains(v),
                                    "Combustible inválido.");
                            yield v;
                        }
                        default -> Validaciones.numero(v, f.etiqueta(), 1, Integer.MAX_VALUE);
                    });
        }
        tx(c -> {
            if (cat == Catalogo.MOTORES && id > 0)
                exigir(
                        existe(
                                c,
                                "SELECT MotorId FROM Motores WITH(UPDLOCK,HOLDLOCK) WHERE MotorId=? AND"
                                    + " Estado='Disponible'",
                                id),
                        "No se puede editar un motor reservado o instalado.");
            String columnas = String.join(",", cat.campos.stream().map(Catalogo.Campo::columna).toList());
            if (id == 0)
                ejecutar(
                        c,
                        "INSERT INTO "
                                + cat.tabla
                                + "("
                                + columnas
                                + ") VALUES("
                                + String.join(",", Collections.nCopies(datos.size(), "?"))
                                + ")",
                        datos.toArray());
            else {
                List<Object> a = new ArrayList<>(datos);
                a.add(id);
                exigir(
                        ejecutar(
                                        c,
                                        "UPDATE "
                                                + cat.tabla
                                                + " SET "
                                                + String.join(
                                                        ",",
                                                        cat.campos.stream()
                                                                .map(f -> f.columna() + "=?")
                                                                .toList())
                                                + " WHERE "
                                                + cat.id
                                                + "=?",
                                        a.toArray())
                                == 1,
                        "El registro no existe.");
            }
            return null;
        });
    }

    public void activar(Catalogo cat, int id, boolean activo) throws SQLException {
        exigir(cat != Catalogo.MOTORES, "Los motores se administran mediante disponibilidad y trazabilidad.");
        tx(c -> {
            if (cat == Catalogo.MECANICOS) {
                exigir(
                        existe(
                                c,
                                "SELECT MecanicoId FROM Mecanicos WITH(UPDLOCK,HOLDLOCK) WHERE MecanicoId=?",
                                id),
                        "Mecánico inexistente.");
                exigir(
                        !existe(
                                c,
                                "SELECT om.OrdenId FROM OrdenMecanicos om JOIN OrdenesTrabajo o ON"
                                    + " o.OrdenId=om.OrdenId WHERE om.MecanicoId=? AND o.Estado NOT"
                                    + " IN(N'Finalizada y entregada',N'Cancelada',N'Rechazada')",
                                id),
                        "No cambies el estado de un mecánico con órdenes abiertas.");
            }
            exigir(
                    ejecutar(c, "UPDATE " + cat.tabla + " SET Activo=? WHERE " + cat.id + "=?", activo, id)
                            == 1,
                    "Selecciona un registro existente.");
            return null;
        });
    }

    public void asociarRepuesto(int servicio, int repuesto, int cantidad) throws SQLException {
        exigir(cantidad > 0, "La cantidad debe ser mayor que cero.");
        tx(c -> {
            ejecutar(
                    c,
                    "DELETE FROM ServicioRepuesto WHERE ServicioId=? AND RepuestoId=?",
                    servicio,
                    repuesto);
            ejecutar(c, "INSERT INTO ServicioRepuesto VALUES(?,?,?)", servicio, repuesto, cantidad);
            return null;
        });
    }

    public void entrada(int repuesto, int cantidad, String motivo) throws SQLException {
        exigir(cantidad > 0, "La entrada debe ser positiva.");
        String m = texto(motivo, "motivo", 300);
        tx(c -> {
            exigir(
                    ejecutar(
                                    c,
                                    "UPDATE Repuestos SET Stock=Stock+? WHERE RepuestoId=? AND Activo=1",
                                    cantidad,
                                    repuesto)
                            == 1,
                    "Repuesto inexistente o inactivo.");
            ejecutar(
                    c,
                    "INSERT INTO MovimientosInventario(RepuestoId,Tipo,Cantidad,Motivo)"
                        + " VALUES(?,'Entrada',?,?)",
                    repuesto,
                    cantidad,
                    m);
            return null;
        });
    }

    public int crearOrden(int vehiculo, int responsable, int km, String sintomas, String preliminar)
            throws SQLException {
        String sint = texto(sintomas, "síntomas", 1000),
                diag = texto(preliminar, "diagnóstico preliminar", 2000);
        exigir(km >= 0, "Kilometraje inválido.");
        return tx(c -> {
            Object propietario =
                    escalar(
                            c,
                            "SELECT v.ClienteId FROM Vehiculos v WITH(UPDLOCK,HOLDLOCK) INNER JOIN Clientes"
                                + " cl ON cl.ClienteId=v.ClienteId WHERE v.VehiculoId=? AND cl.Activo=1 AND"
                                + " v.Kilometraje<=?",
                            vehiculo,
                            km);
            exigir(propietario != null, "Vehículo o cliente inactivo, o kilometraje inferior al registrado.");
            exigir(
                    !existe(
                            c,
                            "SELECT OrdenId FROM OrdenesTrabajo WHERE VehiculoId=? AND Estado NOT"
                                + " IN(N'Finalizada y entregada',N'Cancelada',N'Rechazada')",
                            vehiculo),
                    "El vehículo ya tiene una orden abierta.");
            disponible(c, responsable, 0);
            int id =
                    insertar(
                            c,
                            "INSERT INTO"
                                + " OrdenesTrabajo(ClienteId,VehiculoId,Kilometraje,Sintomas,ResponsableId)"
                                + " VALUES(?,?,?,?,?)",
                            propietario,
                            vehiculo,
                            km,
                            sint,
                            responsable);
            ejecutar(c, "INSERT INTO OrdenMecanicos VALUES(?,?)", id, responsable);
            ejecutar(
                    c,
                    "INSERT INTO Diagnosticos(OrdenId,Evaluacion,Evidencias) VALUES(?,?,?)",
                    id,
                    diag,
                    "Evaluación inicial; completar evidencias antes del presupuesto.");
            ejecutar(
                    c,
                    "INSERT INTO ControlesCalidad(OrdenId,Descripcion) VALUES(?,N'Inspección final y prueba"
                        + " de funcionamiento')",
                    id);
            ejecutar(c, "UPDATE Vehiculos SET Kilometraje=? WHERE VehiculoId=?", km, vehiculo);
            return id;
        });
    }

    public void diagnostico(int orden, String evaluacion, String evidencias) throws SQLException {
        String a = texto(evaluacion, "evaluación", 2000), b = texto(evidencias, "evidencias", 2000);
        tx(c -> {
            orden(c, orden, "En diagnóstico");
            ejecutar(
                    c,
                    "UPDATE Diagnosticos SET Evaluacion=?,Evidencias=?,Fecha=SYSDATETIME() WHERE OrdenId=?",
                    a,
                    b,
                    orden);
            return null;
        });
    }

    public void asignar(int orden, int mecanico) throws SQLException {
        tx(c -> {
            orden(c, orden, "Recibida", "En diagnóstico", "En reparación");
            disponible(c, mecanico, orden);
            exigir(
                    !existe(
                            c,
                            "SELECT OrdenId FROM OrdenMecanicos WHERE OrdenId=? AND MecanicoId=?",
                            orden,
                            mecanico),
                    "Ya está asignado a esta orden.");
            ejecutar(c, "INSERT INTO OrdenMecanicos VALUES(?,?)", orden, mecanico);
            return null;
        });
    }

    private void invalidar(Connection c, int orden) throws SQLException {
        ejecutar(c, "DELETE FROM Presupuestos WHERE OrdenId=?", orden);
    }

    public void agregarServicio(int orden, int servicio, BigDecimal horas) throws SQLException {
        exigir(
                horas.signum() > 0 && horas.compareTo(new BigDecimal("999999.99")) <= 0,
                "Horas fuera de rango.");
        tx(c -> {
            OrdenTrabajo o = orden(c, orden, "En diagnóstico");
            var t =
                    Sql.consultar(
                            c,
                            "SELECT Nombre,Nivel,Costo FROM Servicios WHERE ServicioId=? AND Activo=1",
                            servicio);
            exigir(!t.filas().isEmpty(), "Servicio inexistente o inactivo.");
            var f = t.filas().get(0);
            Servicio s = Servicio.crear((String) f.get(0), (BigDecimal) f.get(2), (int) f.get(1));
            ejecutar(
                    c,
                    "INSERT INTO DetalleOrden(OrdenId,ServicioId,Nombre,Nivel,Horas,Importe)"
                        + " VALUES(?,?,?,?,?,?)",
                    orden,
                    servicio,
                    s.nombre(),
                    f.get(1),
                    horas,
                    s.costoManoObra());
            ejecutar(
                    c,
                    "INSERT INTO ActividadesTrabajo(OrdenId,MecanicoId,Descripcion) VALUES(?,?,?)",
                    orden,
                    o.responsableId(),
                    s.nombre() + ": " + s.requisitoEntrega());
            invalidar(c, orden);
            return null;
        });
    }

    private void sumarConsumo(Connection c, int orden, int repuesto, int cantidad) throws SQLException {
        exigir(cantidad > 0, "La cantidad debe ser positiva.");
        Object precio = escalar(c, "SELECT Precio FROM Repuestos WHERE RepuestoId=? AND Activo=1", repuesto);
        exigir(precio != null, "Repuesto inexistente o inactivo.");
        if (ejecutar(
                        c,
                        "UPDATE ConsumosOrden SET Cantidad=Cantidad+? WHERE OrdenId=? AND RepuestoId=? AND"
                            + " Procesado=0",
                        cantidad,
                        orden,
                        repuesto)
                == 0)
            ejecutar(
                    c,
                    "INSERT INTO ConsumosOrden(OrdenId,RepuestoId,Cantidad,Precio) VALUES(?,?,?,?)",
                    orden,
                    repuesto,
                    cantidad,
                    precio);
    }

    public void agregarRepuesto(int orden, int repuesto, int cantidad) throws SQLException {
        tx(c -> {
            orden(c, orden, "En diagnóstico");
            exigir(
                    !existe(c, "SELECT MotorId FROM Motores WHERE RepuestoId=?", repuesto),
                    "Usa Cambio de motor para elegir el motor por su serie.");
            exigir(cantidad > 0, "La cantidad debe ser positiva.");
            Object precio =
                    escalar(c, "SELECT Precio FROM Repuestos WHERE RepuestoId=? AND Activo=1", repuesto);
            exigir(precio != null, "Repuesto inexistente o inactivo.");
            if (ejecutar(
                            c,
                            "UPDATE ConsumosOrden SET Cantidad=? WHERE OrdenId=? AND RepuestoId=? AND"
                                + " Procesado=0",
                            cantidad,
                            orden,
                            repuesto)
                    == 0)
                ejecutar(
                        c,
                        "INSERT INTO ConsumosOrden(OrdenId,RepuestoId,Cantidad,Precio) VALUES(?,?,?,?)",
                        orden,
                        repuesto,
                        cantidad,
                        precio);
            invalidar(c, orden);
            return null;
        });
    }

    public void quitarDetalle(int orden, int detalle, boolean servicio) throws SQLException {
        tx(c -> {
            orden(c, orden, "En diagnóstico");
            if (servicio)
                ejecutar(c, "DELETE FROM DetalleOrden WHERE OrdenId=? AND DetalleId=?", orden, detalle);
            else {
                exigir(
                        !existe(
                                c,
                                "SELECT cm.CambioId FROM CambiosMotor cm JOIN Motores m ON"
                                    + " m.MotorId=cm.MotorId JOIN ConsumosOrden co ON"
                                    + " co.RepuestoId=m.RepuestoId AND co.OrdenId=cm.OrdenId WHERE"
                                    + " cm.OrdenId=? AND co.ConsumoId=?",
                                orden,
                                detalle),
                        "El consumo pertenece al motor reservado.");
                ejecutar(
                        c,
                        "DELETE FROM ConsumosOrden WHERE OrdenId=? AND ConsumoId=? AND Procesado=0",
                        orden,
                        detalle);
            }
            invalidar(c, orden);
            return null;
        });
    }

    public BigDecimal presupuesto(int orden, BigDecimal descuento, BigDecimal impuesto, String autorizo)
            throws SQLException {
        exigir(
                descuento.signum() >= 0
                        && impuesto.signum() >= 0
                        && impuesto.compareTo(new BigDecimal("100")) <= 0,
                "Descuento o impuesto inválido.");
        String autor =
                descuento.signum() > 0
                        ? texto(autorizo, "quien autoriza el descuento", 100)
                        : "Sin descuento";
        return tx(c -> {
            orden(c, orden, "En diagnóstico");
            exigir(
                    existe(c, "SELECT DetalleId FROM DetalleOrden WHERE OrdenId=?", orden),
                    "Agrega al menos un servicio.");
            BigDecimal mano =
                    (BigDecimal)
                            escalar(
                                    c,
                                    "SELECT COALESCE(SUM(Importe),0) FROM DetalleOrden WHERE OrdenId=?",
                                    orden);
            BigDecimal rep =
                    (BigDecimal)
                            escalar(
                                    c,
                                    "SELECT COALESCE(SUM(Precio*Cantidad),0) FROM ConsumosOrden WHERE"
                                        + " OrdenId=?",
                                    orden);
            exigir(descuento.compareTo(mano.add(rep)) <= 0, "El descuento supera el subtotal.");
            BigDecimal total =
                    mano.add(rep)
                            .subtract(descuento)
                            .multiply(BigDecimal.ONE.add(impuesto.movePointLeft(2)))
                            .setScale(2, RoundingMode.HALF_UP);
            invalidar(c, orden);
            ejecutar(
                    c,
                    "INSERT INTO"
                        + " Presupuestos(OrdenId,ManoObra,Repuestos,Descuento,AutorizoDescuento,Impuesto,Total)"
                        + " VALUES(?,?,?,?,?,?,?)",
                    orden,
                    mano,
                    rep,
                    descuento,
                    autor,
                    impuesto,
                    total);
            return total;
        });
    }

    public void revisarPresupuesto(int id) throws SQLException {
        tx(c -> {
            orden(c, id, "Pendiente de aprobación");
            exigir(
                    existe(
                            c,
                            "SELECT PresupuestoId FROM Presupuestos WHERE OrdenId=? AND Decision='Pendiente'",
                            id),
                    "Solo se puede revisar un presupuesto sin decisión del cliente.");
            exigir(
                    !existe(c, "SELECT PagoId FROM Pagos WHERE OrdenId=?", id)
                            && !existe(
                                    c,
                                    "SELECT ConsumoId FROM ConsumosOrden WHERE OrdenId=? AND Procesado=1",
                                    id),
                    "No se puede revisar con pagos o consumos procesados.");
            invalidar(c, id);
            ejecutar(c, "UPDATE OrdenesTrabajo SET Estado=N'En diagnóstico' WHERE OrdenId=?", id);
            return null;
        });
    }

    public void decidir(int orden, boolean aprobar, String autorizacion) throws SQLException {
        String a = texto(autorizacion, "autorización / rechazo del cliente", 200);
        tx(c -> {
            orden(c, orden, "Pendiente de aprobación");
            exigir(
                    ejecutar(
                                    c,
                                    "UPDATE Presupuestos SET Decision=?,Autorizacion=? WHERE OrdenId=? AND"
                                        + " Decision='Pendiente'",
                                    aprobar ? "Aprobado" : "Rechazado",
                                    a,
                                    orden)
                            == 1,
                    "El presupuesto ya fue decidido o no existe.");
            ejecutar(c, "EXEC sp_CambiarEstado ?,?", orden, aprobar ? "En reparación" : "Rechazada");
            return null;
        });
    }

    public void estado(int orden, String nuevo) throws SQLException {
        tx(c -> {
            ejecutar(c, "EXEC sp_CambiarEstado ?,?", orden, nuevo);
            return null;
        });
    }

    public void consumir(int orden) throws SQLException {
        tx(c -> {
            ejecutar(c, "EXEC sp_ConsumirRepuestos ?", orden);
            return null;
        });
    }

    public void actividad(int orden, int mecanico, String descripcion) throws SQLException {
        String d = texto(descripcion, "actividad", 500);
        tx(c -> {
            orden(c, orden, "En diagnóstico", "En reparación");
            exigir(
                    existe(
                            c,
                            "SELECT om.MecanicoId FROM OrdenMecanicos om JOIN Mecanicos m ON"
                                + " m.MecanicoId=om.MecanicoId WHERE om.OrdenId=? AND om.MecanicoId=? AND"
                                + " m.Activo=1",
                            orden,
                            mecanico),
                    "El mecánico debe estar activo y asignado a esta orden.");
            ejecutar(
                    c,
                    "INSERT INTO ActividadesTrabajo(OrdenId,MecanicoId,Descripcion) VALUES(?,?,?)",
                    orden,
                    mecanico,
                    d);
            return null;
        });
    }

    public void completarActividad(int orden, int actividad) throws SQLException {
        tx(c -> {
            orden(c, orden, "En reparación");
            exigir(
                    ejecutar(
                                    c,
                                    "UPDATE ActividadesTrabajo SET Completada=1 WHERE OrdenId=? AND"
                                        + " ActividadId=?",
                                    orden,
                                    actividad)
                            == 1,
                    "Actividad no encontrada.");
            return null;
        });
    }

    public void control(int orden, String descripcion, boolean obligatorio) throws SQLException {
        String d = texto(descripcion, "control", 500);
        tx(c -> {
            orden(c, orden, "En diagnóstico", "En reparación", "En control de calidad");
            ejecutar(
                    c,
                    "INSERT INTO ControlesCalidad(OrdenId,Descripcion,Obligatorio) VALUES(?,?,?)",
                    orden,
                    d,
                    obligatorio);
            return null;
        });
    }

    public void aprobarControl(int orden, int control, String resultado) throws SQLException {
        String r = texto(resultado, "resultado de la prueba", 1000);
        tx(c -> {
            orden(c, orden, "En control de calidad");
            exigir(
                    ejecutar(
                                    c,
                                    "UPDATE ControlesCalidad SET Aprobado=1,Resultado=? WHERE OrdenId=? AND"
                                        + " ControlId=?",
                                    r,
                                    orden,
                                    control)
                            == 1,
                    "Control no encontrado.");
            return null;
        });
    }

    public void reservarMotor(int orden, int motor, String serieAnterior, String destino)
            throws SQLException {
        String serie = texto(serieAnterior, "serie anterior", 60),
                d = texto(destino, "destino del motor retirado", 300);
        tx(c -> {
            OrdenTrabajo o = orden(c, orden, "En diagnóstico");
            exigir(
                    existe(c, "SELECT DetalleId FROM DetalleOrden WHERE OrdenId=? AND Nivel=5", orden),
                    "Agrega el servicio de nivel 5 antes del motor.");
            exigir(
                    !existe(c, "SELECT CambioId FROM CambiosMotor WHERE OrdenId=?", orden),
                    "Esta orden ya tiene un cambio de motor.");
            var t =
                    Sql.consultar(
                            c,
                            "SELECT"
                                + " m.RepuestoId,m.Marca,m.Modelo,m.AnioDesde,m.AnioHasta,m.Combustible,m.Estado,r.Stock,r.Activo"
                                + " FROM Motores m WITH(UPDLOCK,HOLDLOCK) JOIN Repuestos r ON"
                                + " r.RepuestoId=m.RepuestoId WHERE m.MotorId=?",
                            motor);
            exigir(!t.filas().isEmpty(), "Motor inexistente.");
            var m = t.filas().get(0);
            exigir(
                    m.get(6).equals("Disponible") && (int) m.get(7) > 0 && (boolean) m.get(8),
                    "Motor no disponible, sin stock o repuesto inactivo.");
            // Serializa reservas de distintas series que comparten el mismo artículo de inventario.
            int stock =
                    ((Number)
                                    escalar(
                                            c,
                                            "SELECT Stock FROM Repuestos WITH(UPDLOCK,HOLDLOCK) WHERE"
                                                + " RepuestoId=?",
                                            m.get(0)))
                            .intValue();
            int reservas =
                    ((Number)
                                    escalar(
                                            c,
                                            "SELECT COUNT(*) FROM CambiosMotor cm JOIN Motores mo ON"
                                                + " mo.MotorId=cm.MotorId JOIN ConsumosOrden co ON"
                                                + " co.OrdenId=cm.OrdenId AND co.RepuestoId=mo.RepuestoId"
                                                + " WHERE mo.RepuestoId=? AND mo.Estado='Reservado' AND"
                                                + " co.Procesado=0",
                                            m.get(0)))
                            .intValue();
            exigir(stock > reservas, "El stock de este motor ya está reservado en otra orden.");
            var v =
                    Sql.consultar(
                                    c,
                                    "SELECT Marca,Modelo,Anio,Combustible FROM Vehiculos WHERE VehiculoId=?",
                                    o.vehiculoId())
                            .filas()
                            .get(0);
            exigir(
                    m.get(1).toString().equalsIgnoreCase(v.get(0).toString())
                            && m.get(2).toString().equalsIgnoreCase(v.get(1).toString())
                            && (int) v.get(2) >= (int) m.get(3)
                            && (int) v.get(2) <= (int) m.get(4)
                            && m.get(5).toString().equalsIgnoreCase(v.get(3).toString()),
                    "Motor incompatible: verifica marca, modelo, años y combustible.");
            exigir(
                    !existe(
                            c,
                            "SELECT ConsumoId FROM ConsumosOrden WHERE OrdenId=? AND RepuestoId=?",
                            orden,
                            m.get(0)),
                    "Este repuesto ya figura en la orden. Quita su consumo antes de elegir el motor.");
            ejecutar(
                    c,
                    "INSERT INTO CambiosMotor(OrdenId,MotorId,SerieAnterior,DestinoRetirado) VALUES(?,?,?,?)",
                    orden,
                    motor,
                    serie,
                    d);
            ejecutar(c, "UPDATE Motores SET Estado='Reservado' WHERE MotorId=?", motor);
            sumarConsumo(c, orden, (int) m.get(0), 1);
            for (String paso :
                    List.of(
                            "Desmontaje del motor anterior",
                            "Instalación del motor nuevo",
                            "Pruebas del motor instalado"))
                ejecutar(
                        c,
                        "INSERT INTO ActividadesTrabajo(OrdenId,MecanicoId,Descripcion) VALUES(?,?,?)",
                        orden,
                        o.responsableId(),
                        paso);
            ejecutar(
                    c,
                    "INSERT INTO ControlesCalidad(OrdenId,Descripcion) VALUES(?,N'Presión, temperatura, fugas"
                        + " y prueba de ruta del motor')",
                    orden);
            invalidar(c, orden);
            return null;
        });
    }

    public void pruebasMotor(int orden, String pruebas, String garantia) throws SQLException {
        String p = texto(pruebas, "pruebas del motor", 2000), g = texto(garantia, "garantía y vigencia", 500);
        tx(c -> {
            orden(c, orden, "En reparación");
            exigir(
                    ejecutar(c, "UPDATE CambiosMotor SET Pruebas=?,Garantia=? WHERE OrdenId=?", p, g, orden)
                            == 1,
                    "Registra primero el cambio de motor.");
            return null;
        });
    }

    public void pagar(int orden, BigDecimal monto, String metodo, String referencia) throws SQLException {
        String m = texto(metodo, "método de pago", 30), r = texto(referencia, "referencia", 100);
        tx(c -> {
            ejecutar(c, "EXEC sp_RegistrarPago ?,?,?,?", orden, monto, m, r);
            return null;
        });
    }

    public String comprobanteHtml(int orden) throws SQLException {
        try (Connection c = fabrica.abrir()) {
            exigir(
                    existe(
                            c,
                            "SELECT PresupuestoId FROM Presupuestos WHERE OrdenId=? AND Decision='Aprobado'",
                            orden),
                    "La orden no tiene un presupuesto aprobado.");
            StringBuilder b =
                    new StringBuilder(
                            Documento.ficha(
                                    Sql.consultar(
                                            c,
                                            "SELECT Fecha,Cliente,Placa,Estado FROM vw_SaldosOrdenes WHERE"
                                                + " OrdenId=?",
                                            orden)));
            b.append(
                    Documento.tabla(
                            "Servicios",
                            Sql.consultar(
                                    c,
                                    "SELECT Nombre Servicio,Horas,Importe FROM DetalleOrden WHERE OrdenId=?",
                                    orden)));
            b.append(
                    Documento.tabla(
                            "Repuestos",
                            Sql.consultar(
                                    c,
                                    "SELECT r.Nombre"
                                        + " Repuesto,co.Cantidad,r.Unidad,co.Precio,co.Cantidad*co.Precio"
                                        + " Importe FROM ConsumosOrden co JOIN Repuestos r ON"
                                        + " r.RepuestoId=co.RepuestoId WHERE co.OrdenId=?",
                                    orden)));
            b.append("<h2>Resumen de cuenta</h2>")
                    .append(
                            Documento.ficha(
                                    Sql.consultar(
                                            c,
                                            "SELECT p.ManoObra,p.Repuestos,p.ManoObra+p.Repuestos"
                                                + " Subtotal,p.Descuento,p.Impuesto,p.Total-(p.ManoObra+p.Repuestos-p.Descuento)"
                                                + " ImpuestoImporte,p.Total,s.Abonado,s.Saldo FROM"
                                                + " Presupuestos p JOIN vw_SaldosOrdenes s ON"
                                                + " s.OrdenId=p.OrdenId WHERE p.OrdenId=?",
                                            orden)));
            b.append(
                    Documento.tabla(
                            "Pagos recibidos",
                            Sql.consultar(
                                    c,
                                    "SELECT Fecha,Monto,Metodo,Referencia FROM Pagos WHERE OrdenId=? ORDER BY"
                                        + " PagoId",
                                    orden)));
            var motor =
                    Sql.consultar(
                            c,
                            "SELECT cm.SerieAnterior,m.Serie"
                                + " SerieNueva,cm.DestinoRetirado,cm.Pruebas,cm.Garantia FROM CambiosMotor cm"
                                + " JOIN Motores m ON m.MotorId=cm.MotorId WHERE cm.OrdenId=?",
                            orden);
            if (!motor.filas().isEmpty())
                b.append("<h2>Sustitucion de motor y garantia</h2>").append(Documento.ficha(motor));
            b.append(
                    "<br><hr><p>Gracias por confiar en AutoTech Motors.<br>Comprobante interno de servicio."
                        + " No constituye documento fiscal.</p>");
            return Documento.pagina(
                    "AUTOTECH MOTORS", "Comprobante de servicio · Orden #" + orden, b.toString());
        }
    }
}
