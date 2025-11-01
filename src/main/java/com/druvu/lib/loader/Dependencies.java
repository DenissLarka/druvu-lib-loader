package com.druvu.lib.loader;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable container for dependency injection.
 * This class is final to prevent subclass finalizer attacks when constructor validation fails.
 *
 * @author Deniss Larka
 * on 17 Aug 2025
 */
public final class Dependencies {

	private final Map<Class<?>, Object> map;

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
			//we technically allow nulls for a value but skip registering them
			if (v != null) {
				register(buildingMap, Map.entry((Class<?>) k, v));
			}
		}
		this.map = Map.copyOf(buildingMap);
	}

	public static <T1> Dependencies of(Class<T1> k1, T1 v1) {
		return new Dependencies(k1, v1);
	}

	public static <T1, T2> Dependencies of(
			Class<T1> k1, T1 v1,
			Class<T2> k2, T2 v2) {
		return new Dependencies(k1, v1, k2, v2);
	}

	public static <T1, T2, T3> Dependencies of(
			Class<T1> k1, T1 v1,
			Class<T2> k2, T2 v2,
			Class<T3> k3, T3 v3) {
		return new Dependencies(k1, v1, k2, v2, k3, v3);
	}

	public static <T1, T2, T3, T4> Dependencies of(
			Class<T1> k1, T1 v1,
			Class<T2> k2, T2 v2,
			Class<T3> k3, T3 v3,
			Class<T4> k4, T3 v4) {
		return new Dependencies(k1, v1, k2, v2, k3, v3, k4, v4);
	}

	public static <T1, T2, T3, T4, T5> Dependencies of(
			Class<T1> k1, T1 v1,
			Class<T2> k2, T2 v2,
			Class<T3> k3, T3 v3,
			Class<T4> k4, T3 v4,
			Class<T5> k5, T3 v5) {
		return new Dependencies(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
	}

	public static Dependencies of(Object... objects) {
		return new Dependencies(objects);
	}

	public <T> T getDependency(Class<T> dependencyClass) {
		final T dependency = dependencyNullable(dependencyClass);
		if (dependency == null) {
			throw new IllegalStateException("Dependency not found: " + dependencyClass);
		}
		return dependency;
	}

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
