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
    private final JTextField nombre=new JTextField(), telefono=new JTextField(), correo=new JTextField();
    private final JTextField placa=new JTextField(), vin=new JTextField(), marca=new JTextField(),
            modelo=new JTextField(), anio=new JTextField(), color=new JTextField(), km=new JTextField();
    private final JComboBox<Cliente> propietario=new JComboBox<>();
    private final JComboBox<String> combustible=new JComboBox<>(new String[]{"Gasolina","Diésel","Híbrido","Eléctrico","GLP"});
    private final DefaultTableModel mc=tablaModelo("ID","Nombre","Teléfono","Correo","Estado");
    private final DefaultTableModel mv=tablaModelo("ID","Placa","Propietario","Marca","Modelo","Año","Kilometraje");
    private final JTable tc=new JTable(mc), tv=new JTable(mv);
    private final JLabel estado=new JLabel("Configura la conexión para comenzar."), resumen=new JLabel("Clientes y vehículos");
    private int clienteId, vehiculoId;

    public PanelTaller() {
        setLayout(new BorderLayout(0,16)); setBackground(TemaOscuro.FONDO);
        setBorder(BorderFactory.createEmptyBorder(20,24,18,24));
        JPanel cabecera=new JPanel(new BorderLayout()); cabecera.setOpaque(false);
        JLabel titulo=new JLabel("AUTOTECH  /  Gestión de taller");
        titulo.setFont(new Font("Segoe UI",Font.BOLD,24));
        resumen.setForeground(TemaOscuro.VERDE);
        cabecera.add(titulo,BorderLayout.NORTH); cabecera.add(resumen,BorderLayout.SOUTH);
        add(cabecera,BorderLayout.NORTH);
        JTabbedPane tabs=new JTabbedPane();
        tabs.addTab("Clientes",panelClientes()); tabs.addTab("Vehículos",panelVehiculos());
        add(tabs,BorderLayout.CENTER);
        estado.setFont(new Font("Segoe UI",Font.PLAIN,12));
        add(estado,BorderLayout.SOUTH);
        SwingUtilities.invokeLater(this::actualizar);
    }
    private static DefaultTableModel tablaModelo(String... columnas) {
        return new DefaultTableModel(columnas,0) {
            @Override public boolean isCellEditable(int row,int column) { return false; }
        };
    }
    private JPanel formulario() {
        JPanel p=new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(12,12,12,12)); return p;
    }
    private void campo(JPanel panel,String texto,JComponent entrada,int fila,int col) {
        GridBagConstraints g=new GridBagConstraints(); g.gridx=col*2; g.gridy=fila;
        g.anchor=GridBagConstraints.WEST; g.insets=new Insets(6,6,6,10);
        panel.add(new JLabel(texto),g);
        g.gridx++; g.weightx=1; g.fill=GridBagConstraints.HORIZONTAL;
        entrada.setPreferredSize(new Dimension(180,30)); panel.add(entrada,g);
    }
    private JButton boton(String texto,Runnable accion,boolean principal) {
        JButton b=new JButton(texto); b.setFocusPainted(false);
        if(principal) b.setBackground(TemaOscuro.AZUL);
        b.addActionListener(e->accion.run()); botones.add(b); return b;
    }
    private JPanel base(JPanel campos,JPanel acciones,JTable tabla,String ayuda) {
        JPanel p=new JPanel(new BorderLayout(0,10));
        p.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        JPanel arriba=new JPanel(new BorderLayout()); arriba.add(campos,BorderLayout.CENTER);
        arriba.add(acciones,BorderLayout.SOUTH); p.add(arriba,BorderLayout.NORTH);
        tabla.setRowHeight(30); tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setAutoCreateRowSorter(true); tabla.getTableHeader().setReorderingAllowed(false);
        JScrollPane scroll=new JScrollPane(tabla); scroll.setColumnHeaderView(tabla.getTableHeader()); scroll.getViewport().setBackground(TemaOscuro.PANEL);
        p.add(scroll,BorderLayout.CENTER); p.add(new JLabel(ayuda),BorderLayout.SOUTH); return p;
    }
    private JPanel panelClientes() {
        JPanel f=formulario(); campo(f,"Nombre completo *",nombre,0,0);
        campo(f,"Teléfono *",telefono,0,1); campo(f,"Correo electrónico",correo,1,0);
        JPanel a=new JPanel(new FlowLayout(FlowLayout.LEFT));
        a.add(boton("Guardar cliente",this::guardarCliente,true));
        a.add(boton("Desactivar",this::desactivar,false));
        a.add(boton("Nuevo / limpiar",this::limpiarCliente,false));
        a.add(boton("Actualizar / conectar",this::actualizar,false));
        tc.getSelectionModel().addListSelectionListener(e->{
            if(e.getValueIsAdjusting() || tc.getSelectedRow()<0) return;
            Cliente c=clientes.get(tc.convertRowIndexToModel(tc.getSelectedRow()));
            clienteId=c.id(); nombre.setText(c.nombre()); telefono.setText(c.telefono()); correo.setText(c.correo());
        });
        return base(f,a,tc,"Selecciona una fila para editar. Desactivar conserva los vehículos y el historial.  * Obligatorio");
    }
    private JPanel panelVehiculos() {
        JPanel f=formulario(); campo(f,"Propietario activo *",propietario,0,0); campo(f,"Placa *",placa,0,1);
        campo(f,"VIN / chasis *",vin,1,0); campo(f,"Marca *",marca,1,1);
        campo(f,"Modelo *",modelo,2,0); campo(f,"Año *",anio,2,1);
        campo(f,"Color *",color,3,0); campo(f,"Kilometraje *",km,3,1);
        campo(f,"Combustible *",combustible,4,0);
        JPanel a=new JPanel(new FlowLayout(FlowLayout.LEFT));
        a.add(boton("Guardar vehículo",this::guardarVehiculo,true));
        a.add(boton("Nuevo / limpiar",this::limpiarVehiculo,false));
        a.add(boton("Actualizar / conectar",this::actualizar,false));
        tv.getSelectionModel().addListSelectionListener(e->{
            if(e.getValueIsAdjusting() || tv.getSelectedRow()<0) return;
            Vehiculo v=vehiculos.get(tv.convertRowIndexToModel(tv.getSelectedRow()));
            vehiculoId=v.id(); propietario.setSelectedIndex(-1);
            for(int i=0;i<propietario.getItemCount();i++) if(propietario.getItemAt(i).id()==v.clienteId()) propietario.setSelectedIndex(i);
            placa.setText(v.placa()); vin.setText(v.vin()); marca.setText(v.marca()); modelo.setText(v.modelo());
            anio.setText(String.valueOf(v.anio())); color.setText(v.color()); km.setText(String.valueOf(v.kilometraje()));
            combustible.setSelectedItem(v.combustible());
        });
        return base(f,a,tv,"Cada vehículo tiene su propia placa y VIN únicos. Un cliente puede registrar varios vehículos.");
    }
    private record Datos(List<Cliente> clientes,List<Vehiculo> vehiculos) {}
    private Datos leer() throws SQLException { return new Datos(clientesDAO.listar(),vehiculosDAO.listar()); }
    private void mostrar(Datos d) {
        tc.clearSelection(); tv.clearSelection(); mc.setRowCount(0); mv.setRowCount(0);
        clientes=d.clientes(); vehiculos=d.vehiculos(); propietario.removeAllItems();
        for(Cliente c:clientes) {
            mc.addRow(new Object[]{c.id(),c.nombre(),c.telefono(),c.correo(),c.activo()?"Activo":"Inactivo"});
            if(c.activo()) propietario.addItem(c);
        }
        for(Vehiculo v:vehiculos) mv.addRow(new Object[]{v.id(),v.placa(),v.propietario(),v.marca(),v.modelo(),v.anio(),v.kilometraje()});
        limpiarCliente(); limpiarVehiculo();
        resumen.setText(clientes.stream().filter(Cliente::activo).count()+" clientes activos   •   "+vehiculos.size()+" vehículos registrados");
    }
    private void actualizar() { tarea(this::leer,this::mostrar,"Conexión lista. Datos actualizados."); }
    private void limpiarCliente() {
        clienteId=0; tc.clearSelection(); nombre.setText(""); telefono.setText(""); correo.setText("");
    }
    private void limpiarVehiculo() {
        vehiculoId=0; tv.clearSelection();
        for(JTextField t:new JTextField[]{placa,vin,marca,modelo,anio,color,km}) t.setText("");
        combustible.setSelectedIndex(0); propietario.setSelectedIndex(-1);
    }
    private void guardarCliente() {
        try {
            if(clienteId!=0 && clientes.stream().noneMatch(c->c.id()==clienteId && c.activo()))
                throw new IllegalArgumentException("Un cliente inactivo no se puede editar.");
            Cliente c=new Cliente(clienteId,Validaciones.texto(nombre.getText(),"nombre",120),
                    Validaciones.telefono(telefono.getText()),Validaciones.correo(correo.getText()),true);
            guardar(()->clientesDAO.guardar(c),"Cliente guardado.");
        } catch(IllegalArgumentException e) { aviso(e.getMessage()); }
    }
    private void desactivar() {
        if(clienteId==0) { aviso("Selecciona un cliente activo."); return; }
        if(JOptionPane.showConfirmDialog(this,"¿Desactivar al cliente seleccionado? Se conservarán sus registros.",
                "Desactivar cliente",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;
        int id=clienteId; guardar(()->clientesDAO.desactivar(id),"Cliente desactivado.");
    }
    private void guardarVehiculo() {
        try {
            Cliente c=(Cliente)propietario.getSelectedItem();
            if(c==null) throw new IllegalArgumentException("Selecciona un propietario activo.");
            Vehiculo v=new Vehiculo(vehiculoId,c.id(),c.nombre(),
                    Validaciones.texto(placa.getText(),"placa",15).toUpperCase(Locale.ROOT),
                    Validaciones.texto(vin.getText(),"VIN / chasis",30).toUpperCase(Locale.ROOT),
                    Validaciones.texto(marca.getText(),"marca",60),Validaciones.texto(modelo.getText(),"modelo",60),
                    Validaciones.anio(anio.getText()),Validaciones.texto(color.getText(),"color",40),
                    Validaciones.numero(km.getText(),"Kilometraje",0,Integer.MAX_VALUE),(String)combustible.getSelectedItem());
            guardar(()->vehiculosDAO.guardar(v),"Vehículo guardado.");
        } catch(IllegalArgumentException e) { aviso(e.getMessage()); }
    }
    @FunctionalInterface private interface Escritura { void ejecutar() throws SQLException; }
    private void guardar(Escritura escritura,String mensaje) {
        tarea(()->{ escritura.ejecutar(); return true; },ok->{
            // La escritura ya se confirmó: un fallo posterior al recargar no debe invitar a duplicarla.
            limpiarCliente(); limpiarVehiculo();
            tarea(this::leer,this::mostrar,mensaje+" Lista actualizada.");
        },mensaje);
    }
    private <T> void tarea(Callable<T> trabajo,Consumer<T> exito,String mensaje) {
        ocupado(true); estado.setText("Procesando…");
        new SwingWorker<T,Void>() {
            @Override protected T doInBackground() throws Exception { return trabajo.call(); }
            @Override protected void done() {
                ocupado(false);
                try { T dato=get(); estado.setText(mensaje); exito.accept(dato); }
                catch(Exception e) {
                    Throwable causa=e.getCause()==null?e:e.getCause();
                    String detalle=causa.getMessage();
                    if(causa instanceof SQLException sql && (sql.getErrorCode()==2601 || sql.getErrorCode()==2627))
                        detalle="Ya existe un vehículo con esa placa o VIN / chasis.";
                    estado.setText("No se completó la operación. Revisa la conexión o los datos.");
                    aviso(detalle==null?"No se pudo completar la operación.":detalle);
                }
            }
        }.execute();
    }
    private void ocupado(boolean ocupado) {
        for(JButton b:botones) b.setEnabled(!ocupado);
        tc.setEnabled(!ocupado); tv.setEnabled(!ocupado);
        for(JComponent c:new JComponent[]{nombre,telefono,correo,propietario,placa,vin,marca,modelo,anio,color,km,combustible}) c.setEnabled(!ocupado);
        setCursor(Cursor.getPredefinedCursor(ocupado?Cursor.WAIT_CURSOR:Cursor.DEFAULT_CURSOR));
    }
    private void aviso(String mensaje) { JOptionPane.showMessageDialog(this,mensaje,"AutoTech Manager",JOptionPane.INFORMATION_MESSAGE); }
}
