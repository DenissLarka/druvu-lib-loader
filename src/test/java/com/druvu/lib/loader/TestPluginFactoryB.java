package com.druvu.lib.loader;

/**
 * Factory for TestPluginB.
 *
 * @author Deniss Larka
 * on 15 Nov 2025
 */
public class TestPluginFactoryB implements ComponentFactory<TestPlugin> {

	@Override
	public TestPlugin createComponent(Dependencies dependencies) {
		return new TestPluginB();
	}

	@Override
	public Class<TestPlugin> getComponentType() {
		return TestPlugin.class;
	}
}
