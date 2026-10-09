package com.myapp.csv;

import com.druvu.lib.loader.ComponentFactory;
import com.druvu.lib.loader.Dependencies;
import com.myapp.Printer;

/** Registered under {@code java.util.ServiceLoader.Provider} in module-info (tier 2). */
public final class CsvPrinterFactory implements ComponentFactory<Printer> {
    @Override
    public Printer createComponent(Dependencies dependencies) {
        return new CsvPrinter();
    }

    @Override
    public Class<Printer> type() {
        return Printer.class;
    }
}
