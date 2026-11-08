package com.druvu.lib.loader;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Immutable, type-keyed container for the dependencies handed to a factory: the key is the exact type given, a
 * supertype or an implemented interface finds nothing. Built through the {@code of} overloads, which refuse a wrong
 * pair where it is written.
 *
 * @author Deniss Larka on 17 Aug 2025
 */
public final class Dependencies {

    private static final Dependencies NONE = new Dependencies(Map.of());

    private final Map<Class<?>, Object> map;

    private Dependencies(Map<Class<?>, Object> map) {
        this.map = map;
    }

    /**
     * No dependencies.
     *
     * @return the empty dependencies
     */
    public static Dependencies of() {
        return NONE;
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
        return fromPairs(k1, v1);
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
        return fromPairs(k1, v1, k2, v2);
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
        return fromPairs(k1, v1, k2, v2, k3, v3);
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
        return fromPairs(k1, v1, k2, v2, k3, v3, k4, v4);
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
        return fromPairs(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
    }

    /**
     * Any number of dependencies as type-value pairs, {@code Path.class, path, Config.class, config, ...}, unchecked by
     * the compiler: an even number of elements, each key a {@code Class}, each value an instance of its key, no type
     * twice. The typed overloads are the usual way in.
     *
     * @param pairs the pairs, an even number of elements
     * @return the dependencies
     * @throws IllegalArgumentException on an odd length, a key that is not a {@code Class}, a null value, or a value
     *     that is not an instance of its key
     * @throws IllegalStateException on a type given twice
     */
    public static Dependencies of(Object... pairs) {
        return fromPairs(pairs);
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

    /**
     * Whether nothing was handed over; the loaders ask before warning that a direct registration received none of it.
     */
    boolean isEmpty() {
        return map.isEmpty();
    }

    private static Dependencies fromPairs(Object... pairs) {
        if ((pairs.length & 1) != 0) { // implicit null check
            throw new IllegalArgumentException("length is odd");
        }
        final Map<Class<?>, Object> map = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            register(map, pairs[i], pairs[i + 1]);
        }
        return new Dependencies(Map.copyOf(map));
    }

    private static void register(Map<Class<?>, Object> map, Object key, Object value) {
        if (!(key instanceof Class<?> type)) {
            throw new IllegalArgumentException(String.format("%s is not of type Class<?>", key));
        }
        if (value == null) {
            // nothing could ever be asked for it: the mistake is at the call site, so it fails there
            throw new IllegalArgumentException("Null value for " + type.getName());
        }
        if (!type.isInstance(value)) {
            throw new IllegalArgumentException(
                    "Value " + value.getClass().getName() + " is not of type " + type.getName());
        }
        if (map.containsKey(type)) {
            throw new IllegalStateException("Already registered: " + type);
        }
        map.put(type, value);
    }

    @SuppressWarnings("unchecked") // the value was checked against its key at registration
    private <T> T dependencyNullable(Class<T> dependencyType) {
        return (T) map.get(Objects.requireNonNull(dependencyType));
    }

    /** The types handed over, never the values: a value may be a credential, and this ends up in logs. */
    @Override
    public String toString() {
        return map.keySet().stream()
                .map(Class::getName)
                .sorted()
                .collect(Collectors.joining(", ", "Dependencies[", "]"));
    }
}
