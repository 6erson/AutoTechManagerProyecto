package com.autotech.modelo;

public record Cliente(int id, String nombre, String telefono, String correo, boolean activo) {
    @Override public String toString() { return nombre + " (#" + id + ")"; }
}
