package com.autotech.modelo;

public record Vehiculo(int id, int clienteId, String propietario, String placa, String vin,
        String marca, String modelo, int anio, String color, int kilometraje, String combustible) {}
