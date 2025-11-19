package com.druvu.lib.loader;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Instantiating components by first creating a factory by calling {@link  ServiceLoaderExtended}
 * Factory is a {@link  ComponentFactory} capable of creating of a desired target class.
 * Factory must be stateless and have a default constructor.
 *
 * @author Deniss Larka
 * on 06 Aug 2025
 */
public final class ComponentLoader {

	private ComponentLoader() {
	}

	public static <T> T load(final Class<T> targetClass) {
		return load(targetClass, Dependencies.of());
	}

	public static <T> T load(final Class<T> targetClass, Dependencies dependencies) {

		Objects.requireNonNull(targetClass);
		Objects.requireNonNull(dependencies);

		synchronized (targetClass) {
			final ComponentFactory<T> componentFactory = createComponentFactory(targetClass);
			final T result = componentFactory.createComponent(dependencies);
			if (result == null) {
				throw new IllegalStateException(String.format("Factory %s created a null", componentFactory));
			}
			return result;
		}

	}

	private static <T> ComponentFactory<T> createComponentFactory(Class<T> targetClass) {
		final Predicate<ComponentFactory> candidateChooser = factory -> targetClass == factory.getComponentType();
		return serviceLoaderExtendedCreate(targetClass, candidateChooser);
	}

	private static <T> ComponentFactory<T> serviceLoaderExtendedCreate(Class<T> targetClass, Predicate<ComponentFactory> candidateChooser) {
		try {
			return ServiceLoaderExtended.load(ComponentFactory.class, candidateChooser);
		}
		catch (TargetClassNotFoundException e) {
			var newException = new TargetClassNotFoundException("ComponentFactory providing a %s not found".formatted(targetClass.getName()));
			newException.addSuppressed(e);
			throw newException;
		}
	}

	public static <T> T dispose(Class<T> targetClass, T instance) {
		final ComponentFactory<T> componentFactory = createComponentFactory(targetClass);
		return componentFactory.disposeComponent(instance);
	}

}
