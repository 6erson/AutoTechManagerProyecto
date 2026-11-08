/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.autotech;

public class AutoTechManager {

    public static void main(String[] args) {
        com.autotech.ui.TemaOscuro.aplicar();
        java.awt.EventQueue.invokeLater(() -> {
            new FrmPrincipal().setVisible(true);
        });
    }
}
