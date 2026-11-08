package com.autotech.ui;

import com.autotech.negocio.TallerService;
import java.awt.*;
import javax.swing.*;

public class PanelSistema extends JPanel {
    public PanelSistema() {
        super(new BorderLayout(0, 16));
        setBackground(TemaOscuro.FONDO);
        setBorder(BorderFactory.createEmptyBorder(20, 22, 18, 22));
        JLabel marca = new JLabel("AUTOTECH MANAGER", SwingConstants.CENTER);
        marca.setFont(new Font("Segoe UI", Font.BOLD, 23));
        marca.setForeground(TemaOscuro.TEXTO);
        add(marca, BorderLayout.NORTH);

        TallerService servicio = new TallerService();

        JTabbedPane tabs = new Pestanas();
        PanelCatalogos catalogos = new PanelCatalogos(servicio);
        PanelOrdenes ordenes = new PanelOrdenes(servicio);
        PanelReportes reportes = new PanelReportes(servicio);
        tabs.addTab("Clientes y vehículos", new PanelTaller());
        tabs.addTab("Catálogos e inventario", catalogos);
        tabs.addTab("Órdenes y pagos", ordenes);
        tabs.addTab("Reportes e historial", reportes);

        tabs.addChangeListener(e -> {
            switch (tabs.getSelectedIndex()) {
                case 1 -> catalogos.cargar();
                case 2 -> ordenes.cargar();
                case 3 -> reportes.cargarVehiculos();
                default -> {}
            }
        });
        add(tabs);
        UI.presentar(this);
    }
}
