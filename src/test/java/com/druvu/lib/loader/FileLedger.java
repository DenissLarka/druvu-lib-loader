package com.druvu.lib.loader;

import java.nio.file.Path;

/** Needs its path at construction, which is why a factory stands in front of it. Package-private on purpose. */
final class FileLedger implements Ledger {

    private final Path path;

    FileLedger(Path path) {
        this.path = path;
    }

    @Override
    public String storage() {
        return path.toString();
    }
}
