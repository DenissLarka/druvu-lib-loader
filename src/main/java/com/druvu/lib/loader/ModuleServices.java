package com.druvu.lib.loader;

import java.util.ServiceLoader;

/**
 * The one place this module asks {@link ServiceLoader} for a service.
 *
 * <p>On the module path the JDK checks the <em>calling</em> module: it must declare {@code uses} for the service type,
 * or {@link ServiceLoader#load(Class)} fails with a {@link java.util.ServiceConfigurationError}. The caller here is
 * always this module, and the service type is often a consumer's own interface, which no {@code module-info} of ours
 * can name. So the use is declared at run time, through {@link Module#addUses(Class)}, right before the lookup. On the
 * class path everything lives in the unnamed module and that call does nothing.
 *
 * <p>The JDK also requires the service type to be accessible to the calling module: public, in a package the consumer's
 * module exports, to everyone or to {@code com.druvu.lib.loader}. That rule cannot be lifted from here.
 *
 * @author Deniss Larka <br>
 *     on 09 Oct 2026
 */
final class ModuleServices {

    private ModuleServices() {}

    /**
     * {@link ServiceLoader#load(Class)} with this module's use of the service declared first.
     *
     * @param service the service type, a consumer's interface or one of ours
     * @param <S> the service type
     * @return the loader, not yet iterated
     */
    static <S> ServiceLoader<S> load(Class<S> service) {
        ModuleServices.class.getModule().addUses(service);
        return ServiceLoader.load(service);
    }
}
