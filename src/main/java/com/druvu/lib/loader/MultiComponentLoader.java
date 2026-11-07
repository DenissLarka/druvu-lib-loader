package com.druvu.lib.loader;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Loads every component of a type: the plugin case. Unlike {@link ComponentLoader}, this loader allows and returns
 * multiple implementations.
 *
 * <p>The components come from the {@link ComponentFactory} instances registered for the type, each handed the same
 * {@link Dependencies}. When no factory is registered, they come from the implementations registered directly under the
 * type, as for a plain {@link ServiceLoader}; those receive no dependencies, the JDK constructs them (the conditions of
 * a direct registration: {@link ComponentLoader}). Factories must be stateless and have a public no-arg constructor.
 *
 * <h2>Loading is strict, disposing is not</h2>
 *
 * <p>{@link #loadAll} fails the whole call when a factory creates a null component or a direct registration cannot be
 * instantiated: a half loaded plugin set at startup is a deployment error worth seeing. {@link #disposeAll} usually
 * runs at shutdown instead, so it disposes every component it can and logs a WARN for each one it cannot, rather than
 * letting a single failing plugin leave the others un-released.
 *
 * @author Deniss Larka on 15 Nov 2025
 */
public final class MultiComponentLoader {

    private static final Logger LOG = System.getLogger(MultiComponentLoader.class.getName());

    private static final ConcurrentHashMap<Class<?>, Object> LOCKS = new ConcurrentHashMap<>();

    /**
     * Remembers who created which component, so {@link #disposeAll} can hand every component back to its own creator,
     * whatever order or subset it is given - pairing by position silently disposes a component with a foreign factory.
     * Keyed by identity: two equal components are still two components. An entry lives until the component is disposed,
     * so a caller loading without ever disposing keeps them alive.
     */
    private static final Map<Object, Creator> CREATORS = Collections.synchronizedMap(new IdentityHashMap<>());

    /**
     * Who made a component: its factory, or the JDK's {@link ServiceLoader} for a direct registration, which has
     * nothing to dispose the component through and hands it back as is.
     */
    private record Creator(ComponentFactory<?> factory) {

        static final Creator SERVICE_LOADER = new Creator(null);

        // unchecked: a factory in this record produced the very instance it is asked to dispose, so it is a
        // ComponentFactory<T> by construction
        @SuppressWarnings("unchecked")
        <T> T dispose(T component) {
            if (factory == null) {
                return component;
            }
            return ((ComponentFactory<T>) factory).disposeComponent(component);
        }

        String name() {
            return factory == null
                    ? ServiceLoader.class.getName()
                    : factory.getClass().getName();
        }
    }

    private MultiComponentLoader() {}

    /**
     * Load all instances of the target class with empty dependencies.
     *
     * @param targetClass the component type to load
     * @param <T> the type parameter
     * @return unmodifiable list of all component instances (empty if nothing is registered)
     * @throws NullPointerException if targetClass is null
     * @throws IllegalStateException if any factory creates a null component
     * @throws ServiceConfigurationError if a direct registration cannot be instantiated
     */
    public static <T> List<T> loadAll(final Class<T> targetClass) {
        return loadAll(targetClass, Dependencies.of());
    }

    /**
     * Load all instances of the target class with the given dependencies.
     *
     * @param targetClass the component type to load
     * @param dependencies dependencies to pass to each factory; a direct registration receives none
     * @param <T> the type parameter
     * @return unmodifiable list of all component instances (empty if nothing is registered)
     * @throws NullPointerException if targetClass or dependencies is null
     * @throws IllegalStateException if any factory creates a null component
     * @throws ServiceConfigurationError if a direct registration cannot be instantiated
     */
    public static <T> List<T> loadAll(final Class<T> targetClass, Dependencies dependencies) {

        Objects.requireNonNull(targetClass, "targetClass cannot be null");
        Objects.requireNonNull(dependencies, "dependencies cannot be null");

        synchronized (lockOf(targetClass)) {
            final List<ComponentFactory<T>> factories = factoriesOf(targetClass);
            if (factories.isEmpty()) {
                return loadDirectly(targetClass);
            }
            final List<T> results = new ArrayList<>(factories.size());
            for (ComponentFactory<T> factory : factories) {
                final T component = factory.createComponent(dependencies);
                if (component == null) {
                    throw new IllegalStateException(String.format("Factory %s created a null component", factory));
                }
                CREATORS.put(component, new Creator(factory));
                results.add(component);
            }
            return Collections.unmodifiableList(results);
        }
    }

    /**
     * Dispose the given components, each through its creator. A component from a direct registration is handed back as
     * is, there is no factory to release it.
     *
     * <p>Components this loader does not know about - never loaded here, or already disposed - and factories failing to
     * dispose are logged as a WARN and skipped. One bad component never stops the others from being released, which is
     * what a shutdown path needs.
     *
     * @param targetClass the component type
     * @param instances components previously obtained from {@link #loadAll}, in any order or subset
     * @param <T> the type parameter
     * @return unmodifiable list of the components actually disposed, shorter than the input when something was skipped
     * @throws NullPointerException if targetClass or instances is null
     */
    public static <T> List<T> disposeAll(Class<T> targetClass, List<T> instances) {
        Objects.requireNonNull(targetClass, "targetClass cannot be null");
        Objects.requireNonNull(instances, "instances cannot be null");

        synchronized (lockOf(targetClass)) {
            final List<T> disposedComponents = new ArrayList<>(instances.size());
            int skipped = 0;

            for (T instance : instances) {
                final Creator creator = takeCreatorOf(instance);
                if (creator == null) {
                    skipped++;
                    LOG.log(
                            Level.WARNING,
                            () -> "Not disposing " + describe(instance) + ": it was not created by loadAll("
                                    + targetClass.getName() + "), or it is already disposed");
                    continue;
                }
                skipped += disposeOne(creator, instance, disposedComponents);
            }

            if (skipped > 0) {
                final int disposedCount = disposedComponents.size();
                final int handedOver = instances.size();
                final int skippedCount = skipped;
                LOG.log(
                        Level.WARNING,
                        () -> "Disposed " + disposedCount + " of the " + handedOver + " "
                                + targetClass.getSimpleName() + " components handed over, " + skippedCount
                                + " skipped, see the warnings above");
            }

            return Collections.unmodifiableList(disposedComponents);
        }
    }

    /** The implementations registered directly under the type, constructed by the JDK. */
    private static <T> List<T> loadDirectly(Class<T> targetClass) {
        final ServiceLoader<T> registrations;
        try {
            registrations = ModuleServices.load(targetClass);
        } catch (ServiceConfigurationError e) {
            // the JDK refuses the lookup itself: on the module path, a type this module cannot see. No direct
            // registration can be loaded for it, which is an empty plugin set, not a failure.
            LOG.log(Level.DEBUG, () -> "No direct registrations for " + targetClass.getName() + ": " + e.getMessage());
            return List.of();
        }
        final List<T> results = new ArrayList<>();
        // a registration that cannot be instantiated throws here, as a deployment error should
        for (T component : registrations) {
            CREATORS.put(component, Creator.SERVICE_LOADER);
            results.add(component);
        }
        return Collections.unmodifiableList(results);
    }

    /** @return 1 when the component was skipped, 0 when it was disposed and collected */
    private static <T> int disposeOne(Creator creator, T instance, List<T> disposedComponents) {
        try {
            final T disposed = creator.dispose(instance);
            if (disposed == null) {
                LOG.log(
                        Level.WARNING,
                        () -> "Factory " + creator.name() + " returned a null disposing " + describe(instance));
                return 1;
            }
            disposedComponents.add(disposed);
            return 0;
        } catch (RuntimeException e) {
            LOG.log(
                    Level.WARNING,
                    "Factory " + creator.name() + " failed to dispose " + describe(instance)
                            + ", going on with the remaining components",
                    e);
            return 1;
        }
    }

    private static Object lockOf(Class<?> targetClass) {
        return LOCKS.computeIfAbsent(targetClass, key -> new Object());
    }

    /** Null instances are tolerated on purpose, the caller gets a WARN rather than an exception. */
    private static Creator takeCreatorOf(Object instance) {
        if (instance == null) {
            return null;
        }
        return CREATORS.remove(instance);
    }

    @SuppressWarnings("unchecked")
    private static <T> List<ComponentFactory<T>> factoriesOf(Class<T> targetClass) {
        final Predicate<ComponentFactory> producesTheType = factory -> targetClass == factory.type();
        final List<ComponentFactory> factories = ServiceLoaderExtended.loadAll(ComponentFactory.class, producesTheType);
        return (List<ComponentFactory<T>>) (List<?>) factories;
    }

    private static String describe(Object instance) {
        if (instance == null) {
            return "null";
        }
        return instance.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(instance));
    }
}
