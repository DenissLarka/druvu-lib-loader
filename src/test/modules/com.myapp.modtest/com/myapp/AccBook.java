package com.myapp;

import com.druvu.lib.loader.ComponentLoader;
import java.nio.file.Path;

/**
 * The README's example. Registered directly, it is constructed by the JDK; through its own {@link Factory}, provided by
 * an implementation module with no library type, it is created with the path the caller hands over.
 */
public interface AccBook {
    String storage();

    /** The API-owned factory: the implementation module provides this, and receives the path at creation. */
    interface Factory {
        AccBook create(Path path);
    }

    /** Finds the one registered factory and creates the book with it. */
    static AccBook create(Path path) {
        return ComponentLoader.load(Factory.class).create(path);
    }
}
