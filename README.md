# ph-diver

ph-diver - Philip Helger Digitally Versioned Resources

The modules contained in this repository provide access to versioned resources that reside on several 
  external resource types like HTTP servers, local disks or in-memory data structures.

This library consists of the following submodules:
* **`ph-diver-api`** - contains the basic API like version structured
* **`ph-diver-repo`** - contains the data structures for a generic repository of version objects that can read, write and delete data. Also contains an in-memory based repository and afile system based repository.
* **`ph-diver-repo-http`** - contains specific support for HTTP based repositories
* **`ph-diver-repo-s3`** - contains specific support for AWS S3 based repositories

The reason why the several types of repositories are separated, is mainly because of specific runtime dependencies needed, and to 
  avoid that your dependencies are bloated if you only need a specific kind of repository.

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

* v1.0.0 - 2023-09-13
     * Initial version with support for in-memory, file system, HTTP and S3 repositories

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodingStyleguide.md) |
It is appreciated if you star the GitHub project if you like it.