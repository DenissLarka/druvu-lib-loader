# druvu-lib-loader

[![Maven Central](https://img.shields.io/maven-central/v/com.druvu/druvu-lib-loader.svg)](https://central.sonatype.com/artifact/com.druvu/druvu-lib-loader)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=DenissLarka_druvu-lib-loader&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=DenissLarka_druvu-lib-loader)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=DenissLarka_druvu-lib-loader&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=DenissLarka_druvu-lib-loader)

## Overview

`druvu-lib-loader` is a type-safe component loading library built on top of Java's ServiceLoader mechanism.
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

The static factory method provides a clean API for loading components. It can be placed in any class:

```java
public interface AccBookFactory {

    static AccBook load(Path path) {
        return ComponentLoader.load(AccBook.class, Dependencies.of(Path.class, path));
    }
}
```

The `Dependencies.of()` method accepts type-value pairs. The type serves as a key, and the value is passed to the factory:

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
        var path = dependencies.getOptionalDependency(Path.class)
            .orElseThrow(() -> new IllegalArgumentException("Path dependency required"));

        return new GnucashAccBook(path);  // Your implementation
    }

    @Override
    public Class<AccBook> getComponentType() {
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
    requires druvu.lib.loader;
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

The implementation is discovered automatically via ServiceLoader. Your application code only depends on the API module.

## Core Components

### ComponentLoader

Entry point for loading a **single** component instance. Throws an exception if multiple factories exist for the same type.

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

Manages global singletons with two-phase initialization.

```java
// Initialize once at startup
SingletonLoader.init(AppConfig.class, dependencies);

// Retrieve anywhere in application
AppConfig config = SingletonLoader.instance(AppConfig.class);
```

### Dependencies

Type-safe container for passing dependencies to factories.

```java
// Empty
Dependencies.of()

// Single dependency (type as key, value as value)
Dependencies.of(Path.class, path)

// Multiple dependencies
Dependencies.of(Path.class, path, Config.class, config)

// In factory - required dependency (throws if missing)
Path path = dependencies.getDependency(Path.class);

// In factory - optional dependency
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
- **Immutability**: Dependencies are immutable after construction
