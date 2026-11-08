package com.autotech.ui;

import com.autotech.datos.Sql;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

final class UI {
    private UI() {}
    record Opcion(int id,String nombre) { @Override public String toString() { return nombre+" (#"+id+")"; } }
    static List<Opcion> opciones(Sql.Tabla t) {
        return t.filas().stream().map(f->new Opcion(((Number)f.get(0)).intValue(),String.valueOf(f.get(1)))).toList();
    }
    static JTable tabla() {
        JTable t=new JTable();t.setRowHeight(29);t.setAutoCreateRowSorter(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override public void mouseMoved(java.awt.event.MouseEvent e) {
                int r=t.rowAtPoint(e.getPoint()),c=t.columnAtPoint(e.getPoint());
                t.setToolTipText(r<0 || c<0?null:String.valueOf(t.getValueAt(r,c)));
            }
        });
        t.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if(e.getClickCount()!=2 || t.getSelectedRow()<0)return;
                StringBuilder detalle=new StringBuilder();
                for(int i=0;i<t.getColumnCount();i++) detalle.append(t.getColumnName(i)).append(": ").append(t.getValueAt(t.getSelectedRow(),i)).append("\n\n");
                texto(t,"Detalle del registro",detalle.toString());
            }
        }); t.getTableHeader().setReorderingAllowed(false);return t;
    }
    static void mostrar(JTable t,Sql.Tabla datos) {
        DefaultTableModel m=new DefaultTableModel(datos.columnas().toArray(),0) {
            @Override public boolean isCellEditable(int r,int c) { return false; }
            @Override public Class<?> getColumnClass(int col) {
                for(int i=0;i<getRowCount();i++) if(getValueAt(i,col)!=null) return getValueAt(i,col).getClass();
                return Object.class;
            }
        };
        for(var fila:datos.filas()) m.addRow(fila.toArray()); t.setModel(m);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        for(int i=0;i<t.getColumnCount();i++) {
            String encabezado=t.getColumnName(i);int ancho=Math.max(100,t.getFontMetrics(t.getFont()).stringWidth(encabezado)+24);
            for(int r=0;r<t.getRowCount();r++)ancho=Math.max(ancho,Math.min(320,t.getFontMetrics(t.getFont()).stringWidth(String.valueOf(t.getValueAt(r,i)))+24));
            t.getColumnModel().getColumn(i).setPreferredWidth(ancho);
        }
    }
    static JButton boton(String texto,Runnable r) {
        JButton b=new JButton(texto);b.setFocusPainted(false);b.addActionListener(e->{try { r.run(); } catch(Exception ex) { error(b,ex); }});return b;
    }
    static JPanel acciones(JButton... botones) {
        JPanel p=new JPanel(new FlowLayout(FlowLayout.LEFT,8,5));for(JButton b:botones)p.add(b);return p;
    }
    static JPanel pagina(String titulo) {
        JPanel p=new JPanel(new BorderLayout(10,12));p.setBackground(TemaOscuro.FONDO);
        p.setBorder(BorderFactory.createEmptyBorder(16,18,16,18));
        JLabel l=new JLabel(titulo); l.setFont(new Font("Segoe UI",Font.BOLD,22));p.add(l,BorderLayout.NORTH);return p;
    }
    static JScrollPane scroll(JTable t) { JScrollPane s=new JScrollPane(t);s.setColumnHeaderView(t.getTableHeader());s.getViewport().setBackground(TemaOscuro.PANEL);return s; }
    static int id(JTable t) {
        if(t.getSelectedRow()<0) throw new IllegalArgumentException("Selecciona una fila de la tabla.");
        return ((Number)t.getValueAt(t.getSelectedRow(),0)).intValue();
    }
    static void error(Component c,Throwable e) {
        while(e.getCause()!=null) e=e.getCause();
        String mensaje=e.getMessage();
        if(e instanceof java.sql.SQLException s) {
            if(s.getErrorCode()==2601 || s.getErrorCode()==2627) mensaje="El registro ya existe. Revisa el código, la serie o la asignación.";
            if(s.getErrorCode()==547) mensaje="Los datos no cumplen una regla: revisa cantidades, años y registros relacionados.";
            if(s.getErrorCode()==208) mensaje="Faltan tablas del taller. Ejecuta los scripts SQL 01 y 02 en AutoTechManager.";
        }
        JOptionPane.showMessageDialog(c,mensaje==null?"No se pudo completar la operación.":mensaje,"AutoTech Manager",JOptionPane.WARNING_MESSAGE);
    }
    private static void componentes(Container p,Map<Component,Boolean> estados) {
        for(Component x:p.getComponents()) { estados.put(x,x.isEnabled()); x.setEnabled(false); if(x instanceof Container h) componentes(h,estados); }
    }
    static <T> void tarea(JPanel panel,Callable<T> trabajo,Consumer<T> listo) {
        Map<Component,Boolean> estados=new IdentityHashMap<>();componentes(panel,estados);
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<T,Void>() {
            @Override protected T doInBackground() throws Exception { return trabajo.call(); }
            @Override protected void done() {
                estados.forEach(Component::setEnabled);panel.setCursor(Cursor.getDefaultCursor());
                try { listo.accept(get()); } catch(Exception e) { error(panel,e); }
            }
        }.execute();
    }
    static String valor(JComponent campo) {
        if(campo instanceof JComboBox<?> c) {
            Object o=c.getSelectedItem();if(o==null) throw new IllegalArgumentException("Selecciona una opción en todos los campos desplegables.");
            return o instanceof Opcion op?String.valueOf(op.id()):o.toString();
        }
        return ((javax.swing.text.JTextComponent)campo).getText().trim();
    }
    static boolean formulario(Component padre,String titulo,String[] etiquetas,JComponent... campos) {
        JPanel p=new JPanel(new GridBagLayout());
        for(int i=0;i<campos.length;i++) {
            GridBagConstraints g=new GridBagConstraints();g.gridx=0;g.gridy=i;g.anchor=GridBagConstraints.NORTHWEST;g.insets=new Insets(6,6,6,12);
            p.add(new JLabel(etiquetas[i]),g);g.gridx=1;g.weightx=1;g.fill=GridBagConstraints.HORIZONTAL;
            if(campos[i] instanceof JTextArea a) { a.setLineWrap(true);a.setWrapStyleWord(true);p.add(new JScrollPane(a),g); }
            else {campos[i].setPreferredSize(new Dimension(340,30));p.add(campos[i],g);}
        }
        return JOptionPane.showConfirmDialog(padre,p,titulo,JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION;
    }
    static JComboBox<Opcion> combo(List<Opcion> ops) { return new JComboBox<>(ops.toArray(Opcion[]::new)); }
    static JTextArea area() { return new JTextArea(3,30); }
    static void texto(Component padre,String titulo,String texto) {
        JTextArea a=new JTextArea(texto,25,85);a.setEditable(false);a.setFont(new Font("Consolas",Font.PLAIN,13));a.setCaretPosition(0);
        JButton guardar=boton("Guardar .txt",()->{
            JFileChooser fc=new JFileChooser();fc.setSelectedFile(new java.io.File("AutoTech-comprobante.txt"));
            if(fc.showSaveDialog(padre)==JFileChooser.APPROVE_OPTION) {
                java.io.File f=fc.getSelectedFile();
                if(f.exists() && JOptionPane.showConfirmDialog(padre,"¿Reemplazar el archivo?","Guardar",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
                try { java.nio.file.Files.writeString(f.toPath(),texto); } catch(Exception e) { error(padre,e); }
            }
        });
        JButton imprimir=boton("Imprimir",()->{try{a.print();}catch(Exception e){error(padre,e);}});
        JPanel p=new JPanel(new BorderLayout());p.add(new JScrollPane(a));p.add(acciones(guardar,imprimir),BorderLayout.SOUTH);
        JOptionPane.showMessageDialog(padre,p,titulo,JOptionPane.PLAIN_MESSAGE);
    }
}
