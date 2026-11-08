package com.autotech.modelo;
public record OrdenTrabajo(int id,int vehiculoId,int responsableId,String estado) {
    public boolean cerrada() { return estado.equals("Finalizada y entregada") || estado.equals("Cancelada") || estado.equals("Rechazada"); }
}
