package com.autotech.modelo;

import java.math.BigDecimal;

public abstract class Servicio {
    private final String nombre;
    private final BigDecimal costo;

    protected Servicio(String nombre, BigDecimal costo) {
        this.nombre = nombre;
        this.costo = costo;
    }

    public String nombre() {
        return nombre;
    }

    public BigDecimal costoManoObra() {
        return costo;
    }

    public abstract String requisitoEntrega();

    public static Servicio crear(String nombre, BigDecimal costo, int nivel) {
        return nivel == 5 ? new ServicioMayor(nombre, costo) : new ServicioGeneral(nombre, costo);
    }
}
