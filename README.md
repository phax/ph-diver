# ph-diver

<!-- ph-badge-start -->
[![Sonatype Central](https://maven-badges.sml.io/sonatype-central/com.helger.diver/ph-diver-parent-pom/badge.svg)](https://maven-badges.sml.io/sonatype-central/com.helger.diver/ph-diver-parent-pom/)
[![javadoc](https://javadoc.io/badge2/com.helger.diver/ph-diver-api/javadoc.svg)](https://javadoc.io/doc/com.helger.diver/ph-diver-api)

> If this project saved you some time or made your day a little easier, a star would mean a lot — it helps others find it too.
<!-- ph-badge-end -->

ph-diver - PH Digitally Versioned Resources - in collaboration with [![ecosio Logo](https://raw.githubusercontent.com/phax/ph-diver/main/docs/ecosio-logo-rgb_black_blue_xs.jpg)](https://www.ecosio.com)

The modules contained in this repository provide access to versioned resources that reside on several 
  external resource types like HTTP servers, local disks or in-memory data structures.

This library consists of the following submodules:
* **`ph-diver-api`** - contains the basic API like version structured
* **`ph-diver-repo`** - contains the data structures for a generic repository of version objects that can read, write and delete data. Also contains an in-memory based repository and afile system based repository.
* **`ph-diver-repo-http`** - contains specific support for HTTP based repositories
* **`ph-diver-repo-s3`** - contains specific support for AWS S3 based repositories

The reason why the several types of repositories are separated, is mainly because of specific runtime dependencies needed, and to
  avoid that your dependencies are bloated if you only need a specific kind of repository.

# Requirements

* Java 17 or later

# Quick Start

## Creating a DVR Coordinate

```java
// Using the factory method
DVRCoordinate coord = DVRCoordinate.create ("com.ecosio", "invoice-rules", "1.2.0");

// With an optional classifier
DVRCoordinate coord = DVRCoordinate.create ("com.ecosio", "invoice-rules", "1.2.0", "sources");

// Parsing from a string
DVRCoordinate coord = DVRCoordinate.parseOrThrow ("com.ecosio:invoice-rules:1.2.0");

// Safe parsing (returns null on invalid input)
DVRCoordinate coord = DVRCoordinate.parseOrNull ("com.ecosio:invoice-rules:1.2.0");
```

## Using Repositories

```java
// Create a local file system repository
RepoStorageLocalFileSystem localRepo = new RepoStorageLocalFileSystem (
    new File ("/path/to/repo"),
    "my-local-repo",
    ERepoWritable.WITH_WRITE,
    ERepoDeletable.WITH_DELETE);

// Or an in-memory repository
RepoStorageInMemory memRepo = RepoStorageInMemory.createDefault ("my-mem-repo");

// Create a storage key from a coordinate
DVRCoordinate coord = DVRCoordinate.create ("com.ecosio", "invoice-rules", "1.2.0");
RepoStorageKeyOfArtefact key = RepoStorageKeyOfArtefact.of (coord, ".xml");
// Resolves to path: com/ecosio/invoice-rules/1.2.0/invoice-rules-1.2.0.xml

// Write content
IRepoStorageContent content = RepoStorageContentByteArray.ofUtf8 ("<rules>...</rules>");
localRepo.write (key, content);

// Read content back
IRepoStorageReadItem item = localRepo.read (key);
if (item != null)
{
  String data = item.getContent ().getAsUtf8String ();
}

// Delete
localRepo.delete (key);
```

## Chaining Repositories

Multiple repositories can be composed into a chain. Reads are attempted in order; if caching is enabled, content found in a later (e.g. remote) repository is automatically written to the first writable repository (e.g. a local cache).

```java
RepoStorageLocalFileSystem localCache = new RepoStorageLocalFileSystem (
    new File ("/tmp/cache"),
    "local-cache",
    ERepoWritable.WITH_WRITE,
    ERepoDeletable.WITHOUT_DELETE);

RepoStorageHttp remoteRepo = /* ... */;

// Reads check localCache first, then remoteRepo
RepoStorageChain chain = RepoStorageChain.of (localCache, remoteRepo);
chain.setCacheRemoteContent (true);

IRepoStorageReadItem item = chain.read (key);
// If found remotely, the item is now cached locally for subsequent reads
```

## Table of Contents

Repositories implementing `IRepoStorageWithToc` maintain a per-artefact table of contents (`toc-diver.xml`) listing all available versions. This allows resolving pseudo-versions without scanning directories.

```java
IRepoStorageWithToc tocRepo = /* e.g. RepoStorageInMemory, RepoStorageLocalFileSystem */;

// Look up the latest release version of an artefact
DVRCoordinate latest = tocRepo.getLatestReleaseVersion ("com.ecosio", "invoice-rules");

// Read the full ToC model
RepoToc toc = tocRepo.readTocModel ("com.ecosio", "invoice-rules");
```

# DVR Coordinate

The DVR Coordinate, short for Digitally Versioned Resource Coordinate, is an identifier for any technical artefact (file) 
  very similar to [Maven Coordinates](https://maven.apache.org/pom.html#Maven_Coordinates).

Hint: The original term was "VESID" which was very much focused on validation artefacts. 
   Each VESID is a DVR Coordinate, but not vice versa.
   DVR Coordinate defines the syntax constraints required to be adhered to by all applications.
   The terminology was changed for version 2 (DVRID) and version 3 of the library. 

## DVR Coordinate Contents

Each DVR Coordinate consists of a combination of:
* Mandatory **Group ID** 
    * Represents an organisation or group that provides a set of artefacts. That must be using the reverse domain name notation (as in `com.helger`)
    * It MUST NOT be empty and follow the regular expression `[a-zA-Z0-9_\-\.]{1,64}`
    * The usage of dot (`.`) in a Group ID represents the separation of different hierarchy levels (e.g. directory and sub-directory).
    * The Group ID MUST be treated case sensitive
* Mandatory **Artefact ID**
    * Uniquely represents an artefact offered by a specific group. Artefact IDs must be unique per Group ID in which they are used. 
    * It MUST NOT be empty and follow the regular expression `[a-zA-Z0-9_\-\.]{1,64}`
    * The Artefact ID MUST be treated case sensitive
* Mandatory **Version Number** that enforces strict ordering
    * Each Version Number must be unique per combination of *Group ID* and *Artefact ID*
    * The usage of semantic version supports the strict ordering of elements
    * Each version must follow either the form `major[.minor[.micro[-classifier]]]` where `major`, `minor` and `micro` must be unsigned integer values (like 1 or 2023) or the form `classifier` which is interpreted as `0.0.0-classifier`.
    * The version classifiers `SNAPSHOT`, `alpha`, `beta`, `milestone` and `rc` are special cases and identify "pre-release" artefacts that are not final yet. They are ordered BEFORE the respective final release - see *Version Classifier Ordering* below
    * The Version Number MUST be treated case sensitive. The only exception is the recognition of the pre-release classifiers listed above, which is case **insensitive**. That affects the ordering only - `1.0.0-RC1` and `1.0.0-rc1` remain two different versions
* Optional **Classifier**
    * It MAY be empty and follow the regular expression `[a-zA-Z0-9_\-\.]{0,64}`
    * The Classifier MUST be treated case sensitive

The limitations in the allowed characters for the different parts are meant to allow an easy representation on file systems. 

## Naming Best Practices for Group ID and Artefact ID

The syntactic rules above define what is *allowed*. The recommendations below are conventions for keeping coordinates readable, predictable, and collision-free across organisations and over time.
They are particularly relevant when a coordinate is going to be referenced by external consumers (as with validation rule sets, schema bundles, or other shared artefacts).

### Group ID

1. **Use reverse-DNS notation, all lowercase.**
   Example: `eu.cen.en16931` — not `EU_CEN_EN16931` or `cen.en16931.eu`. Reverse-DNS aligns with Maven coordinates and Java package names, and keeps the namespace globally unique.

2. **Pick the root segment by who *owns* the artefact, not where it is used.**
    * National format issued by a state body → ISO 3166-1 alpha-2 country code as the root, e.g. `de.xrechnung`, `nl.setu`, `at.ebinterface`.
    * International standards body → `org.{body}`, e.g. `org.oasis-open`.
    * UN-controlled standard → `un.{body}`, e.g. `un.unece.uncefact`.
    * EU-level body or initiative → `eu.{name}`, e.g. `eu.cen.en16931`.
    * Private company → reverse of the actual domain, e.g. `com.acme`.

3. **Use at least three segments when the root is generic.**
   Roots like `eu.`, `org.`, `gov.`, `gob.` are easily collided. Spell out the body or initiative under them: prefer `es.gob.facturae` over `es.gob`. Short flat IDs may seem clean, but they leave no room for sibling artefacts from the same authority.

4. **Pick one root per ecosystem and keep it stable.**
   Do not oscillate between e.g. `eu.foo.*` and `org.foo.*` for artefacts that belong to the same logical ecosystem. Either is fine; consistency matters more than the exact choice. Switching the root after the fact is a breaking change for every consumer.

5. **Reserve sub-namespaces for genuine sub-projects, not ad-hoc variants.**
   A sub-namespace like `de.foo.extension` is appropriate only when "extension" is a separately governed product. If it is merely a flag on an existing artefact, encode it in the artefact ID instead — group IDs should describe the *publisher*, not a property of an individual artefact.

6. **Do not embed version numbers in the Group ID.**
   Versioning belongs in the version field. `vendor.format-2025` looks broken the moment `2026` arrives.

7. **Avoid acronyms in the Group ID unless they are the official, externally-recognised identifier.**
   If the issuing authority publishes itself as `CTC`, `CTC` is fine. Internal abbreviations are not.

### Artefact ID

1. **Use lowercase kebab-case.**
   Example: `ubl-invoice`, `credit-note`, `application-response`. Avoid camelCase (`invoiceData`), snake_case (`invoice_data`), and gratuitous concatenation (`creditnote`) when a separator improves readability. Pick one style per Group ID and stay with it.

2. **Describe the *artefact*, not its variant.**
   Differences such as version, profile, or environment belong in the version field, not the artefact ID. Prefer the pair `(invoice, 1.3.0)` and `(invoice, 1.3.1)` over `invoice-1-3-0` / `invoice-1-3-1`. This is what makes pseudo-versions like `latest` and `latest-release` work.

3. **Include the syntax/format only when one Group ID covers multiple syntaxes.**
   `ubl-invoice`, `cii-invoice`, `cdar-invoice` is appropriate where the Group ID spans them. When a Group ID only ever contains one syntax, the syntax prefix is noise.

4. **Be consistent across releases.**
   An artefact ID that exists in v1 should keep the same spelling in v2. Renaming an artefact ID forces every consumer to update lookups; bumping the version does not.

5. **Avoid classifiers.**
   The classifier slot exists for marginal cases (e.g. distinguishing a `sources` jar from the main artefact). For most domains a distinct artefact ID or a different version is clearer than a classifier.

6. **Stay well within the 64-character limit.**
   Long IDs are valid but harder to read in logs and file paths. Aim for 30 characters or less.

### Why these conventions matter

Group IDs and Artefact IDs feed directly into file system paths (see *Storage Key Path Mapping* below) and into the Table of Contents. Inconsistent names produce parallel paths for the same logical artefact; version numbers embedded in the artefact ID break pseudo-version resolution (`latest`, `latest-release`) and clutter the ToC.

## DVR Coordinate string representation

Each DVR Coordinate can be represented in a single string in the form `groupID:artifactID:version[:classifier]`.

The string representation of version numbers is a bit tricky, because `1`, `1.0` and `1.0.0` are all semantically equivalent.
  Thats why it was decided, that trailing zeroes for minor and micro versions are NOT contained in the string representation, to be as brief as possible
  So e.g., for version `1.0.0` the string representation must be `1`; for version `3.2.0`, the string representation must be `3.2`.
  Versions using a version classifier like `3.0.0-SNAPSHOT` are represented as `3-SNAPSHOT`.
  Versions that only consist of a version classifier like `0.0.0-XYZ` are represented only as the version  classifier `XYZ`.
  That is a work around to be able to handle all kind of versions, but they are treated with a major version of `0`, a minor version of `0` and a micro version of `0`.
  
## Version Classifier Ordering

Versions with the same `major.minor.micro` numbers are ordered by their version classifier as follows:

```
SNAPSHOT  <  alpha  <  beta  <  milestone  <  rc  <  release (no classifier)  <  any other classifier
```

Example: `1.0.0-SNAPSHOT` < `1.0.0-alpha1` < `1.0.0-beta` < `1.0.0-milestone2` < `1.0.0-rc9` < `1.0.0-rc10` < `1.0.0` < `1.0.0-01`

* All pre-release classifiers except `SNAPSHOT` MAY be followed by a number, as in `rc2`.
  The number is compared **numerically**, so `rc9` is correctly ordered before `rc10`.
  A classifier without a number comes before the same classifier with a number, so `rc` < `rc1`.
* The pre-release classifiers are matched **case insensitively** and must match as a whole, optionally followed by digits.
  `RC1`, `rc1` and `Rc1` therefore all have the same rank. They are still three different versions though, and are ordered deterministically among each other, so that no two versions ever compare as equal.
* Single letter abbreviations like `a` for "alpha" or `b` for "beta" are deliberately **not** supported, because they collide with revision letters. `1.3.6-a` is therefore *newer* than `1.3.6`, not older.
* Every classifier that is none of the above - like `03`, `a` or `hotfix03` - keeps being compared as a String and is ordered **after** the respective release version.
  A purely numeric classifier is compared as a String as well, so `2.0.3-9` comes after `2.0.3-13`.
  If numeric classifiers are used, they should be zero padded to a fixed length (`2.0.3-09`) to keep them comparable.

The relevant API is the enum `EDVRPreReleaseQualifier`.

## DVR Pseudo Versions

There are use cases, where the usage of a specific version number (like `1.0.5`) is not suitable and instead a more generic approach is needed.
That's the reason to introduce so called "pseudo versions".
Pseudo versions can be used in all places where specific versions are unknown.
However, pseudo version MUST always be resolved to actual versions before they can be used effectively.

All the pseudo versions supported by *ph-diver* are registered in class `DVRPseudoVersionRegistry` and are:
* `oldest` - always refer to the oldest version of an artefact. This includes snapshot and non-snapshot versions.
* `latest` - always refer to the latest version of an artefact. This includes snapshot and non-snapshot versions.
* `latest-release` - always refer to the latest version of an artefact. This includes only non-snapshot versions.

Other components might define their own pseudo versions by
1. implementing the interface `IDVRPseudoVersion` and
1. implementing the SPI interface `IDVRPseudoVersionRegistrarSPI` and
1. in this implementation registering all pseudo version definitions

Note: the resolution logic is not implemented in this project. This is e.g. provided by the [phive](https://github.com/phax/phive) project.

# Storage Key Path Mapping

A `DVRCoordinate` is mapped to a file path (the `RepoStorageKeyOfArtefact`) using the following structure:

```
{groupID with dots replaced by /}/{artifactID}/{version}/{artifactID}-{version}[-{classifier}]{extension}
```

Examples:
| DVR Coordinate | Extension | Storage Path |
|---|---|---|
| `com.ecosio:invoice-rules:1.2.0` | `.xml` | `com/ecosio/invoice-rules/1.2.0/invoice-rules-1.2.0.xml` |
| `com.ecosio:invoice-rules:1.2.0:sources` | `.jar` | `com/ecosio/invoice-rules/1.2.0/invoice-rules-1.2.0-sources.jar` |
| `com.ecosio:invoice-rules:3-SNAPSHOT` | `.xml` | `com/ecosio/invoice-rules/3-SNAPSHOT/invoice-rules-3-SNAPSHOT.xml` |

The Table of Contents file for a given Group ID / Artefact ID combination is stored at:
```
{groupID with dots replaced by /}/{artifactID}/toc-diver.xml
```

# Repository

A repository is an abstract tree like structure to act as the source for artefacts (files).

Each repository item is uniquely addressed with a `RepoStorageKey` that basically is a path structure.
The content of a repository item is represented via class `RepoStorageItem`.

A repository itself is represented as implementations of class `IRepoStorage`.
Each repository is always readable, and optionally writable and optionally allows for deletion.

Several repositories may be chained together for reading.
  E.g., first the local file system is queried for a resource - if the artefact is not found locally, another remote repository might be used instead.
  The local caching of remote resources is also supported, to limit the necessity for external access.
  
## Table of Contents per Group ID and Artefact ID

Since v1.0.1 a special "table of contents" (ToC) is supported per Group ID and Artefact ID.
It contains all the versions of that combination and allows for easy access of the latest version, without iterating any directory structure.
The filename used is `toc-diver.xml` per default.

An extended API is available for repository storage implementations via the `IRepoStorageWithToc` interface. 

# Maven usage

Add the following to your `pom.xml` to use e.g. the HTTP repository artifact, replacing `x.y.z` with the latest version:

```xml
<dependency>
  <groupId>com.helger.diver</groupId>
  <artifactId>ph-diver-repo-http</artifactId>
  <version>x.y.z</version>
</dependency>
```

Alternate usage as a Maven BOM:

```xml
<dependency>
  <groupId>com.helger.diver</groupId>
  <artifactId>ph-diver-parent-pom</artifactId>
  <version>x.y.z</version>
  <type>pom</type>
  <scope>import</scope>
</dependency>
```

# News and Noteworthy

v4.2.2 - work in progress
* Updated to ph-commons 12.3.5
* The well known pre-release version classifiers `alpha`, `beta`, `milestone` and `rc` are now ordered BEFORE the respective final release version, as in `1.0.0-rc1` &lt; `1.0.0`.
  Previously a version with a classifier was always ordered after the same version without one, so a release candidate was considered *newer* than its own release, and the pseudo versions `latest` and `latest-release` resolved to the release candidate.
  The complete ordering is now `SNAPSHOT < alpha < beta < milestone < rc < release < any other classifier` - see the new section *Version Classifier Ordering* in this document
* Added the new enum `EDVRPreReleaseQualifier` that contains the well known pre-release version classifiers
* All pre-release classifiers except `SNAPSHOT` may be followed by a number, which is compared numerically, so that `1.0.0-rc9` is correctly ordered before `1.0.0-rc10`
* The pre-release version classifiers are now matched case insensitively. This also applies to `DVRVersion.isStaticSnapshotVersion`, so `1.0.0-snapshot` is now recognized as a SNAPSHOT version, and is excluded by the `latest-release` pseudo version.
  Note that this affects the ordering and the SNAPSHOT detection only - the version itself is still case sensitive, hence `1.0.0-RC1` and `1.0.0-rc1` remain two different versions
* Note: classifiers that are none of the well known pre-release classifiers are unaffected and keep being compared as Strings. That especially includes revision letters like in `1.3.6-a` and numeric classifiers like in `1.4.0-03`, which are both still ordered after the respective release version
* Fixed the handling of purely numeric version classifiers like in `1.4.0-03`.
  Previously the string representation `1.4-03` could not be parsed back, because `Version.parse` takes the numeric `03` as the micro version number, resulting in `1.4.3`.
  `DVRVersion` now falls back to the strict version layout `major[.minor[.micro]][-classifier]` (via the new `Version.parseStrictOrNull`), so `1.4-03`, `1.4.0-03` and `1.4.0.03` all lead to the same version, and `getAsString ()` is round trip safe again.
  Non-numeric version classifiers like `SNAPSHOT`, `RC1` or `hotfix03` were never affected.
  The strict layout is deliberately only used as a fallback: for an ambiguous version like `1.4-1.2.3` the previously established interpretation (`0.0.0` with the classifier `4-1.2.3`) still wins, so no previously valid version changes its meaning

v4.2.1 - 2026-07-16
* Extended the API of `RepoStorageS3` to access the parameters from the constructor
* Security: `RepoStorageKey` now rejects `..` path segments and backslashes to prevent path traversal outside the repository base directory
* Security: `RepoStorageChain` now only caches remote content locally if the content hash was verified and matching (`VERIFIED_MATCHING`), to avoid persisting unverified or tampered artefacts (backwards incompatible change)
* `RepoStorageHttp.exists` now uses an HTTP `HEAD` request instead of a full download, and falls back to `GET` if the server does not support `HEAD`
* Fixed the Maven scope of the `jetty-ee10-servlet` dependency in `ph-diver-repo-http` to `test`, so it is no longer pulled into consumers' runtime classpath

v4.2.0 - 2026-02-20
* Added new method `IRepoTopTocService.refreshFromRepo`. See [#1](https://github.com/phax/ph-diver/issues/1)
* Added a "default key prefix" to be applied to S3 repos. See [#2](https://github.com/phax/ph-diver/issues/2)
* Extracted new class `DoNothingRepoTopTocService` as an empty implementation of `IRepoTopTocService`
* The `IRepoStorageAuditor` is now optional in `AbstractRepoStorage` implementations
* Moved the usage of the `IRepoTopTocService` into a specific `IRepoStorageAuditor` implementation
* Removed the `IRepoTopTocService` parameter from the actual repository storage implementations (backwards incompatible change)
* The resulting JAR files are no longer provided as OSGI bundles

v4.1.0 - 2025-11-16
* Updated to ph-commons 12.1.0
* Using JSpecify annotations

v4.0.0 - 2025-08-25
* Requires Java 17 as the minimum version
* Updated to ph-commons 12.0.0
* Added new method `DVRPseudoVersion.getPseudoVersionComparable`

v3.0.1 - 2024-09-15
* Moved classes `RepoToc1Marshaller` and `RepoTopToc1Marshaller` to sub-package `jaxb`
* Added new method `DVRVersion.getStaticVersionAcceptor`

v3.0.0 - 2024-09-13
* Renamed `DVRID` to `DVRCoordinate`
* Made a lot of API changes and extension on the API part. Now it is stable.

v2.0.0 - 2024-09-12
* Renamed `*VESID*` to `*DVRID*`
* Renamed `IVES*` to `IDVR*`
* Renamed `IPseudoVersionComparable` to `IDVRPseudoVersionComparable`
* Removed all deprecated APIs marked for removal
* Moved `DVRID` related classes in package `com.helger.diver.api.id`
* Added class `DVRException`

v1.2.0 - 2024-04-25
* Extended the API of `IRepoStorageWithToc` with `getLatest(Release)Version`
* Extended the API of `RepoToc`
* Replaced the enum `EVESPseudoVersion` with `IVESPseudoVersion` and `VESPseudoVersionRegistry`
* Now supporting the following pseudo versions: `oldest`, `latest` and `latest-release`

v1.1.1 - 2024-03-29
* Updated to ph-commons 11.1.5
* Ensured Java 21 compatibility

v1.1.0 - 2024-03-09
* Extracted `RepoStorageKeyOfArtefact` from `RepoStorageKey` (backwards incompatible change)
* Class `RepoStorageHttp` got an API extension, so that the used HTTP requests can be customized
* Added a top-level table of contents (ToC) service that contains all groups and the artefacts of all groups (via `IRepoTopTocService`) and an XML based implementation (class `RepoTopTocServiceRepoBasedXML`)
* Added a new interface `IRepoStorageAuditor` to be able to handle accesses to the repository 
* Extended `RepoToc` API
* Renamed `RepoTopToc` to `RepoTopTocXML`
* Reworked `RepoStorageItem` to `RepoStorageContentByteArray` and `RepoStorageReadItem` and extracted interfaces for both of them
* Changed the writable repo API to use `IRepoStorageContent` instead of `byte[]` for stream based activities
* Extracted `IRepoStorageType` interface

v1.0.2 - 2023-12-12
* Restricted VESID part maximum lengths - defaults to 64 but customizable via `VESIDSettings`.

v1.0.1 - 2023-11-07
* Added support for a "Table of contents" per Group ID and Artefact ID.

v1.0.0 - 2023-09-13
* Initial version with support for in-memory, file system, HTTP and S3 repositories

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodingStyleguide.md) |
It is appreciated if you star the GitHub project if you like it.
