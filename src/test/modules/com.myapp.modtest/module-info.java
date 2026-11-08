/**
 * A consumer of druvu-lib-loader on the module path: the API and the app in one module, every registration way the
 * README offers, and a separate implementation module ({@code com.myapp.plugins}) that nothing requires.
 *
 * <p>Compiled and launched by {@code ModulePathTest}; not part of the library.
 */
module com.myapp.modtest {
    requires com.druvu.lib.loader;

    // The API package. The library's module must be able to see a type registered directly.
    exports com.myapp;

    // The resolution hint for the factories an implementation module provides: without these lines a module nobody
    // requires stays out of the module graph, and the library's run-time `uses` cannot bring it in.
    uses com.myapp.AccBook.Factory;
    uses com.myapp.Exporter.Factory;

    // A factory, registered under the library's own service type.
    provides com.druvu.lib.loader.ComponentFactory with
            com.myapp.csv.CsvGreeterFactory;

    // A direct registration: an implementation under its interface; no library type on this side.
    provides com.myapp.AccBook with
            com.myapp.csv.CsvAccBook;

    // A direct registration for an interface in a package this module does not export: the loader cannot see it.
    provides com.myapp.internal.Hidden with
            com.myapp.internal.HiddenImpl;
}
