package com.autotech.ui;

import com.autotech.negocio.TallerService;
import com.autotech.datos.Sql;
import java.awt.*;
import java.util.List;
import javax.swing.*;

public class PanelReportes extends JPanel {
    private final TallerService servicio;
    private final JTable tabla=UI.tabla();
    private final JComboBox<String> tipo=new JComboBox<>(new String[]{"Saldos y órdenes","Stock bajo","Movimientos de inventario","Ingresos por día","Carga de mecánicos","Historial por vehículo","Servicios por vehículo","Repuestos por vehículo","Mecánicos por vehículo","Trazabilidad de motores"});
    private final JComboBox<UI.Opcion> vehiculo=UI.combo(List.of());
    public PanelReportes(TallerService servicio) {
        this.servicio=servicio;setLayout(new BorderLayout());JPanel p=UI.pagina("Reportes e historial");add(p);
        JPanel c=new JPanel(new BorderLayout(0,10));JPanel top=UI.acciones(UI.boton("Consultar",this::consultar),UI.boton("Actualizar vehículos",this::cargarVehiculos));
        top.add(tipo,0);top.add(vehiculo,1);c.add(top,BorderLayout.NORTH);c.add(UI.scroll(tabla));p.add(c);
        p.add(new JLabel("Las consultas por vehículo separan su historial, servicios, repuestos, mecánicos y costos."),BorderLayout.SOUTH);
    }
    public void cargarVehiculos() {UI.tarea(this,()->UI.opciones(servicio.consultar("SELECT VehiculoId,Placa+' - '+Marca+' '+Modelo FROM Vehiculos ORDER BY Placa")),ops->{vehiculo.removeAllItems();for(var o:ops)vehiculo.addItem(o);});}
    public void consultar() {
        String t=(String)tipo.getSelectedItem(); boolean filtra=t.contains("vehículo");
        int id=filtra?Integer.parseInt(UI.valor(vehiculo)):0;
        String sql=switch(t) {
            case "Saldos y órdenes"->"SELECT * FROM vw_SaldosOrdenes ORDER BY OrdenId DESC";
            case "Stock bajo"->"SELECT * FROM vw_StockBajo ORDER BY Stock";
            case "Movimientos de inventario"->"SELECT m.MovimientoId,m.Fecha,r.Nombre,m.Tipo,m.Cantidad,m.OrdenId,m.Motivo FROM MovimientosInventario m JOIN Repuestos r ON r.RepuestoId=m.RepuestoId ORDER BY m.MovimientoId DESC";
            case "Ingresos por día"->"SELECT CAST(Fecha AS DATE) Dia,COUNT(*) Pagos,SUM(Monto) Ingreso FROM Pagos GROUP BY CAST(Fecha AS DATE) ORDER BY Dia DESC";
            case "Carga de mecánicos"->"SELECT m.MecanicoId,m.Nombre,m.Especialidad,m.Activo,(SELECT COUNT(*) FROM OrdenMecanicos om JOIN OrdenesTrabajo o ON o.OrdenId=om.OrdenId WHERE om.MecanicoId=m.MecanicoId AND o.Estado NOT IN(N'Finalizada y entregada',N'Cancelada',N'Rechazada')) OrdenesAbiertas FROM Mecanicos m";
            case "Historial por vehículo"->"SELECT o.OrdenId,o.Fecha,o.FechaEntrega,o.Estado,o.Kilometraje,o.Sintomas,d.Evaluacion,s.Total,s.Abonado,s.Saldo FROM OrdenesTrabajo o LEFT JOIN Diagnosticos d ON d.OrdenId=o.OrdenId JOIN vw_SaldosOrdenes s ON s.OrdenId=o.OrdenId WHERE o.VehiculoId=? ORDER BY o.Fecha DESC";
            case "Servicios por vehículo"->"SELECT o.OrdenId,o.Fecha,d.Nombre,d.Nivel,d.Horas,d.Importe FROM OrdenesTrabajo o JOIN DetalleOrden d ON d.OrdenId=o.OrdenId WHERE o.VehiculoId=? ORDER BY o.OrdenId DESC";
            case "Repuestos por vehículo"->"SELECT o.OrdenId,o.Fecha,r.Nombre,c.Cantidad,c.Precio,c.Cantidad*c.Precio Importe,c.Procesado FROM OrdenesTrabajo o JOIN ConsumosOrden c ON c.OrdenId=o.OrdenId JOIN Repuestos r ON r.RepuestoId=c.RepuestoId WHERE o.VehiculoId=? ORDER BY o.OrdenId DESC";
            case "Mecánicos por vehículo"->"SELECT o.OrdenId,o.Fecha,m.Nombre,m.Especialidad FROM OrdenesTrabajo o JOIN OrdenMecanicos om ON om.OrdenId=o.OrdenId JOIN Mecanicos m ON m.MecanicoId=om.MecanicoId WHERE o.VehiculoId=? ORDER BY o.OrdenId DESC";
            default->"SELECT cm.CambioId,o.OrdenId,v.Placa,cm.SerieAnterior,m.Serie SerieNueva,m.Estado,cm.DestinoRetirado,cm.Pruebas,cm.Garantia FROM CambiosMotor cm JOIN Motores m ON m.MotorId=cm.MotorId JOIN OrdenesTrabajo o ON o.OrdenId=cm.OrdenId JOIN Vehiculos v ON v.VehiculoId=o.VehiculoId";
        };
        UI.tarea(this,()->filtra?servicio.consultar(sql,id):servicio.consultar(sql),tDatos->UI.mostrar(tabla,tDatos));
    }
}
