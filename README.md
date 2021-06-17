<img src="https://www.artipie.com/logo.svg" width="64px" height="64px"/>

[![EO principles respected here](https://www.elegantobjects.org/badge.svg)](https://www.elegantobjects.org)
[![DevOps By Rultor.com](http://www.rultor.com/b/artipie/asto)](http://www.rultor.com/p/artipie/asto)
[![We recommend IntelliJ IDEA](https://www.elegantobjects.org/intellij-idea.svg)](https://www.jetbrains.com/idea/)

[![Build Status](https://img.shields.io/travis/artipie/asto/master.svg)](https://travis-ci.org/artipie/asto)
[![Javadoc](http://www.javadoc.io/badge/com.artipie/asto.svg)](http://www.javadoc.io/doc/com.artipie/asto)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/artipie/asto/blob/master/LICENSE.txt)
[![codecov](https://codecov.io/gh/artipie/asto/branch/master/graph/badge.svg)](https://codecov.io/gh/artipie/asto)
[![Hits-of-Code](https://hitsofcode.com/github/artipie/asto)](https://hitsofcode.com/view/github/artipie/asto)
[![Maven Central](https://img.shields.io/maven-central/v/com.artipie/asto.svg)](https://maven-badges.herokuapp.com/maven-central/com.artipie/asto)
[![PDD status](http://www.0pdd.com/svg?name=artipie/asto)](http://www.0pdd.com/p?name=artipie/asto)

Asto stands for Abstract Storage, an abstraction over physical data storage system.
The main entity of the library is an interface `com.artipie.asto.Storage`, a contract
which requires to implement the following functionalities:

* put/get operations
* transaction support
* list files in a directory
* check if a file/directory exists

Dictionary used for ASTO:
 - `Storage` - key-value based storage
 - `Key` - storage keys, could be converted to strings
 - `Content` - storage data, reactive publisher with optional size attribute
 - `SubStorage` - isolated storage based on origin storage


The list of back-ends supported:
 - FileStorage - file-system based storage, uses paths as keys, stores content in files
 - S3Storage - uses S3 compatible HTTP web-server as storage, uses keys as names and blobs for content
 - EtcdStorage - uses ETCD cluster as storage back-end
 - InMemoryStorage - storage uses `HashMap` to store data


This is the dependency you need:

```xml
<dependency>
  <groupId>com.artipie</groupId>
  <artifactId>asto</artifactId>
  <version>[...]</version>
</dependency>
```

Read the [Javadoc](http://www.javadoc.io/doc/com.artipie/asto)
for more technical details.

# Usage

The main types here are:
 - `Storage` interface provides API for key-value storage
 - `Key` represents storage key
 - `Content` represents storage binary value

[Storage](https://www.javadoc.io/doc/com.artipie/asto/latest/com/artipie/asto/Storage.html)
[Key](https://www.javadoc.io/doc/com.artipie/asto/latest/com/artipie/asto/Key.html) and other types are
documented in [javadoc](https://www.javadoc.io/doc/com.artipie/asto/latest/index.html).

TO DO: add more details

## How to contribute

Fork repository, make changes, send us a pull request. We will review
your changes and apply them to the `master` branch shortly, provided
they don't violate our quality standards. To avoid frustration, before
sending us your pull request please run full Maven build:

```
$ mvn clean install -Pqulice
```

To avoid build errors use Maven 3.2+.

