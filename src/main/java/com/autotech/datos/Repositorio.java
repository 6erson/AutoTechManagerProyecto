package com.autotech.datos;

import java.sql.SQLException;
import java.util.List;

public interface Repositorio<T> {
    List<T> listar() throws SQLException;
    void guardar(T entidad) throws SQLException;
}
