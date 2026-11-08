package com.autotech.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

final class Pestanas extends JTabbedPane {
    @Override
    public void addTab(String titulo, Component contenido) {
        super.addTab(com.autotech.negocio.Presentacion.sinTildes(titulo), contenido);
    }

    Pestanas() {
        setFont(new Font("Segoe UI", Font.BOLD, 13));
        setBackground(TemaOscuro.FONDO);
        setUI(
                new BasicTabbedPaneUI() {
                    @Override
                    protected void installDefaults() {
                        super.installDefaults();
                        tabInsets = new Insets(11, 20, 11, 20);
                        selectedTabPadInsets = new Insets(0, 0, 0, 0);
                        tabAreaInsets = new Insets(4, 4, 10, 4);
                        contentBorderInsets = new Insets(0, 0, 0, 0);
                    }

                    @Override
                    protected LayoutManager createLayoutManager() {
                        return new TabbedPaneLayout() {
                            @Override
                            protected void calculateTabRects(int placement, int cantidad) {
                                super.calculateTabRects(placement, cantidad);
                                if (cantidad == 0 || runCount != 1) return;
                                Insets margen = tabPane.getInsets();
                                int ancho = rects[cantidad - 1].x + rects[cantidad - 1].width - rects[0].x;
                                int desplazamiento =
                                        margen.left
                                                + (tabPane.getWidth() - margen.left - margen.right - ancho)
                                                        / 2
                                                - rects[0].x;
                                for (int i = 0; i < cantidad; i++) rects[i].x += desplazamiento;
                            }
                        };
                    }

                    @Override
                    protected void paintTabBackground(
                            Graphics g,
                            int placement,
                            int index,
                            int x,
                            int y,
                            int w,
                            int h,
                            boolean selected) {
                        Graphics2D copia = (Graphics2D) g.create();
                        copia.setRenderingHint(
                                RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        copia.setColor(selected ? TemaOscuro.AZUL : TemaOscuro.PANEL);
                        copia.fillRoundRect(x + 2, y, w - 4, h - 2, 8, 8);
                        copia.dispose();
                    }

                    @Override
                    protected void paintTabBorder(
                            Graphics g,
                            int placement,
                            int index,
                            int x,
                            int y,
                            int w,
                            int h,
                            boolean selected) {}

                    @Override
                    protected void paintContentBorder(Graphics g, int placement, int index) {}

                    @Override
                    protected void paintFocusIndicator(
                            Graphics g,
                            int placement,
                            Rectangle[] rects,
                            int index,
                            Rectangle icon,
                            Rectangle text,
                            boolean selected) {}
                });
    }
}
