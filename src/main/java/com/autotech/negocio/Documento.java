package com.autotech.negocio;

import com.autotech.datos.Sql;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.*;

public final class Documento {
    private Documento() {}

    public static String escapar(Object o) {
        return String.valueOf(o == null ? "—" : o)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("\n", "<br>");
    }

    public static String etiqueta(String s) {
        return Presentacion.sinTildes(
                switch (s) {
                    case "OrdenId" -> "Orden";
                    case "ClienteId" -> "Cliente";
                    case "VehiculoId" -> "Vehículo";
                    case "ImpuestoImporte" -> "Impuesto (L)";
                    case "BaseImponible" -> "Base imponible";
                    case "ManoObra" -> "Mano de obra";
                    case "Impuesto" -> "Impuesto (%)";
                    case "SerieAnterior" -> "Serie anterior";
                    case "SerieNueva", "MotorNuevo" -> "Serie del motor nuevo";
                    case "DestinoRetirado" -> "Destino del motor retirado";
                    case "AutorizoDescuento" -> "Descuento autorizado por";
                    default ->
                            s.replaceAll("([a-z])([A-Z])", "$1 $2")
                                    .replace("Descripcion", "Descripción")
                                    .replace("Autorizacion", "Autorización")
                                    .replace("Garantia", "Garantía");
                });
    }

    public static String valor(String campo, Object o) {
        if (o instanceof java.sql.Timestamp t)
            return t.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        if (o instanceof java.sql.Date d)
            return d.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        if (o instanceof Boolean b) return b ? "Si" : "No";
        if (o instanceof Number
                && Set.of(
                                "Total",
                                "Abonado",
                                "Saldo",
                                "Importe",
                                "Precio",
                                "Monto",
                                "ManoObra",
                                "Repuestos",
                                "Descuento",
                                "Costo",
                                "Subtotal",
                                "BaseImponible",
                                "ImpuestoImporte",
                                "Ingreso")
                        .contains(campo)) {
            NumberFormat f = NumberFormat.getNumberInstance(Locale.US);
            f.setMinimumFractionDigits(2);
            f.setMaximumFractionDigits(2);
            return "L " + f.format(o);
        }
        if (o instanceof Number n
                && !campo.endsWith("Id")
                && !Set.of("ID", "Orden", "Anio", "Año", "AnioDesde", "AnioHasta").contains(campo))
            return Presentacion.numero(
                    n,
                    o instanceof BigDecimal d
                            ? Math.max(0, d.stripTrailingZeros().scale())
                            : o instanceof Float || o instanceof Double ? 2 : 0);
        if (o instanceof String s && Set.of("Estado", "Combustible", "Decision").contains(campo))
            return Presentacion.sinTildes(s);
        return o == null ? "—" : o.toString();
    }

    public static String pagina(String titulo, String subtitulo, String cuerpo) {
        return "<html><head><meta charset='utf-8'><style>"
                   + "body{font-family:Arial,sans-serif;font-size:11pt;color:#111111;background-color:#ffffff;margin:0;padding:0;}h1{font-size:22pt;color:#111111;margin:0"
                   + " 0 4px"
                   + " 0;}h2{font-size:13pt;color:#111111;margin-top:20px;margin-bottom:8px;}p{color:#111111;margin:6px"
                   + " 0;}table{width:100%;border-collapse:collapse;margin-top:8px;}td,th{color:#111111;padding:8px;border-bottom:1px"
                   + " solid"
                   + " #d4d9de;text-align:left;vertical-align:top;}th{font-weight:bold;background-color:#edf0f3;}"
                   + " .label{width:34%;font-weight:bold;}</style></head><body><h1>"
                + escapar(Presentacion.sinTildes(titulo))
                + "</h1><p>"
                + escapar(Presentacion.sinTildes(subtitulo))
                + "</p><hr>"
                + cuerpo
                + "</body></html>";
    }

    public static String ficha(Sql.Tabla t) {
        if (t.filas().isEmpty()) return "<p>Sin registros.</p>";
        StringBuilder b = new StringBuilder("<table>");
        var fila = t.filas().get(0);
        for (int i = 0; i < t.columnas().size(); i++) {
            boolean destacado = Set.of("Total", "Saldo").contains(t.columnas().get(i));
            b.append("<tr><td class='label'>")
                    .append(escapar(etiqueta(t.columnas().get(i))))
                    .append("</td><td>");
            if (destacado) b.append("<b>");
            b.append(escapar(valor(t.columnas().get(i), fila.get(i))));
            if (destacado) b.append("</b>");
            b.append("</td></tr>");
        }
        return b.append("</table>").toString();
    }

    public static String tabla(String titulo, Sql.Tabla t) {
        if (t.filas().isEmpty()) return "";
        StringBuilder b = new StringBuilder("<h2>" + escapar(titulo) + "</h2><table><tr>");
        for (String col : t.columnas()) b.append("<th>").append(escapar(etiqueta(col))).append("</th>");
        b.append("</tr>");
        for (var fila : t.filas()) {
            b.append("<tr>");
            for (int i = 0; i < fila.size(); i++)
                b.append("<td>").append(escapar(valor(t.columnas().get(i), fila.get(i)))).append("</td>");
            b.append("</tr>");
        }
        return b.append("</table>").toString();
    }
}
