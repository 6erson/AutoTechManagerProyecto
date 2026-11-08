package com.autotech.ui;

import com.autotech.negocio.TallerService;
import com.autotech.datos.Sql;
import java.awt.*;
import java.util.List;
import javax.swing.*;

public class PanelReportes extends JPanel {
    private final TallerService servicio;
    private final JTable tabla = UI.tabla();
    private final JComboBox<String> tipo =
            new JComboBox<>(
                    new String[] {
                        "Saldos y órdenes",
                        "Stock bajo",
                        "Movimientos de inventario",
                        "Ingresos por día",
                        "Carga de mecánicos",
                        "Historial por vehículo",
                        "Servicios por vehículo",
                        "Repuestos por vehículo",
                        "Mecánicos por vehículo",
                        "Trazabilidad de motores"
                    });
    private final JComboBox<UI.Opcion> vehiculo = UI.combo(List.of());
    private final JLabel resultado = new JLabel(" ", SwingConstants.CENTER);

    public PanelReportes(TallerService servicio) {
        this.servicio = servicio;
        setLayout(new BorderLayout());
        JPanel p = UI.pagina();
        add(p);
        JPanel c = new JPanel(new BorderLayout(0, 10));
        JPanel top =
                UI.acciones(
                        UI.boton("Consultar", this::consultar),
                        UI.boton("Actualizar vehiculos", this::cargarVehiculos));
        top.add(tipo, 0);
        top.add(vehiculo, 1);
        c.add(top, BorderLayout.NORTH);
        c.add(UI.scroll(tabla));
        p.add(c);
        p.add(resultado, BorderLayout.SOUTH);
        vehiculo.addItem(new UI.Opcion(0, "Todos los vehiculos"));
        tipo.addActionListener(e -> {
            actualizarFiltro();
            limpiar();
        });
        vehiculo.addActionListener(e -> limpiar());
        UI.presentar(this);
    }

    private boolean admiteVehiculo() {
        int i = tipo.getSelectedIndex();
        return i == 0 || i >= 5;
    }

    private void actualizarFiltro() {
        vehiculo.setEnabled(admiteVehiculo());
    }

    private void limpiar() {
        UI.mostrar(tabla, new Sql.Tabla(List.of("Resultados"), List.of()));
        resultado.setText(" ");
    }

    public void cargarVehiculos() {
        int previo = vehiculo.getSelectedItem() instanceof UI.Opcion op ? op.id() : 0;
        UI.tarea(
                this,
                () ->
                        UI.opciones(
                                servicio.consultar(
                                        "SELECT VehiculoId,Marca+' '+Modelo+' '+CAST(Anio AS VARCHAR(4))+' |"
                                            + " '+Placa FROM Vehiculos ORDER BY Placa")),
                ops -> {
                    vehiculo.removeAllItems();
                    vehiculo.addItem(new UI.Opcion(0, "Todos los vehiculos"));
                    for (var o : ops) {
                        vehiculo.addItem(o);
                        if (o.id() == previo) vehiculo.setSelectedItem(o);
                    }
                    actualizarFiltro();
                    consultar();
                });
    }

