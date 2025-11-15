# PannLib

**PannLib** is a lightweight, standalone, and robust Java library designed primarily for personal use in Minecraft server projects, but engineered as a fully independent, modular framework.  
It currently provides a **complete ORM system** (`PannORM`) and is designed to expand with a **powerful YAML configuration system** in future modules.

> **Modular** • **Asynchronous** • **Production-Ready** • **Minecraft-Optimized**

---

## Overview

PannLib is built for **simplicity**, **performance**, and **total control**.  
It is **not** a bloated framework — it’s a focused, clean, and extensible toolkit.

### Current Module: `PannORM` (ORM)

| Feature                | Status   | Description                                        |
|------------------------|----------|----------------------------------------------------|
| **Entity Mapping**     | Complete | `@Entity`, `@Id`, `@Column`, `@Transient`          |
| **CRUD Operations**    | Complete | `persist`, `update`, `delete`, `find`, `findAll`   |
| **Async API**          | Complete | Full `CompletableFuture` support                   |
| **Transactions**       | Complete | Explicit & thread-local transaction control        |
| **Connection Pooling** | Complete | HikariCP (best-in-class)                           |
| **Multi-DB Support**   | Complete | MySQL, PostgreSQL, SQLite                          |
| **Type Safety**        | Complete | `LocalDateTime`, `UUID`, `Instant`, `byte[]`, etc. |
| **Error Handling**     | Complete | Unified `DatabaseException`                        |
| **Lombok Integration** | Complete | `@Data`, `@Builder`, etc.                          |

### Upcoming Module: `PannConfig` (YAML Configuration)

| Feature                     | Status         | Description                      |
|-----------------------------|----------------|----------------------------------|
| **Annotated Config**        | In Development | `@ConfigValue`, `@ConfigSection` |
| **Defaults & Missing Keys** | Planned        | Auto-fill, warnings, exclusions  |
| **Hot Reload**              | Planned        | File watcher + callbacks         |
| **Persistence**             | Planned        | Save changes back to file        |
| **Validation**              | Planned        | Range, regex, enum checks        |

---

## Project Structure
```markdown
src/main/java/fr/panncake/pannlib/
├── orm/
│   ├── annotation/      → @Entity, @Id, @Column, @Transient
│   ├── config/          → DatabaseType, DatabaseConfig
│   ├── connection/      → ConnectionManager (HikariCP)
│   ├── entity/          → EntityState, ManagedEntity
│   ├── exception/       → DatabaseException
│   ├── mapping/         → EntityMetadata
│   ├── query/           → QueryBuilder
│   ├── session/         → EntityManager (core API)
│   ├── transaction/     → Transaction, TransactionManager
│   └── util/            → ReflectionUtils, SqlTypeConverter
└── config/ (future)
│   └── annotation/      → @ConfigValue, @ConfigSection
│   ├── loader/          → YAML parsing & binding
│   └── model/           → Config POJOs
└────── util/            → Path, validation, persistence
```

---

## Getting Started (ORM)

### 1. Gradle Dependency

```kotlin
// settings.gradle.kts

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
	repositories {
		mavenCentral()
		maven { url = uri("https://jitpack.io") }
	}
}
```

```kotlin
// build.gradle.kts

dependencies {
    implementation("com.github.itspanncake:PannLib:VERSION")
}
```

### 2. Initialize Database
```java
import fr.panncake.pannlib.orm.config.*;
import fr.panncake.connection.orm.fr.panncake.pannlib.orm.ConnectionManager;
import fr.panncake.session.orm.fr.panncake.pannlib.orm.EntityManager;

DatabaseConfig config = DatabaseConfig.builder()
    .type(DatabaseType.SQLITE)
    .database("data/pannlib.db")
    .maxPoolSize(10)
    .connectionTimeout(30_000)
    .autoCommit(true)
    .build();

ConnectionManager.initialize(config);
EntityManager em = new EntityManager();
```

### 3. Define an Entity
```java
import fr.panncake.pannlib.orm.annotation.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity(tableName = "players")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Player {

    @Id(autoIncrement = true)
    private Long id;

    @Column(name = "uuid", nullable = false, unique = true, length = 36)
    private String uuid;

    @Column(name = "username", nullable = false, length = 16)
    private String username;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @Column(name = "is_premium")
    private boolean premium;

    @Transient
    private String sessionToken; // Not persisted
}
```

