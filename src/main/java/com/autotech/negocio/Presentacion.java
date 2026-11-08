package com.autotech.negocio;

import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.Locale;

public final class Presentacion {
    private Presentacion() {}

    public static String sinTildes(String texto) {
        return Normalizer.normalize(texto.replace("Año", "Anio"), Normalizer.Form.NFD)
                .replaceAll("[\\p{M}]", "");
    }

    public static String numero(Number numero, int decimales) {
        NumberFormat f = NumberFormat.getNumberInstance(Locale.US);
        f.setMinimumFractionDigits(decimales);
        f.setMaximumFractionDigits(decimales);
        return f.format(numero);
    }

    public static String entrada(String texto) {
        String limpio = texto.trim();
        if (!limpio.matches("[+-]?(?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+)(?:\\.[0-9]+)?"))
            throw new IllegalArgumentException(
                    "Numero invalido. Usa coma para miles y punto para decimales.");
        return limpio.replace(",", "");
    }
}
