package com.druvu.lib.loader;

/**
 * Factory contract for creating and optionally disposing components discovered via {@link java.util.ServiceLoader}.
 *
 * <p>A factory is registered under this interface, selected by the type it produces, and handed the caller's
 * {@link Dependencies} at creation. Implementors provide two methods:
 *
 * <ul>
 *   <li>{@link #type()} — the component type this factory produces, used for factory selection
 *   <li>{@link #createComponent(Dependencies)} — component creation from the dependencies it receives
 * </ul>
 *
 * <h2>Usage example</h2>
 *
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
 *
 * <p>Register in {@code META-INF/services/com.druvu.lib.loader.ComponentFactory}:
 *
 * <pre>com.example.MyFactory</pre>
 *
 * <p>Or in {@code module-info.java}:
 *
 * <pre>{@code
 * provides com.druvu.lib.loader.ComponentFactory with com.example.MyFactory;
 * }</pre>
 *
 * <p>A factory needs a public no-arg constructor: {@link java.util.ServiceLoader} instantiates it.
 *
 * @param <T> the type of component this factory creates
 * @author Deniss Larka
 * @see ComponentLoader
 * @see MultiComponentLoader
 * @see Dependencies
 */
public interface ComponentFactory<T> {

    /**
     * The component type this factory produces; the loaders select a factory by it.
     *
     * @return the component type, never {@code null}
     */
    Class<T> type();

    /**
     * Creates a component with the provided dependencies.
     *
     * @param dependencies runtime dependencies required for construction; never {@code null}
     * @return a new component instance; must not return {@code null}
     */
    T createComponent(Dependencies dependencies);

    /**
     * Releases resources held by the given component.
     *
     * <p>No-op by default. Override to perform cleanup such as closing connections, unregistering listeners, or
     * flushing buffers.
     *
     * @param component the component to dispose
     * @return the disposed component
     */
    default T disposeComponent(T component) {
        return component;
    }
}
