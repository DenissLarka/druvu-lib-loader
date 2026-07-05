# druvu-lib-loader

[![Maven Central](https://img.shields.io/maven-central/v/com.druvu/druvu-lib-loader.svg)](https://central.sonatype.com/artifact/com.druvu/druvu-lib-loader)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=DenissLarka_druvu-lib-loader&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=DenissLarka_druvu-lib-loader)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=DenissLarka_druvu-lib-loader&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=DenissLarka_druvu-lib-loader)

## Overview

`druvu-lib-loader` is a type-safe component loading library built on top of Java's `ServiceLoader` mechanism.
It enables clean separation between API and implementation modules through factories and dependency injection.
Fully compatible with JPMS (Java Platform Module System).

Project page: [druvu.com/projects/druvu-lib-loader](https://druvu.com/projects/druvu-lib-loader.html)

## Why not plain `ServiceLoader`?

`ServiceLoader` is the right tool when an implementation has a no-arg constructor (or a static
`provider()` method). The friction starts when the implementation needs dependencies to be
created — a `Path`, a `Config`, a `DataSource`. Discovery is solved; supplying is not — there is
no way to hand the component its dependencies:

```java
// Two-phase initialization: the object exists before it is valid
AccBook book = ServiceLoader.load(AccBook.class).findFirst().orElseThrow();
((Configurable) book).init(path);
```

The usual escape is a hand-written factory SPI — define an `AccBookFactory` interface, register
*it* as the service, call `factory.create(path)`. That works; after rewriting the same glue in
project after project, it became this library. What it adds over the raw pattern:

- **Dependencies at creation** — the factory receives type-keyed `Dependencies`; the
  implementation is created valid, with no `init()` phase.
- **Fail-fast cardinality** — `ComponentLoader.load` throws when several factories match
  (`findFirst()` silently picks one) and throws when none do.
- **The plugin case** — `MultiComponentLoader.loadAll` when you want *all* implementations.
- **Transparency preserved** — underneath it is still `ServiceLoader` plus JPMS
  `provides`/`uses`: no reflection scanning, no annotations, no container. The
  `AccBook.load(path)` one-liner below is a convention you write once per interface,
  not code generation.

**When not to use it:** if your implementations are no-arg and unique, plain `ServiceLoader`
already serves you well (this library discovers such implementations as a fallback anyway — see
[Implementing Without a Library Dependency](#implementing-without-a-library-dependency)). And if
your application runs on a DI container (Spring, Guice), the container owns construction; this
library targets libraries and plugins that should not impose one.

## Quick Start Example

This example shows how to create a file reader with pluggable implementations.

### Step 1: Define Your Component Interface (API Module)

Add a `static load(...)` factory method to the interface itself, so callers need only one type:

```java
// In your API module (e.g., myapp-api)
public interface AccBook {
    String id();
    List<Account> accounts();

    // Convenience factory — discovers the implementation via ServiceLoader
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

> Prefer a dedicated `AccBookFactory` interface to hold `load` if you want the data interface
> to stay free of any `druvu-lib-loader` reference, or expect several load/config entry points.

### Step 2: Implement ComponentFactory (Implementation Module)

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

### Step 3: Register the Factory

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

### Step 4: Use It

```java
AccBook book = AccBook.load(Paths.get("/path/to/file.xml"));
System.out.println(book.id());
```

The implementation is discovered automatically via `ServiceLoader`. Your application code only depends on the API module.

Need **all** implementations instead of exactly one — plugin style? Same pattern, opposite cardinality: see [MultiComponentLoader](#multicomponentloader) below.

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

Loads **all** implementations of a component type — the plugin case. Every registered factory receives the same `Dependencies`; you get all the instances. Same discovery and argument-passing as `ComponentLoader`, opposite cardinality rule.

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
- **Type Safety**: Generic API type-checks dependency key/value pairs at compile time; which dependencies a factory requires is resolved (and validated) at runtime
- **Immutability**: `Dependencies` are immutable after construction

## Installation

Requires **Java 21 LTS** or later.

Runtime dependency: the SLF4J API (`org.slf4j`) for logging — bring any binding you like, or none
(SLF4J falls back to no-op).

### Maven Central (recommended)

The artifact is published to Maven Central — no additional repository configuration needed.

```xml
<dependency>
    <groupId>com.druvu</groupId>
    <artifactId>druvu-lib-loader</artifactId>
    <version>1.1.0</version>
</dependency>
```

Gradle:

```groovy
implementation 'com.druvu:druvu-lib-loader:1.1.0'
```

Gradle (Kotlin DSL):

```kotlin
implementation("com.druvu:druvu-lib-loader:1.1.0")
```

### GitHub Packages (alternative)

The artifact is also published to GitHub Packages. Using this channel requires authentication with a GitHub Personal Access Token.

1. Create a GitHub Personal Access Token with the `read:packages` scope at https://github.com/settings/tokens.

2. Add the credentials to `~/.m2/settings.xml`:

   ```xml
   <settings>
       <servers>
           <server>
               <id>github-druvu-lib-loader</id>
               <username>YOUR_GITHUB_USERNAME</username>
               <password>YOUR_PAT</password>
           </server>
       </servers>
   </settings>
   ```

3. Add the repository to your consumer project's `pom.xml`:

   ```xml
   <repositories>
       <repository>
           <id>github-druvu-lib-loader</id>
           <url>https://maven.pkg.github.com/DenissLarka/druvu-lib-loader</url>
       </repository>
   </repositories>
   ```

4. Declare the dependency as usual:

   ```xml
   <dependency>
       <groupId>com.druvu</groupId>
       <artifactId>druvu-lib-loader</artifactId>
       <version>1.1.0</version>
   </dependency>
   ```

### JPMS consumers

If your project uses the Java Platform Module System, add to your `module-info.java`:

```java
requires com.druvu.lib.loader;
```

Note: in releases prior to 1.1.0 the module name was `druvu.lib.loader`.

## Feedback

- Found a bug or missing a feature? [Open an issue](https://github.com/DenissLarka/druvu-lib-loader/issues/new/choose) — templates provided.
- Direction input welcome on the pinned issue: [What should druvu-lib-loader do next?](https://github.com/DenissLarka/druvu-lib-loader/issues/28)
- More druvu libraries and tools: [druvu.com](https://druvu.com)
