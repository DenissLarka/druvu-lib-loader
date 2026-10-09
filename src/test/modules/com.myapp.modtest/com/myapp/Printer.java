package com.myapp;

/** Tier 2 target: created by a {@code ComponentFactory} registered under {@code java.util.ServiceLoader.Provider}. */
public interface Printer {
    String print(String text);
}
