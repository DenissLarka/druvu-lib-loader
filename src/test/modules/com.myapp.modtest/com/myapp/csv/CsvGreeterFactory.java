package com.myapp.csv;

import com.druvu.lib.loader.ComponentFactory;
import com.druvu.lib.loader.Dependencies;
import com.myapp.Greeter;

/** Registered under {@code com.druvu.lib.loader.ComponentFactory} in module-info (tier 1). */
public final class CsvGreeterFactory implements ComponentFactory<Greeter> {
    @Override
    public Greeter createComponent(Dependencies dependencies) {
        return new CsvGreeter();
    }

    @Override
    public Class<Greeter> type() {
        return Greeter.class;
    }
}
