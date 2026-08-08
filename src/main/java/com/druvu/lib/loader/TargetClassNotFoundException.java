package com.druvu.lib.loader;

/**
 * Thrown when no factory or provider can be found for a requested component type.
 *
 * <p>Raised by {@link ComponentLoader} and {@link MultiComponentLoader} when neither a {@link ComponentFactory} nor a
 * direct {@link java.util.ServiceLoader} provider is registered for the target class.
 *
 * @author Deniss Larka
 * @see ComponentLoader
 */
public class TargetClassNotFoundException extends RuntimeException {
    public TargetClassNotFoundException(String message) {
        super(message);
    }
}
