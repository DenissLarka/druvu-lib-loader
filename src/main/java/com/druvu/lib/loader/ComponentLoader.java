package com.druvu.lib.loader;

import java.util.Objects;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Loads a single component instance using a three-tier discovery strategy.
 *
 * <h2>Tier 1 — ComponentFactory service file</h2>
 *
 * <p>Searches for a {@link ComponentFactory} registered under
 * {@code META-INF/services/com.druvu.lib.loader.ComponentFactory} (or via {@code module-info.java}) whose
 * {@link ComponentFactory#type()} matches the requested target class. The factory's
 * {@link ComponentFactory#createComponent(Dependencies)} is called with the supplied dependencies. Throws if more than
 * one matching factory is found.
 *
 * <h2>Tier 2 — ServiceLoader.Provider service file</h2>
 *
 * <p>If no {@code ComponentFactory} is found in Tier 1, searches for a {@link ComponentFactory} registered under
 * {@code META-INF/services/java.util.ServiceLoader$Provider}. Implementors who prefer to use the JDK provider file as
 * the registration point can use this path while still implementing {@link ComponentFactory} and gaining full
 * {@link Dependencies} support.
 *
 * <h2>Tier 3 — Direct ServiceLoader fallback (no Dependencies)</h2>
 *
 * <p>If no {@code ComponentFactory} is found in either registry, falls back to {@link ServiceLoader#load(Class)
 * ServiceLoader.load(targetClass)}, discovering implementations registered directly under the target type. This path
 * requires no dependency on this library from the implementing side. Dependencies are not available on this path.
 *
 * <h2>Thread safety</h2>
 *
 * <p>Load and dispose operations synchronize on the target class, making concurrent calls safe.
 *
 * @author Deniss Larka
 * @see ComponentFactory
 * @see MultiComponentLoader
 * @see SingletonLoader
 */
public final class ComponentLoader {

    private static final ConcurrentHashMap<Class<?>, Object> LOCKS = new ConcurrentHashMap<>();

    private ComponentLoader() {}

    public static <T> T load(final Class<T> targetClass) {
        return load(targetClass, Dependencies.of());
    }

    public static <T> T load(final Class<T> targetClass, Dependencies dependencies) {

        Objects.requireNonNull(targetClass);
        Objects.requireNonNull(dependencies);

        synchronized (LOCKS.computeIfAbsent(targetClass, k -> new Object())) {
            try {
                final ComponentFactory<T> componentFactory = createComponentFactory(targetClass);
                final T result = componentFactory.createComponent(dependencies);
                if (result == null) {
                    throw new IllegalStateException(String.format("Factory %s created a null", componentFactory));
                }
                return result;
            } catch (TargetClassNotFoundException e) {
                try {
                    return ServiceLoader.load(targetClass).stream()
                            .map(ServiceLoader.Provider::get)
                            .findFirst()
                            .orElseThrow(() -> {
                                var notFound = new TargetClassNotFoundException(
                                        "No ComponentFactory or ServiceLoader provider for %s"
                                                .formatted(targetClass.getName()));
                                notFound.addSuppressed(e);
                                return notFound;
                            });
                } catch (ServiceConfigurationError sce) {
                    var notFound = new TargetClassNotFoundException(
                            "No ComponentFactory or ServiceLoader provider for %s".formatted(targetClass.getName()));
                    notFound.addSuppressed(e);
                    throw notFound;
                }
            }
        }
    }

    private static <T> ComponentFactory<T> createComponentFactory(Class<T> targetClass) {
        final Predicate<ComponentFactory> candidateChooser = factory -> targetClass == factory.type();
        try {
            return ServiceLoaderExtended.load(ComponentFactory.class, candidateChooser);
        } catch (TargetClassNotFoundException e) {
            return findInProviderRegistry(targetClass).orElseThrow(() -> {
                var notFound = new TargetClassNotFoundException(
                        "ComponentFactory providing a %s not found".formatted(targetClass.getName()));
                notFound.addSuppressed(e);
                return notFound;
            });
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> Optional<ComponentFactory<T>> findInProviderRegistry(Class<T> targetClass) {
        try {
            ComponentFactory<T> found = null;
            for (ServiceLoader.Provider p : ServiceLoader.load(ServiceLoader.Provider.class)) {
                if (!(p instanceof ComponentFactory<?> cf) || targetClass != cf.type()) {
                    continue;
                }
                if (found != null) {
                    throw new IllegalStateException(
                            "More than one ComponentFactory found in Provider registry for: " + targetClass.getName());
                }
                found = (ComponentFactory<T>) cf;
            }
            return Optional.ofNullable(found);
        } catch (ServiceConfigurationError e) {
            return Optional.empty();
        }
    }

    public static <T> T dispose(Class<T> targetClass, T instance) {
        try {
            final ComponentFactory<T> componentFactory = createComponentFactory(targetClass);
            return componentFactory.disposeComponent(instance);
        } catch (TargetClassNotFoundException e) {
            return instance; // no ComponentFactory registered, no-op dispose
        }
    }
}
