# druvu-lib-loader

## Overview

`druvu-lib-loader` Is a library that provides a type-safe component loading system built on top of Java's ServiceLoader
mechanism.
It enables dependency injection and singleton management through factories. Suitable for use in JPMS applications.


## Architecture

The library implements a two-tier component loading pattern:

### Core Loading Mechanism

1. **ServiceLoaderExtended** (`ServiceLoaderExtended.java`)
    - Wraps Java's ServiceLoader with predicate-based filtering
    - Enforces a single implementation constraint (throws exception if multiple candidates are found)
    - Used internally by both ComponentLoader and SingletonLoader

2. **ComponentFactory Interface** (`ComponentFactory.java`)
    - Factory pattern for creating components
    - Methods: `createComponent(Dependencies)`, `disposeComponent(T)`, `getComponentType()`
    - All component creation must go through a factory registered via ServiceLoader

### Component Creation Patterns

3. **ComponentLoader** (`ComponentLoader.java`)
    - Main entry point for creating components
    - Uses ServiceLoader to find appropriate ComponentFactory
    - Thread-safe (synchronized on target class)
    - Methods: `create(Class<T>)`, `create(Class<T>, Dependencies)`, `dispose(Class<T>, T)`

4. **SingletonLoader** (`SingletonLoader.java`)
    - Wraps ComponentLoader with singleton semantics
    - Maintains global singleton registry (ConcurrentHashMap)
    - Two-phase usage: `init(Class<T>)` to create, `instance(Class<T>)` to retrieve
    - Prevents double initialization (throws IllegalStateException)
    - **Important**: Does NOT prevent creating multiple instances via ComponentLoader directly

5. **Dependencies** (`Dependencies.java`)
    - Type-safe map of dependencies passed to factories
    - Immutable after construction
    - Static factory methods: `of()`, `of(Class<T1>, T1)`, `of(Class<T1>, T1, Class<T2>, T2)`, etc.
    - Methods: `getDependency(Class<T>)`, `getOptionalDependency(Class<T>)`

## ServiceLoader Registration

To make a component loadable, you must:

1. Create a factory implementing `ComponentFactory<YourClass>`
2. Register it in `META-INF/services/com.druvu.lib.loader.ComponentFactory`

Example from test code:

```
src/test/resources/META-INF/services/com.druvu.lib.loader.ComponentFactory
```

Contains:

```
com.druvu.lib.loader.MySingletonFactory
```

## Testing Conventions

- Tests use TestNG
- Exception testing: `@Test(expectedExceptions = ExceptionClass.class)` or AssertJ's `assertThatThrownBy()`
- AssertJ is the assertion library

## Key Design Principles

1. **Thread Safety**: Both ComponentLoader and SingletonLoader synchronize on the target class
2. **Null Safety**: Extensive null checks with NullPointerException for contract violations
3. **Fail Fast**: Throws exceptions for missing factories, duplicate registrations, null components
4. **Type Safety**: Generic-based API ensures compile-time type checking
5. **Immutability**: Dependencies class creates immutable maps