package com.druvu.lib.loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Loads multiple component instances by discovering all ComponentFactory implementations
 * via ServiceLoader that match the target class.
 * Unlike {@link ComponentLoader}, this loader allows and returns multiple implementations.
 * <p>
 * Factory is a {@link ComponentFactory} capable of creating instances of the desired target class.
 * Factories must be stateless and have a default constructor.
 *
 * @author Deniss Larka
 * on 15 Nov 2025
 */
public final class MultiComponentLoader {

	private static final ConcurrentHashMap<Class<?>, Object> LOCKS = new ConcurrentHashMap<>();

	private MultiComponentLoader() {
	}

	/**
	 * Load all instances of the target class with empty dependencies.
	 *
	 * @param targetClass the component type to load
	 * @param <T>         the type parameter
	 * @return unmodifiable list of all component instances (empty if no factories found)
	 * @throws NullPointerException if targetClass is null
	 * @throws IllegalStateException if any factory creates a null component
	 */
	public static <T> List<T> loadAll(final Class<T> targetClass) {
		return loadAll(targetClass, Dependencies.of());
	}

	/**
	 * Load all instances of the target class with the given dependencies.
	 *
	 * @param targetClass  the component type to load
	 * @param dependencies dependencies to pass to each factory
	 * @param <T>          the type parameter
	 * @return unmodifiable list of all component instances (empty if no factories found)
	 * @throws NullPointerException if targetClass or dependencies is null
	 * @throws IllegalStateException if any factory creates a null component
	 */
	public static <T> List<T> loadAll(final Class<T> targetClass, Dependencies dependencies) {

		Objects.requireNonNull(targetClass, "targetClass cannot be null");
		Objects.requireNonNull(dependencies, "dependencies cannot be null");

		synchronized (LOCKS.computeIfAbsent(targetClass, _ -> new Object())) {
			final List<ComponentFactory<T>> componentFactories = createComponentFactories(targetClass);
			final List<T> results = new ArrayList<>(componentFactories.size());

			for (ComponentFactory<T> factory : componentFactories) {
				final T component = factory.createComponent(dependencies);
				if (component == null) {
					throw new IllegalStateException(String.format("Factory %s created a null component", factory));
				}
				results.add(component);
			}

			return Collections.unmodifiableList(results);
		}
	}

	/**
	 * Dispose all component instances by calling their respective factories' dispose methods.
	 *
	 * @param targetClass the component type
	 * @param instances   list of instances to dispose
	 * @param <T>         the type parameter
	 * @return unmodifiable list of disposed components
	 * @throws NullPointerException if targetClass or instances is null
	 */
	public static <T> List<T> disposeAll(Class<T> targetClass, List<T> instances) {
		Objects.requireNonNull(targetClass, "targetClass cannot be null");
		Objects.requireNonNull(instances, "instances cannot be null");

		synchronized (LOCKS.computeIfAbsent(targetClass, _ -> new Object())) {
			final List<ComponentFactory<T>> componentFactories = createComponentFactories(targetClass);
			final List<T> disposedComponents = new ArrayList<>(instances.size());

			for (int i = 0; i < instances.size(); i++) {
				T instance = instances.get(i);
				// Match instance to factory by index if sizes match, otherwise use first factory
				ComponentFactory<T> factory = i < componentFactories.size()
						? componentFactories.get(i)
						: componentFactories.getFirst();
				T disposed = factory.disposeComponent(instance);
				disposedComponents.add(disposed);
			}

			return Collections.unmodifiableList(disposedComponents);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> List<ComponentFactory<T>> createComponentFactories(Class<T> targetClass) {
		final Predicate<ComponentFactory> candidateChooser = factory -> targetClass == factory.getComponentType();
		List<ComponentFactory> rawFactories = ServiceLoaderExtended.loadAll(ComponentFactory.class, candidateChooser);
		// Safe cast because we filter factories by getComponentType() matching targetClass
		return (List<ComponentFactory<T>>) (List<?>) rawFactories;
	}

}
