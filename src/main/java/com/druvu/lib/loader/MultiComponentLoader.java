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
 * Loads multiple component instances by discovering all ComponentFactory implementations via ServiceLoader that match
 * the target class. Unlike {@link ComponentLoader}, this loader allows and returns multiple implementations.
 *
 * <p>Factory is a {@link ComponentFactory} capable of creating instances of the desired target class. Factories must be
 * stateless and have a default constructor.
 *
 * <h2>Loading is strict, disposing is not</h2>
 *
 * <p>{@link #loadAll} fails the whole call when a factory creates a null component: a half loaded plugin set at startup
 * is a deployment error worth seeing. {@link #disposeAll} usually runs at shutdown instead, so it disposes every
 * component it can and logs a WARN for each one it cannot, rather than letting a single failing plugin leave the others
 * un-released.
 *
 * @author Deniss Larka on 15 Nov 2025
 */
public final class MultiComponentLoader {

    private static final Logger LOG = System.getLogger(MultiComponentLoader.class.getName());

    private static final ConcurrentHashMap<Class<?>, Object> LOCKS = new ConcurrentHashMap<>();

    /**
     * Remembers which factory created which component, so {@link #disposeAll} can hand every component back to its own
     * creator, whatever order or subset it is given - pairing by position silently disposes a component with a foreign
     * factory. Keyed by identity: two equal components are still two components. An entry lives until the component is
     * disposed, so a caller loading without ever disposing keeps them alive.
     */
    private static final Map<Object, ComponentFactory<?>> CREATORS =
            Collections.synchronizedMap(new IdentityHashMap<>());

    private MultiComponentLoader() {}

    /**
     * Load all instances of the target class with empty dependencies.
     *
     * @param targetClass the component type to load
     * @param <T> the type parameter
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
     * @param targetClass the component type to load
     * @param dependencies dependencies to pass to each factory
     * @param <T> the type parameter
     * @return unmodifiable list of all component instances (empty if no factories found)
     * @throws NullPointerException if targetClass or dependencies is null
     * @throws IllegalStateException if any factory creates a null component
     */
    public static <T> List<T> loadAll(final Class<T> targetClass, Dependencies dependencies) {

        Objects.requireNonNull(targetClass, "targetClass cannot be null");
        Objects.requireNonNull(dependencies, "dependencies cannot be null");

        synchronized (lockOf(targetClass)) {
            final List<ComponentFactory<T>> componentFactories = createComponentFactories(targetClass);
            final List<T> results = new ArrayList<>(componentFactories.size());

            for (ComponentFactory<T> factory : componentFactories) {
                final T component = factory.createComponent(dependencies);
                if (component == null) {
                    throw new IllegalStateException(String.format("Factory %s created a null component", factory));
                }
                CREATORS.put(component, factory);
                results.add(component);
            }

            return Collections.unmodifiableList(results);
        }
    }

    /**
     * Dispose the given components, each through the factory that created it.
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
                final ComponentFactory<T> creator = takeCreatorOf(instance);
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

    /** @return 1 when the component was skipped, 0 when it was disposed and collected */
    private static <T> int disposeOne(ComponentFactory<T> creator, T instance, List<T> disposedComponents) {
        try {
            final T disposed = creator.disposeComponent(instance);
            if (disposed == null) {
                LOG.log(
                        Level.WARNING,
                        () -> "Factory " + creator.getClass().getName() + " returned a null disposing "
                                + describe(instance));
                return 1;
            }
            disposedComponents.add(disposed);
            return 0;
        } catch (RuntimeException e) {
            LOG.log(
                    Level.WARNING,
                    "Factory " + creator.getClass().getName() + " failed to dispose " + describe(instance)
                            + ", going on with the remaining components",
                    e);
            return 1;
        }
    }

    private static Object lockOf(Class<?> targetClass) {
        return LOCKS.computeIfAbsent(targetClass, key -> new Object());
    }

    // unchecked: CREATORS holds the very factory that produced this instance, so it is a ComponentFactory<T> by
    // construction. Null instances are tolerated on purpose, the caller gets a WARN rather than an exception.
    @SuppressWarnings("unchecked")
    private static <T> ComponentFactory<T> takeCreatorOf(T instance) {
        if (instance == null) {
            return null;
        }
        return (ComponentFactory<T>) CREATORS.remove(instance);
    }

    @SuppressWarnings("unchecked")
    private static <T> List<ComponentFactory<T>> createComponentFactories(Class<T> targetClass) {
        final Predicate<ComponentFactory> candidateChooser = factory -> targetClass == factory.type();
        List<ComponentFactory> rawFactories = ServiceLoaderExtended.loadAll(ComponentFactory.class, candidateChooser);
        if (!rawFactories.isEmpty()) {
            return (List<ComponentFactory<T>>) (List<?>) rawFactories;
        }
        return findAllInProviderRegistry(targetClass);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> List<ComponentFactory<T>> findAllInProviderRegistry(Class<T> targetClass) {
        try {
            List<ComponentFactory<T>> found = new ArrayList<>();
            for (ServiceLoader.Provider p : ServiceLoader.load(ServiceLoader.Provider.class)) {
                if (p instanceof ComponentFactory<?> cf && targetClass == cf.type()) {
                    found.add((ComponentFactory<T>) cf);
                }
            }
            return Collections.unmodifiableList(found);
        } catch (ServiceConfigurationError e) {
            return Collections.emptyList();
        }
    }

    private static String describe(Object instance) {
        if (instance == null) {
            return "null";
        }
        return instance.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(instance));
    }
}
