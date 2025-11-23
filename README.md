# druvu-lib-loader

## Overview

`druvu-lib-loader` Is a library that provides a type-safe component loading system built on top of Java's ServiceLoader
mechanism.
It enables dependency injection and singleton management through factories. Suitable for use in JPMS applications.

## Usage

### 1. Add Repository and Dependency to `pom.xml`

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/DenissLarka/druvu-lib-loader</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.druvu</groupId>
        <artifactId>druvu-lib-loader</artifactId>
        <version>1.0.2</version>
    </dependency>
</dependencies>
```

### 2. Create a GitHub Personal Access Token

1. Go to https://github.com/settings/tokens
2. Generate a new token (classic)
3. Select scope: **`read:packages`**
4. Copy the token and use it as a password in settings.xml

### 3. Configure Authentication in `~/.m2/settings.xml`

```xml
<settings>
    <servers>
        <server>
            <id>github</id>
            <!-- Replace it with your GitHub username -->
            <username>YOUR_GITHUB_USERNAME</username>
            <!-- Replace it with your GitHub token (created in step 2) -->
            <password>YOUR_GITHUB_TOKEN</password>
        </server>
    </servers>
</settings>
```



## Architecture

The library implements a two-tier component loading pattern:

### Core Loading Mechanism

1. **ServiceLoaderExtended** (`ServiceLoaderExtended.java`)
    - Wraps Java's ServiceLoader with predicate-based filtering
    - Two modes: `load()` enforces single implementation (fails if multiple found), `loadAll()` returns all matching candidates
    - Used internally by ComponentLoader, MultiComponentLoader, and SingletonLoader

2. **ComponentFactory Interface** (`ComponentFactory.java`)
    - Factory pattern for creating components
    - Methods: `createComponent(Dependencies)`, `disposeComponent(T)`, `getComponentType()`
    - All component creation must go through a factory registered via ServiceLoader

### Component Creation Patterns

3. **ComponentLoader** (`ComponentLoader.java`)
    - Entry point for creating **single** component instances
    - Uses ServiceLoader to find appropriate ComponentFactory
    - **Enforces single implementation**: Throws exception if multiple factories for the same component type exist
    - Thread-safe (synchronized on target class)
    - Methods: `load(Class<T>)`, `load(Class<T>, Dependencies)`, `dispose(Class<T>, T)`

4. **MultiComponentLoader** (`MultiComponentLoader.java`)
    - Entry point for loading **multiple** component instances (e.g., plugin systems)
    - Uses ServiceLoader to find **all** matching ComponentFactory implementations
    - Returns empty list if no factories found (does not throw exception)
    - Thread-safe (synchronized on target class)
    - Methods: `loadAll(Class<T>)`, `loadAll(Class<T>, Dependencies)`, `disposeAll(Class<T>, List<T>)`

5. **SingletonLoader** (`SingletonLoader.java`)
    - Wraps ComponentLoader with singleton semantics
    - Maintains global singleton registry (ConcurrentHashMap)
    - Two-phase usage: `init(Class<T>)` to create, `instance(Class<T>)` to retrieve
    - Prevents double initialization (throws IllegalStateException)
    - **Important**: Does NOT prevent creating multiple instances via ComponentLoader directly

6. **Dependencies** (`Dependencies.java`)
    - Type-safe map of dependencies passed to factories
    - Immutable after construction
    - Static factory methods: `of()`, `of(Class<T1>, T1)`, `of(Class<T1>, T1, Class<T2>, T2)`, etc.
    - Methods: `getDependency(Class<T>)`, `getOptionalDependency(Class<T>)`

## When to Use Each Loader

- **Use ComponentLoader** when:
  - You expect exactly one implementation (enforced at runtime)
  - You want fail-fast behavior if multiple implementations accidentally exist
  - Example: Database connection manager, configuration service, main application service

- **Use MultiComponentLoader** when:
  - You explicitly want to load multiple implementations (e.g., plugin architecture)
  - It's acceptable to have zero implementations (returns empty list)
  - Example: Plugin systems, event listeners, middleware handlers, feature flags

- **Use SingletonLoader** when:
  - You need global singleton semantics with lifecycle management
  - You want two-phase initialization (`init()` then `instance()`)
  - Example: Application-wide services, resource managers

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

1. **Thread Safety**: All loaders (ComponentLoader, MultiComponentLoader, SingletonLoader) synchronize on the target class
2. **Null Safety**: Extensive null checks with NullPointerException for contract violations
3. **Fail Fast**: Throws exceptions for missing factories, duplicate registrations, null components
4. **Type Safety**: Generic-based API ensures compile-time type checking
5. **Immutability**: Dependencies class creates immutable maps