package com.autotech.ui;

import com.autotech.negocio.*;
import com.autotech.datos.Sql;
import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

public class PanelCatalogos extends JPanel {
    private final TallerService servicio;
    private final JComboBox<Catalogo> tipo = new JComboBox<>(Catalogo.values());
    private final JTable tabla = UI.tabla();

    public PanelCatalogos(TallerService servicio) {
        this.servicio = servicio;
        setLayout(new BorderLayout());
        JPanel p = UI.pagina();
        add(p);

        JPanel centro = new JPanel(new BorderLayout(0, 10));
        JPanel top =
                UI.acciones(
                        UI.boton("Nuevo", () -> editar(false)),
                        UI.boton("Editar", () -> editar(true)),
                        UI.boton("Activar / desactivar", this::activar),
                        UI.boton("Entrada de inventario", this::entrada),
                        UI.boton("Repuesto de servicio", this::asociar),
                        UI.boton("Actualizar", this::cargar));
        top.add(tipo, 0);
        centro.add(top, BorderLayout.NORTH);
        centro.add(UI.scroll(tabla));
        p.add(centro);

        tipo.addActionListener(e -> cargar());
    }

    public void cargar() {
        Catalogo c = (Catalogo) tipo.getSelectedItem();
        UI.tarea(this, () -> servicio.catalogo(c), t -> UI.mostrar(tabla, t));
    }

    private void editar(boolean editar) {
        Catalogo cat = (Catalogo) tipo.getSelectedItem();
        int id = editar ? UI.id(tabla) : 0;
        Map<String, Object> valores = new HashMap<>();
        if (editar) {
            int row = tabla.getSelectedRow();
            for (int i = 0; i < tabla.getColumnCount(); i++)
                valores.put(tabla.getColumnName(i), tabla.getValueAt(row, i));
        }
        UI.tarea(
                this,
                () -> {
                    Map<String, List<UI.Opcion>> opciones = new HashMap<>();
                    opciones.put(
                            "proveedor",
                            UI.opciones(
                                    servicio.consultar(
                                            "SELECT ProveedorId,Nombre FROM Proveedores WHERE Activo=1 ORDER"
                                                + " BY Nombre")));
                    opciones.put(
                            "repuesto",
                            UI.opciones(
                                    servicio.consultar(
                                            "SELECT RepuestoId,Nombre FROM Repuestos WHERE Activo=1 ORDER BY"
                                                + " Nombre")));
                    return opciones;
                },
                ops -> {
                    List<JComponent> campos = new ArrayList<>();
                    for (Catalogo.Campo f : cat.campos) {
                        JComponent entrada;
                        if (ops.containsKey(f.tipo())) {
                            JComboBox<UI.Opcion> combo = UI.combo(ops.get(f.tipo()));
                            if (editar) {
                                combo.setSelectedIndex(-1);
                                for (int i = 0; i < combo.getItemCount(); i++)
                                    if (combo.getItemAt(i).id()
                                            == ((Number) valores.get(f.columna())).intValue())
                                        combo.setSelectedIndex(i);
                            }
                            entrada = combo;
                        } else if (f.tipo().equals("unidad")) {
                            JComboBox<String> combo =
                                    new JComboBox<>(new String[] {"Unidad", "Litro", "Juego"});
                            if (editar) combo.setSelectedItem(valores.get(f.columna()));
                            entrada = combo;
                        } else if (f.tipo().equals("combustible")) {
                            JComboBox<String> combo =
                                    new JComboBox<>(
                                            new String[] {
                                                "Gasolina", "Diésel", "Híbrido", "Eléctrico", "GLP"
                                            });
                            if (editar) combo.setSelectedItem(valores.get(f.columna()));
                            entrada = combo;
                        } else
                            entrada = new JTextField(editar ? String.valueOf(valores.get(f.columna())) : "");
                        campos.add(entrada);
                    }
                    if (UI.formulario(
                            this,
                            (editar ? "Editar " : "Nuevo registro: ") + cat,
                            cat.campos.stream().map(Catalogo.Campo::etiqueta).toArray(String[]::new),
                            campos.toArray(JComponent[]::new))) {
                        List<String> v = campos.stream().map(UI::valor).toList();
                        UI.tarea(
                                this,
                                () -> {
                                    servicio.guardarCatalogo(cat, id, v);
                                    return true;
                                },
                                ok -> cargar());
                    }
                });
    }

    private void activar() {
        Catalogo cat = (Catalogo) tipo.getSelectedItem();
        int id = UI.id(tabla);
        if (cat == Catalogo.MOTORES)
            throw new IllegalArgumentException("El estado del motor se controla desde la orden de trabajo.");
        boolean activo =
                (boolean)
                        tabla.getValueAt(
                                tabla.getSelectedRow(), tabla.getColumnModel().getColumnIndex("Activo"));
        UI.tarea(
                this,
                () -> {
                    servicio.activar(cat, id, !activo);
                    return true;
                },
                ok -> cargar());
    }

    private void entrada() {
        if (tipo.getSelectedItem() != Catalogo.REPUESTOS)
            throw new IllegalArgumentException("Selecciona Repuestos y una fila de su inventario.");
        int id = UI.id(tabla);
        JTextField cantidad = new JTextField(), motivo = new JTextField();
        if (UI.formulario(
                this,
                "Entrada de inventario",
                new String[] {"Cantidad", "Compra / motivo"},
                cantidad,
                motivo)) {
            int n = Validaciones.numero(cantidad.getText(), "Cantidad", 1, Integer.MAX_VALUE);
            String m = motivo.getText();
            UI.tarea(
                    this,
                    () -> {
                        servicio.entrada(id, n, m);
                        return true;
                    },
                    ok -> cargar());
        }
    }

    private void asociar() {
        if (tipo.getSelectedItem() != Catalogo.SERVICIOS)
            throw new IllegalArgumentException("Selecciona Servicios y una fila del catálogo.");
        int id = UI.id(tabla);
        UI.tarea(
                this,
                () ->
                        UI.opciones(
                                servicio.consultar(
                                        "SELECT RepuestoId,Nombre FROM Repuestos WHERE Activo=1 AND"
                                            + " RepuestoId NOT IN(SELECT RepuestoId FROM Motores) ORDER BY"
                                            + " Nombre")),
                ops -> {
                    JComboBox<UI.Opcion> r = UI.combo(ops);
                    JTextField cantidad = new JTextField("1");
                    if (UI.formulario(
                            this,
                            "Repuesto sugerido para el servicio",
                            new String[] {"Repuesto", "Cantidad por servicio"},
                            r,
                            cantidad)) {
                        int rep = Integer.parseInt(UI.valor(r)),
                                n = Validaciones.numero(cantidad.getText(), "Cantidad", 1, Integer.MAX_VALUE);
                        UI.tarea(
                                this,
                                () -> {
                                    servicio.asociarRepuesto(id, rep, n);
                                    return true;
                                },
                                ok ->
                                        Dialogos.showMessageDialog(
                                                this,
                                                "Asociación guardada como referencia. Las cantidades de cada"
                                                    + " orden se registran por separado."));
                    }
                });
    }
}
