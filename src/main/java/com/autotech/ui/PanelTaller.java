package com.autotech.ui;

import com.autotech.datos.*;
import com.autotech.modelo.*;
import com.autotech.negocio.Validaciones;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class PanelTaller extends JPanel {
    private final ClienteDAO clientesDAO = new ClienteDAO();
    private final VehiculoDAO vehiculosDAO = new VehiculoDAO();
    private List<Cliente> clientes = new ArrayList<>();
    private List<Vehiculo> vehiculos = new ArrayList<>();
    private final List<JButton> botones = new ArrayList<>();
    private final JTextField nombre = new JTextField(),
            telefono = new JTextField(),
            correo = new JTextField();
    private final JTextField placa = new JTextField(),
            vin = new JTextField(),
            marca = new JTextField(),
            modelo = new JTextField(),
            anio = new JTextField(),
            color = new JTextField(),
            km = new JTextField();
    private final JComboBox<Cliente> propietario = new JComboBox<>();
    private final JComboBox<String> combustible =
            new JComboBox<>(new String[] {"Gasolina", "Diésel", "Híbrido", "Eléctrico", "GLP"});
    private final DefaultTableModel mc = tablaModelo("ID", "Nombre", "Teléfono", "Correo", "Estado");
    private final DefaultTableModel mv =
            tablaModelo("ID", "Placa", "Propietario", "Marca", "Modelo", "Año", "Kilometraje");
    private final JTable tc = UI.tabla(), tv = UI.tabla();
    private final JLabel estado = new JLabel("Configura la conexión para comenzar."),
            resumen = new JLabel(" ");
    private int clienteId, vehiculoId;
    private JButton estadoCliente;

    public PanelTaller() {
        UI.campoNumerico(km, 0);
        tc.setModel(mc);
        tv.setModel(mv);
        UI.ajustarColumnas(tc);
        UI.ajustarColumnas(tv);

        setLayout(new BorderLayout(0, 16));
        setBackground(TemaOscuro.FONDO);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 18, 24));
        resumen.setForeground(TemaOscuro.VERDE);

        JTabbedPane tabs = new Pestanas();
        tabs.addTab("Clientes", panelClientes());
        tabs.addTab("Vehículos", panelVehiculos());
        add(tabs, BorderLayout.CENTER);

        estado.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JPanel pie = new JPanel(new BorderLayout());
        pie.setOpaque(false);
        pie.add(estado, BorderLayout.WEST);
        pie.add(resumen, BorderLayout.EAST);
        add(pie, BorderLayout.SOUTH);

        SwingUtilities.invokeLater(this::actualizar);
    }

    private static DefaultTableModel tablaModelo(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private JPanel formulario() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        return p;
    }

    private void campo(JPanel panel, String texto, JComponent entrada, int fila, int col) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = col * 2;
        g.gridy = fila;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(6, 6, 6, 10);
        panel.add(new JLabel(texto), g);
        g.gridx++;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        entrada.setPreferredSize(new Dimension(180, 30));
        panel.add(entrada, g);
    }

    private JButton boton(String texto, Runnable accion) {
        JButton b = UI.boton(texto, accion);
        botones.add(b);
        return b;
    }

    private JPanel base(JPanel campos, JPanel acciones, JTable tabla) {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel arriba = new JPanel(new BorderLayout());
        arriba.add(campos, BorderLayout.CENTER);
        arriba.add(acciones, BorderLayout.SOUTH);
        p.add(arriba, BorderLayout.NORTH);
        JScrollPane scroll = UI.scroll(tabla);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    private JPanel panelClientes() {
        JPanel f = formulario();
        campo(f, "Nombre completo *", nombre, 0, 0);
        campo(f, "Teléfono *", telefono, 0, 1);
        campo(f, "Correo electrónico", correo, 1, 0);
        JPanel a = new JPanel(new FlowLayout(FlowLayout.LEFT));
        a.add(boton("Guardar cliente", this::guardarCliente));
        estadoCliente = boton("Activar / desactivar", this::cambiarEstadoCliente);
        a.add(estadoCliente);
        a.add(boton("Nuevo", this::limpiarCliente));
        a.add(boton("Actualizar", this::actualizar));
        tc.getSelectionModel()
                .addListSelectionListener(e -> {
                    if (e.getValueIsAdjusting() || tc.getSelectedRow() < 0) return;
                    Cliente c = clientes.get(tc.convertRowIndexToModel(tc.getSelectedRow()));
                    estadoCliente.setText(c.activo() ? "Desactivar cliente" : "Reactivar cliente");
                    UI.estiloBoton(estadoCliente);
                    clienteId = c.id();
                    nombre.setText(c.nombre());
                    telefono.setText(c.telefono());
                    correo.setText(c.correo());
                });
        return base(f, a, tc);
    }

    private JPanel panelVehiculos() {
        JPanel f = formulario();
        campo(f, "Propietario activo *", propietario, 0, 0);
        campo(f, "Placa *", placa, 0, 1);
        campo(f, "VIN / chasis *", vin, 1, 0);
        campo(f, "Marca *", marca, 1, 1);
        campo(f, "Modelo *", modelo, 2, 0);
        campo(f, "Año *", anio, 2, 1);
        campo(f, "Color *", color, 3, 0);
        campo(f, "Kilometraje *", km, 3, 1);
        campo(f, "Combustible *", combustible, 4, 0);
        JPanel a = new JPanel(new FlowLayout(FlowLayout.LEFT));
        a.add(boton("Guardar vehículo", this::guardarVehiculo));
        a.add(boton("Nuevo", this::limpiarVehiculo));
        a.add(boton("Actualizar", this::actualizar));
        tv.getSelectionModel()
                .addListSelectionListener(e -> {
                    if (e.getValueIsAdjusting() || tv.getSelectedRow() < 0) return;
                    Vehiculo v = vehiculos.get(tv.convertRowIndexToModel(tv.getSelectedRow()));
                    vehiculoId = v.id();
                    propietario.setSelectedIndex(-1);
                    for (int i = 0; i < propietario.getItemCount(); i++)
                        if (propietario.getItemAt(i).id() == v.clienteId()) propietario.setSelectedIndex(i);
                    placa.setText(v.placa());
                    vin.setText(v.vin());
                    marca.setText(v.marca());
                    modelo.setText(v.modelo());
                    anio.setText(String.valueOf(v.anio()));
                    color.setText(v.color());
                    km.setText(com.autotech.negocio.Presentacion.numero(v.kilometraje(), 0));
                    combustible.setSelectedItem(v.combustible());
                });
        return base(f, a, tv);
    }

    private record Datos(List<Cliente> clientes, List<Vehiculo> vehiculos) {}

    private Datos leer() throws SQLException {
        return new Datos(clientesDAO.listar(), vehiculosDAO.listar());
    }

    private void mostrar(Datos d) {
        tc.clearSelection();
        tv.clearSelection();
        mc.setRowCount(0);
        mv.setRowCount(0);
        clientes = d.clientes();
        vehiculos = d.vehiculos();
        propietario.removeAllItems();
        for (Cliente c : clientes) {
            mc.addRow(
                    new Object[] {
                        c.id(), c.nombre(), c.telefono(), c.correo(), c.activo() ? "Activo" : "Inactivo"
                    });
            if (c.activo()) propietario.addItem(c);
        }
        for (Vehiculo v : vehiculos)
            mv.addRow(
                    new Object[] {
                        v.id(), v.placa(), v.propietario(), v.marca(), v.modelo(), v.anio(), v.kilometraje()
                    });
        UI.ajustarColumnas(tc);
        UI.ajustarColumnas(tv);
        limpiarCliente();
        limpiarVehiculo();
        resumen.setText(
                com.autotech.negocio.Presentacion.numero(clientes.stream().filter(Cliente::activo).count(), 0)
                        + " clientes activos   •   "
                        + com.autotech.negocio.Presentacion.numero(vehiculos.size(), 0)
                        + " vehículos registrados");
    }

    private void actualizar() {
        tarea(this::leer, this::mostrar, "Conexión lista. Datos actualizados.");
    }

    private void limpiarCliente() {
        clienteId = 0;
        tc.clearSelection();
        estadoCliente.setText("Activar / desactivar");
        UI.estiloBoton(estadoCliente);
        nombre.setText("");
        telefono.setText("");
        correo.setText("");
    }

    private void limpiarVehiculo() {
        vehiculoId = 0;
        tv.clearSelection();
        for (JTextField t : new JTextField[] {placa, vin, marca, modelo, anio, color, km}) t.setText("");
        combustible.setSelectedIndex(0);
        propietario.setSelectedIndex(-1);
    }

    private void guardarCliente() {
        try {
            if (clienteId != 0 && clientes.stream().noneMatch(c -> c.id() == clienteId && c.activo()))
                throw new IllegalArgumentException("Reactiva al cliente antes de editar sus datos.");
            Cliente c =
                    new Cliente(
                            clienteId,
                            Validaciones.texto(nombre.getText(), "nombre", 120),
                            Validaciones.telefono(telefono.getText()),
                            Validaciones.correo(correo.getText()),
                            true);
            guardar(() -> clientesDAO.guardar(c), "Cliente guardado.");
        } catch (IllegalArgumentException e) {
            aviso(e.getMessage());
        }
    }

    private void cambiarEstadoCliente() {
        Cliente cliente = clientes.stream().filter(c -> c.id() == clienteId).findFirst().orElse(null);
        if (cliente == null) {
            aviso("Selecciona un cliente.");
            return;
        }
        boolean activo = !cliente.activo();
        String accion = activo ? "Reactivar" : "Desactivar";
        if (Dialogos.showConfirmDialog(
                        this,
                        "¿" + accion + " a " + cliente.nombre() + "?",
                        accion + " cliente",
                        JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION) return;
        guardar(
                () -> clientesDAO.cambiarEstado(cliente.id(), activo),
                activo ? "Cliente reactivado." : "Cliente desactivado.");
    }

    private void guardarVehiculo() {
        try {
            Cliente c = (Cliente) propietario.getSelectedItem();
            if (c == null) throw new IllegalArgumentException("Selecciona un propietario activo.");
            Vehiculo v =
                    new Vehiculo(
                            vehiculoId,
                            c.id(),
                            c.nombre(),
                            Validaciones.texto(placa.getText(), "placa", 15).toUpperCase(Locale.ROOT),
                            Validaciones.texto(vin.getText(), "VIN / chasis", 30).toUpperCase(Locale.ROOT),
                            Validaciones.texto(marca.getText(), "marca", 60),
                            Validaciones.texto(modelo.getText(), "modelo", 60),
                            Validaciones.anio(anio.getText()),
                            Validaciones.texto(color.getText(), "color", 40),
                            Validaciones.numero(km.getText(), "Kilometraje", 0, Integer.MAX_VALUE),
                            (String) combustible.getSelectedItem());
            guardar(() -> vehiculosDAO.guardar(v), "Vehículo guardado.");
        } catch (IllegalArgumentException e) {
            aviso(e.getMessage());
        }
    }

    @FunctionalInterface
    private interface Escritura {
        void ejecutar() throws SQLException;
    }

    private void guardar(Escritura escritura, String mensaje) {
        tarea(
                () -> {
                    escritura.ejecutar();
                    return true;
                },
                ok -> {
                    // Recargar no debe repetir una escritura confirmada.
                    limpiarCliente();
                    limpiarVehiculo();
                    tarea(this::leer, this::mostrar, mensaje + " Lista actualizada.");
                },
                mensaje);
    }

    private <T> void tarea(Callable<T> trabajo, Consumer<T> exito, String mensaje) {
        ocupado(true);
        estado.setText("Procesando…");
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return trabajo.call();
            }

            @Override
            protected void done() {
                ocupado(false);
                try {
                    T dato = get();
                    estado.setText(mensaje);
                    exito.accept(dato);
                } catch (Exception e) {
                    Throwable causa = e.getCause() == null ? e : e.getCause();
                    String detalle = causa.getMessage();
                    if (causa instanceof SQLException sql
                            && (sql.getErrorCode() == 2601 || sql.getErrorCode() == 2627))
                        detalle = "Ya existe un vehículo con esa placa o VIN / chasis.";
                    estado.setText("No se completó la operación. Revisa la conexión o los datos.");
                    aviso(detalle == null ? "No se pudo completar la operación." : detalle);
                }
            }
        }.execute();
    }

    private void ocupado(boolean ocupado) {
        for (JButton b : botones) b.setEnabled(!ocupado);
        tc.setEnabled(!ocupado);
        tv.setEnabled(!ocupado);
        for (JComponent c :
                new JComponent[] {
                    nombre,
                    telefono,
                    correo,
                    propietario,
                    placa,
                    vin,
                    marca,
                    modelo,
                    anio,
                    color,
                    km,
                    combustible
                }) c.setEnabled(!ocupado);
        setCursor(Cursor.getPredefinedCursor(ocupado ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void aviso(String mensaje) {
        Dialogos.showMessageDialog(this, mensaje, "AutoTech Manager", JOptionPane.INFORMATION_MESSAGE);
    }
}
