package com.druvu.lib.loader;

/**
 * Factory for TestPluginC.
 *
 * @author Deniss Larka
 * on 15 Nov 2025
 */
public class TestPluginFactoryC implements ComponentFactory<TestPlugin> {

	@Override
	public TestPlugin createComponent(Dependencies dependencies) {
		return new TestPluginC();
	}

	@Override
	public Class<TestPlugin> getComponentType() {
		return TestPlugin.class;
	}
}
