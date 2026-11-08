package com.druvu.lib.loader;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

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
 * <p>An implementation registered under the requested type itself, as for a plain {@link ServiceLoader}: a public class
 * with a public no-arg constructor or, in an explicit module only (one with a {@code module-info}), a class with a
 * public static {@code provider()} method. Nothing on that side depends on this library, and nothing receives the
 * dependencies: the JDK constructs the object, and dependencies handed over anyway are logged as a WARNING, since they
 * are almost always a mistake. Two registrations are an error here too.
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
 * <p>This loader keeps no state, so concurrent calls are safe; no lock is held while a factory runs.
 *
 * @author Deniss Larka
 * @see ComponentFactory
 * @see MultiComponentLoader
 */
public final class ComponentLoader {

    private static final Logger LOG = System.getLogger(ComponentLoader.class.getName());

    private ComponentLoader() {}

    /**
     * Loads the one component of the given type, with no dependencies.
     *
     * @param targetClass the component type
     * @param <T> the component type
     * @return the component
     * @throws TargetClassNotFoundException when no factory produces the type and no registration under it could be
     *     loaded
     * @throws IllegalStateException when two factories or two direct registrations exist, or the factory created a null
     * @throws ServiceConfigurationError when a registration exists but cannot be instantiated
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
     * @throws TargetClassNotFoundException when no factory produces the type and no registration under it could be
     *     loaded
     * @throws IllegalStateException when two factories or two direct registrations exist, or the factory created a null
     * @throws ServiceConfigurationError when a registration exists but cannot be instantiated
     */
    public static <T> T load(final Class<T> targetClass, Dependencies dependencies) {
        Objects.requireNonNull(targetClass);
        Objects.requireNonNull(dependencies);

        final Optional<ComponentFactory<T>> found = findFactory(targetClass);
        if (found.isEmpty()) {
            return loadDirectly(targetClass, dependencies);
        }
        final ComponentFactory<T> factory = found.get();
        final T component = factory.createComponent(dependencies);
        if (component == null) {
            throw new IllegalStateException(String.format("Factory %s created a null", factory));
        }
        return component;
    }

    /**
     * Hands a component back to the factory producing its type. A component without a factory, a direct registration,
     * comes back as is: there is nothing to dispose it through.
     *
     * @param targetClass the component type
     * @param instance the component
     * @param <T> the component type
     * @return what the factory returned, or the instance itself
     * @throws IllegalStateException when two factories produce the type
     */
    public static <T> T dispose(Class<T> targetClass, T instance) {
        return findFactory(targetClass)
                .map(factory -> factory.disposeComponent(instance))
                .orElse(instance);
    }

    /** The one registration under the type itself, constructed by the JDK. */
    private static <T> T loadDirectly(Class<T> targetClass, Dependencies dependencies) {
        final ServiceLoader<T> registrations;
        try {
            registrations = ModuleServices.load(targetClass);
        } catch (ServiceConfigurationError refused) {
            // the JDK refuses the lookup itself: on the module path, a type this module cannot see. Its reason is the
            // one the caller needs, so it travels along
            final TargetClassNotFoundException notFound = notFound(targetClass);
            notFound.addSuppressed(refused);
            throw notFound;
        }
        // a registration that cannot be instantiated throws the JDK's error out of here, as a deployment error should
        final T component =
                ServiceLoaderExtended.one(targetClass, registrations).orElseThrow(() -> notFound(targetClass));
        if (!dependencies.isEmpty()) {
            LOG.log(
                    Level.WARNING,
                    () -> targetClass.getName()
                            + " is registered directly: the JDK constructed it, and the dependencies"
                            + " handed over reached nothing: " + dependencies);
        }
        return component;
    }

    private static TargetClassNotFoundException notFound(Class<?> targetClass) {
        return new TargetClassNotFoundException(
                "No ComponentFactory produces %s, and no registration under it could be loaded"
                        .formatted(targetClass.getName()));
    }

    /** The factory producing the type, empty when none does; two are an error. */
    @SuppressWarnings({"unchecked", "rawtypes"
    }) // a factory whose type() is targetClass produces T: the contract of type()
    private static <T> Optional<ComponentFactory<T>> findFactory(Class<T> targetClass) {
        final Iterable<ComponentFactory> registered = ModuleServices.load(ComponentFactory.class);
        return ServiceLoaderExtended.one(ComponentFactory.class, registered, factory -> targetClass == factory.type())
                .map(factory -> (ComponentFactory<T>) factory);
    }
}
