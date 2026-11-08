package com.druvu.lib.loader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import org.testng.annotations.Test;

/** Dependencies: a type-keyed container, exact types only, a wrong pair refused where it is written. */
public class DependenciesTest {

    @Test
    public void testOf_WithTypedPairs_FindsEachByItsType() {
        Dependencies dependencies = Dependencies.of(String.class, "test", Integer.class, 1, Double.class, 2.0);

        assertThat(dependencies.getDependency(String.class)).isEqualTo("test");
        assertThat(dependencies.getDependency(Integer.class)).isEqualTo(1);
        assertThat(dependencies.getDependency(Double.class)).isEqualTo(2.0);
    }

    @Test
    public void testGetDependency_WhenMissing_ThrowsIllegalState() {
        Dependencies dependencies = Dependencies.of(String.class, "test");

        assertThatThrownBy(() -> dependencies.getDependency(Long.class))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Dependency not found");
    }

    @Test
    public void testGetOptionalDependency_PresentOrEmpty() {
        Dependencies dependencies = Dependencies.of(String.class, "test");

        assertThat(dependencies.getOptionalDependency(String.class)).contains("test");
        assertThat(dependencies.getOptionalDependency(Integer.class)).isEmpty();
    }

    @Test
    public void testGetDependency_ByASupertype_FindsNothing() {
        // the key is the exact type given: neither the parent class nor an implemented interface answers
        Dependencies dependencies = Dependencies.of(Child.class, new Child(), String.class, "test");

        assertThat(dependencies.getOptionalDependency(Parent.class)).isEmpty();
        assertThat(dependencies.getOptionalDependency(Marker.class)).isEmpty();
        assertThat(dependencies.getOptionalDependency(CharSequence.class)).isEmpty();
    }

    @Test
    public void testOf_WithATypeGivenTwice_Refuses() {
        assertThatThrownBy(() -> Dependencies.of(String.class, "one", String.class, "two"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Already registered");
    }

    @Test
    public void testOf_WithANullValue_Refuses() {
        // a null could never be asked for later; the mistake is at this call site, so it fails here
        assertThatThrownBy(() -> Dependencies.of(Path.class, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(Path.class.getName());
    }

    @Test
    public void testOf_Unchecked_WithAValueOfAnotherType_Refuses() {
        assertThatThrownBy(() -> Dependencies.of((Object) String.class, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not of type");
    }

    @Test
    public void testOf_Unchecked_WithAnOddCount_Refuses() {
        assertThatThrownBy(() -> Dependencies.of(String.class, "test", Integer.class))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("odd");
    }

    @Test
    public void testOf_Unchecked_WithAKeyThatIsNoClass_Refuses() {
        assertThatThrownBy(() -> Dependencies.of("not a class", "test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not of type Class");
    }

    @Test
    public void testToString_NamesTheTypesNotTheValues() {
        // a value may be a credential or a connection: what gets logged is which types were handed over
        Dependencies dependencies = Dependencies.of(String.class, "s3cret");

        assertThat(dependencies.toString()).contains(String.class.getName()).doesNotContain("s3cret");
    }

    static class Parent {}

    static class Child extends Parent implements Marker {}

    interface Marker {}
}
