package com.autotech.modelo;

import java.math.BigDecimal;

public final class ServicioMayor extends Servicio {
    public ServicioMayor(String nombre, BigDecimal costo) {
        super(nombre, costo);
    }

    @Override
    public String requisitoEntrega() {
        return "Motor compatible, pruebas, garantía y control de calidad aprobado";
    }
}
