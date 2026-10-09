package com.myapp.internal;

/** Tier 3 target in a package the module keeps to itself: the loader's module cannot see it, so it cannot load it. */
public interface Hidden {
    String secret();
}
