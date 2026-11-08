package com.autotech.negocio;

import java.time.Year;

public final class Validaciones {
    private Validaciones() {}
    public static String texto(String valor, String campo, int max) {
        String limpio=valor.trim();
        if(limpio.isEmpty()) throw new IllegalArgumentException("Completa el campo " + campo + ".");
        if(limpio.length()>max) throw new IllegalArgumentException(campo + ": máximo " + max + " caracteres.");
        return limpio;
    }
    public static String telefono(String valor) {
        String t=texto(valor,"teléfono",25);
        if(!t.matches("[+0-9() .-]+") || t.replaceAll("[^0-9]", "").length()<8)
            throw new IllegalArgumentException("Escribe un teléfono válido de al menos 8 dígitos.");
        return t;
    }
    public static String correo(String valor) {
        String t=valor.trim();
        if(t.length()>120 || (!t.isEmpty() && !t.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")))
            throw new IllegalArgumentException("El correo electrónico no es válido.");
        return t;
    }
    public static int numero(String valor,String campo,int min,int max) {
        try { int n=Integer.parseInt(valor.trim()); if(n>=min && n<=max) return n; }
        catch(NumberFormatException ignored) {}
        throw new IllegalArgumentException(campo + " debe ser un entero entre " + min + " y " + max + ".");
    }
    public static int anio(String valor) { return numero(valor,"Año",1900,Year.now().getValue()+1); }
}
