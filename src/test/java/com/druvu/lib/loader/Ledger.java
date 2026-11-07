package com.druvu.lib.loader;

import java.nio.file.Path;

/**
 * The README's recipe on the class path: the API owns the factory, the implementation module provides it without a
 * library type, the caller's argument reaches the constructor.
 */
public interface Ledger {

    String storage();

    /**
     * Registered under {@code META-INF/services/com.druvu.lib.loader.Ledger$Factory}: the JDK's name for a nested type.
     */
    interface Factory {
        Ledger create(Path path);
    }
}
