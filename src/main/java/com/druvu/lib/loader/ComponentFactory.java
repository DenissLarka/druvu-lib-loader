package com.druvu.lib.loader;

import java.util.ServiceLoader;

/**
 * Factory contract for creating and optionally disposing components discovered via {@link ServiceLoader}.
 * <p>Extends {@link ServiceLoader.Provider} to align with the JDK's provider model. Implementors
 * must provide two methods:
 * <ul>
 *   <li>{@link #type()} — the component type this factory produces, used for factory selection</li>
 *   <li>{@link #createComponent(Dependencies)} — component creation with injected dependencies</li>
 * </ul>
 * <p>{@link #get()} is provided as a default that delegates to {@code createComponent(new Dependencies())},
 * enabling this factory to also serve as a no-arg {@link ServiceLoader.Provider} for the fallback
 * loading path in {@link ComponentLoader}.
 * <h2>Usage example</h2>
 * <pre>{@code
 * public class MyFactory implements ComponentFactory<MyComponent> {
 *     @Override
 *     public MyComponent createComponent(Dependencies deps) {
 *         return new MyComponentImpl(deps.getDependency(Config.class));
 *     }
 *     @Override
 *     public Class<MyComponent> type() {
 *         return MyComponent.class;
 *     }
 * }
 * }</pre>
 * <p>Register in {@code META-INF/services/com.druvu.lib.loader.ComponentFactory}:
 * <pre>com.example.MyFactory</pre>
 * <p>Or in {@code module-info.java}:
 * <pre>{@code
 * provides com.druvu.lib.loader.ComponentFactory with com.example.MyFactory;
 * }</pre>
 *
 * @param <T> the type of component this factory creates
 * @author Deniss Larka
 * @see ComponentLoader
 * @see MultiComponentLoader
 * @see Dependencies
 */
public interface ComponentFactory<T> extends ServiceLoader.Provider<T> {

	/**
	 * Creates a component with the provided dependencies.
	 *
	 * @param dependencies runtime dependencies required for construction; never {@code null}
	 * @return a new component instance; must not return {@code null}
	 */
	T createComponent(Dependencies dependencies);

	/**
	 * Releases resources held by the given component.
	 * <p>No-op by default. Override to perform cleanup such as closing connections,
	 * unregistering listeners, or flushing buffers.
	 *
	 * @param component the component to dispose
	 * @return the disposed component
	 */
	default T disposeComponent(T component) {
		return component;
	}

	/**
	 * Creates a component with no dependencies.
	 * <p>Delegates to {@link #createComponent(Dependencies)} with an empty {@link Dependencies}
	 * instance. This satisfies the {@link ServiceLoader.Provider} contract, allowing the factory
	 * to be discovered via the direct {@code ServiceLoader.load(targetClass)} fallback path.
	 *
	 * @return a new component instance; must not return {@code null}
	 */
	@Override
	default T get() {
		return createComponent(Dependencies.of());
	}
}