### 4. CRUD Operations
```java
// CREATE
Player player = Player.builder()
                .uuid("123e4567-e89b-12d3-a456-426614174000")
                .username("ItsPanncake")
                .lastSeen(LocalDateTime.now())
                .premium(true)
                .build();

em.persist(player);
System.out.println("Saved ID: " + player.getId());

// READ
Player found = em.find(Player.class, player.getId());

// UPDATE
found.setUsername("ItsPanncake");
em.update(found);

// DELETE
em.delete(found);

// LIST
em.findAll(Player.class).forEach(System.out::println);
```

### 5. Async & Transactions
```java
// Async
em.persistAsync(newPlayer)
    .thenAccept(p -> System.out.println("Async ID: " + p.getId()))
    .join();

// Transaction
TransactionManager.requireTransaction(() -> {
    em.persist(player1);
    em.update(player2);
    // Atomic: all or nothing
});
```

---

## Database Support


| DB         | Config Example                                                                                                |
|------------|---------------------------------------------------------------------------------------------------------------|
| SQLite     | .type(SQLITE)<br>.database("pannlib.db")                                                                      |
| MySQL      | .type(MYSQL)<br>.host("localhost")<br>.port(3306)<br>.database("mc")<br>.username("root")<br>.password("...") |
| PostgreSQL | .type(POSTGRESQL)<br>.host("localhost")<br>.port(5432)<br>.database("mc")                                     |

---

## Shutdown

```java
ConnectionManager.getInstance().shutdown();
```

---

## Roadmap

| Version  | Status         | Features                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        |
|----------|----------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **v1.0** | Released       | **PannORM** – Complete ORM<br>• Entity annotations (`@Entity`, `@Id`, `@Column`, `@Transient`)<br>• Full CRUD (`persist`, `update`, `delete`, `find`, `findAll`)<br>• Async API (`CompletableFuture`)<br>• Explicit & thread-local transactions<br>• HikariCP connection pooling<br>• MySQL, PostgreSQL, SQLite (bundled drivers)<br>• Type-safe mapping (`LocalDateTime`, `UUID`, `Instant`, etc.)<br>• `DatabaseException` & reflection utilities<br>• Fat JAR (`-all`) with all dependencies |
| **v1.1** | In Development | **PannConfig** – YAML Configuration System<br>• `@ConfigValue`, `@ConfigSection`<br>• Auto-fill missing keys with defaults<br>• Exclusions & persistence<br>• Hot reload (file watcher)<br>• Validation (range, regex, enum)                                                                                                                                                                                                                                                                    |
| **v1.2** | Planned        | **Schema Auto-Migration**<br>• `em.createSchemaIfNotExists()`<br>• `ALTER TABLE` support<br>• Versioned migrations                                                                                                                                                                                                                                                                                                                                                                              |
| **v1.3** | Planned        | **Entity Relationships**<br>• `@OneToMany`, `@ManyToOne`, `@OneToOne`<br>• Lazy & eager loading<br>• Cascade operations                                                                                                                                                                                                                                                                                                                                                                         |
| **v1.4** | Planned        | **L2 Cache**<br>• Caffeine integration<br>• Cache invalidation on write<br>• Query cache                                                                                                                                                                                                                                                                                                                                                                                                        |
| **v1.5** | Planned        | **Query DSL**<br>• Fluent API: `em.query(Player.class).where("premium", true).list()`<br>• Pagination, sorting                                                                                                                                                                                                                                                                                                                                                                                  |
| **v2.0** | Future         | **Full Modularity**<br>• Separate Maven modules: `pannlib-orm`, `pannlib-config`<br>• Plugin system for custom drivers/types                                                                                                                                                                                                                                                                                                                                                                    |

---

## Author

- **@itspanncake**
- France (CET)
- Built on November 15, 2025

---

## License

**MIT License –** Use freely in personal or commercial projects.

---

> **PannLib –** Your code. Your rules. No bloat.