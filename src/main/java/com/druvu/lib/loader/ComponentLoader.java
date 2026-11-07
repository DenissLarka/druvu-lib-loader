package com.druvu.lib.loader;

import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Loads a single component instance, registered in one of two ways.
 *
 * <h2>A factory</h2>
 *
 * <p>A {@link ComponentFactory} registered under {@code META-INF/services/com.druvu.lib.loader.ComponentFactory} (or
 * {@code provides com.druvu.lib.loader.ComponentFactory with ...} in {@code module-info.java}) whose
 * {@link ComponentFactory#type()} is the requested type. Its {@link ComponentFactory#createComponent(Dependencies)} is
 * called with the supplied dependencies. Two matching factories are an error.
 *
 * <h2>A direct registration, when no factory matches</h2>
 *
 * <p>An implementation registered under the requested type itself, as for a plain {@link java.util.ServiceLoader}: a
 * public class with a public no-arg constructor or, in an explicit module only (one with a {@code module-info}), a
 * class with a public static {@code provider()} method. Nothing on that side depends on this library, and nothing
 * receives the dependencies: the JDK constructs the object. Two registrations are an error here too.
 *
 * <p>On the module path this module declares its use of the requested type at run time, so the registering module needs
 * its {@code provides} line and nothing of ours. Two conditions the JDK sets: the type is public in a package its
 * module exports, to everyone or to {@code com.druvu.lib.loader}; and the registering module is in the module graph,
 * which it is when a module of the graph {@code requires} it or declares {@code uses} for the type (the API module that
 * owns the type is the place), or when {@code --add-modules} names it. A lookup the JDK refuses surfaces as the
 * suppressed cause of the {@link TargetClassNotFoundException}.
 *
 * <h2>Thread safety</h2>
 *
 * <p>Load and dispose operations synchronize on the target class, making concurrent calls safe.
 *
 * @author Deniss Larka
 * @see ComponentFactory
 * @see MultiComponentLoader
 */
public final class ComponentLoader {

    private static final ConcurrentHashMap<Class<?>, Object> LOCKS = new ConcurrentHashMap<>();

    private ComponentLoader() {}

    /**
     * Loads the one component of the given type, with no dependencies.
     *
     * @param targetClass the component type
     * @param <T> the component type
     * @return the component
     * @throws TargetClassNotFoundException when neither a factory nor a direct registration exists for the type
     * @throws IllegalStateException when two factories or two direct registrations exist, or the factory created a null
     */
    public static <T> T load(final Class<T> targetClass) {
        return load(targetClass, Dependencies.of());
    }

    /**
     * Loads the one component of the given type.
     *
     * @param targetClass the component type
     * @param dependencies what the factory receives; a direct registration receives nothing
     * @param <T> the component type
     * @return the component
     * @throws TargetClassNotFoundException when neither a factory nor a direct registration exists for the type
     * @throws IllegalStateException when two factories or two direct registrations exist, or the factory created a null
     */
    public static <T> T load(final Class<T> targetClass, Dependencies dependencies) {

        Objects.requireNonNull(targetClass);
        Objects.requireNonNull(dependencies);

        synchronized (LOCKS.computeIfAbsent(targetClass, k -> new Object())) {
            final ComponentFactory<T> componentFactory;
            try {
                componentFactory = findFactory(targetClass);
            } catch (TargetClassNotFoundException noFactory) {
                // Only the factory lookup is guarded: a TargetClassNotFoundException raised by the component's own
                // construction is the component's failure, and must not be mistaken for a missing factory.
                return loadDirectly(targetClass, noFactory);
            }
            final T result = componentFactory.createComponent(dependencies);
            if (result == null) {
                throw new IllegalStateException(String.format("Factory %s created a null", componentFactory));
            }
            return result;
        }
    }

    /**
     * Hands a component back to the factory producing its type. A component without a factory, a direct registration,
     * comes back as is: there is nothing to dispose it through.
     *
     * @param targetClass the component type
     * @param instance the component
     * @param <T> the component type
     * @return what the factory returned, or the instance itself
     */
    public static <T> T dispose(Class<T> targetClass, T instance) {
        try {
            return findFactory(targetClass).disposeComponent(instance);
        } catch (TargetClassNotFoundException e) {
            return instance;
        }
    }

    private static <T> T loadDirectly(Class<T> targetClass, TargetClassNotFoundException noFactory) {
        try {
            return ServiceLoaderExtended.load(targetClass);
        } catch (TargetClassNotFoundException noRegistration) {
            throw notFound(targetClass, noFactory);
        } catch (ServiceConfigurationError e) {
            // the JDK refused the lookup (a registration that cannot be instantiated, or on the module path a type this
            // module cannot see): keep its reason, it is the one the caller needs
            final TargetClassNotFoundException notFound = notFound(targetClass, noFactory);
            notFound.addSuppressed(e);
            throw notFound;
        }
    }

    private static TargetClassNotFoundException notFound(Class<?> targetClass, TargetClassNotFoundException noFactory) {
        var notFound = new TargetClassNotFoundException(
                "No ComponentFactory or ServiceLoader provider for %s".formatted(targetClass.getName()));
        notFound.addSuppressed(noFactory);
        return notFound;
    }

    private static <T> ComponentFactory<T> findFactory(Class<T> targetClass) {
        final Predicate<ComponentFactory> producesTheType = factory -> targetClass == factory.type();
        try {
            return ServiceLoaderExtended.load(ComponentFactory.class, producesTheType);
        } catch (TargetClassNotFoundException e) {
            var notFound = new TargetClassNotFoundException(
                    "ComponentFactory producing %s not found".formatted(targetClass.getName()));
            notFound.addSuppressed(e);
            throw notFound;
        }
    }
}
