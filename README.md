# ph-diver

[![javadoc](https://javadoc.io/badge2/com.helger.diver/ph-diver-api/javadoc.svg)](https://javadoc.io/doc/com.helger.diver/ph-diver-api)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.helger.diver/ph-diver-api/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.helger.diver/ph-diver-api) 

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

# DVRID

The DVRID, short for Digitally Version Resource ID, is an identifier for any technical artefact (file) 
  very similar to [Maven coordinates](https://maven.apache.org/pom.html#Maven_Coordinates).

The original term was "VESID" which was very much focused on validation artefacts. 
   Each VESID is a DVRID, but not vice versa.
   DVRID defines the syntax constraints required to be adhered to by all applications.
   The terminology was changed for version 2.0.0 of the library. 

## DVRID contents

Each DVRID consists of:
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
    * The version classifier `SNAPSHOT` is a special case and identifies "work in progress" artefacts that are not final yet
    * The Version Number MUST be treated case sensitive
* Optional **Classifier**
    * It MAY be empty and follow the regular expression `[a-zA-Z0-9_\-\.]{0,64}`
    * The Classifier MUST be treated case sensitive

The limitations in the allowed characters for the different parts are meant to allow an easy representation on file systems. 

## DVRID string representation

Each DVRID can be represented in a single string in the form `groupID:artifactID:version[:classifier]`.

The string representation of version numbers is a bit tricky, because `1`, `1.0` and `1.0.0` are all semantically equivalent.
  Thats why it was decided, that trailing zeroes for minor and micro versions are NOT contained in the string representation, to be as brief as possible
  So e.g., for version `1.0.0` the string representation must be `1`; for version `3.2.0`, the string representation must be `3.2`.
  Versions using a classifier like `3.0.0-SNAPSHOT` are represented as `3-SNAPSHOT`.
  Versions that only consist of a classifier like `0.0.0-XYZ` are represented only as the classifier `XYZ`.
  That is a work around to be able to handle all kind of versions, but they are treated with a major version of 0, a minor version of 0 and a micro version of 0. 

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

* v3.0.0 - 2024-09-13
    * Renamed `DVRID` to `DVRCoordinate`
    * Made a lot of API changes and extension on the API part. Now it is stable.
* v2.0.0 - 2024-09-11
    * Renamed `*VESID*` to `*DVRID*`
    * Renamed `IVES*` to `IDVR*`
    * Renamed `IPseudoVersionComparable` to `IDVRPseudoVersionComparable`
    * Removed all deprecated APIs marked for removal
    * Moved `DVRID` related classes in package `com.helger.diver.api.id`
    * Added class `DVRException`
* v1.2.0 - 2024-04-25
    * Extended the API of `IRepoStorageWithToc` with `getLatest(Release)Version`
    * Extended the API of `RepoToc`
    * Replaced the enum `EVESPseudoVersion` with `IVESPseudoVersion` and `VESPseudoVersionRegistry`
    * Now supporting the following pseudo versions: `oldest`, `latest` and `latest-release`
* v1.1.1 - 2024-03-29
    * Updated to ph-commons 11.1.5
    * Ensured Java 21 compatibility
* v1.1.0 - 2024-03-09
    * Extracted `RepoStorageKeyOfArtefact` from `RepoStorageKey` [backwards incompatible change]
    * Class `RepoStorageHttp` got an API extension, so that the used HTTP requests can be customized
    * Added a top-level table of contents (ToC) service that contains all groups and the artefacts of all groups (via `IRepoTopTocService`) and an XML based implementation (class `RepoTopTocServiceRepoBasedXML`)
    * Added a new interface `IRepoStorageAuditor` to be able to handle accesses to the repository 
    * Extended `RepoToc` API
    * Renamed `RepoTopToc` to `RepoTopTocXML`
    * Reworked `RepoStorageItem` to `RepoStorageContentByteArray` and `RepoStorageReadItem` and extracted interfaces for both of them
    * Changed the writable repo API to use `IRepoStorageContent` instead of `byte[]` for stream based activities
    * Extracted `IRepoStorageType` interface
* v1.0.2 - 2023-12-12
    * Restricted VESID part maximum lengths - defaults to 64 but customizable via `VESIDSettings`.
* v1.0.1 - 2023-11-07
    * Added support for a "Table of contents" per Group ID and Artefact ID.
* v1.0.0 - 2023-09-13
    * Initial version with support for in-memory, file system, HTTP and S3 repositories

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodingStyleguide.md) |
It is appreciated if you star the GitHub project if you like it.