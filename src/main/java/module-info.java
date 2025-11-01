/**
 * Module descriptor for druvu-lib-loader - a type-safe component loading library.
 *
 * This module provides a ServiceLoader-based dependency injection framework with
 * singleton management capabilities.
 */
module druvu.lib.loader {
	// Export the main API package for consumers
	exports com.druvu.lib.loader;

	// Declare ServiceLoader usage - required for JPMS compliance
	// This tells the module system we'll be loading ComponentFactory implementations
	uses com.druvu.lib.loader.ComponentFactory;

	// SLF4J dependency for logging
	requires org.slf4j;

	// Lombok annotation processing (optional at runtime, only needed for compilation)
	requires static lombok;
}