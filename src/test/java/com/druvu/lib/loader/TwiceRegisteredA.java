package com.druvu.lib.loader;

/** The first direct registration of {@link TwiceRegistered}. */
public class TwiceRegisteredA implements TwiceRegistered {
    @Override
    public String name() {
        return "A";
    }
}
