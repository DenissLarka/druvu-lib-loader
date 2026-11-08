package com.druvu.lib.loader;

/** A direct registration whose constructor fails: a deployment error, which the JDK reports as its own error. */
public interface BrokenDirect {

    class Impl implements BrokenDirect {
        public Impl() {
            throw new IllegalStateException("constructor fails on purpose");
        }
    }
}
