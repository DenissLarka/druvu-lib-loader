# druvu-lib-loader

[![CI](https://github.com/DenissLarka/druvu-lib-loader/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/DenissLarka/druvu-lib-loader/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.druvu/druvu-lib-loader.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.druvu/druvu-lib-loader)
![Java](https://img.shields.io/badge/Java-21-blue)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue)](LICENSE)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=DenissLarka_druvu-lib-loader&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=DenissLarka_druvu-lib-loader)

## Overview

`ServiceLoader` finds an implementation but cannot pass it a constructor argument. `druvu-lib-loader` is
the factory that does, written once: the caller hands over typed `Dependencies`, the implementation comes
out constructed, and two registrations for one type are an error instead of a silent pick. Underneath it is
still `ServiceLoader` and JPMS `provides`/`uses`: no container, no classpath scanning, no annotations.
Java 21+, class path or module path, no runtime dependencies.

Project page: [druvu.com/projects/druvu-lib-loader](https://druvu.com/projects/druvu-lib-loader.html)

## Why not plain `ServiceLoader`?

`ServiceLoader` is the right tool when an implementation has a no-arg constructor (or, in a module
with a `module-info`, a static `provider()` method). The friction starts when the implementation needs dependencies
to be created — a `Path`, a `Config`, a `DataSource`. Discovery is solved; supplying is not — there is
no way to hand the component its dependencies:

```java
// Two-phase initialization: the object exists before it is valid
AccBook book = ServiceLoader.load(AccBook.class).findFirst().orElseThrow();
((Configurable) book).init(path);
```

The usual escape is a hand-written factory SPI: define an `AccBookFactory` interface, register
*it* as the service, call `factory.create(path)`.

```java
// A factory interface per component type, registered in place of the implementation
ServiceLoader<AccBookFactory> factories = ServiceLoader.load(AccBookFactory.class);
AccBook book = factories.findFirst().orElseThrow().create(path);
```

That works; after rewriting the same glue in project after project, it became this library.
What it adds over the raw pattern:

- **Dependencies at creation** — the factory receives type-keyed `Dependencies`; the
  implementation is created valid, with no `init()` phase.
- **Fail-fast cardinality** — `ComponentLoader.load` throws when several registrations match
  (`findFirst()` silently picks one) and throws when none does.
- **The plugin case** — `MultiComponentLoader.loadAll` when you want *all* implementations.
- **Transparency preserved** — underneath it is still `ServiceLoader` plus JPMS
  `provides`/`uses`: no reflection scanning, no annotations, no container. The
  `AccBook.load(path)` one-liner below is a convention you write once per interface,
  not code generation.

**State:** `ComponentLoader` keeps no components and no registry between calls. `Dependencies` is
built by the caller at the call site and handed to the factory; the only lookup is `ServiceLoader`'s
own. `MultiComponentLoader` remembers which factory created each component until `disposeAll`.

**Limit:** the compiler checks what goes into `Dependencies`; it cannot check that you supplied
everything the factory will ask for. A missing dependency fails at `load()`, not at compile time.
Call it at startup and you find out at startup.

**When not to use it:** if your implementations are no-arg and unique, plain `ServiceLoader`
already serves you well (this library loads such implementations too — see
[Registering Under Your Own Type](#registering-under-your-own-type)). And if your application runs
on a DI container (Spring, Guice), the container owns construction; this library targets libraries
and plugins that should not impose one.

## Quick Start Example

This example shows how to create a file reader with pluggable implementations.

### Step 1: Define Your Component Interface (API Module)

Add a `static load(...)` factory method to the interface itself, so callers need only one type:

```java
// In your API module (e.g., myapp-api)
package com.myapp.api;

import java.nio.file.Path;
import java.util.List;

import com.druvu.lib.loader.ComponentLoader;
import com.druvu.lib.loader.Dependencies;

public interface AccBook {
    String id();
    List<String> accountNames();

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
> When the *implementation* module must stay free of the library, see
> [A factory of your own](#a-factory-of-your-own-dependencies-without-the-library-dependency).

### Step 2: Implement ComponentFactory (Implementation Module)

In your implementation module (e.g., `myapp-gnucash-xml`), create a factory. `GnucashAccBook` is your
implementation of `AccBook`; its constructor takes the `Path`:

```java
package com.myapp.gnucash.io;

import java.nio.file.Path;

import com.druvu.lib.loader.ComponentFactory;
import com.druvu.lib.loader.Dependencies;
import com.myapp.api.AccBook;

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

The factory needs a public no-arg constructor: `ServiceLoader` instantiates it.

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
package com.myapp;

import java.nio.file.Path;
import com.myapp.api.AccBook;

public class Main {
    public static void main(String[] args) {
        AccBook book = AccBook.load(Path.of("/path/to/file.xml"));
        System.out.println(book.id());
    }
}
```

The implementation is discovered automatically via `ServiceLoader`. Your application code only depends on the API module.

Need **all** implementations instead of exactly one — plugin style? Same pattern, opposite cardinality: see [MultiComponentLoader](#multicomponentloader) below.

## Registering Under Your Own Type

An implementation can also be registered under the component interface itself, as for a plain
`ServiceLoader`, with no `druvu-lib-loader` type anywhere on that side. The loaders consult such a
registration when no `ComponentFactory` produces the requested type. The JDK constructs the object, so
it receives no `Dependencies`: a public class with a public no-arg constructor. Dependencies handed over
anyway are logged as a WARNING, since the component was constructed without them.

```java
// Implementation module — no library dependency
package com.myapp.csv;

import com.myapp.api.AccBook;

public class CsvAccBook implements AccBook {
    public CsvAccBook() { ... }  // the JDK calls this
    // ...
}
```

Register in `META-INF/services/com.myapp.api.AccBook`:
```
com.myapp.csv.CsvAccBook
```

Or in `module-info.java`:
```java
provides com.myapp.api.AccBook with com.myapp.csv.CsvAccBook;
```

In an **explicit module only** (one with a `module-info.java`), the JDK also accepts a class with a public
static `provider()` method in place of the no-arg constructor; on the class path and in an automatic module
such a registration is refused by the JDK itself.

The cardinality rules are the ones of the factory way: `ComponentLoader.load` refuses two registrations
of one type, `MultiComponentLoader.loadAll` returns them all. `dispose` and `disposeAll` hand such a
component back as is: there is no factory to release it through.

### A factory of your own: dependencies without the library dependency

When the implementation does need a constructor argument *and* its module must stay free of
`druvu-lib-loader`, let the API module own the factory interface and register *that*:

```java
// API module — the factory lives next to the interface it creates
package com.myapp.api;

import java.nio.file.Path;

import com.druvu.lib.loader.ComponentLoader;

public interface AccBook {
    String id();

    interface Factory {
        AccBook create(Path path);
    }

    // Exactly one factory registered, handed the caller's path
    static AccBook load(Path path) {
        return ComponentLoader.load(Factory.class).create(path);
    }
}
```

```java
// Implementation module — no library type in sight
package com.myapp.xml;

import java.nio.file.Path;

import com.myapp.api.AccBook;

public class XmlBookFactory implements AccBook.Factory {
    @Override
    public AccBook create(Path path) {
        return new XmlBook(path);  // package-private, constructed with its path
    }
}
```

The registration names the factory type. In `META-INF/services/com.myapp.api.AccBook$Factory`
(the `$` is the JDK's name for a nested type):
```
com.myapp.xml.XmlBookFactory
```

Or in the implementation module's `module-info.java`:
```java
provides com.myapp.api.AccBook.Factory with com.myapp.xml.XmlBookFactory;
```

And in the API module's `module-info.java`, the line that brings the implementation module into the
module graph (see the conditions below):
```java
uses com.myapp.api.AccBook.Factory;
```

The plugin case has the same shape: `MultiComponentLoader.loadAll(Exporter.Factory.class)` returns
every registered factory, and the caller hands each one the same argument.

### On the module path

The library declares its `uses` of your type at run time, so no module of yours needs a `uses` line for
the lookup itself. Two conditions the JDK sets:

1. **The type must be visible to the library's module:** public, in a package your module `exports` —
   to everyone or `to com.druvu.lib.loader`. A lookup the JDK refuses surfaces as the suppressed cause
   of the `TargetClassNotFoundException`.
2. **The registering module must be in the module graph.** A module nobody `requires` is left out at
   startup, and the library's run-time `uses` cannot bring it in — the usual plugin layout, an
   implementation module that only `provides`, hits exactly this. Declare `uses` for the type in the
   module that owns it (the API module), and the JDK binds every module that `provides` it at startup.
   The other way is `--add-modules` on the command line.

## Core Components

### ComponentFactory

The central extension point. Requires:
- `type()` — declares which component type this factory produces
- `createComponent(Dependencies)` — creates the component from the dependencies it receives

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

`disposeComponent(T)` is optional: override it to release what the component holds.

### ComponentLoader

Entry point for loading a **single** component instance. Exactly one registration: two factories for
the type, or two direct registrations, are an error.

```java
// Without dependencies
MyComponent component = ComponentLoader.load(MyComponent.class);

// With dependencies
MyComponent component = ComponentLoader.load(MyComponent.class,
    Dependencies.of(Config.class, config));

// Dispose when done
ComponentLoader.dispose(MyComponent.class, component);
```

`TargetClassNotFoundException` means nothing is registered for the type (or, on the module path, nothing
the library may see). A registration that exists but cannot be instantiated surfaces as the JDK's own
`ServiceConfigurationError`, on either loader.

### MultiComponentLoader

Loads **all** implementations of a component type — the plugin case. Every registered factory receives the same `Dependencies`; you get all the instances. Same discovery and argument-passing as `ComponentLoader`, opposite cardinality rule. When no factory produces the type, the implementations registered directly under it come back instead (see [Registering Under Your Own Type](#registering-under-your-own-type)).

```java
// Load all plugins
List<Plugin> plugins = MultiComponentLoader.loadAll(Plugin.class);

// With dependencies
List<Plugin> plugins = MultiComponentLoader.loadAll(Plugin.class, dependencies);

// Dispose all
MultiComponentLoader.disposeAll(Plugin.class, plugins);
```

Loading is strict, disposing is not. `loadAll` takes the set whole or not at all: if a factory fails or
creates a null, or a direct registration cannot be instantiated, the components created before the failure
go back to their factories and the error leaves — a half-loaded plugin set at startup is a deployment error
worth seeing. `disposeAll` is the shutdown path, so it releases everything it can and logs a WARN for
whatever it cannot: a component this loader never created (or already disposed), and a factory
throwing on dispose. One failing plugin never leaves the others un-released.

Each component is disposed by **the factory that created it**, so a subset or a reordered list is
disposed correctly; a direct registration is handed back as is. The returned list holds the components
actually disposed — shorter than the input when something was skipped.

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

The key is the exact type given: a supertype or an implemented interface finds nothing, so register under
the type the factory asks for (`Dependencies.of(Config.class, fileConfig)` keys the value by `Config`). A
null value is refused where it is written: nothing could be asked for it later.

## When to Use Each Loader

| Loader | Use Case | Behavior |
|--------|----------|----------|
| `ComponentLoader` | Single implementation expected | Fails if several are registered, or none |
| `MultiComponentLoader` | Plugin systems, multiple implementations | Returns an empty list if none is registered |

## Design Principles

- **Thread safety**: `ComponentLoader` keeps no state; `MultiComponentLoader`'s only state is a synchronized map of who created what. No lock is held while your factory runs.
- **Fail fast**: a missing registration, two registrations for one type, a null dependency or a half-loaded plugin set is an error where it happens
- **Type safety**: the generic API type-checks dependency key/value pairs at compile time; which dependencies a factory requires is resolved (and validated) at runtime
- **Immutability**: `Dependencies` are immutable after construction
- **One registry**: every factory is registered under `ComponentFactory`, so a load instantiates all of them (they are stateless and cheap), and a factory that cannot be instantiated breaks every load, whatever type it produces — a deployment error you see at the first load

## Installation

Requires **Java 21 LTS** or later.

No runtime dependencies. The few warnings the loader logs (see `disposeAll`) go through the JDK's
`System.Logger`, so they land wherever your application routes platform logging — `java.util.logging`
by default.

### Maven Central

The artifact is on Maven Central — no extra repository, no credentials.

```xml
<dependency>
    <groupId>com.druvu</groupId>
    <artifactId>druvu-lib-loader</artifactId>
    <version>2.0.0</version>
</dependency>
```

Gradle:

```groovy
implementation 'com.druvu:druvu-lib-loader:2.0.0'
```

Gradle (Kotlin DSL):

```kotlin
implementation("com.druvu:druvu-lib-loader:2.0.0")
```

### JPMS consumers

If your project uses the Java Platform Module System, add to your `module-info.java`:

```java
requires com.druvu.lib.loader;
```

## Feedback

- Found a bug or missing a feature? [Open an issue](https://github.com/DenissLarka/druvu-lib-loader/issues/new).
- More druvu libraries and tools: [druvu.com](https://druvu.com)
