package com.druvu.lib.loader;

/**
 * First test plugin implementation.
 *
 * @author Deniss Larka on 15 Nov 2025
 */
public class TestPluginA implements TestPlugin {

    private String disposedBy;

    @Override
    public String getName() {
        return "PluginA";
    }

    @Override
    public void disposedBy(String factoryName) {
        this.disposedBy = factoryName;
    }

    @Override
    public String disposedBy() {
        return disposedBy;
    }
}
