package com.autotech.ui;

import java.awt.print.*;
import javax.swing.*;

final class Impresion {
    private Impresion() {}

    static PageFormat formato(PageFormat original) {
        PageFormat pagina = (PageFormat) original.clone();
        Paper papel = pagina.getPaper();
        double margen = 12.0 * 72.0 / 25.4;
        papel.setImageableArea(margen, margen, papel.getWidth() - 2 * margen, papel.getHeight() - 2 * margen);
        pagina.setPaper(papel);
        return pagina;
    }

    static void imprimir(JEditorPane documento) throws PrinterException {
        PrinterJob trabajo = PrinterJob.getPrinterJob();
        trabajo.setJobName("AutoTech Manager");
        // La impresora puede necesitar un margen mínimo mayor.
        PageFormat pagina = trabajo.validatePage(formato(trabajo.defaultPage()));
        trabajo.setPrintable(documento.getPrintable(null, null), pagina);
        if (trabajo.printDialog()) {
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws PrinterException {
                    trabajo.print();
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                    } catch (Exception e) {
                        UI.error(documento, e);
                    }
                }
            }.execute();
        }
    }
}
