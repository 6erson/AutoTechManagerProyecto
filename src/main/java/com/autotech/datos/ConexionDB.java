package com.autotech.datos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class ConexionDB {

    private static final String URL = "jdbc:sqlserver://localhost:1433;"
            + "databaseName=AutoTechManager;" // Escribe el nombre de tu base de datos después del signo =.
            + "encrypt=true;"
            + "trustServerCertificate=true;"
            + "loginTimeout=5;";

    private static final String USER = "sa";
    private static final String PASSWORD = "";

    private ConexionDB() {}

    public static Connection getConexion() throws SQLException {
        if (URL.contains("databaseName=;")) {
            throw new SQLException("Escribe el nombre de tu base de datos en ConexionDB.java, en databaseName=;.");
        }
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            // Cada operación obtiene su conexión y la cierra con try-with-resources en el DAO.
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("No se encontró el driver de MSSQL-JDBC. Recarga las dependencias Maven.", e);
        }
    }
}
