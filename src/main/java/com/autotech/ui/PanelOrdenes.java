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
    private final JTable ordenes = UI.tabla(), detalles = UI.tabla();
    private final JComboBox<String> seccion =
            new JComboBox<>(
                    new String[] {
                        "Servicios",
                        "Repuestos",
                        "Diagnóstico",
                        "Presupuesto",
                        "Mecánicos",
                        "Actividades",
                        "Calidad",
                        "Cambio de motor",
                        "Pagos",
                        "Auditoría"
                    });
    private final JLabel seleccion = new JLabel("Selecciona una orden para consultar su detalle.");
    private final JLabel mensaje = new JLabel(" ");
    private final JPanel guia = new JPanel(new BorderLayout(6, 6));
    private final JPanel accionesEtapa = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
    private final JLabel pista = new JLabel("Selecciona una orden o crea una nueva para comenzar.");
    private final JLabel pendientes = new JLabel("Aquí verás qué está listo y qué falta en cada etapa.");
    private final JPanel pasos = new JPanel(new GridLayout(1, 6, 6, 0));
    private final String[] etapas = {
        "Recepción", "Diagnóstico", "Aprobación", "Reparación", "Calidad", "Entrega"
    };
    private final JLabel[] indicadores = new JLabel[6];
    private boolean mostrando;
    private String ultimaEtapa = "";
    private int seleccionarId;
    private static final String LISTADO =
            "SELECT OrdenId,Placa,Cliente,Estado,Total,Abonado,Saldo FROM vw_SaldosOrdenes ORDER BY OrdenId"
                + " DESC";

    public PanelOrdenes(TallerService servicio) {
        this.servicio = servicio;
        setLayout(new BorderLayout());
        JPanel p = UI.pagina();
        add(p);
        JPanel contenido = new JPanel(new BorderLayout(0, 10));
        JPanel arriba = new JPanel(new BorderLayout(0, 8));
        arriba.add(
                UI.acciones(UI.boton("+ Nueva orden", this::nueva), UI.boton("Actualizar", this::cargar)),
                BorderLayout.NORTH);
        for (int i = 0; i < etapas.length; i++) {
            indicadores[i] = new JLabel((i + 1) + ". " + etapas[i], SwingConstants.CENTER);
            indicadores[i].setOpaque(true);
            indicadores[i].setBorder(BorderFactory.createEmptyBorder(9, 4, 9, 4));
            pasos.add(indicadores[i]);
        }

        JPanel textos = new JPanel(new GridLayout(2, 1, 0, 5));
        pista.setHorizontalAlignment(SwingConstants.CENTER);
        pendientes.setHorizontalAlignment(SwingConstants.CENTER);
        pista.setFont(new Font("Segoe UI", Font.BOLD, 15));
        textos.add(pista);
        textos.add(pendientes);

        JPanel cuerpoGuia = new JPanel(new BorderLayout(0, 8));
        cuerpoGuia.add(textos, BorderLayout.NORTH);
        cuerpoGuia.add(accionesEtapa);
        guia.add(pasos, BorderLayout.NORTH);
        guia.add(cuerpoGuia);
        guia.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(65, 80, 99)),
                        BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        arriba.add(guia);
        contenido.add(arriba, BorderLayout.NORTH);
        colorear(-1);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, UI.scroll(ordenes), panelDetalle());
        split.setResizeWeight(0.48);
        split.setDividerLocation(200);
        contenido.add(split);
        p.add(contenido);
        p.add(mensaje, BorderLayout.SOUTH);

        ordenes.getSelectionModel()
                .addListSelectionListener(e -> {
                    if (!mostrando && !e.getValueIsAdjusting() && ordenes.getSelectedRow() >= 0)
                        cargarDetalle();
                });
        seccion.addActionListener(e -> {
            if (!mostrando && ordenes.getSelectedRow() >= 0) cargarDetalle();
        });
    }

    private JPanel panelDetalle() {
        JPanel p = new JPanel(new BorderLayout(0, 6));
        JPanel top = new JPanel(new BorderLayout());
        top.add(seleccion);
        top.add(seccion, BorderLayout.EAST);
        p.add(top, BorderLayout.NORTH);
        p.add(UI.scroll(detalles));
        return p;
    }

    public void cargar() {
        int anterior =
                seleccionarId > 0 ? seleccionarId : (ordenes.getSelectedRow() < 0 ? 0 : UI.id(ordenes));
        seleccionarId = 0;
        UI.tarea(
                this,
                () -> servicio.consultar(LISTADO),
                t -> {
                    mostrando = true;
                    UI.mostrar(ordenes, t);
                    UI.mostrar(detalles, new Sql.Tabla(List.of("Detalle"), List.of()));
                    int fila = -1;
                    for (int i = 0; i < ordenes.getRowCount(); i++)
                        if (((Number) ordenes.getValueAt(i, 0)).intValue() == anterior) fila = i;
                    if (fila < 0 && ordenes.getRowCount() > 0) fila = 0;
                    if (fila >= 0) ordenes.setRowSelectionInterval(fila, fila);
                    mostrando = false;
                    if (fila >= 0) cargarDetalle();
                    else {
                        colorear(-1);
                        accionesEtapa.removeAll();
                        pista.setText("Crea tu primera orden con + Nueva orden.");
                        pendientes.setText("Antes: registra cliente, vehículo y un mecánico disponible.");
                        seleccion.setText("Sin órdenes registradas.");
                        accionesEtapa.revalidate();
                        accionesEtapa.repaint();
                    }
                });
    }

    private int orden() {
        return UI.id(ordenes);
    }

    private String estado() {
        orden();
        return String.valueOf(ordenes.getValueAt(ordenes.getSelectedRow(), 3));
    }

    private String consultaDetalle(String s) {
        return switch (s) {
            case "Servicios" ->
                    "SELECT DetalleId,Nombre,Nivel,Horas,Importe FROM DetalleOrden WHERE OrdenId=?";
            case "Repuestos" ->
                    "SELECT c.ConsumoId,r.Nombre,c.Cantidad,r.Unidad,c.Precio,c.Cantidad*c.Precio"
                        + " Importe,c.Procesado FROM ConsumosOrden c JOIN Repuestos r ON"
                        + " r.RepuestoId=c.RepuestoId WHERE c.OrdenId=?";
            case "Diagnóstico" ->
                    "SELECT DiagnosticoId,Evaluacion,Evidencias,Fecha FROM Diagnosticos WHERE OrdenId=?";
            case "Presupuesto" ->
                    "SELECT PresupuestoId,ManoObra,Repuestos,ManoObra+Repuestos"
                        + " Subtotal,Descuento,AutorizoDescuento,Impuesto,Total-(ManoObra+Repuestos-Descuento)"
                        + " ImpuestoImporte,Total,Decision,Autorizacion FROM Presupuestos WHERE OrdenId=?";
            case "Mecánicos" ->
                    "SELECT m.MecanicoId,m.Nombre,m.Especialidad,CASE WHEN m.MecanicoId=o.ResponsableId THEN"
                        + " N'Principal' ELSE N'Apoyo' END Rol FROM OrdenMecanicos om JOIN Mecanicos m ON"
                        + " m.MecanicoId=om.MecanicoId JOIN OrdenesTrabajo o ON o.OrdenId=om.OrdenId WHERE"
                        + " om.OrdenId=?";
            case "Actividades" ->
                    "SELECT a.ActividadId,m.Nombre,a.Descripcion,a.Completada FROM ActividadesTrabajo a JOIN"
                        + " Mecanicos m ON m.MecanicoId=a.MecanicoId WHERE a.OrdenId=?";
            case "Calidad" ->
                    "SELECT ControlId,Descripcion,Obligatorio,Aprobado,Resultado FROM ControlesCalidad WHERE"
                        + " OrdenId=?";
            case "Cambio de motor" ->
                    "SELECT cm.CambioId,cm.SerieAnterior,m.Serie"
                        + " SerieNueva,cm.DestinoRetirado,cm.Pruebas,cm.Garantia,m.Estado FROM CambiosMotor"
                        + " cm JOIN Motores m ON m.MotorId=cm.MotorId WHERE cm.OrdenId=?";
            case "Pagos" -> "SELECT PagoId,Fecha,Monto,Metodo,Referencia FROM Pagos WHERE OrdenId=?";
            default ->
                    "SELECT AuditoriaId,EstadoAnterior,EstadoNuevo,Fecha,Usuario FROM AuditoriaOrdenes WHERE"
                        + " OrdenId=? ORDER BY AuditoriaId";
        };
    }

    private void cargarDetalle() {
        int id = orden();
        String clave = id + ":" + estado();
        if (!clave.equals(ultimaEtapa)) {
            ultimaEtapa = clave;
            mostrando = true;
            seccion.setSelectedItem(
                    switch (estado()) {
                        case "Recibida", "En diagnóstico" -> "Diagnóstico";
                        case "Pendiente de aprobación" -> "Presupuesto";
                        case "En reparación" -> "Actividades";
                        case "En control de calidad" -> "Calidad";
                        case "Finalizada y entregada" -> "Pagos";
                        default -> "Auditoría";
                    });
            mostrando = false;
        }
        String tipo = (String) seccion.getSelectedItem();
        seleccion.setText("Orden #" + id + " — " + estado());
        UI.tarea(
                this,
                () ->
                        new DatosGuia(
                                servicio.consultar(consultaDetalle(tipo), id),
                                servicio.consultar(
                                        "SELECT (SELECT COUNT(*) FROM DetalleOrden WHERE OrdenId=o.OrdenId)"
                                            + " Servicios, (SELECT COUNT(*) FROM Presupuestos WHERE"
                                            + " OrdenId=o.OrdenId) Presupuesto, (SELECT COUNT(*) FROM"
                                            + " ActividadesTrabajo WHERE OrdenId=o.OrdenId AND Completada=0)"
                                            + " Actividades, (SELECT COUNT(*) FROM ConsumosOrden WHERE"
                                            + " OrdenId=o.OrdenId AND Procesado=0) Consumos, (SELECT COUNT(*)"
                                            + " FROM ControlesCalidad WHERE OrdenId=o.OrdenId AND"
                                            + " Obligatorio=1 AND Aprobado=0) Calidad, (SELECT COUNT(*) FROM"
                                            + " DetalleOrden WHERE OrdenId=o.OrdenId AND Nivel=5) Mayor,"
                                            + " (SELECT COUNT(*) FROM CambiosMotor WHERE OrdenId=o.OrdenId)"
                                            + " Motor, (SELECT COUNT(*) FROM CambiosMotor WHERE"
                                            + " OrdenId=o.OrdenId AND (LEN(LTRIM(Pruebas))=0 OR"
                                            + " LEN(LTRIM(Garantia))=0)) Pruebas ,(SELECT COUNT(*) FROM"
                                            + " Diagnosticos WHERE OrdenId=o.OrdenId AND"
                                            + " LEN(LTRIM(Evaluacion))>0 AND LEN(LTRIM(Evidencias))>0 AND"
                                            + " Evidencias NOT LIKE N'Evaluación inicial;%') DiagnosticoListo"
                                            + " FROM OrdenesTrabajo o WHERE o.OrdenId=?",
                                        id)),
                datos -> {
                    if (ordenes.getSelectedRow() < 0 || orden() != id) return;
                    UI.mostrar(detalles, datos.detalle());
                    if (!datos.resumen().filas().isEmpty()) actualizarGuia(datos.resumen().filas().get(0));
                });
    }

    private record DatosGuia(Sql.Tabla detalle, Sql.Tabla resumen) {}

    private void colorear(int actual) {
        for (int i = 0; i < indicadores.length; i++) {
            indicadores[i].setBackground(
                    i == actual ? TemaOscuro.AZUL : i < actual ? new Color(35, 85, 74) : TemaOscuro.PANEL);
            indicadores[i].setForeground(i <= actual ? Color.WHITE : new Color(153, 166, 184));
            indicadores[i].setText((i < actual ? "OK · " : (i + 1) + ". ") + etapas[i]);
        }
    }

    private void accion(String nombre, Runnable r) {
        accionesEtapa.add(UI.boton(nombre, r));
    }

    private void ver(String nombre) {
        if (seccion.getSelectedItem().equals(nombre)) cargarDetalle();
        else seccion.setSelectedItem(nombre);
    }

    private void actualizarGuia(List<Object> r) {
        int servicios = ((Number) r.get(0)).intValue(),
                pres = ((Number) r.get(1)).intValue(),
                act = ((Number) r.get(2)).intValue(),
                cons = ((Number) r.get(3)).intValue(),
                cal = ((Number) r.get(4)).intValue(),
                mayor = ((Number) r.get(5)).intValue(),
                motorRegistrado = ((Number) r.get(6)).intValue(),
                pruebas = ((Number) r.get(7)).intValue(),
                diag = ((Number) r.get(8)).intValue();
        String e = estado();
        accionesEtapa.removeAll();
        int paso =
                switch (e) {
                    case "Recibida" -> 0;
                    case "En diagnóstico" -> 1;
                    case "Pendiente de aprobación" -> 2;
                    case "En reparación" -> 3;
                    case "En control de calidad" -> 4;
                    case "Finalizada y entregada" -> 5;
                    default -> -1;
                };
        colorear(paso == 5 ? 6 : paso);
        switch (e) {
            case "Recibida" -> {
                pista.setText("Siguiente: iniciar el diagnóstico del vehículo");
                pendientes.setText("Recepción lista. Revisa los síntomas y comienza la evaluación técnica.");
                accion("Iniciar diagnóstico →", this::avanzar);
            }
            case "En diagnóstico" -> {
                pista.setText(
                        diag == 0
                                ? "Siguiente: completar diagnóstico y evidencias"
                                : servicios == 0
                                        ? "Siguiente: agregar el servicio"
                                        : mayor > 0 && motorRegistrado == 0
                                                ? "Siguiente: seleccionar el motor compatible"
                                                : pres == 0
                                                        ? "Siguiente: preparar el presupuesto"
                                                        : "Siguiente: enviar el presupuesto al cliente");
                pendientes.setText(
                        "Diagnóstico: "
                                + (diag > 0 ? "listo" : "pendiente")
                                + "  •  Servicios: "
                                + servicios
                                + "  •  Presupuesto: "
                                + (pres > 0 ? "listo" : "pendiente")
                                + (mayor > 0
                                        ? "  •  Motor: " + (motorRegistrado > 0 ? "registrado" : "pendiente")
                                        : ""));
                accion("1. Diagnóstico", this::diagnostico);
                accion("2. Agregar servicio", this::agregarServicio);
                accion("Agregar repuesto", this::agregarRepuesto);
                if (mayor > 0) accion("Seleccionar motor", this::motor);
                accion("3. Presupuesto", this::presupuesto);
                if (diag > 0 && pres > 0 && (mayor == 0 || motorRegistrado > 0))
                    accion("Enviar a aprobación →", this::avanzar);
            }
            case "Pendiente de aprobación" -> {
                pista.setText("Siguiente: registrar la decisión del cliente");
                pendientes.setText(
                        "El presupuesto está preparado. Registra la autorización o el rechazo para"
                            + " continuar.");
                accion("Ver presupuesto", () -> ver("Presupuesto"));
                accion("Corregir presupuesto", this::revisarPresupuesto);
                accion("Registrar decisión →", this::decidir);
            }
            case "En reparación" -> {
                pista.setText(
                        act > 0
                                ? "Siguiente: completar las actividades de reparación"
                                : cons > 0
                                        ? "Siguiente: descontar los repuestos utilizados"
                                        : pruebas > 0
                                                ? "Siguiente: registrar pruebas y garantía"
                                                : "Reparación lista: continúa al control de calidad");
                pendientes.setText(
                        "Actividades pendientes: "
                                + act
                                + "  •  Consumos pendientes: "
                                + cons
                                + (mayor > 0
                                        ? "  •  Pruebas y garantía: "
                                                + (pruebas > 0 ? "pendientes" : "listas")
                                        : ""));
                accion("Ver actividades (" + act + ")", () -> ver("Actividades"));
                if (seccion.getSelectedItem().equals("Actividades"))
                    accion("Completar seleccionada", this::completar);
                if (cons > 0) accion("Procesar consumos (" + cons + ")", this::consumir);
                if (mayor > 0) accion("Pruebas y garantía", this::motor);
                if (act == 0 && cons == 0 && pruebas == 0) accion("Pasar a calidad →", this::avanzar);
            }
            case "En control de calidad" -> {
                pista.setText(
                        cal > 0
                                ? "Siguiente: aprobar los controles de calidad pendientes"
                                : "Todo listo: entrega el vehículo");
                pendientes.setText(
                        "Controles obligatorios pendientes: "
                                + cal
                                + ". Selecciona cada control y registra su resultado.");
                accion("Ver controles (" + cal + ")", () -> ver("Calidad"));
                if (seccion.getSelectedItem().equals("Calidad"))
                    accion("Aprobar seleccionado", this::aprobar);
                if (cal == 0) accion("Entregar vehículo →", this::avanzar);
            }
            case "Finalizada y entregada" -> {
                pista.setText("Vehículo entregado · Consulta el saldo y genera el comprobante");
                pendientes.setText(
                        "Saldo: "
                                + com.autotech.negocio.Documento.valor(
                                        "Saldo", ordenes.getValueAt(ordenes.getSelectedRow(), 6))
                                + ". El historial de esta orden está protegido.");
                accion("Registrar pago", this::pagar);
                accion("Comprobante", this::comprobante);
            }
            default -> {
                pista.setText("Orden " + e.toLowerCase() + " · Historial disponible");
                pendientes.setText(
                        "Esta orden está cerrada. Puedes consultar sus detalles en la tabla inferior.");
            }
        }
        if (paso >= 0 && paso < 5) {
            JPopupMenu menu = new JPopupMenu();
            if (paso == 0 || paso == 1 || paso == 3)
                agregarMenu(menu, "Asignar mecánico de apoyo", this::asignar);
            if (paso == 1 || paso == 3 || paso == 4)
                agregarMenu(menu, "Agregar actividad / control", this::trabajo);
            if (paso == 1) agregarMenu(menu, "Quitar servicio / repuesto seleccionado", this::quitar);
            if (paso >= 3) {
                agregarMenu(menu, "Registrar pago", this::pagar);
                agregarMenu(menu, "Comprobante", this::comprobante);
            }
            agregarMenu(menu, "Cancelar orden", this::cancelar);
            JButton mas = UI.boton("Más opciones...", () -> {});
            mas.addActionListener(ev -> menu.show(mas, 0, mas.getHeight()));
            accionesEtapa.add(mas);
        }
        accionesEtapa.revalidate();
        accionesEtapa.repaint();
    }

    private void agregarMenu(JPopupMenu m, String nombre, Runnable r) {
        JMenuItem item = new JMenuItem(com.autotech.negocio.Presentacion.sinTildes(nombre));
        item.addActionListener(e -> {
            try {
                r.run();
            } catch (Exception ex) {
                UI.error(this, ex);
            }
        });
        m.add(item);
    }

    @FunctionalInterface
    private interface Accion {
        void ejecutar() throws Exception;
    }

    private void ejecutar(Accion a, String mensaje) {
        UI.tarea(
                this,
                () -> {
                    a.ejecutar();
                    return true;
                },
                ok -> {
                    this.mensaje.setText(mensaje);
                    cargar();
                });
    }

    private void elegir(
            String titulo, String consulta, java.util.function.IntConsumer elegido, Object... args) {
        UI.tarea(
                this,
                () -> UI.opciones(servicio.consultar(consulta, args)),
                ops -> {
                    if (ops.isEmpty())
                        throw new IllegalArgumentException("No hay registros disponibles para esta acción.");
                    JComboBox<UI.Opcion> c = UI.combo(ops);
                    if (UI.formulario(this, titulo, new String[] {"Selecciona"}, c))
                        elegido.accept(Integer.parseInt(UI.valor(c)));
                });
    }

    private void nueva() {
        UI.tarea(
                this,
                () ->
                        Map.of(
                                "vehiculos",
                                        UI.opciones(
                                                servicio.consultar(
                                                        "SELECT v.VehiculoId,v.Marca+' '+v.Modelo+'"
                                                            + " '+CAST(v.Anio AS VARCHAR(4))+' | '+v.Placa+'"
                                                            + " | '+c.Nombre FROM Vehiculos v JOIN Clientes c"
                                                            + " ON c.ClienteId=v.ClienteId WHERE c.Activo=1"
                                                            + " AND NOT EXISTS(SELECT 1 FROM OrdenesTrabajo o"
                                                            + " WHERE o.VehiculoId=v.VehiculoId AND o.Estado"
                                                            + " NOT IN(N'Finalizada y"
                                                            + " entregada',N'Cancelada',N'Rechazada'))")),
                                "mecanicos",
                                        UI.opciones(
                                                servicio.consultar(
                                                        "SELECT m.MecanicoId,m.Nombre FROM Mecanicos m WHERE"
                                                            + " m.Activo=1 AND NOT EXISTS(SELECT 1 FROM"
                                                            + " OrdenMecanicos om JOIN OrdenesTrabajo o ON"
                                                            + " o.OrdenId=om.OrdenId WHERE"
                                                            + " om.MecanicoId=m.MecanicoId AND o.Estado NOT"
                                                            + " IN(N'Finalizada y"
                                                            + " entregada',N'Cancelada',N'Rechazada'))"))),
                datos -> {
                    JComboBox<UI.Opcion> v = UI.combo(datos.get("vehiculos")),
                            m = UI.combo(datos.get("mecanicos"));
                    JTextField km = new JTextField();
                    JTextArea sintomas = UI.area(), diag = UI.area();
                    if (UI.formulario(
                            this,
                            "Recepción del vehículo",
                            new String[] {
                                "Vehículo",
                                "Responsable disponible",
                                "Kilometraje de ingreso",
                                "Síntomas",
                                "Diagnóstico preliminar"
                            },
                            v,
                            m,
                            km,
                            sintomas,
                            diag)) {
                        int vi = Integer.parseInt(UI.valor(v)),
                                mi = Integer.parseInt(UI.valor(m)),
                                k = Validaciones.numero(km.getText(), "Kilometraje", 0, Integer.MAX_VALUE);
                        String s = sintomas.getText(), d = diag.getText();
                        UI.tarea(
                                this,
                                () -> servicio.crearOrden(vi, mi, k, s, d),
                                id -> {
                                    mensaje.setText("Orden #" + id + " recibida.");
                                    seleccionarId = id;
                                    cargar();
                                });
                    }
                });
    }

    private void avanzar() {
        int id = orden();
        String actual = estado();
        String nuevo =
                switch (actual) {
                    case "Recibida" -> "En diagnóstico";
                    case "En diagnóstico" -> "Pendiente de aprobación";
                    case "En reparación" -> "En control de calidad";
                    case "En control de calidad" -> "Finalizada y entregada";
                    default ->
                            throw new IllegalArgumentException(
                                    "Usa Decisión del cliente para aprobar/rechazar el presupuesto. Las"
                                        + " órdenes cerradas no avanzan.");
                };
        if (Dialogos.showConfirmDialog(
                        this,
                        "¿Mover orden #" + id + " a " + nuevo + "?",
                        "Estado",
                        JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION)
            ejecutar(() -> servicio.estado(id, nuevo), "Estado actualizado: " + nuevo);
    }

    private void cancelar() {
        int id = orden();
        if (Dialogos.showConfirmDialog(
                        this, "¿Cancelar la orden #" + id + "?", "Cancelar orden", JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION)
            ejecutar(() -> servicio.estado(id, "Cancelada"), "Orden cancelada.");
    }

    private void diagnostico() {
        int id = orden();
        UI.tarea(
                this,
                () ->
                        servicio.consultar(
                                "SELECT Evaluacion,Evidencias FROM Diagnosticos WHERE OrdenId=?", id),
                t -> {
                    JTextArea a = UI.area(), b = UI.area();
                    if (!t.filas().isEmpty()) {
                        a.setText(t.filas().get(0).get(0).toString());
                        b.setText(t.filas().get(0).get(1).toString());
                    }
                    if (UI.formulario(
                            this,
                            "Evaluación técnica",
                            new String[] {"Diagnóstico", "Evidencias / mediciones"},
                            a,
                            b)) {
                        String x = a.getText(), y = b.getText();
                        ejecutar(() -> servicio.diagnostico(id, x, y), "Diagnóstico registrado.");
                    }
                });
    }

    private void asignar() {
        int id = orden();
        elegir(
                "Mecánico de apoyo",
                "SELECT MecanicoId,Nombre FROM Mecanicos WHERE Activo=1",
                m -> ejecutar(() -> servicio.asignar(id, m), "Mecánico asignado."));
    }

    private void agregarServicio() {
        int id = orden();
        UI.tarea(
                this,
                () ->
                        servicio.consultar(
                                "SELECT ServicioId,Nombre,Horas,Costo,Nivel FROM Servicios WHERE Activo=1"),
                t -> {
                    JComboBox<UI.Opcion> c = UI.combo(UI.opciones(t));
                    JTextField h = new JTextField();
                    JLabel precio = new JLabel();
                    Runnable cambio =
                            () -> {
                                if (c.getSelectedIndex() >= 0) {
                                    var f = t.filas().get(c.getSelectedIndex());
                                    h.setText(f.get(2).toString());
                                    precio.setText("L " + f.get(3) + "  / Nivel " + f.get(4));
                                }
                            };
                    c.addActionListener(e -> cambio.run());
                    cambio.run();
                    if (UI.formulario(
                            this,
                            "Servicio (costo base fijo por trabajo)",
                            new String[] {"Servicio", "Horas estimadas", "Mano de obra", "Repuestos"},
                            c,
                            h,
                            precio,
                            new JLabel("Se registran por separado"))) {
                        int s = Integer.parseInt(UI.valor(c));
                        BigDecimal horas = TallerService.dinero(h.getText());
                        ejecutar(
                                () -> servicio.agregarServicio(id, s, horas),
                                "Servicio agregado y presupuesto anterior invalidado.");
                    }
                });
    }

    private void agregarRepuesto() {
        int id = orden();
        UI.tarea(
                this,
                () ->
                        servicio.consultar(
                                "SELECT r.RepuestoId,r.Nombre+' ('+r.Unidad+') / stock:"
                                    + " '+FORMAT(r.Stock,'N0','en-US'),COALESCE(co.Cantidad,0) CantidadActual"
                                    + " FROM Repuestos r LEFT JOIN ConsumosOrden co ON"
                                    + " co.RepuestoId=r.RepuestoId AND co.OrdenId=? WHERE r.Activo=1 AND"
                                    + " r.RepuestoId NOT IN(SELECT RepuestoId FROM Motores)",
                                id),
                datos -> {
                    JComboBox<UI.Opcion> c = UI.combo(UI.opciones(datos));
                    JTextField n = new JTextField("1");
                    JLabel actual = new JLabel();
                    Runnable seleccion =
                            () -> {
                                if (c.getSelectedIndex() < 0) return;
                                int cantidad =
                                        ((Number) datos.filas().get(c.getSelectedIndex()).get(2)).intValue();
                                n.setText(String.valueOf(cantidad > 0 ? cantidad : 1));
                                actual.setText(
                                        cantidad > 0
                                                ? "Registrado en la orden: " + cantidad
                                                : "Sin registrar en esta orden");
                            };
                    c.addActionListener(e -> seleccion.run());
                    seleccion.run();
                    if (UI.formulario(
                            this,
                            "Repuestos de la orden",
                            new String[] {"Repuesto", "Estado", "Cantidad total en la orden"},
                            c,
                            actual,
                            n)) {
                        int r = Integer.parseInt(UI.valor(c)),
                                cant = Validaciones.numero(n.getText(), "Cantidad", 1, Integer.MAX_VALUE);
                        ejecutar(
                                () -> servicio.agregarRepuesto(id, r, cant),
                                "Cantidad guardada. Genera de nuevo el presupuesto.");
                    }
                });
    }

    private void revisarPresupuesto() {
        int id = orden();
        if (Dialogos.showConfirmDialog(
                        this,
                        "La orden volverá a diagnóstico para corregir sus detalles. Deberás generar un nuevo"
                            + " presupuesto.",
                        "Corregir presupuesto",
                        JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) return;
        ejecutar(
                () -> servicio.revisarPresupuesto(id),
                "Orden disponible para corregir servicios y cantidades.");
    }

    private void presupuesto() {
        int id = orden();
        JTextField d = new JTextField("0"), i = new JTextField("15"), a = new JTextField();
        if (UI.formulario(
                this,
                "Presupuesto desglosado",
                new String[] {
                    "Descuento autorizado (L)", "Impuesto configurable (%)", "Quién autoriza el descuento"
                },
                d,
                i,
                a)) {
            BigDecimal des = TallerService.dinero(d.getText()), imp = TallerService.dinero(i.getText());
            String autor = a.getText();
            UI.tarea(
                    this,
                    () -> servicio.presupuesto(id, des, imp, autor),
                    total -> {
                        mensaje.setText(
                                "Presupuesto generado: "
                                        + com.autotech.negocio.Documento.valor("Total", total));
                        cargar();
                    });
        }
    }

    private void decidir() {
        int id = orden();
        JComboBox<String> decision = new JComboBox<>(new String[] {"Aprobar", "Rechazar"});
        JTextField autor = new JTextField();
        if (UI.formulario(
                this,
                "Decisión del cliente",
                new String[] {"Decisión", "Nombre del cliente / constancia"},
                decision,
                autor)) {
            boolean si = decision.getSelectedIndex() == 0;
            String a = autor.getText();
            ejecutar(
                    () -> servicio.decidir(id, si, a),
                    si
                            ? "Presupuesto aprobado; orden en reparación."
                            : "Presupuesto rechazado; orden cerrada.");
        }
    }

    private void motor() {
        int id = orden();
        String[] opciones = {"Seleccionar motor compatible", "Registrar pruebas y garantía"};
        int eleccion =
                Dialogos.showOptionDialog(
                        this,
                        "Selecciona la etapa del cambio de motor.",
                        "Cambio de motor",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        opciones,
                        opciones[0]);
        if (eleccion == 0)
            UI.tarea(
                    this,
                    () ->
                            UI.opciones(
                                    servicio.consultar(
                                            "SELECT MotorId,Serie+' / '+Marca+' '+Modelo+' / '+Estado FROM"
                                                + " Motores ORDER BY MotorId")),
                    ops -> {
                        JComboBox<UI.Opcion> m = UI.combo(ops);
                        JTextField serie = new JTextField(), destino = new JTextField();
                        if (UI.formulario(
                                this,
                                "Reserva y compatibilidad del motor",
                                new String[] {
                                    "Motor nuevo", "Serie del motor anterior", "Destino del motor retirado"
                                },
                                m,
                                serie,
                                destino)) {
                            int nuevo = Integer.parseInt(UI.valor(m));
                            String s = serie.getText(), d = destino.getText();
                            ejecutar(
                                    () -> servicio.reservarMotor(id, nuevo, s, d),
                                    "Motor reservado; actividades y consumo registrados.");
                        }
                    });
        if (eleccion == 1) {
            JTextArea pruebas = UI.area(), garantia = UI.area();
            if (UI.formulario(
                    this,
                    "Pruebas y garantía",
                    new String[] {"Resultados de pruebas", "Garantía: plazo y condiciones"},
                    pruebas,
                    garantia)) {
                String p = pruebas.getText(), g = garantia.getText();
                ejecutar(() -> servicio.pruebasMotor(id, p, g), "Pruebas y garantía registradas.");
            }
        }
    }

    private void trabajo() {
        int id = orden();
        String[] opciones = {"Nueva actividad", "Nuevo control de calidad"};
        int e =
                Dialogos.showOptionDialog(
                        this,
                        "Completa o aprueba los registros desde la tabla inferior.",
                        "Trabajo y calidad",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        opciones,
                        opciones[0]);
        if (e == 0)
            UI.tarea(
                    this,
                    () ->
                            UI.opciones(
                                    servicio.consultar(
                                            "SELECT m.MecanicoId,m.Nombre FROM OrdenMecanicos om JOIN"
                                                + " Mecanicos m ON m.MecanicoId=om.MecanicoId WHERE"
                                                + " om.OrdenId=?",
                                            id)),
                    ops -> {
                        JComboBox<UI.Opcion> m = UI.combo(ops);
                        JTextArea d = UI.area();
                        if (UI.formulario(
                                this,
                                "Actividad de trabajo",
                                new String[] {"Mecánico asignado", "Actividad"},
                                m,
                                d)) {
                            int mi = Integer.parseInt(UI.valor(m));
                            String desc = d.getText();
                            ejecutar(() -> servicio.actividad(id, mi, desc), "Actividad creada.");
                        }
                    });
        if (e == 1) {
            JTextField d = new JTextField();
            JComboBox<String> obligatorio = new JComboBox<>(new String[] {"Obligatorio", "Opcional"});
            if (UI.formulario(
                    this, "Control de calidad", new String[] {"Descripción", "Tipo"}, d, obligatorio)) {
                String desc = d.getText();
                boolean ob = obligatorio.getSelectedIndex() == 0;
                ejecutar(() -> servicio.control(id, desc, ob), "Control creado.");
            }
        }
    }

    private void completar() {
        int id = orden();
        if (!seccion.getSelectedItem().equals("Actividades"))
            throw new IllegalArgumentException("Selecciona la sección Actividades y una fila.");
        int a = UI.id(detalles);
        ejecutar(() -> servicio.completarActividad(id, a), "Actividad completada.");
    }

    private void aprobar() {
        int id = orden();
        if (!seccion.getSelectedItem().equals("Calidad"))
            throw new IllegalArgumentException("Selecciona la sección Calidad y una fila.");
        int c = UI.id(detalles);
        JTextArea r = UI.area();
        if (UI.formulario(this, "Resultado de control aprobado", new String[] {"Resultado / evidencia"}, r)) {
            String resultado = r.getText();
            ejecutar(() -> servicio.aprobarControl(id, c, resultado), "Control aprobado.");
        }
    }

    private void quitar() {
        int id = orden();
        String s = (String) seccion.getSelectedItem();
        if (!s.equals("Servicios") && !s.equals("Repuestos"))
            throw new IllegalArgumentException("Selecciona Servicios o Repuestos y una fila.");
        int d = UI.id(detalles);
        ejecutar(
                () -> servicio.quitarDetalle(id, d, s.equals("Servicios")),
                "Detalle retirado. Revisa actividades y vuelve a generar el presupuesto.");
    }

    private void consumir() {
        int id = orden();
        if (Dialogos.showConfirmDialog(
                        this,
                        "¿Descontar del inventario todos los consumos pendientes de la orden #" + id + "?",
                        "Procesar consumos",
                        JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION)
            ejecutar(() -> servicio.consumir(id), "Inventario actualizado mediante transacción.");
    }

    private void pagar() {
        int id = orden();
        JTextField monto = new JTextField(), referencia = new JTextField();
        JComboBox<String> metodo = new JComboBox<>(new String[] {"Efectivo", "Tarjeta", "Transferencia"});
        if (UI.formulario(
                this,
                "Pago parcial o total",
                new String[] {"Monto (L)", "Método", "Referencia / recibo"},
                monto,
                metodo,
                referencia)) {
            BigDecimal m = TallerService.dinero(monto.getText());
            String met = UI.valor(metodo), r = referencia.getText();
            ejecutar(() -> servicio.pagar(id, m, met, r), "Pago registrado y saldo actualizado.");
        }
    }

    private void comprobante() {
        int id = orden();
        UI.tarea(this, () -> servicio.comprobanteHtml(id), s -> UI.texto(this, "Comprobante #" + id, s));
    }
}
