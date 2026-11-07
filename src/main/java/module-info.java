/**
 * Module descriptor for druvu-lib-loader - a type-safe component loading library.
 *
 * <p>This module provides type-safe component loading over ServiceLoader: factories receive their dependencies at
 * creation, with fail-fast cardinality.
 */
module com.druvu.lib.loader {
    // Export the main API package for consumers
    exports com.druvu.lib.loader;

    // Declare ServiceLoader usage - required for JPMS compliance
    // This tells the module system we'll be loading ComponentFactory implementations
    // A consumer's own type (a direct registration) cannot be named here: ModuleServices declares that use at run time
    uses com.druvu.lib.loader.ComponentFactory;
}
