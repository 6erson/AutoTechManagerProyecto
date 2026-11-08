package com.autotech.ui;

import com.autotech.negocio.*;
import com.autotech.datos.Sql;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import javax.swing.*;

public class PanelOrdenes extends JPanel {
    private final TallerService servicio;
    private final JTable ordenes=UI.tabla(), detalles=UI.tabla();
    private final JComboBox<String> seccion=new JComboBox<>(new String[]{"Servicios","Repuestos","Diagnóstico","Presupuesto","Mecánicos","Actividades","Calidad","Cambio de motor","Pagos","Auditoría"});
    private final JLabel seleccion=new JLabel("Selecciona una orden para consultar su detalle.");
    private final JLabel mensaje=new JLabel("Sigue el flujo: recepción → diagnóstico → aprobación → reparación → calidad → entrega.");
    private static final String LISTADO="SELECT OrdenId,Placa,Cliente,Estado,Total,Abonado,Saldo FROM vw_SaldosOrdenes ORDER BY OrdenId DESC";
    public PanelOrdenes(TallerService servicio) {
        this.servicio=servicio;setLayout(new BorderLayout());JPanel p=UI.pagina("Órdenes de trabajo");add(p);
        JPanel contenido=new JPanel(new BorderLayout(0,10));
        JPanel acciones=new JPanel(new GridLayout(3,5,6,6));
        String[] nombres={"Nueva orden","Actualizar","Avanzar estado","Cancelar orden","Diagnóstico", "Asignar mecánico","Agregar servicio","Agregar repuesto","Presupuesto","Decisión del cliente", "Cambio de motor","Trabajo y calidad","Procesar consumos","Registrar pago","Comprobante"};
        Runnable[] tareas={this::nueva,this::cargar,this::avanzar,this::cancelar,this::diagnostico,this::asignar,this::agregarServicio,this::agregarRepuesto,this::presupuesto,this::decidir,this::motor,this::trabajo,this::consumir,this::pagar,this::comprobante};
        for(int i=0;i<nombres.length;i++)acciones.add(UI.boton(nombres[i],tareas[i]));
        contenido.add(acciones,BorderLayout.NORTH);
        JSplitPane split=new JSplitPane(JSplitPane.VERTICAL_SPLIT,UI.scroll(ordenes),panelDetalle());
        split.setResizeWeight(0.48);split.setDividerLocation(200);contenido.add(split);
        p.add(contenido);p.add(mensaje,BorderLayout.SOUTH);
        ordenes.getSelectionModel().addListSelectionListener(e->{if(!e.getValueIsAdjusting() && ordenes.getSelectedRow()>=0)cargarDetalle();});
        seccion.addActionListener(e->{if(ordenes.getSelectedRow()>=0)cargarDetalle();});
    }
    private JPanel panelDetalle() {
        JPanel p=new JPanel(new BorderLayout(0,6));JPanel top=new JPanel(new BorderLayout());top.add(seleccion);top.add(seccion,BorderLayout.EAST);
        p.add(top,BorderLayout.NORTH);p.add(UI.scroll(detalles));
        p.add(UI.acciones(UI.boton("Completar actividad",this::completar),UI.boton("Aprobar control",this::aprobar),UI.boton("Quitar servicio / repuesto",this::quitar)),BorderLayout.SOUTH);return p;
    }
    public void cargar() {
        int anterior=ordenes.getSelectedRow()<0?0:UI.id(ordenes);
        UI.tarea(this,()->servicio.consultar(LISTADO),t->{UI.mostrar(ordenes,t);UI.mostrar(detalles,new Sql.Tabla(List.of("Detalle"),List.of()));
            for(int i=0;i<ordenes.getRowCount();i++)if(((Number)ordenes.getValueAt(i,0)).intValue()==anterior){ordenes.setRowSelectionInterval(i,i);break;}
        });
    }
    private int orden() {return UI.id(ordenes);}
    private String estado() {orden();return String.valueOf(ordenes.getValueAt(ordenes.getSelectedRow(),3));}
    private String consultaDetalle(String s) {
        return switch(s) {
            case "Servicios" -> "SELECT DetalleId,Nombre,Nivel,Horas,Importe FROM DetalleOrden WHERE OrdenId=?";
            case "Repuestos" -> "SELECT c.ConsumoId,r.Nombre,c.Cantidad,c.Precio,c.Cantidad*c.Precio Importe,c.Procesado FROM ConsumosOrden c JOIN Repuestos r ON r.RepuestoId=c.RepuestoId WHERE c.OrdenId=?";
            case "Diagnóstico" -> "SELECT DiagnosticoId,Evaluacion,Evidencias,Fecha FROM Diagnosticos WHERE OrdenId=?";
            case "Presupuesto" -> "SELECT PresupuestoId,ManoObra,Repuestos,Descuento,AutorizoDescuento,Impuesto,Total,Decision,Autorizacion FROM Presupuestos WHERE OrdenId=?";
            case "Mecánicos" -> "SELECT m.MecanicoId,m.Nombre,m.Especialidad,CASE WHEN m.MecanicoId=o.ResponsableId THEN N'Principal' ELSE N'Apoyo' END Rol FROM OrdenMecanicos om JOIN Mecanicos m ON m.MecanicoId=om.MecanicoId JOIN OrdenesTrabajo o ON o.OrdenId=om.OrdenId WHERE om.OrdenId=?";
            case "Actividades" -> "SELECT a.ActividadId,m.Nombre,a.Descripcion,a.Completada FROM ActividadesTrabajo a JOIN Mecanicos m ON m.MecanicoId=a.MecanicoId WHERE a.OrdenId=?";
            case "Calidad" -> "SELECT ControlId,Descripcion,Obligatorio,Aprobado,Resultado FROM ControlesCalidad WHERE OrdenId=?";
            case "Cambio de motor" -> "SELECT cm.CambioId,cm.SerieAnterior,m.Serie SerieNueva,cm.DestinoRetirado,cm.Pruebas,cm.Garantia,m.Estado FROM CambiosMotor cm JOIN Motores m ON m.MotorId=cm.MotorId WHERE cm.OrdenId=?";
            case "Pagos" -> "SELECT PagoId,Fecha,Monto,Metodo,Referencia FROM Pagos WHERE OrdenId=?";
            default -> "SELECT AuditoriaId,EstadoAnterior,EstadoNuevo,Fecha,Usuario FROM AuditoriaOrdenes WHERE OrdenId=? ORDER BY AuditoriaId";
        };
    }
    private void cargarDetalle() {
        int id=orden();String tipo=(String)seccion.getSelectedItem();seleccion.setText("Orden #"+id+" — "+estado());
        UI.tarea(this,()->servicio.consultar(consultaDetalle(tipo),id),t->UI.mostrar(detalles,t));
    }
    @FunctionalInterface private interface Accion {void ejecutar() throws Exception;}
    private void ejecutar(Accion a,String mensaje) {
        UI.tarea(this,()->{a.ejecutar();return true;},ok->{this.mensaje.setText(mensaje);cargar();});
    }
    private void elegir(String titulo,String consulta,java.util.function.IntConsumer elegido,Object...args) {
        UI.tarea(this,()->UI.opciones(servicio.consultar(consulta,args)),ops->{
            if(ops.isEmpty())throw new IllegalArgumentException("No hay registros disponibles para esta acción.");
            JComboBox<UI.Opcion> c=UI.combo(ops);
            if(UI.formulario(this,titulo,new String[]{"Selecciona"},c))elegido.accept(Integer.parseInt(UI.valor(c)));
        });
    }
    private void nueva() {
        UI.tarea(this,()->Map.of(
            "vehiculos",UI.opciones(servicio.consultar("SELECT v.VehiculoId,v.Placa+' - '+c.Nombre FROM Vehiculos v JOIN Clientes c ON c.ClienteId=v.ClienteId WHERE c.Activo=1 AND NOT EXISTS(SELECT 1 FROM OrdenesTrabajo o WHERE o.VehiculoId=v.VehiculoId AND o.Estado NOT IN(N'Finalizada y entregada',N'Cancelada',N'Rechazada'))")),
            "mecanicos",UI.opciones(servicio.consultar("SELECT m.MecanicoId,m.Nombre FROM Mecanicos m WHERE m.Activo=1 AND NOT EXISTS(SELECT 1 FROM OrdenMecanicos om JOIN OrdenesTrabajo o ON o.OrdenId=om.OrdenId WHERE om.MecanicoId=m.MecanicoId AND o.Estado NOT IN(N'Finalizada y entregada',N'Cancelada',N'Rechazada'))"))),datos->{
            JComboBox<UI.Opcion> v=UI.combo(datos.get("vehiculos")),m=UI.combo(datos.get("mecanicos"));
            JTextField km=new JTextField();JTextArea sintomas=UI.area(),diag=UI.area();
            if(UI.formulario(this,"Recepción del vehículo",new String[]{"Vehículo","Responsable disponible","Kilometraje de ingreso","Síntomas","Diagnóstico preliminar"},v,m,km,sintomas,diag)) {
                int vi=Integer.parseInt(UI.valor(v)),mi=Integer.parseInt(UI.valor(m)),k=Validaciones.numero(km.getText(),"Kilometraje",0,Integer.MAX_VALUE);
                String s=sintomas.getText(),d=diag.getText();
                UI.tarea(this,()->servicio.crearOrden(vi,mi,k,s,d),id->{mensaje.setText("Orden #"+id+" recibida.");cargar();});
            }
        });
    }
    private void avanzar() {
        int id=orden();String actual=estado();
        String nuevo=switch(actual) {
            case "Recibida"->"En diagnóstico";case "En diagnóstico"->"Pendiente de aprobación";
            case "En reparación"->"En control de calidad";case "En control de calidad"->"Finalizada y entregada";
            default->throw new IllegalArgumentException("Usa Decisión del cliente para aprobar/rechazar el presupuesto. Las órdenes cerradas no avanzan.");};
        if(JOptionPane.showConfirmDialog(this,"¿Mover orden #"+id+" a "+nuevo+"?","Estado",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)
            ejecutar(()->servicio.estado(id,nuevo),"Estado actualizado: "+nuevo);
    }
    private void cancelar() {int id=orden();if(JOptionPane.showConfirmDialog(this,"¿Cancelar la orden #"+id+"?","Cancelar orden",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)ejecutar(()->servicio.estado(id,"Cancelada"),"Orden cancelada.");}
    private void diagnostico() {
        int id=orden();UI.tarea(this,()->servicio.consultar("SELECT Evaluacion,Evidencias FROM Diagnosticos WHERE OrdenId=?",id),t->{
            JTextArea a=UI.area(),b=UI.area();if(!t.filas().isEmpty()){a.setText(t.filas().get(0).get(0).toString());b.setText(t.filas().get(0).get(1).toString());}
            if(UI.formulario(this,"Evaluación técnica",new String[]{"Diagnóstico","Evidencias / mediciones"},a,b)){String x=a.getText(),y=b.getText();ejecutar(()->servicio.diagnostico(id,x,y),"Diagnóstico registrado.");}
        });
    }
    private void asignar() {int id=orden();elegir("Mecánico de apoyo","SELECT MecanicoId,Nombre FROM Mecanicos WHERE Activo=1",m->ejecutar(()->servicio.asignar(id,m),"Mecánico asignado."));}
    private void agregarServicio() {
        int id=orden();UI.tarea(this,()->servicio.consultar("SELECT ServicioId,Nombre,Horas,Costo,Nivel FROM Servicios WHERE Activo=1"),t->{
            JComboBox<UI.Opcion> c=UI.combo(UI.opciones(t));JTextField h=new JTextField();JLabel precio=new JLabel();
            Runnable cambio=()->{if(c.getSelectedIndex()>=0){var f=t.filas().get(c.getSelectedIndex());h.setText(f.get(2).toString());precio.setText("L "+f.get(3)+"  / Nivel "+f.get(4));}};
            c.addActionListener(e->cambio.run());cambio.run();
            if(UI.formulario(this,"Servicio (costo base fijo por trabajo)",new String[]{"Servicio","Horas estimadas","Mano de obra"},c,h,precio)) {
                int s=Integer.parseInt(UI.valor(c));BigDecimal horas=TallerService.dinero(h.getText());
                ejecutar(()->servicio.agregarServicio(id,s,horas),"Servicio agregado y presupuesto anterior invalidado.");
            }
        });
    }
    private void agregarRepuesto() {
        int id=orden();UI.tarea(this,()->UI.opciones(servicio.consultar("SELECT RepuestoId,Nombre+' / stock: '+CAST(Stock AS VARCHAR(12)) FROM Repuestos WHERE Activo=1 AND RepuestoId NOT IN(SELECT RepuestoId FROM Motores)")),ops->{
            JComboBox<UI.Opcion> c=UI.combo(ops);JTextField n=new JTextField("1");
            if(UI.formulario(this,"Planificar consumo (no descuenta todavía)",new String[]{"Repuesto","Cantidad"},c,n)) {
                int r=Integer.parseInt(UI.valor(c)),cant=Validaciones.numero(n.getText(),"Cantidad",1,Integer.MAX_VALUE);
                ejecutar(()->servicio.agregarRepuesto(id,r,cant),"Consumo planificado. Genera de nuevo el presupuesto.");
            }
        });
    }
    private void presupuesto() {
        int id=orden();JTextField d=new JTextField("0"),i=new JTextField("15"),a=new JTextField();
        if(UI.formulario(this,"Presupuesto desglosado",new String[]{"Descuento autorizado (L)","Impuesto configurable (%)","Quién autoriza el descuento"},d,i,a)) {
            BigDecimal des=TallerService.dinero(d.getText()),imp=TallerService.dinero(i.getText());String autor=a.getText();
            UI.tarea(this,()->servicio.presupuesto(id,des,imp,autor),total->{mensaje.setText("Presupuesto generado: L "+total);cargar();});
        }
    }
    private void decidir() {
        int id=orden();JComboBox<String> decision=new JComboBox<>(new String[]{"Aprobar","Rechazar"});JTextField autor=new JTextField();
        if(UI.formulario(this,"Decisión del cliente",new String[]{"Decisión","Nombre del cliente / constancia"},decision,autor)) {
            boolean si=decision.getSelectedIndex()==0;String a=autor.getText();ejecutar(()->servicio.decidir(id,si,a),si?"Presupuesto aprobado; orden en reparación.":"Presupuesto rechazado; orden cerrada.");
        }
    }
    private void motor() {
        int id=orden();String[] opciones={"Seleccionar motor compatible","Registrar pruebas y garantía"};
        int eleccion=JOptionPane.showOptionDialog(this,"Selecciona la etapa del cambio de motor.","Cambio de motor",JOptionPane.DEFAULT_OPTION,JOptionPane.PLAIN_MESSAGE,null,opciones,opciones[0]);
        if(eleccion==0)UI.tarea(this,()->UI.opciones(servicio.consultar("SELECT MotorId,Serie+' / '+Marca+' '+Modelo+' / '+Estado FROM Motores ORDER BY MotorId")),ops->{
            JComboBox<UI.Opcion> m=UI.combo(ops);JTextField serie=new JTextField(),destino=new JTextField();
            if(UI.formulario(this,"Reserva y compatibilidad del motor",new String[]{"Motor nuevo","Serie del motor anterior","Destino del motor retirado"},m,serie,destino)) {
                int nuevo=Integer.parseInt(UI.valor(m));String s=serie.getText(),d=destino.getText();
                ejecutar(()->servicio.reservarMotor(id,nuevo,s,d),"Motor reservado; actividades y consumo registrados.");
            }
        });
        if(eleccion==1) {JTextArea pruebas=UI.area(),garantia=UI.area();
            if(UI.formulario(this,"Pruebas y garantía",new String[]{"Resultados de pruebas","Garantía: plazo y condiciones"},pruebas,garantia)){String p=pruebas.getText(),g=garantia.getText();ejecutar(()->servicio.pruebasMotor(id,p,g),"Pruebas y garantía registradas.");}}
    }
    private void trabajo() {
        int id=orden();String[] opciones={"Nueva actividad","Nuevo control de calidad"};
        int e=JOptionPane.showOptionDialog(this,"Completa o aprueba los registros desde la tabla inferior.","Trabajo y calidad",JOptionPane.DEFAULT_OPTION,JOptionPane.PLAIN_MESSAGE,null,opciones,opciones[0]);
        if(e==0)UI.tarea(this,()->UI.opciones(servicio.consultar("SELECT m.MecanicoId,m.Nombre FROM OrdenMecanicos om JOIN Mecanicos m ON m.MecanicoId=om.MecanicoId WHERE om.OrdenId=?",id)),ops->{
            JComboBox<UI.Opcion> m=UI.combo(ops);JTextArea d=UI.area();
            if(UI.formulario(this,"Actividad de trabajo",new String[]{"Mecánico asignado","Actividad"},m,d)){int mi=Integer.parseInt(UI.valor(m));String desc=d.getText();ejecutar(()->servicio.actividad(id,mi,desc),"Actividad creada.");}
        });
        if(e==1) {JTextField d=new JTextField();JComboBox<String> obligatorio=new JComboBox<>(new String[]{"Obligatorio","Opcional"});
            if(UI.formulario(this,"Control de calidad",new String[]{"Descripción","Tipo"},d,obligatorio)){String desc=d.getText();boolean ob=obligatorio.getSelectedIndex()==0;ejecutar(()->servicio.control(id,desc,ob),"Control creado.");}}
    }
    private void completar() {int id=orden();if(!seccion.getSelectedItem().equals("Actividades"))throw new IllegalArgumentException("Selecciona la sección Actividades y una fila.");int a=UI.id(detalles);ejecutar(()->servicio.completarActividad(id,a),"Actividad completada.");}
    private void aprobar() {int id=orden();if(!seccion.getSelectedItem().equals("Calidad"))throw new IllegalArgumentException("Selecciona la sección Calidad y una fila.");int c=UI.id(detalles);JTextArea r=UI.area();if(UI.formulario(this,"Resultado de control aprobado",new String[]{"Resultado / evidencia"},r)){String resultado=r.getText();ejecutar(()->servicio.aprobarControl(id,c,resultado),"Control aprobado.");}}
    private void quitar() {int id=orden();String s=(String)seccion.getSelectedItem();if(!s.equals("Servicios")&&!s.equals("Repuestos"))throw new IllegalArgumentException("Selecciona Servicios o Repuestos y una fila.");int d=UI.id(detalles);ejecutar(()->servicio.quitarDetalle(id,d,s.equals("Servicios")),"Detalle retirado. Revisa actividades y vuelve a generar el presupuesto.");}
    private void consumir() {int id=orden();if(JOptionPane.showConfirmDialog(this,"¿Descontar del inventario todos los consumos pendientes de la orden #"+id+"?","Procesar consumos",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)ejecutar(()->servicio.consumir(id),"Inventario actualizado mediante transacción.");}
    private void pagar() {
        int id=orden();JTextField monto=new JTextField(),referencia=new JTextField();JComboBox<String> metodo=new JComboBox<>(new String[]{"Efectivo","Tarjeta","Transferencia"});
        if(UI.formulario(this,"Pago parcial o total",new String[]{"Monto (L)","Método","Referencia / recibo"},monto,metodo,referencia)) {
            BigDecimal m=TallerService.dinero(monto.getText());String met=UI.valor(metodo),r=referencia.getText();ejecutar(()->servicio.pagar(id,m,met,r),"Pago registrado y saldo actualizado.");
        }
    }
    private void comprobante() {int id=orden();UI.tarea(this,()->servicio.comprobante(id),s->UI.texto(this,"Comprobante #"+id,s));}
}
