package com.druvu.lib.loader;

/**
 * Factory for TestPluginB.
 *
 * @author Deniss Larka on 15 Nov 2025
 */
public class TestPluginFactoryB implements ComponentFactory<TestPlugin> {

    @Override
    public TestPlugin get() {
        return new TestPluginB();
    }

    @Override
    public TestPlugin createComponent(Dependencies dependencies) {
        return get();
    }

    @Override
    public Class<TestPlugin> type() {
        return TestPlugin.class;
    }
}
