package com.autotech.datos;

import java.sql.*;
import java.util.*;

public final class Sql {
    private Sql() {}

    public record Tabla(List<String> columnas, List<List<Object>> filas) {}

    public static PreparedStatement preparar(Connection c, String sql, Object... args) throws SQLException {
        PreparedStatement s = c.prepareStatement(sql);
        for (int i = 0; i < args.length; i++) s.setObject(i + 1, args[i]);
        return s;
    }

    public static int ejecutar(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement s = preparar(c, sql, args)) {
            return s.executeUpdate();
        }
    }

    public static Tabla consultar(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement s = preparar(c, sql, args);
                ResultSet r = s.executeQuery()) {
            List<String> columnas = new ArrayList<>();
            List<List<Object>> filas = new ArrayList<>();
            for (int i = 1; i <= r.getMetaData().getColumnCount(); i++)
                columnas.add(r.getMetaData().getColumnLabel(i));
            while (r.next()) {
                List<Object> fila = new ArrayList<>();
                for (int i = 1; i <= columnas.size(); i++) fila.add(r.getObject(i));
                filas.add(fila);
            }
            return new Tabla(columnas, filas);
        }
    }

    public static Object escalar(Connection c, String sql, Object... args) throws SQLException {
        Tabla t = consultar(c, sql, args);
        return t.filas().isEmpty() ? null : t.filas().get(0).get(0);
    }

    public static int insertar(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < args.length; i++) s.setObject(i + 1, args[i]);
            s.executeUpdate();
            try (ResultSet r = s.getGeneratedKeys()) {
                if (r.next()) return r.getInt(1);
            }
            throw new SQLException("No se obtuvo el identificador del registro.");
        }
    }
}
