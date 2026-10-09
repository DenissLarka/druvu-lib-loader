/**
 * A consumer of druvu-lib-loader on the module path: one module, every registration style the README offers.
 *
 * <p>Compiled and launched by {@code ModulePathTest}; not part of the library.
 */
module com.myapp.modtest {
    requires com.druvu.lib.loader;

    // The API package. The library's module must be able to see a type it loads directly (tier 3).
    exports com.myapp;

    // Tier 1: a ComponentFactory registered under the library's own service type.
    provides com.druvu.lib.loader.ComponentFactory with
            com.myapp.csv.CsvGreeterFactory;

    // Tier 2: the same kind of factory, registered under the JDK's provider type.
    provides java.util.ServiceLoader.Provider with
            com.myapp.csv.CsvPrinterFactory;

    // Tier 3: an implementation registered directly under its interface; no library type on this side.
    provides com.myapp.AccBook with
            com.myapp.csv.CsvAccBook;

    // Tier 3 again, for an interface in a package this module does not export: the loader cannot see it.
    provides com.myapp.internal.Hidden with
            com.myapp.internal.HiddenImpl;
}
