package com.druvu.lib.loader;

import java.lang.StableValue; // NOPMD UnnecessaryImport - StableValue is a Java 25 preview API, not auto-imported
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Use {@link ComponentLoader} internally with singleton logic around it.
 * Leverages Java 25's {@link StableValue} for lock-free, thread-safe singleton initialization
 * with JVM constant-folding optimizations.
 *
 * In this approach we separate the creation of the singleton from the further referencing it.
 * Therefore, creation method: {@link  SingletonLoader#load} and usage method: {@link  SingletonLoader#get}
 *
 * Be aware, nothing prevents creating multiple instances of the same class through direct use of {@link  ComponentLoader}
 *
 * @author Deniss Larka
 * on 18 Jan 2025
 */
public final class SingletonLoader {

	private static final Map<Class<?>, Supplier<Object>> SINGLETONS_REGISTER = new ConcurrentHashMap<>();

	private SingletonLoader() {
		// Prevent instantiation
	}

	/**
	 * Load a singleton instance with no dependencies.
	 *
	 * @param targetClass the class type to load
	 * @param <T> the type parameter
	 * @return the loaded singleton instance
	 * @throws IllegalStateException if singleton already initialized
	 */
	public static <T> T load(final Class<T> targetClass) {
		return load(targetClass, new Dependencies());
	}

	/**
	 * Load a singleton instance with the provided dependencies.
	 * Uses {@link StableValue} to ensure thread-safe, lock-free initialization with JVM optimization support.
	 *
	 * @param targetClass the class type to load
	 * @param dependencies the dependencies to inject
	 * @param <T> the type parameter
	 * @return the loaded singleton instance
	 * @throws IllegalStateException if singleton already initialized
	 */
	public static <T> T load(final Class<T> targetClass, Dependencies dependencies) {
		// Create a StableValue supplier that will lazily load the component
		Supplier<Object> stableSupplier = StableValue.supplier(
			() -> ComponentLoader.load(targetClass, dependencies)
		);

		// Atomically register the supplier; putIfAbsent returns null if this is the first registration
		Supplier<Object> existing = SINGLETONS_REGISTER.putIfAbsent(targetClass, stableSupplier);

		if (existing != null) {
			throw new IllegalStateException("Already created: " + targetClass);
		}

		// Trigger initialization and return the instance
		// The StableValue ensures thread-safe creation even if get() is called concurrently
		return targetClass.cast(stableSupplier.get());
	}

	/**
	 * Retrieve a previously loaded singleton instance.
	 *
	 * @param targetClass the class type to retrieve
	 * @param <T> the type parameter
	 * @return the singleton instance
	 * @throws IllegalStateException if singleton isn't yet loaded via {@link #load}
	 */
	public static <T> T get(final Class<T> targetClass) {
		Supplier<Object> supplier = SINGLETONS_REGISTER.get(targetClass);
		if (supplier == null) {
			throw new IllegalStateException("Singleton must be loaded before use: " + targetClass);
		}
		// The StableValue supplier guarantees thread-safe access and enables JVM optimizations
		return targetClass.cast(supplier.get());
	}
}