    public void consultar() {
        int tipoReporte = tipo.getSelectedIndex();
        int id = admiteVehiculo() && vehiculo.getSelectedItem() instanceof UI.Opcion op ? op.id() : 0;
        String filtro = " WHERE (?=0 OR o.VehiculoId=?) ";
        String sql =
                switch (tipoReporte) {
                    case 0 ->
                            "SELECT s.* FROM vw_SaldosOrdenes s JOIN OrdenesTrabajo o ON o.OrdenId=s.OrdenId"
                                    + filtro
                                    + "ORDER BY s.OrdenId DESC";
                    case 1 ->
                            "SELECT b.*,r.Unidad FROM vw_StockBajo b JOIN Repuestos r ON"
                                + " r.RepuestoId=b.RepuestoId ORDER BY b.Stock";
                    case 2 ->
                            "SELECT"
                                + " m.MovimientoId,m.Fecha,r.Nombre,m.Tipo,m.Cantidad,r.Unidad,m.OrdenId,m.Motivo"
                                + " FROM MovimientosInventario m JOIN Repuestos r ON"
                                + " r.RepuestoId=m.RepuestoId ORDER BY m.MovimientoId DESC";
                    case 3 ->
                            "SELECT CAST(Fecha AS DATE) Dia,COUNT(*) Pagos,SUM(Monto) Ingreso FROM Pagos"
                                + " GROUP BY CAST(Fecha AS DATE) ORDER BY Dia DESC";
                    case 4 ->
                            "SELECT m.MecanicoId,m.Nombre,m.Especialidad,m.Activo,(SELECT COUNT(*) FROM"
                                + " OrdenMecanicos om JOIN OrdenesTrabajo o ON o.OrdenId=om.OrdenId WHERE"
                                + " om.MecanicoId=m.MecanicoId AND o.Estado NOT IN(N'Finalizada y"
                                + " entregada',N'Cancelada',N'Rechazada')) OrdenesAbiertas FROM Mecanicos m";
                    case 5 ->
                            "SELECT"
                                + " o.OrdenId,o.Fecha,o.FechaEntrega,o.Estado,o.Kilometraje,o.Sintomas,d.Evaluacion,s.Total,s.Abonado,s.Saldo"
                                + " FROM OrdenesTrabajo o LEFT JOIN Diagnosticos d ON d.OrdenId=o.OrdenId"
                                + " JOIN vw_SaldosOrdenes s ON s.OrdenId=o.OrdenId"
                                    + filtro
                                    + "ORDER BY o.Fecha DESC";
                    case 6 ->
                            "SELECT o.OrdenId,o.Fecha,d.Nombre,d.Nivel,d.Horas,d.Importe FROM OrdenesTrabajo"
                                + " o JOIN DetalleOrden d ON d.OrdenId=o.OrdenId"
                                    + filtro
                                    + "ORDER BY o.OrdenId DESC";
                    case 7 ->
                            "SELECT"
                                + " o.OrdenId,o.Fecha,r.Nombre,c.Cantidad,r.Unidad,c.Precio,c.Cantidad*c.Precio"
                                + " Importe,c.Procesado FROM OrdenesTrabajo o JOIN ConsumosOrden c ON"
                                + " c.OrdenId=o.OrdenId JOIN Repuestos r ON r.RepuestoId=c.RepuestoId"
                                    + filtro
                                    + "ORDER BY o.OrdenId DESC";
                    case 8 ->
                            "SELECT o.OrdenId,o.Fecha,m.Nombre,m.Especialidad FROM OrdenesTrabajo o JOIN"
                                + " OrdenMecanicos om ON om.OrdenId=o.OrdenId JOIN Mecanicos m ON"
                                + " m.MecanicoId=om.MecanicoId"
                                    + filtro
                                    + "ORDER BY o.OrdenId DESC";
                    default ->
                            "SELECT cm.CambioId,o.OrdenId,v.Placa,cm.SerieAnterior,m.Serie"
                                + " SerieNueva,m.Estado,cm.DestinoRetirado,cm.Pruebas,cm.Garantia FROM"
                                + " CambiosMotor cm JOIN Motores m ON m.MotorId=cm.MotorId JOIN"
                                + " OrdenesTrabajo o ON o.OrdenId=cm.OrdenId JOIN Vehiculos v ON"
                                + " v.VehiculoId=o.VehiculoId"
                                    + filtro;
                };
        boolean filtrable = admiteVehiculo();
        UI.tarea(
                this,
                () -> filtrable ? servicio.consultar(sql, id, id) : servicio.consultar(sql),
                datos -> {
                    UI.mostrar(tabla, datos);
                    actualizarFiltro();
                    resultado.setText(
                            datos.filas().isEmpty()
                                    ? (id > 0 ? "Sin registros para este vehiculo" : "Sin registros")
                                    : com.autotech.negocio.Presentacion.numero(datos.filas().size(), 0)
                                            + " registros");
                });
    }
}
