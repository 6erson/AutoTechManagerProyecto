package com.autotech.ui;

import com.autotech.datos.*;
import com.autotech.negocio.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.sql.*;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicReference;

public class CapturarPantallas {
    private static String dato(String n) throws Exception {
        Field f = ConexionDB.class.getDeclaredField(n);
        f.setAccessible(true);
        return (String) f.get(null);
    }

    private static Object campo(Object o, String n) throws Exception {
        Field f = o.getClass().getDeclaredField(n);
        f.setAccessible(true);
        return f.get(o);
    }

    private static void edt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    private static void esperar(JPanel p) throws Exception {
        for (int i = 0; i < 300; i++) {
            boolean[] listo = {false};
            edt(() -> listo[0] = p.getCursor().getType() != Cursor.WAIT_CURSOR);
            if (listo[0]) return;
            Thread.sleep(100);
        }
        throw new AssertionError("Tiempo agotado al cargar la pantalla.");
    }

    private static void layout(Container c) {
        c.doLayout();
        for (Component x : c.getComponents()) if (x instanceof Container h) layout(h);
    }

    private static void guardar(JPanel p, String nombre) throws Exception {
        edt(() -> {
            try {
                p.setSize(1200, 760);
                layout(p);
                layout(p);
                BufferedImage img = new BufferedImage(1200, 760, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = img.createGraphics();
                g.setRenderingHint(
                        RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                p.printAll(g);
                g.dispose();
                ImageIO.write(img, "png", new File("docs/capturas/" + nombre + ".png"));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !args[0].matches("AutoTechManager_Pruebas_[0-9]+"))
            throw new IllegalArgumentException("Indica la base aislada de pruebas.");
        new File("docs/capturas").mkdirs();
        String url = dato("URL").replaceAll("databaseName=[^;]*;", "databaseName=" + args[0] + ";"),
                user = dato("USER"),
                pass = dato("PASSWORD");
        TallerService s = new TallerService(() -> DriverManager.getConnection(url, user, pass));
        AtomicReference<PanelCatalogos> cat = new AtomicReference<>();
        AtomicReference<PanelOrdenes> ord = new AtomicReference<>();
        AtomicReference<PanelReportes> rep = new AtomicReference<>();
        edt(() -> {
            TemaOscuro.aplicar();
            cat.set(new PanelCatalogos(s));
            ord.set(new PanelOrdenes(s));
            rep.set(new PanelReportes(s));
            cat.get().cargar();
        });
        esperar(cat.get());
        guardar(cat.get(), "01-servicios");
        JComboBox<?> tipo = (JComboBox<?>) campo(cat.get(), "tipo");
        edt(() -> tipo.setSelectedItem(Catalogo.REPUESTOS));
        esperar(cat.get());
        guardar(cat.get(), "02-inventario");
        edt(() -> ord.get().cargar());
        esperar(ord.get());
        JTable tabla = (JTable) campo(ord.get(), "ordenes");
        JComboBox<?> seccion = (JComboBox<?>) campo(ord.get(), "seccion");
        edt(() -> {
            for (int i = 0; i < tabla.getRowCount(); i++)
                if (tabla.getValueAt(i, 1).equals("TEST-02")) {
                    tabla.setRowSelectionInterval(i, i);
                    break;
                }
        });
        esperar(ord.get());
        edt(() -> seccion.setSelectedItem("Actividades"));
        esperar(ord.get());
        guardar(ord.get(), "03-orden-actividades");
        edt(() -> seccion.setSelectedItem("Cambio de motor"));
        esperar(ord.get());
        guardar(ord.get(), "04-cambio-motor");
        edt(() -> seccion.setSelectedItem("Presupuesto"));
        esperar(ord.get());
        guardar(ord.get(), "05-presupuesto");
        edt(() -> rep.get().consultar());
        esperar(rep.get());
        guardar(rep.get(), "06-reportes");
        System.out.println("Seis capturas renderizadas con datos de prueba.");
        System.exit(0);
    }
}
