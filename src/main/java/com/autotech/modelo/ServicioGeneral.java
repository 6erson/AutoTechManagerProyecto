package com.autotech.modelo;

import java.math.BigDecimal;

public final class ServicioGeneral extends Servicio {
    public ServicioGeneral(String nombre, BigDecimal costo) {
        super(nombre, costo);
    }

    @Override
    public String requisitoEntrega() {
        return "Actividad terminada y control de calidad aprobado";
    }
}
