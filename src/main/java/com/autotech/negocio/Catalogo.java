package com.autotech.negocio;

import java.util.List;

/** Lista cerrada de tablas y columnas administrables. Nunca recibe nombres SQL del usuario. */
public enum Catalogo {
    SERVICIOS("Servicios","ServicioId",List.of(
        new Campo("Codigo","Código",20),new Campo("Nombre","Nombre",100),new Campo("Descripcion","Descripción",400),
        new Campo("Nivel","Nivel (1 a 5)","nivel"),new Campo("Costo","Mano de obra (L)","dinero"),new Campo("Horas","Horas estimadas","horas"))),
    MECANICOS("Mecanicos","MecanicoId",List.of(new Campo("Nombre","Nombre",100),new Campo("Especialidad","Especialidad",100))),
    PROVEEDORES("Proveedores","ProveedorId",List.of(new Campo("Nombre","Nombre",100),new Campo("Telefono","Teléfono",25))),
    REPUESTOS("Repuestos","RepuestoId",List.of(new Campo("Codigo","Código",25),new Campo("Nombre","Nombre",100),
        new Campo("ProveedorId","Proveedor","proveedor"),new Campo("Minimo","Existencia mínima","entero"),
        new Campo("Costo","Costo de compra (L)","dinero"),new Campo("Precio","Precio de venta (L)","dinero"))),
    MOTORES("Motores","MotorId",List.of(new Campo("RepuestoId","Repuesto del inventario","repuesto"),new Campo("Serie","Serie del motor",60),
        new Campo("Marca","Marca compatible",60),new Campo("Modelo","Modelo compatible",60),new Campo("AnioDesde","Año desde","anio"),
        new Campo("AnioHasta","Año hasta","anio"),new Campo("Combustible","Combustible","combustible")));
    public record Campo(String columna,String etiqueta,String tipo,int max) {
        public Campo(String c,String e,int max) { this(c,e,"texto",max); }
        public Campo(String c,String e,String tipo) { this(c,e,tipo,0); }
    }
    public final String tabla,id;
    public final List<Campo> campos;
    Catalogo(String tabla,String id,List<Campo> campos) { this.tabla=tabla; this.id=id; this.campos=campos; }
    @Override public String toString() { return switch(this) { case MECANICOS->"Mecánicos"; default->tabla; }; }
}
