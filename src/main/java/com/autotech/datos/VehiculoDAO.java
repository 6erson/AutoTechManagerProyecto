package com.autotech.datos;

import com.autotech.modelo.Vehiculo;
import java.sql.*;
import java.util.*;

public class VehiculoDAO implements Repositorio<Vehiculo> {
    public List<Vehiculo> listar() throws SQLException {
        List<Vehiculo> resultado = new ArrayList<>();
        try (Connection c = ConexionDB.getConexion();
                PreparedStatement s =
                        c.prepareStatement(
                                "SELECT"
                                    + " v.VehiculoId,v.ClienteId,c.Nombre,v.Placa,v.VIN,v.Marca,v.Modelo,v.Anio,v.Color,v.Kilometraje,v.Combustible"
                                    + " FROM Vehiculos v INNER JOIN Clientes c ON c.ClienteId=v.ClienteId"
                                    + " ORDER BY v.Placa");
                ResultSet r = s.executeQuery()) {
            while (r.next())
                resultado.add(
                        new Vehiculo(
                                r.getInt(1),
                                r.getInt(2),
                                r.getString(3),
                                r.getString(4),
                                r.getString(5),
                                r.getString(6),
                                r.getString(7),
                                r.getInt(8),
                                r.getString(9),
                                r.getInt(10),
                                r.getString(11)));
        }
        return resultado;
    }

    public void guardar(Vehiculo v) throws SQLException {
        String sql =
                v.id() == 0
                        ? "INSERT INTO"
                              + " Vehiculos(ClienteId,Placa,VIN,Marca,Modelo,Anio,Color,Kilometraje,Combustible)"
                              + " SELECT ?,?,?,?,?,?,?,?,? WHERE EXISTS(SELECT 1 FROM Clientes WITH"
                              + " (UPDLOCK,HOLDLOCK) WHERE ClienteId=? AND Activo=1)"
                        : "UPDATE Vehiculos SET"
                              + " ClienteId=?,Placa=?,VIN=?,Marca=?,Modelo=?,Anio=?,Color=?,Kilometraje=?,Combustible=?"
                              + " WHERE EXISTS(SELECT 1 FROM Clientes WITH (UPDLOCK,HOLDLOCK) WHERE"
                              + " ClienteId=? AND Activo=1) AND VehiculoId=?";
        try (Connection c = ConexionDB.getConexion();
                PreparedStatement s = c.prepareStatement(sql)) {
            s.setInt(1, v.clienteId());
            s.setString(2, v.placa());
            s.setString(3, v.vin());
            s.setString(4, v.marca());
            s.setString(5, v.modelo());
            s.setInt(6, v.anio());
            s.setString(7, v.color());
            s.setInt(8, v.kilometraje());
            s.setString(9, v.combustible());
            s.setInt(10, v.clienteId());
            if (v.id() != 0) s.setInt(11, v.id());
            if (s.executeUpdate() != 1)
                throw new SQLException("El propietario está inactivo o el vehículo ya no existe.");
        }
    }
}
