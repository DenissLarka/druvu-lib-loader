package com.myapp.plugins;

import com.myapp.AccBook;
import java.nio.file.Path;

/**
 * Provided under {@code com.myapp.AccBook.Factory} in module-info: the JDK constructs this, the caller's path reaches
 * the book.
 */
public final class XmlBookFactory implements AccBook.Factory {
    @Override
    public AccBook create(Path path) {
        return new XmlBook(path);
    }
}
