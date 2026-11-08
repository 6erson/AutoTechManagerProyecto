package com.autotech.ui;

import java.awt.Component;
import javax.swing.*;
import com.autotech.negocio.Presentacion;

final class Dialogos {
    private Dialogos() {}

    private static Object contenido(Object mensaje) {
        if (mensaje instanceof Component c) UI.presentar(c);
        return mensaje instanceof String s ? Presentacion.sinTildes(s) : mensaje;
    }

    static void showMessageDialog(Component padre, Object mensaje) {
        showMessageDialog(padre, mensaje, "AutoTech Manager", JOptionPane.INFORMATION_MESSAGE);
    }

    static void showMessageDialog(Component padre, Object mensaje, String titulo, int tipo) {
        JOptionPane.showMessageDialog(
                UI.ventanaPrincipal(padre), contenido(mensaje), Presentacion.sinTildes(titulo), tipo);
    }

    static int showConfirmDialog(Component padre, Object mensaje, String titulo, int opciones) {
        return showConfirmDialog(padre, mensaje, titulo, opciones, JOptionPane.QUESTION_MESSAGE);
    }

    static int showConfirmDialog(Component padre, Object mensaje, String titulo, int opciones, int tipo) {
        return JOptionPane.showConfirmDialog(
                UI.ventanaPrincipal(padre),
                contenido(mensaje),
                Presentacion.sinTildes(titulo),
                opciones,
                tipo);
    }

    static int showOptionDialog(
            Component padre,
            Object mensaje,
            String titulo,
            int opciones,
            int tipo,
            Icon icono,
            Object[] botones,
            Object inicial) {
        Object[] textos = new Object[botones.length];
        for (int i = 0; i < botones.length; i++) textos[i] = contenido(botones[i]);
        Object seleccionado = inicial instanceof String s ? Presentacion.sinTildes(s) : inicial;
        return JOptionPane.showOptionDialog(
                UI.ventanaPrincipal(padre),
                contenido(mensaje),
                Presentacion.sinTildes(titulo),
                opciones,
                tipo,
                icono,
                textos,
                seleccionado);
    }
}
