package com.druvu.lib.loader;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Use {@link ComponentLoader} internally with singleton logic around it.
 *
 * <p>In this approach we separate the creation of the singleton from the further referencing it. Therefore, creation
 * method: {@link SingletonLoader#load} and usage method: {@link SingletonLoader#get}
 *
 * <p>Be aware, nothing prevents creating multiple instances of the same class through direct use of
 * {@link ComponentLoader}
 *
 * @author Deniss Larka on 18 Jan 2025
 */
public final class SingletonLoader {

    private static final Map<Class<?>, Object> SINGLETONS_REGISTER = new ConcurrentHashMap<>();

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
     *
     * @param targetClass the class type to load
     * @param dependencies the dependencies to inject
     * @param <T> the type parameter
     * @return the loaded singleton instance
     * @throws IllegalStateException if singleton already initialized
     */
    public static <T> T load(final Class<T> targetClass, Dependencies dependencies) {
        T instance = ComponentLoader.load(targetClass, dependencies);

        Object existing = SINGLETONS_REGISTER.putIfAbsent(targetClass, instance);
        if (existing != null) {
            throw new IllegalStateException("Already created: " + targetClass);
        }

        return instance;
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
        Object instance = SINGLETONS_REGISTER.get(targetClass);
        if (instance == null) {
            throw new IllegalStateException("Singleton must be loaded before use: " + targetClass);
        }
        return targetClass.cast(instance);
    }
}
