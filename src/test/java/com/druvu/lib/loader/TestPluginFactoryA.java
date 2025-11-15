package com.druvu.lib.loader;

/**
 * Factory for TestPluginA.
 *
 * @author Deniss Larka
 * on 15 Nov 2025
 */
public class TestPluginFactoryA implements ComponentFactory<TestPlugin> {

	@Override
	public TestPlugin createComponent(Dependencies dependencies) {
		return new TestPluginA();
	}

	@Override
	public Class<TestPlugin> getComponentType() {
		return TestPlugin.class;
	}
}
