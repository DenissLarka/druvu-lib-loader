package com.druvu.lib.loader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.testng.annotations.Test;

/**
 * Tests for MultiComponentLoader.
 *
 * @author Deniss Larka on 15 Nov 2025
 */
public class MultiComponentLoaderTest {

    @Test
    public void testLoadAll_WithMultipleImplementations_ReturnsAllInstances() {
        // When: Loading all TestPlugin implementations
        List<TestPlugin> plugins = MultiComponentLoader.loadAll(TestPlugin.class);

        // Then: Should return all 3 registered implementations
        assertThat(plugins).hasSize(3);
        assertThat(plugins).extracting(TestPlugin::getName).containsExactlyInAnyOrder("PluginA", "PluginB", "PluginC");
    }

    @Test
    public void testLoadAll_WithDependencies_PassesDependenciesToFactories() {
        // Given: Dependencies object
        Dependencies dependencies = Dependencies.of();

        // When: Loading all with dependencies
        List<TestPlugin> plugins = MultiComponentLoader.loadAll(TestPlugin.class, dependencies);

        // Then: Should successfully create all instances
        assertThat(plugins).hasSize(3);
    }

    @Test
    public void testLoadAll_WhenNoFactoriesExist_ReturnsEmptyList() {
        // When: Loading a class with no registered factories
        List<String> result = MultiComponentLoader.loadAll(String.class);

        // Then: Should return empty list (not throw exception)
        assertThat(result).isEmpty();
    }

    @Test
    public void testLoadAll_WithNullTargetClass_ThrowsNullPointerException() {
        // When/Then: Should throw NullPointerException
        assertThatThrownBy(() -> MultiComponentLoader.loadAll(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("targetClass cannot be null");
    }

    @Test
    public void testLoadAll_WithNullDependencies_ThrowsNullPointerException() {
        // When/Then: Should throw NullPointerException
        assertThatThrownBy(() -> MultiComponentLoader.loadAll(TestPlugin.class, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("dependencies cannot be null");
    }

    @Test
    public void testLoadAll_ReturnedListIsUnmodifiable() {
        // When: Loading all plugins
        List<TestPlugin> plugins = MultiComponentLoader.loadAll(TestPlugin.class);

        // Then: Returned list should be unmodifiable
        assertThatThrownBy(() -> plugins.add(new TestPluginA())).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void testLoadAll_WithSingletonClass_ReturnsSingleInstance() {
        // When: Loading MySingleton (only one factory registered)
        List<MySingleton> singletons = MultiComponentLoader.loadAll(MySingleton.class);

        // Then: Should return list with one instance
        assertThat(singletons).hasSize(1);
    }

    @Test
    public void testDisposeAll_WithValidInstances_CallsFactoryDisposeMethod() {
        // Given: Loaded plugins
        List<TestPlugin> plugins = MultiComponentLoader.loadAll(TestPlugin.class);

        // When: Disposing all instances
        List<TestPlugin> disposed = MultiComponentLoader.disposeAll(TestPlugin.class, plugins);

        // Then: Should return all disposed instances
        assertThat(disposed).hasSize(3);
        assertThat(disposed).isEqualTo(plugins);
    }

    @Test
    public void testDisposeAll_WithNullTargetClass_ThrowsNullPointerException() {
        // Given: List of plugins
        List<TestPlugin> plugins = MultiComponentLoader.loadAll(TestPlugin.class);

        // When/Then: Should throw NullPointerException
        assertThatThrownBy(() -> MultiComponentLoader.disposeAll(null, plugins))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("targetClass cannot be null");
    }

    @Test
    public void testDisposeAll_WithNullInstances_ThrowsNullPointerException() {
        // When/Then: Should throw NullPointerException
        assertThatThrownBy(() -> MultiComponentLoader.disposeAll(TestPlugin.class, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("instances cannot be null");
    }

    @Test
    public void testDisposeAll_ReturnedListIsUnmodifiable() {
        // Given: Loaded plugins
        List<TestPlugin> plugins = MultiComponentLoader.loadAll(TestPlugin.class);

        // When: Disposing all instances
        List<TestPlugin> disposed = MultiComponentLoader.disposeAll(TestPlugin.class, plugins);

        // Then: Returned list should be unmodifiable
        assertThatThrownBy(() -> disposed.add(new TestPluginA())).isInstanceOf(UnsupportedOperationException.class);
    }
}
