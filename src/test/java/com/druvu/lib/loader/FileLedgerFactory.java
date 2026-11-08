package com.druvu.lib.loader;

import java.nio.file.Path;

/** The one {@link Ledger.Factory} registration: the JDK constructs it, the path goes to the ledger. */
public final class FileLedgerFactory implements Ledger.Factory {

    @Override
    public Ledger create(Path path) {
        return new FileLedger(path);
    }
}
