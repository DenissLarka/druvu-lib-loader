package com.myapp;

import com.druvu.lib.loader.MultiComponentLoader;
import java.nio.file.Path;
import java.util.List;

/** A plugin type with several API-owned factories registered: every exporter receives the same target at creation. */
public interface Exporter {
    String format();

    /** The API-owned factory: each implementation module provides one. */
    interface Factory {
        Exporter create(Path target);
    }

    /** Every registered factory, each handed the target. */
    static List<Exporter> createAll(Path target) {
        return MultiComponentLoader.loadAll(Factory.class).stream()
                .map(factory -> factory.create(target))
                .toList();
    }
}
