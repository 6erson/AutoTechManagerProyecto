package com.autotech;

import com.autotech.ui.PanelSistema;
import java.awt.Dimension;
import javax.swing.JFrame;

public class FrmPrincipal extends JFrame {
    public FrmPrincipal() {
        setTitle("AutoTech Manager - Taller Mecanico");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(new PanelSistema());
        setSize(1200, 800);
        setMinimumSize(new Dimension(1100, 750));
        setLocationRelativeTo(null);
    }
}
