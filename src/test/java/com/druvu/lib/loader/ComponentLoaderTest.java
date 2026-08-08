package com.druvu.lib.loader;

import org.testng.annotations.Test;

public class ComponentLoaderTest {

    @Test(expectedExceptions = NullPointerException.class)
    public void testCreate_WithNullTargetClass_ThrowsNullPointerException() {
        Dependencies dependencies = createDefaultDependencies();
        ComponentLoader.load(null, dependencies);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testCreate_WithNullDependencies_ThrowsNullPointerException() {
        ComponentLoader.load(String.class, null);
    }

    @Test(expectedExceptions = TargetClassNotFoundException.class)
    public void testCreate_WhenFactoryCreatesNull_ThrowsIllegalStateException() {
        Dependencies dependencies = createDefaultDependencies();
        ComponentLoader.load(FakeComponent.class, dependencies);
    }

    @Test(expectedExceptions = TargetClassNotFoundException.class)
    public void testCreate_WhenFactoryNotFound_ThrowsTargetClassNotFoundException() {
        Dependencies dependencies = createDefaultDependencies();
        ComponentLoader.load(Integer.class, dependencies);
    }

    private Dependencies createDefaultDependencies() {
        // Create default Dependencies object
        return new Dependencies();
    }

    private static class FakeComponent {
        // Dummy class for negative test cases
    }

    private static class ServiceLoaderExtendedStub implements ComponentFactory {
        @Override
        public Class type() {
            return FakeComponent.class;
        }

        @Override
        public Object get() {
            return null;
        }

        @Override
        public Object createComponent(Dependencies dependencies) {
            return null; // Simulate an invalid factory
        }
    }
}
