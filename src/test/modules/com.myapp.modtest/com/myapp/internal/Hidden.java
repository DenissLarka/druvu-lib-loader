package com.myapp.internal;

/**
 * A direct registration in a package the module keeps to itself: the loader's module cannot see the type, so it cannot
 * load it.
 */
public interface Hidden {
    String secret();
}
