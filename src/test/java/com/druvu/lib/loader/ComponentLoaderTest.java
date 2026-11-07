package com.druvu.lib.loader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.testng.annotations.Test;

/** ComponentLoader on the class path: a factory, a direct registration, and what each refuses. */
public class ComponentLoaderTest {

    @Test
    public void testLoad_WithOneFactory_CreatesThroughIt() {
        LonePlugin plugin = ComponentLoader.load(LonePlugin.class);

        assertThat(plugin).isNotNull();
    }

    @Test
    public void testLoad_WithSeveralFactories_Refuses() {
        // TestPlugin has three factories registered: one instance is not an answer
        assertThatThrownBy(() -> ComponentLoader.load(TestPlugin.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("More than one");
    }

    @Test
    public void testLoad_WithADirectRegistration_CreatesThroughTheJdk() {
        // the dependencies go nowhere on this way: the JDK constructs the object
        DirectPlugin plugin = ComponentLoader.load(DirectPlugin.class, Dependencies.of(String.class, "unused"));

        assertThat(plugin).isInstanceOf(DirectPluginImpl.class);
    }

    @Test
    public void testLoad_WithAnApiOwnedFactory_CreatesWithTheArgument() {
        // the README's recipe: the factory registered under Ledger$Factory, no library type on its side; the JDK
        // constructs the factory, the caller's path reaches the ledger
        Ledger ledger = ComponentLoader.load(Ledger.Factory.class).create(Path.of("ledger.csv"));

        assertThat(ledger).isInstanceOf(FileLedger.class);
        assertThat(ledger.storage()).isEqualTo("ledger.csv");
    }

    @Test
    public void testLoad_WithTwoDirectRegistrations_Refuses() {
        assertThatThrownBy(() -> ComponentLoader.load(TwiceRegistered.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("More than one");
    }

    @Test
    public void testLoad_WhenNothingIsRegistered_ThrowsTargetClassNotFound() {
        assertThatThrownBy(() -> ComponentLoader.load(Integer.class))
                .isInstanceOf(TargetClassNotFoundException.class)
                .hasMessageContaining(Integer.class.getName())
                // the factory lookup's own reason travels along
                .satisfies(e -> assertThat(e.getSuppressed()).hasSize(1));
    }

    @Test
    public void testLoad_WhenTheFactoryCreatesNull_ThrowsIllegalState() {
        assertThatThrownBy(() -> ComponentLoader.load(NullComponent.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("created a null");
    }

    @Test
    public void testLoad_WithNullTargetClass_ThrowsNullPointerException() {
        assertThatThrownBy(() -> ComponentLoader.load(null, Dependencies.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testLoad_WithNullDependencies_ThrowsNullPointerException() {
        assertThatThrownBy(() -> ComponentLoader.load(String.class, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void testDispose_WithAFactory_HandsTheComponentToIt() {
        LonePlugin plugin = ComponentLoader.load(LonePlugin.class);

        assertThat(ComponentLoader.dispose(LonePlugin.class, plugin)).isSameAs(plugin);
    }

    @Test
    public void testDispose_WithADirectRegistration_HandsTheComponentBackAsIs() {
        DirectPlugin plugin = ComponentLoader.load(DirectPlugin.class);

        assertThat(ComponentLoader.dispose(DirectPlugin.class, plugin)).isSameAs(plugin);
    }
}
