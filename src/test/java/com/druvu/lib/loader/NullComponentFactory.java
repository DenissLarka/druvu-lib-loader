package com.druvu.lib.loader;

/** The factory of {@link NullComponent}: returns null on purpose. */
public class NullComponentFactory implements ComponentFactory<NullComponent> {

    @Override
    public NullComponent createComponent(Dependencies dependencies) {
        return null;
    }

    @Override
    public Class<NullComponent> type() {
        return NullComponent.class;
    }
}
