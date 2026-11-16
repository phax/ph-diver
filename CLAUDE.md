# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

ph-diver (Philip Helger Digitally Versioned Resources) is a Java library providing access to versioned resources across multiple storage backends: in-memory, local file system, HTTP, and AWS S3. Collaboration with ecosio.

## Build Commands

```bash
# Full build with tests
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Run all tests
mvn clean test

# Run a single test class
mvn test -pl ph-diver-api -Dtest=DVRCoordinateTest

# Run a single test method
mvn test -pl ph-diver-api -Dtest=DVRCoordinateTest#testBasic
```

## Module Structure

```
ph-diver-bom          → Maven BOM with the versions of all ph-diver artefacts (no dependencies on the other modules)
ph-diver-api          → Core API: DVRCoordinate, DVRVersion, DVRPseudoVersion
ph-diver-repo         → Repository abstraction + in-memory/local-FS implementations + JAXB-generated ToC classes
ph-diver-repo-config  → XML repository configuration (urn:fdc:com:helger:diver:repoconfig:v1.0) + storage factories
ph-diver-repo-http    → HTTP repository implementation (uses ph-httpclient, Jetty for tests)
ph-diver-repo-s3      → AWS S3 repository implementation
```

Dependencies flow left-to-right: `api` <- `repo` <- `repo-config` / `repo-http` / `repo-s3`.

## Architecture

**Core concept:** A `DVRCoordinate` (groupID:artifactID:version[:classifier]) identifies any versioned artefact. Repositories store and retrieve artefacts by coordinate.

**Repository layer:**
- `IRepoStorageBase` -> `IRepoStorage` -> `IRepoStorageWithToc` (interface hierarchy)
- `AbstractRepoStorage` / `AbstractRepoStorageWithToc` (base implementations)
- Concrete: `RepoStorageInMemory`, `RepoStorageLocalFileSystem`, `RepoStorageHttp`, `RepoStorageS3`
- `RepoStorageChain` composes multiple repositories into a chain

**Pseudo-versions:** `oldest`, `latest`, `latest-release` are resolved at runtime via SPI (`IDVRPseudoVersionRegistrarSPI`).

**Repository configuration:** `RepoStorageConfigReader` reads the XML into a `RepoStorageConfig` (list of `RepoStorageDefinition`); `createAllRepoStorages` uses the `IRepoStorageFactory` registered per type in `RepoStorageFactoryRegistry` (default: `RepoStorageFactoryLocalFileSystem`, type `localfs`, property `basedir`) and initialises the top-level ToC service. Factories are registered explicitly, not via SPI.

**JAXB code generation:** `ph-diver-repo` and `ph-diver-repo-config` generate Java classes from XSD schemas in `src/main/resources/schemas/` using the `ph-jaxb-plugin`. New XML namespace URIs must start with `urn:fdc:`.

## Testing

- JUnit 4 (`@Test` from `org.junit.Test`)
- Helper: `com.helger.unittest.support.TestHelper` for equals/hashCode contract testing
- HTTP tests use embedded Jetty

## CI

GitHub Actions runs on every push, testing against Java 17, 21, and 25. Snapshot deployment to Maven Central happens only on Java 17 builds.
