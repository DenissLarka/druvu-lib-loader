package com.druvu.lib.loader;

/** A component type with two direct registrations: {@code load} refuses it, {@code loadAll} takes both. */
public interface TwiceRegistered {
    String name();
}
