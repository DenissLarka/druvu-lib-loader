package com.druvu.lib.loader;

/**
 * Test interface for multi-component loading. Multiple implementations will be created to test MultiComponentLoader.
 *
 * <p>Every implementation records which factory disposed it, so a component handed to a foreign factory is visible to
 * the tests instead of passing silently.
 *
 * @author Deniss Larka on 15 Nov 2025
 */
public interface TestPlugin {
    String getName();

    void disposedBy(String factoryName);

    String disposedBy();
}
