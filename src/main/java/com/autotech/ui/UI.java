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

    record Opcion(int id, String nombre) {
        @Override
        public String toString() {
            return nombre;
        }
    }

    static List<Opcion> opciones(Sql.Tabla t) {
        return t.filas().stream()
                .map(f -> new Opcion(((Number) f.get(0)).intValue(), String.valueOf(f.get(1))))
                .toList();
    }

    static JTable tabla() {
        JTable t =
                new JTable() {
                    @Override
                    public boolean getScrollableTracksViewportWidth() {
                        if (!(getParent() instanceof JViewport vista)) return true;
                        int minimo = 0;
                        for (int i = 0; i < getColumnCount(); i++)
                            minimo += getColumnModel().getColumn(i).getMinWidth();
                        return minimo <= vista.getWidth();
                    }

                    @Override
                    public void doLayout() {
                        int modo =
                                getScrollableTracksViewportWidth()
                                        ? AUTO_RESIZE_ALL_COLUMNS
                                        : AUTO_RESIZE_OFF;
                        if (getAutoResizeMode() != modo) setAutoResizeMode(modo);
                        super.doLayout();
                    }
                };
        t.setRowHeight(34);
        t.setShowVerticalLines(false);
        t.setIntercellSpacing(new Dimension(0, 1));
        javax.swing.table.DefaultTableCellRenderer celdas =
                new javax.swing.table.DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(
                            JTable tabla, Object valor, boolean seleccion, boolean foco, int fila, int col) {
                        super.getTableCellRendererComponent(tabla, valor, seleccion, false, fila, col);
                        String campo = tabla.getColumnName(col);
                        setText(com.autotech.negocio.Documento.valor(campo, valor));
                        setHorizontalAlignment(SwingConstants.CENTER);
                        setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                        return this;
                    }
                };
        for (Class<?> tipo :
                new Class<?>[] {
                    Object.class,
                    String.class,
                    Number.class,
                    Integer.class,
                    Long.class,
                    Double.class,
                    Float.class,
                    java.math.BigDecimal.class,
                    java.sql.Timestamp.class,
                    java.sql.Date.class
                }) t.setDefaultRenderer(tipo, celdas);
        javax.swing.table.DefaultTableCellRenderer encabezado =
                new javax.swing.table.DefaultTableCellRenderer();
        encabezado.setHorizontalAlignment(SwingConstants.CENTER);
        encabezado.setBackground(TemaOscuro.AZUL);
        encabezado.setForeground(Color.WHITE);
        encabezado.setFont(new Font("Segoe UI", Font.BOLD, 13));
        encabezado.setBorder(BorderFactory.createEmptyBorder(9, 10, 9, 10));
        t.getTableHeader().setDefaultRenderer(encabezado);
        t.setAutoCreateRowSorter(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.addMouseMotionListener(
                new java.awt.event.MouseMotionAdapter() {
                    @Override
                    public void mouseMoved(java.awt.event.MouseEvent e) {
                        int r = t.rowAtPoint(e.getPoint()), c = t.columnAtPoint(e.getPoint());
                        t.setToolTipText(r < 0 || c < 0 ? null : String.valueOf(t.getValueAt(r, c)));
                    }
                });
        t.addMouseListener(
                new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        if (e.getClickCount() != 2
                                || t.getSelectedRow() < 0
                                || !necesitaDetalle(t, t.getSelectedRow())) return;
                        List<String> columnas = new ArrayList<>();
                        List<Object> valores = new ArrayList<>();
                        for (int i = 0; i < t.getColumnCount(); i++) {
                            columnas.add(t.getColumnName(i));
                            valores.add(t.getValueAt(t.getSelectedRow(), i));
                        }
                        String html =
                                com.autotech.negocio.Documento.pagina(
                                        "Detalle del registro",
                                        "AutoTech Manager",
                                        com.autotech.negocio.Documento.ficha(
                                                new Sql.Tabla(columnas, List.of(valores))));
                        texto(t, "Detalle del registro", html);
                    }
                });
        t.getTableHeader().setReorderingAllowed(false);
        return t;
    }

    static void presentar(Component componente) {
        if (componente instanceof JLabel || componente instanceof AbstractButton) {
            if (componente instanceof JComponent c && c.getClientProperty("textoNormalizado") == null) {
                c.putClientProperty("textoNormalizado", true);
                c.addPropertyChangeListener(
                        "text",
                        e -> {
                            if (!(e.getNewValue() instanceof String actual)) return;
                            String limpio = com.autotech.negocio.Presentacion.sinTildes(actual);
                            if (!actual.equals(limpio)) {
                                if (c instanceof JLabel l) l.setText(limpio);
                                else ((AbstractButton) c).setText(limpio);
                            }
                        });
            }
            if (componente instanceof JLabel l && l.getText() != null)
                l.setText(com.autotech.negocio.Presentacion.sinTildes(l.getText()));
            if (componente instanceof AbstractButton b && b.getText() != null)
                b.setText(com.autotech.negocio.Presentacion.sinTildes(b.getText()));
        }
        if (componente instanceof JComboBox<?> combo) {
            combo.setRenderer(
                    new DefaultListCellRenderer() {
                        @Override
                        public Component getListCellRendererComponent(
                                JList<?> lista,
                                Object valor,
                                int indice,
                                boolean seleccionado,
                                boolean foco) {
                            Object visible =
                                    valor instanceof String || valor instanceof com.autotech.negocio.Catalogo
                                            ? com.autotech.negocio.Presentacion.sinTildes(valor.toString())
                                            : valor;
                            return super.getListCellRendererComponent(
                                    lista, visible, indice, seleccionado, foco);
                        }
                    });
        }
        if (componente instanceof JComboBox<?>
                || componente instanceof JTable
                || componente instanceof javax.swing.text.JTextComponent) return;
        if (componente instanceof Container contenedor)
            for (Component hijo : contenedor.getComponents()) presentar(hijo);
    }

    static boolean necesitaDetalle(JTable tabla, int fila) {
        for (int col = 0; col < tabla.getColumnCount(); col++) {
            Object dato = tabla.getValueAt(fila, col);
            if (!(dato instanceof String texto) || texto.isBlank()) continue;
            int ancho = tabla.getColumnModel().getColumn(col).getWidth() - 20;
            if (texto.contains("\n") || tabla.getFontMetrics(tabla.getFont()).stringWidth(texto) > ancho)
                return true;
        }
        return false;
    }

    static Component ventanaPrincipal(Component componente) {
        Window ventana =
                componente instanceof Window w
                        ? w
                        : componente == null ? null : SwingUtilities.getWindowAncestor(componente);
        if (ventana == null) return componente;
        while (ventana.getOwner() != null) ventana = ventana.getOwner();
        return ventana;
    }

    static void mostrar(JTable t, Sql.Tabla datos) {
        DefaultTableModel m =
                new DefaultTableModel(datos.columnas().toArray(), 0) {
                    @Override
                    public boolean isCellEditable(int r, int c) {
                        return false;
                    }

                    @Override
                    public Class<?> getColumnClass(int col) {
                        for (int i = 0; i < getRowCount(); i++)
                            if (getValueAt(i, col) != null) return getValueAt(i, col).getClass();
                        return Object.class;
                    }
                };
        for (var fila : datos.filas()) m.addRow(fila.toArray());
        t.setModel(m);
        ajustarColumnas(t);
    }

    static void ajustarColumnas(JTable t) {
        FontMetrics cabecera = t.getFontMetrics(new Font("Segoe UI", Font.BOLD, 13));
        FontMetrics contenido = t.getFontMetrics(t.getFont());
        for (int i = 0; i < t.getColumnCount(); i++) {
            String campo = t.getColumnName(i), titulo = com.autotech.negocio.Documento.etiqueta(campo);
            int minimo = Math.max(65, cabecera.stringWidth(titulo) + 28);
            int ancho = minimo;
            for (int r = 0; r < t.getRowCount(); r++) {
                String valor = com.autotech.negocio.Documento.valor(campo, t.getValueAt(r, i));
                ancho = Math.max(ancho, Math.min(330, contenido.stringWidth(valor) + 28));
            }
            var columna = t.getColumnModel().getColumn(i);
            columna.setMinWidth(minimo);
            columna.setPreferredWidth(Math.max(minimo, ancho));
            columna.setHeaderValue(titulo);
        }
        t.revalidate();
        t.repaint();
    }

    static JButton boton(String texto, Runnable r) {
        JButton b = new JButton(com.autotech.negocio.Presentacion.sinTildes(texto));
        b.setFocusPainted(false);
        b.setMargin(new Insets(8, 14, 8, 14));
        estiloBoton(b);
        b.addActionListener(e -> {
            try {
                r.run();
            } catch (Exception ex) {
                error(b, ex);
            }
        });
        return b;
    }

    static void estiloBoton(JButton boton) {
        String texto = boton.getText().toLowerCase(Locale.ROOT);
        Color color = TemaOscuro.PANEL;
        if (texto.contains("desactivar") && !texto.contains("activar /")) color = new Color(144, 61, 66);
        else if (texto.contains("cancelar")) color = new Color(144, 61, 66);
        else if (texto.contains("activar /")
                || texto.contains("consumos") && !texto.startsWith("ver")
                || texto.contains("presupuesto") && !texto.startsWith("ver")) color = new Color(133, 94, 37);
        else if (texto.contains("reactivar")
                || texto.startsWith("aprobar")
                || texto.contains("entregar")
                || texto.contains("registrar pago")
                || texto.contains("entrada de inventario")) color = new Color(36, 112, 89);
        else if (texto.contains("guardar")
                || texto.contains("nuevo")
                || texto.contains("nueva")
                || texto.contains("agregar")
                || texto.contains("→")
                || texto.contains("completar")) color = TemaOscuro.AZUL;
        boton.setBackground(color);
        boton.setForeground(Color.WHITE);
    }

    static JPanel acciones(JButton... botones) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        for (JButton b : botones) p.add(b);
        return p;
    }

    static JPanel pagina() {
        JPanel p = new JPanel(new BorderLayout(10, 12));
        p.setBackground(TemaOscuro.FONDO);
        p.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        return p;
    }

    static JScrollPane scroll(JTable t) {
        JScrollPane s = new JScrollPane(t);
        s.setColumnHeaderView(t.getTableHeader());
        s.getViewport().setBackground(TemaOscuro.PANEL);
        return s;
    }

    static int id(JTable t) {
        if (t.getSelectedRow() < 0) throw new IllegalArgumentException("Selecciona una fila de la tabla.");
        return ((Number) t.getValueAt(t.getSelectedRow(), 0)).intValue();
    }

    static void error(Component c, Throwable e) {
        while (e.getCause() != null) e = e.getCause();
        String mensaje = e.getMessage();
        if (e instanceof java.sql.SQLException s) {
            if (s.getErrorCode() == 2601 || s.getErrorCode() == 2627)
                mensaje = "El registro ya existe. Revisa el código, la serie o la asignación.";
            if (s.getErrorCode() == 547)
                mensaje = "Los datos no cumplen una regla: revisa cantidades, años y registros relacionados.";
            if (s.getErrorCode() == 208)
                mensaje = "Faltan tablas del taller. Ejecuta los scripts SQL 01 y 02 en AutoTechManager.";
        }
        Dialogos.showMessageDialog(
                c,
                mensaje == null ? "No se pudo completar la operación." : mensaje,
                "AutoTech Manager",
                JOptionPane.WARNING_MESSAGE);
    }

    private static void componentes(Container p, Map<Component, Boolean> estados) {
        for (Component x : p.getComponents()) {
            estados.put(x, x.isEnabled());
            x.setEnabled(false);
            if (x instanceof Container h) componentes(h, estados);
        }
    }

    static <T> void tarea(JPanel panel, Callable<T> trabajo, Consumer<T> listo) {
        Map<Component, Boolean> estados = new IdentityHashMap<>();
        componentes(panel, estados);
        panel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return trabajo.call();
            }

            @Override
            protected void done() {
                estados.forEach(Component::setEnabled);
                panel.setCursor(Cursor.getDefaultCursor());
                try {
                    listo.accept(get());
                    presentar(panel);
                } catch (Exception e) {
                    error(panel, e);
                }
            }
        }.execute();
    }

    static String valor(JComponent campo) {
        if (campo instanceof JComboBox<?> c) {
            Object o = c.getSelectedItem();
            if (o == null)
                throw new IllegalArgumentException("Selecciona una opción en todos los campos desplegables.");
            return o instanceof Opcion op ? String.valueOf(op.id()) : o.toString();
        }
        return ((javax.swing.text.JTextComponent) campo).getText().trim();
    }

    static boolean formulario(Component padre, String titulo, String[] etiquetas, JComponent... campos) {
        JPanel p = new JPanel(new GridBagLayout());
        for (int i = 0; i < campos.length; i++) {
            GridBagConstraints g = new GridBagConstraints();
            g.gridx = 0;
            g.gridy = i;
            g.anchor = GridBagConstraints.NORTHWEST;
            g.insets = new Insets(6, 6, 6, 12);
            p.add(new JLabel(com.autotech.negocio.Presentacion.sinTildes(etiquetas[i])), g);
            g.gridx = 1;
            g.weightx = 1;
            g.fill = GridBagConstraints.HORIZONTAL;
            String etiqueta =
                    com.autotech.negocio.Presentacion.sinTildes(etiquetas[i]).toLowerCase(Locale.ROOT);
            if (campos[i] instanceof JTextField numero) {
                boolean dinero =
                        etiqueta.contains("(l)") || etiqueta.contains("precio") || etiqueta.contains("costo");
                boolean cantidad =
                        etiqueta.startsWith("cantidad")
                                || etiqueta.contains("kilometraje")
                                || etiqueta.contains("existencia")
                                || etiqueta.startsWith("horas")
                                || etiqueta.contains("(%)");
                if (dinero || cantidad)
                    campoNumerico(
                            numero,
                            dinero || etiqueta.startsWith("horas") || etiqueta.contains("(%)") ? 2 : 0);
            }
            if (campos[i] instanceof JTextArea a) {
                a.setLineWrap(true);
                a.setWrapStyleWord(true);
                p.add(new JScrollPane(a), g);
            } else {
                campos[i].setPreferredSize(new Dimension(480, 30));
                p.add(campos[i], g);
            }
        }
        presentar(p);
        return Dialogos.showConfirmDialog(
                        padre, p, titulo, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
                == JOptionPane.OK_OPTION;
    }

    static void campoNumerico(JTextField campo, int decimales) {
        Runnable formato =
                () -> {
                    if (campo.getText().isBlank()) return;
                    try {
                        java.math.BigDecimal valor =
                                new java.math.BigDecimal(
                                        com.autotech.negocio.Presentacion.entrada(campo.getText()));
                        if (valor.stripTrailingZeros().scale() > decimales) return;
                        campo.setText(com.autotech.negocio.Presentacion.numero(valor, decimales));
                    } catch (IllegalArgumentException ignored) {
                    }
                };
        formato.run();
        if (campo.getClientProperty("formatoNumerico") == null) {
            campo.putClientProperty("formatoNumerico", true);
            campo.addFocusListener(
                    new java.awt.event.FocusAdapter() {
                        @Override
                        public void focusLost(java.awt.event.FocusEvent e) {
                            formato.run();
                        }
                    });
        }
    }

    static JComboBox<Opcion> combo(List<Opcion> ops) {
        return new JComboBox<>(ops.toArray(Opcion[]::new));
    }

    static JTextArea area() {
        return new JTextArea(3, 30);
    }

    static void texto(Component padre, String titulo, String html) {
        JEditorPane a = new JEditorPane("text/html", html);
        a.setEditable(false);
        a.setBackground(Color.WHITE);
        a.setForeground(Color.BLACK);
        a.setMargin(new Insets(0, 0, 0, 0));
        a.setCaretPosition(0);
        a.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.FALSE);
        JButton guardar =
                boton(
                        "Guardar documento",
                        () -> {
                            JFileChooser fc = new JFileChooser();
                            fc.setSelectedFile(new java.io.File("AutoTech-comprobante.html"));
                            if (fc.showSaveDialog(UI.ventanaPrincipal(padre))
                                    == JFileChooser.APPROVE_OPTION) {
                                java.io.File f = fc.getSelectedFile();
                                if (f.exists()
                                        && Dialogos.showConfirmDialog(
                                                        padre,
                                                        "¿Reemplazar el archivo?",
                                                        "Guardar",
                                                        JOptionPane.YES_NO_OPTION)
                                                != JOptionPane.YES_OPTION) return;
                                try {
                                    java.nio.file.Files.writeString(f.toPath(), html);
                                } catch (Exception e) {
                                    error(padre, e);
                                }
                            }
                        });
        JButton imprimir =
                boton(
                        "Imprimir",
                        () -> {
                            try {
                                Impresion.imprimir(a);
                            } catch (Exception e) {
                                error(padre, e);
                            }
                        });
        JPanel p = new JPanel(new BorderLayout());
        JScrollPane vista = new JScrollPane(a);
        vista.setPreferredSize(new Dimension(760, 540));
        vista.getViewport().setBackground(Color.WHITE);
        p.add(vista);
        p.add(acciones(guardar, imprimir), BorderLayout.SOUTH);
        Dialogos.showMessageDialog(padre, p, titulo, JOptionPane.PLAIN_MESSAGE);
    }
}
