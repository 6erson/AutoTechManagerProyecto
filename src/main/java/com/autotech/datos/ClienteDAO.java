package com.autotech.datos;

import com.autotech.modelo.Cliente;
import java.sql.*;
import java.util.*;

public class ClienteDAO implements Repositorio<Cliente> {
    public List<Cliente> listar() throws SQLException {
        List<Cliente> resultado = new ArrayList<>();
        try (Connection c = ConexionDB.getConexion(); PreparedStatement s = c.prepareStatement(
                "SELECT ClienteId,Nombre,Telefono,Correo,Activo FROM Clientes ORDER BY Nombre");
                ResultSet r = s.executeQuery()) {
            while (r.next()) resultado.add(new Cliente(r.getInt(1),r.getString(2),r.getString(3),r.getString(4),r.getBoolean(5)));
        }
        return resultado;
    }
    public void guardar(Cliente x) throws SQLException {
        String sql = x.id() == 0 ? "INSERT INTO Clientes(Nombre,Telefono,Correo) VALUES(?,?,?)"
                : "UPDATE Clientes SET Nombre=?,Telefono=?,Correo=? WHERE ClienteId=? AND Activo=1";
        try (Connection c = ConexionDB.getConexion(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1,x.nombre()); s.setString(2,x.telefono()); s.setString(3,x.correo());
            if (x.id()!=0) s.setInt(4,x.id());
            if(s.executeUpdate()!=1) throw new SQLException("El cliente ya no está activo o no existe. Actualiza la lista.");
        }
    }
    public void desactivar(int id) throws SQLException {
        try (Connection c = ConexionDB.getConexion(); PreparedStatement s = c.prepareStatement(
                "UPDATE Clientes SET Activo=0 WHERE ClienteId=? AND Activo=1")) {
            s.setInt(1,id);
            if(s.executeUpdate()!=1) throw new SQLException("El cliente ya fue desactivado. Actualiza la lista.");
        }
    }
}
