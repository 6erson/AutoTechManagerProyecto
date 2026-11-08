package com.autotech.ui;

import com.autotech.negocio.TallerService;
import java.awt.*;
import javax.swing.*;

public class PanelSistema extends JPanel {
    public PanelSistema() {
        super(new BorderLayout());
        TallerService servicio=new TallerService();
        JTabbedPane tabs=new JTabbedPane();
        PanelCatalogos catalogos=new PanelCatalogos(servicio);
        PanelOrdenes ordenes=new PanelOrdenes(servicio);
        PanelReportes reportes=new PanelReportes(servicio);
        tabs.addTab("Clientes y vehículos",new PanelTaller());
        tabs.addTab("Catálogos e inventario",catalogos);
        tabs.addTab("Órdenes y pagos",ordenes);
        tabs.addTab("Reportes e historial",reportes);
        tabs.addChangeListener(e->{
            switch(tabs.getSelectedIndex()) {
                case 1->catalogos.cargar();case 2->ordenes.cargar();case 3->reportes.cargarVehiculos();
                default->{}
            }
        });
        add(tabs);
    }
}
