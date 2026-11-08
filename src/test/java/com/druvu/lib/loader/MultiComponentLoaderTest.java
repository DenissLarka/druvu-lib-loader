package com.druvu.lib.loader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
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
    public void testLoadAll_WithOneFactory_ReturnsOneInstance() {
        // When: Loading LonePlugin (only one factory registered)
        List<LonePlugin> singletons = MultiComponentLoader.loadAll(LonePlugin.class);

        // Then: Should return list with one instance
        assertThat(singletons).hasSize(1);
    }

    @Test
    public void testDisposeAll_WithValidInstances_CallsFactoryDisposeMethod() {
        // Given: Loaded plugins
        List<TestPlugin> plugins = MultiComponentLoader.loadAll(TestPlugin.class);

        // When: Disposing all instances
        List<TestPlugin> disposed = MultiComponentLoader.disposeAll(TestPlugin.class, plugins);

        // Then: Should return all disposed instances, each through its own factory
        assertThat(disposed).hasSize(3);
        assertThat(disposed).isEqualTo(plugins);
        assertDisposedByOwnFactory(disposed);
    }

    @Test
    public void testDisposeAll_WithASubset_UsesTheCreatingFactoryAndNotTheFirstOne() {
        // Given: Loaded plugins, of which only the last one is to be disposed
        List<TestPlugin> plugins = MultiComponentLoader.loadAll(TestPlugin.class);
        TestPlugin pluginC = pluginNamed(plugins, "PluginC");

        // When: Disposing that one alone
        List<TestPlugin> disposed = MultiComponentLoader.disposeAll(TestPlugin.class, List.of(pluginC));

        // Then: Its own factory disposed it, not the one sitting at index 0
        assertThat(disposed).containsExactly(pluginC);
        assertThat(pluginC.disposedBy()).isEqualTo("TestPluginFactoryC");
    }

    @Test
    public void testDisposeAll_WithAReorderedList_UsesTheCreatingFactoryOfEach() {
        // Given: Loaded plugins handed back in the reverse order
        List<TestPlugin> plugins = MultiComponentLoader.loadAll(TestPlugin.class);

        // When: Disposing them
        MultiComponentLoader.disposeAll(TestPlugin.class, plugins.reversed());

        // Then: Each was still disposed by its own factory
        assertDisposedByOwnFactory(plugins);
    }

    @Test
    public void testDisposeAll_WithAnUnknownComponent_SkipsItWithoutThrowing() {
        // Given: A component this loader never created
        TestPlugin neverLoaded = new TestPluginA();

        // When: Disposing it
        List<TestPlugin> disposed = MultiComponentLoader.disposeAll(TestPlugin.class, List.of(neverLoaded));

        // Then: It is skipped with a WARN, no factory touched it
        assertThat(disposed).isEmpty();
        assertThat(neverLoaded.disposedBy()).isNull();
    }

    @Test
    public void testDisposeAll_CalledTwice_SkipsTheAlreadyDisposedWithoutThrowing() {
        // Given: Plugins already disposed once
        List<TestPlugin> plugins = MultiComponentLoader.loadAll(TestPlugin.class);
        MultiComponentLoader.disposeAll(TestPlugin.class, plugins);

        // When: Disposing them again
        List<TestPlugin> disposedAgain = MultiComponentLoader.disposeAll(TestPlugin.class, plugins);

        // Then: Nothing is disposed twice, and nothing is thrown
        assertThat(disposedAgain).isEmpty();
    }

    @Test
    public void testDisposeAll_WhenAFactoryFails_DisposesTheOtherComponentsAnyway() {
        // Given: Two components, one of which always fails to dispose
        List<FragilePlugin> plugins = MultiComponentLoader.loadAll(FragilePlugin.class);
        assertThat(plugins).hasSize(2);

        // When: Disposing both
        List<FragilePlugin> disposed = MultiComponentLoader.disposeAll(FragilePlugin.class, plugins);

        // Then: The sound one is released, the failing one only logged
        assertThat(disposed).extracting(FragilePlugin::getName).containsExactly("Sound");
    }

    @Test
    public void testDisposeAll_WithANullComponent_SkipsItWithoutThrowing() {
        // Given: A list holding a null
        List<TestPlugin> withANull = new ArrayList<>();
        withANull.add(null);

        // When: Disposing it
        List<TestPlugin> disposed = MultiComponentLoader.disposeAll(TestPlugin.class, withANull);

        // Then: Skipped with a WARN
        assertThat(disposed).isEmpty();
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

    private void assertDisposedByOwnFactory(List<TestPlugin> plugins) {
        assertThat(plugins).allSatisfy(plugin -> assertThat(plugin.disposedBy()).isEqualTo(ownFactoryOf(plugin)));
    }

    private String ownFactoryOf(TestPlugin plugin) {
        // PluginA was created by TestPluginFactoryA, and only that one may dispose it
        return "TestPluginFactory" + plugin.getName().substring("Plugin".length());
    }

    private TestPlugin pluginNamed(List<TestPlugin> plugins, String name) {
        return plugins.stream()
                .filter(plugin -> name.equals(plugin.getName()))
                .findFirst()
                .orElseThrow();
    }

    @Test
    public void testLoadAll_WithDirectRegistrations_TakesThemAll() {
        // When: Loading a type with two direct registrations and no factory; the dependencies go nowhere on this way
        List<TwiceRegistered> both =
                MultiComponentLoader.loadAll(TwiceRegistered.class, Dependencies.of(String.class, "unused"));

        // Then: Both arrive, constructed by the JDK
        assertThat(both).extracting(TwiceRegistered::name).containsExactlyInAnyOrder("A", "B");
    }

    @Test
    public void testDisposeAll_WithDirectRegistrations_HandsThemBackAsIs() {
        // Given: Components from direct registrations
        List<TwiceRegistered> both = MultiComponentLoader.loadAll(TwiceRegistered.class);

        // When: Disposing them
        List<TwiceRegistered> disposed = MultiComponentLoader.disposeAll(TwiceRegistered.class, both);

        // Then: Nothing is skipped: there is no factory to release them, so they come back as they are
        assertThat(disposed).isEqualTo(both);
    }

    @Test
    public void testLoadAll_WhenAFactoryFails_ReleasesWhatWasCreatedBefore() {
        // Given: two factories for HalfLoaded, the sound one registered first, the other creating a null
        HalfLoaded.SoundFactory.DISPOSED.clear();

        // When: Loading the set
        assertThatThrownBy(() -> MultiComponentLoader.loadAll(HalfLoaded.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("created a null");

        // Then: The component created before the failure went back to its factory
        assertThat(HalfLoaded.SoundFactory.DISPOSED).singleElement().isInstanceOf(HalfLoaded.Sound.class);
    }

    @Test
    public void testLoadAll_WithDirectRegistrations_WarnsWhenDependenciesAreHandedOver() {
        // the JDK constructs direct registrations, so dependencies handed over reach nothing: almost always a mistake
        try (LogCapture log = new LogCapture(MultiComponentLoader.class)) {
            MultiComponentLoader.loadAll(TwiceRegistered.class, Dependencies.of(String.class, "goes nowhere"));

            assertThat(log.warnings())
                    .singleElement()
                    .asString()
                    .contains(TwiceRegistered.class.getName())
                    .contains(String.class.getName());
        }
    }
}
