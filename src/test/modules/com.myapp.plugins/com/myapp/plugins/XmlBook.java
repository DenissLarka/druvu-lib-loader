package com.myapp.plugins;

import com.myapp.AccBook;
import java.nio.file.Path;

/** Needs its path at construction: the reason for a factory. Package-private, like an implementation should be. */
final class XmlBook implements AccBook {
    private final Path path;

    XmlBook(Path path) {
        this.path = path;
    }

    @Override
    public String storage() {
        return path.toString();
    }
}
