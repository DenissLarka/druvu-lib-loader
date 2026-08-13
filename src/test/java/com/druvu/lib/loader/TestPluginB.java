package com.druvu.lib.loader;

/**
 * Second test plugin implementation.
 *
 * @author Deniss Larka on 15 Nov 2025
 */
public class TestPluginB implements TestPlugin {

    private String disposedBy;

    @Override
    public String getName() {
        return "PluginB";
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
