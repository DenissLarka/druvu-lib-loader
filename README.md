# druvu-lib-loader

[![Maven Central](https://img.shields.io/maven-central/v/com.druvu/druvu-lib-loader.svg)](https://central.sonatype.com/artifact/com.druvu/druvu-lib-loader)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=DenissLarka_druvu-lib-loader&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=DenissLarka_druvu-lib-loader)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=DenissLarka_druvu-lib-loader&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=DenissLarka_druvu-lib-loader)

## Overview

`druvu-lib-loader` is a type-safe component loading library built on top of Java's `ServiceLoader` mechanism.
It enables clean separation between API and implementation modules through factories and dependency injection.
Fully compatible with JPMS (Java Platform Module System).

## Installation

### Maven

```xml
<dependency>
    <groupId>com.druvu</groupId>
    <artifactId>druvu-lib-loader</artifactId>
    <version>1.0.7</version>
</dependency>
```

### Gradle

```groovy
implementation 'com.druvu:druvu-lib-loader:1.0.7'
```

### Gradle (Kotlin DSL)

```kotlin
implementation("com.druvu:druvu-lib-loader:1.0.7")
```

## Quick Start Example

This example shows how to create a file reader with pluggable implementations.

### Step 1: Define Your Component Interface (API Module)

```java
// In your API module (e.g., myapp-api)
public interface AccBook {
    String id();
    List<Account> accounts();
}
```

### Step 2: Create a Static Factory Method

The static factory method provides a clean API for loading components:

```java
public interface AccBookFactory {

    static AccBook load(Path path) {
        return ComponentLoader.load(AccBook.class, Dependencies.of(Path.class, path));
    }
}
```

The `Dependencies.of()` method accepts type-value pairs. The type serves as a key:

```java
// Single dependency
Dependencies.of(Path.class, path)

// Multiple dependencies
Dependencies.of(Path.class, path, Config.class, config)
```

### Step 3: Implement ComponentFactory (Implementation Module)

In your implementation module (e.g., `myapp-gnucash-xml`), create a factory:

```java
public class GnucashBookFactory implements ComponentFactory<AccBook> {

    @Override
    public AccBook createComponent(Dependencies dependencies) {
        var path = dependencies.getDependency(Path.class);
        return new GnucashAccBook(path);
    }

    @Override
    public Class<AccBook> type() {
        return AccBook.class;
    }
}
```

### Step 4: Register the Factory

**For non-JPMS projects**, create a file at:

```
src/main/resources/META-INF/services/com.druvu.lib.loader.ComponentFactory
```

With content:
```
com.myapp.gnucash.io.GnucashBookFactory
```

**For JPMS projects**, add to your `module-info.java`:

```java
module myapp.gnucash.xml {
    requires com.druvu.lib.loader;
    requires myapp.api;

    provides com.druvu.lib.loader.ComponentFactory
        with com.myapp.gnucash.io.GnucashBookFactory;
}
```

### Step 5: Use It

```java
AccBook book = AccBookFactory.load(Paths.get("/path/to/file.xml"));
System.out.println(book.id());
```

The implementation is discovered automatically via `ServiceLoader`. Your application code only depends on the API module.

## Implementing Without a Library Dependency

If your component does not require runtime dependencies, you can register it directly under the
target interface without depending on `druvu-lib-loader` at all. `ComponentLoader` will discover
it automatically as a fallback.

Register a direct implementation or a static `provider()` factory:

```java
// Direct implementation — no library dependency needed
public class CsvAccBookImpl implements AccBook {
    public CsvAccBookImpl() { ... }  // no-arg constructor required
    // ...
}
```

Or a provider factory with a static method:

```java
public class CsvAccBookFactory {
    public static AccBook provider() {
        return new CsvAccBookImpl();
    }
}
```

Register in `META-INF/services/com.myapp.AccBook`:
```
com.myapp.csv.CsvAccBookImpl
```

Or in `module-info.java`:
```java
provides com.myapp.AccBook with com.myapp.csv.CsvAccBookFactory;
```

`ComponentLoader` tries `ComponentFactory` first. If none is found, it falls back to
`ServiceLoader.load(AccBook.class)` automatically.

## Core Components

### ComponentFactory

The central extension point. Implements `ServiceLoader.Provider<T>` from the JDK. Requires:
- `type()` — declares which component type this factory produces
- `createComponent(Dependencies)` — creates a component with injected dependencies

```java
public class MyFactory implements ComponentFactory<MyService> {

    @Override
    public MyService createComponent(Dependencies deps) {
        return new MyServiceImpl(deps.getDependency(DataSource.class));
    }

    @Override
    public Class<MyService> type() {
        return MyService.class;
    }
}
```

### ComponentLoader

Entry point for loading a **single** component instance. Throws if multiple factories match the same type.

```java
// Without dependencies
MyComponent component = ComponentLoader.load(MyComponent.class);

// With dependencies
MyComponent component = ComponentLoader.load(MyComponent.class,
    Dependencies.of(Config.class, config));

// Dispose when done
ComponentLoader.dispose(MyComponent.class, component);
```

### MultiComponentLoader

Loads **all** implementations of a component type. Useful for plugin systems.

```java
// Load all plugins
List<Plugin> plugins = MultiComponentLoader.loadAll(Plugin.class);

// With dependencies
List<Plugin> plugins = MultiComponentLoader.loadAll(Plugin.class, dependencies);

// Dispose all
MultiComponentLoader.disposeAll(Plugin.class, plugins);
```

### SingletonLoader

Manages global singletons with two-phase initialization — `load` once at startup, `get` everywhere else.

```java
// Initialize once at startup (throws if called again for the same type)
AppConfig config = SingletonLoader.load(AppConfig.class, dependencies);

// Retrieve anywhere in the application
AppConfig config = SingletonLoader.get(AppConfig.class);
```

### Dependencies

Type-safe container for passing dependencies to factories.

```java
// Empty
Dependencies.of()

// Single dependency (type as key, instance as value)
Dependencies.of(Path.class, path)

// Multiple dependencies
Dependencies.of(Path.class, path, Config.class, config)

// In factory — required dependency (throws if missing)
Path path = dependencies.getDependency(Path.class);

// In factory — optional dependency
Optional<Config> config = dependencies.getOptionalDependency(Config.class);
```

## When to Use Each Loader

| Loader | Use Case | Behavior |
|--------|----------|----------|
| `ComponentLoader` | Single implementation expected | Fails if multiple factories exist |
| `MultiComponentLoader` | Plugin systems, multiple implementations | Returns empty list if none found |
| `SingletonLoader` | Application-wide services | Two-phase init, prevents double initialization |

## Design Principles

- **Thread Safety**: All loaders synchronize on the target class
- **Fail Fast**: Throws exceptions for missing factories or duplicate registrations
- **Type Safety**: Generic-based API ensures compile-time type checking
- **Immutability**: `Dependencies` are immutable after construction
