package com.autotech.ui;

import com.autotech.negocio.*;
import com.autotech.datos.ConexionDB;
import java.sql.*;
import java.math.BigDecimal;
import javax.swing.*;
import java.awt.*;

public class PruebasPresentacion {
    static Object campo(Object o, String n) throws Exception {
        var f = o.getClass().getDeclaredField(n);
        f.setAccessible(true);
        return f.get(o);
    }

    static String config(String n) throws Exception {
        var f = ConexionDB.class.getDeclaredField(n);
        f.setAccessible(true);
        return (String) f.get(null);
    }

    static void ok(boolean b, String m) {
        if (!b) throw new AssertionError(m);
        System.out.println("OK: " + m);
    }

    static void edt(Runnable r) throws Exception {
        SwingUtilities.invokeAndWait(r);
    }

    static void esperar(JPanel p) throws Exception {
        for (int i = 0; i < 300; i++) {
            Thread.sleep(50);
            boolean[] listo = {false};
            edt(() -> listo[0] = p.getCursor().getType() != Cursor.WAIT_CURSOR);
            if (listo[0]) return;
        }
        throw new AssertionError("Tiempo de consulta agotado");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !args[0].matches("AutoTechManager_Pruebas_[0-9]+"))
            throw new IllegalArgumentException("Indica la base aislada de pruebas.");
        ok(
                Documento.valor("Total", new BigDecimal("1234567.89")).equals("L 1,234,567.89"),
                "Importes con separador de miles");
        ok(
                Documento.valor("Kilometraje", 120000).equals("120,000")
                        && Documento.valor("Anio", 2015).equals("2015")
                        && Documento.valor("OrdenId", 12345).equals("12345"),
                "Cantidades agrupadas; anios e identificadores intactos");
        ok(
                TallerService.dinero("1,234.50").compareTo(new BigDecimal("1234.50")) == 0
                        && Validaciones.numero("120,000", "Kilometraje", 0, 999999) == 120000,
                "Entradas numericas con miles");
        ok(
                Presentacion.sinTildes("Diagnóstico y autorización").equals("Diagnostico y autorizacion"),
                "Etiquetas sin tildes");
        edt(() -> {
            JComboBox<UI.Opcion> c = UI.combo(java.util.List.of(new UI.Opcion(1, "José - Vehiculo")));
            UI.presentar(c);
            Component render =
                    c.getRenderer()
                            .getListCellRendererComponent(new JList<>(), c.getItemAt(0), 0, false, false);
            ok(((JLabel) render).getText().contains("José"), "Nombres del usuario conservados");
            JTextField f = new JTextField("1.5");
            UI.campoNumerico(f, 0);
            ok(f.getText().equals("1.5"), "No se redondean cantidades invalidas en silencio");
        });
        String url = config("URL").replace("databaseName=AutoTechManager;", "databaseName=" + args[0] + ";"),
                u = config("USER"),
                p = config("PASSWORD");
        TallerService s = new TallerService(() -> DriverManager.getConnection(url, u, p));
        String recibo = s.comprobanteHtml(1);
        ok(
                recibo.contains("Subtotal")
                        && recibo.contains("L 1,850.00")
                        && recibo.contains("L 277.50")
                        && recibo.contains("Litro"),
                "Comprobante con subtotal, impuesto en lempiras y unidad");
        PanelReportes[] panel = {null};
        edt(() -> {
            TemaOscuro.aplicar();
            panel[0] = new PanelReportes(s);
            panel[0].cargarVehiculos();
        });
        esperar(panel[0]);
        JTable tabla = (JTable) campo(panel[0], "tabla");
        JComboBox<?> autos = (JComboBox<?>) campo(panel[0], "vehiculo");
        JComboBox<?> tipo = (JComboBox<?>) campo(panel[0], "tipo");
        JLabel resultado = (JLabel) campo(panel[0], "resultado");
        edt(() -> ok(tabla.getRowCount() > 1, "Reporte de todos los vehiculos"));
        edt(() -> {
            for (int i = 0; i < autos.getItemCount(); i++)
                if (((UI.Opcion) autos.getItemAt(i)).nombre().contains("TEST-01")) {
                    autos.setSelectedIndex(i);
                    break;
                }
            panel[0].consultar();
        });
        esperar(panel[0]);
        edt(() -> {
            ok(tabla.getRowCount() == 1, "Saldos filtra un solo vehiculo");
            ok(tabla.getValueAt(0, 3).equals("TEST-01"), "El resultado pertenece al vehiculo elegido");
            tipo.setSelectedIndex(9);
            panel[0].consultar();
        });
        esperar(panel[0]);
        edt(() -> {
            ok(
                    tabla.getRowCount() == 0
                            && resultado.getText().equals("Sin registros para este vehiculo"),
                    "Aviso de filtro sin resultados");
            tipo.setSelectedIndex(1);
            ok(!autos.isEnabled(), "Selector desactivado donde no corresponde filtrar");
            panel[0].consultar();
        });
        esperar(panel[0]);
        System.out.println("Verificacion de presentacion y filtro completada.");
        System.exit(0);
    }
}
