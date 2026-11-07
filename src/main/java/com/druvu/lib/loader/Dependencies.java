package com.druvu.lib.loader;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable, type-keyed container for the dependencies handed to a factory. This class is final to prevent subclass
 * finalizer attacks when constructor validation fails.
 *
 * @author Deniss Larka on 17 Aug 2025
 */
public final class Dependencies {

    private final Map<Class<?>, Object> map;

    private Dependencies() {
        this.map = Collections.emptyMap();
    }

    /**
     * Builds the dependencies from type-value pairs: {@code Path.class, path, Config.class, config, ...}. A null value
     * registers nothing; the typed {@code of} overloads are the usual way in, this constructor is the unchecked one.
     *
     * @param input the pairs, an even number of elements
     * @throws IllegalArgumentException on an odd length, a key that is not a {@code Class}, or a value that is not an
     *     instance of its key
     * @throws IllegalStateException on a type given twice
     */
    public Dependencies(Object... input) {
        if ((input.length & 1) != 0) { // implicit null check
            throw new IllegalArgumentException("length is odd");
        }
        Map<Class<?>, Object> buildingMap = new HashMap<>();
        for (int i = 0; i < input.length; i += 2) {
            final Object k = input[i];
            if (!Class.class.isInstance(k)) {
                throw new IllegalArgumentException(String.format("%s is not of type Class<?>", k));
            }
            final Object v = input[i + 1];
            // we technically allow nulls for a value but skip registering them
            if (v != null) {
                register(buildingMap, Map.entry((Class<?>) k, v));
            }
        }
        this.map = Map.copyOf(buildingMap);
    }

    /**
     * No dependencies.
     *
     * @return the empty dependencies
     */
    public static Dependencies of() {
        return new Dependencies();
    }

    /**
     * One dependency, each keyed by a type; the compiler checks every value against its key.
     *
     * @param k1 the first type, the key the factory asks for
     * @param v1 its value, an instance of that type
     * @param <T1> the first type
     * @return the dependencies
     */
    public static <T1> Dependencies of(Class<T1> k1, T1 v1) {
        return new Dependencies(k1, v1);
    }

    /**
     * Two dependencies, each keyed by a type; the compiler checks every value against its key.
     *
     * @param k1 the first type, the key the factory asks for
     * @param v1 its value, an instance of that type
     * @param k2 the second type, the key the factory asks for
     * @param v2 its value, an instance of that type
     * @param <T1> the first type
     * @param <T2> the second type
     * @return the dependencies
     */
    public static <T1, T2> Dependencies of(Class<T1> k1, T1 v1, Class<T2> k2, T2 v2) {
        return new Dependencies(k1, v1, k2, v2);
    }

    /**
     * Three dependencies, each keyed by a type; the compiler checks every value against its key.
     *
     * @param k1 the first type, the key the factory asks for
     * @param v1 its value, an instance of that type
     * @param k2 the second type, the key the factory asks for
     * @param v2 its value, an instance of that type
     * @param k3 the third type, the key the factory asks for
     * @param v3 its value, an instance of that type
     * @param <T1> the first type
     * @param <T2> the second type
     * @param <T3> the third type
     * @return the dependencies
     */
    public static <T1, T2, T3> Dependencies of(Class<T1> k1, T1 v1, Class<T2> k2, T2 v2, Class<T3> k3, T3 v3) {
        return new Dependencies(k1, v1, k2, v2, k3, v3);
    }

    /**
     * Four dependencies, each keyed by a type; the compiler checks every value against its key.
     *
     * @param k1 the first type, the key the factory asks for
     * @param v1 its value, an instance of that type
     * @param k2 the second type, the key the factory asks for
     * @param v2 its value, an instance of that type
     * @param k3 the third type, the key the factory asks for
     * @param v3 its value, an instance of that type
     * @param k4 the fourth type, the key the factory asks for
     * @param v4 its value, an instance of that type
     * @param <T1> the first type
     * @param <T2> the second type
     * @param <T3> the third type
     * @param <T4> the fourth type
     * @return the dependencies
     */
    public static <T1, T2, T3, T4> Dependencies of(
            Class<T1> k1, T1 v1, Class<T2> k2, T2 v2, Class<T3> k3, T3 v3, Class<T4> k4, T4 v4) {
        return new Dependencies(k1, v1, k2, v2, k3, v3, k4, v4);
    }

    /**
     * Five dependencies, each keyed by a type; the compiler checks every value against its key.
     *
     * @param k1 the first type, the key the factory asks for
     * @param v1 its value, an instance of that type
     * @param k2 the second type, the key the factory asks for
     * @param v2 its value, an instance of that type
     * @param k3 the third type, the key the factory asks for
     * @param v3 its value, an instance of that type
     * @param k4 the fourth type, the key the factory asks for
     * @param v4 its value, an instance of that type
     * @param k5 the fifth type, the key the factory asks for
     * @param v5 its value, an instance of that type
     * @param <T1> the first type
     * @param <T2> the second type
     * @param <T3> the third type
     * @param <T4> the fourth type
     * @param <T5> the fifth type
     * @return the dependencies
     */
    public static <T1, T2, T3, T4, T5> Dependencies of(
            Class<T1> k1, T1 v1, Class<T2> k2, T2 v2, Class<T3> k3, T3 v3, Class<T4> k4, T4 v4, Class<T5> k5, T5 v5) {
        return new Dependencies(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
    }

    /**
     * Any number of dependencies as type-value pairs, unchecked by the compiler: the rules of
     * {@link #Dependencies(Object...)}.
     *
     * @param objects the pairs, an even number of elements
     * @return the dependencies
     */
    public static Dependencies of(Object... objects) {
        return new Dependencies(objects);
    }

    /**
     * The dependency registered under the type; a factory asks this for what it cannot do without.
     *
     * @param dependencyClass the type the dependency was registered under
     * @param <T> the dependency type
     * @return the dependency, never {@code null}
     * @throws IllegalStateException when nothing is registered under the type
     */
    public <T> T getDependency(Class<T> dependencyClass) {
        final T dependency = dependencyNullable(dependencyClass);
        if (dependency == null) {
            throw new IllegalStateException("Dependency not found: " + dependencyClass);
        }
        return dependency;
    }

    /**
     * The dependency registered under the type, if any; a factory asks this for what it has a default for.
     *
     * @param dependencyClass the type the dependency was registered under
     * @param <T> the dependency type
     * @return the dependency, or empty when nothing is registered under the type
     */
    public <T> Optional<T> getOptionalDependency(Class<T> dependencyClass) {
        return Optional.ofNullable(dependencyNullable(Objects.requireNonNull(dependencyClass)));
    }

    private void register(Map<Class<?>, Object> buildingMap, Map.Entry<Class<?>, Object> entry) {
        final Object value = entry.getValue();
        if (!entry.getKey().isInstance(value)) {
            throw new IllegalArgumentException("Value is not of type " + entry.getKey());
        }
        register(buildingMap, entry.getKey(), value);
    }

    private void register(Map<Class<?>, Object> buildingMap, Class<?> dependencyType, Object dependency) {
        if (buildingMap.containsKey(dependencyType)) {
            throw new IllegalStateException("Already registered: " + dependencyType);
        }
        buildingMap.put(dependencyType, dependency);
    }

    private <T> T dependencyNullable(Class<T> dependencyType) {
        return (T) map.get(Objects.requireNonNull(dependencyType));
    }

    @Override
    public String toString() {
        return String.valueOf(map);
    }
}
