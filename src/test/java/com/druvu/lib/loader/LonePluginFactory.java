package com.druvu.lib.loader;

/** The one factory of {@link LonePlugin}, registered in the test services file. */
public class LonePluginFactory implements ComponentFactory<LonePlugin> {

    @Override
    public LonePlugin createComponent(Dependencies dependencies) {
        return new LonePlugin();
    }

    @Override
    public LonePlugin disposeComponent(LonePlugin component) {
        return component;
    }

    @Override
    public Class<LonePlugin> type() {
        return LonePlugin.class;
    }
}
