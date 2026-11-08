package com.autotech.ui;

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.ColorUIResource;

public final class TemaOscuro {
    public static final Color FONDO=new Color(20,26,35), PANEL=new Color(29,37,49),
            TEXTO=new Color(229,235,244), AZUL=new Color(51,113,184), VERDE=new Color(92,196,165);
    private TemaOscuro() {}
    public static void aplicar() {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch(Exception ignored) {}
        ColorUIResource fondo=new ColorUIResource(PANEL), texto=new ColorUIResource(TEXTO);
        for(String clave : new String[]{"TextArea.background","List.background","Panel.background","TabbedPane.background","Table.background","Viewport.background","TextField.background","ComboBox.background","Button.background","OptionPane.background","ScrollPane.background"}) UIManager.put(clave,fondo);
        for(String clave : new String[]{"TextArea.foreground","List.foreground","Label.foreground","Button.foreground","TextField.foreground","Table.foreground","ComboBox.foreground","TabbedPane.foreground","OptionPane.messageForeground","TableHeader.foreground"}) UIManager.put(clave,texto);
        UIManager.put("TableHeader.background",new ColorUIResource(AZUL));
        UIManager.put("Table.selectionBackground",new ColorUIResource(AZUL));
        UIManager.put("Table.selectionForeground",texto);
        UIManager.put("TextField.caretForeground",texto);
        UIManager.put("TextArea.caretForeground",texto);
        UIManager.put("TextField.border",BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(65,80,99)),BorderFactory.createEmptyBorder(4,6,4,6)));
        UIManager.put("TabbedPane.selected",new ColorUIResource(AZUL));
        UIManager.put("TabbedPane.unselectedBackground",fondo);
        UIManager.put("Button.gradient",java.util.List.of(0.0f, 0.0f, fondo, fondo, fondo));
        UIManager.put("Table.gridColor",new ColorUIResource(49,61,77));
        UIManager.put("ComboBox.selectionBackground",new ColorUIResource(AZUL));
        UIManager.put("ComboBox.selectionForeground",texto);
        UIManager.put("Label.font",new Font("Segoe UI",Font.PLAIN,13));
        UIManager.put("Button.font",new Font("Segoe UI",Font.BOLD,13));
        UIManager.put("TextField.font",new Font("Segoe UI",Font.PLAIN,14));
        UIManager.put("Table.font",new Font("Segoe UI",Font.PLAIN,13));
    }
}
