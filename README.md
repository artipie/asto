<img src="https://www.artipie.com/logo.svg" width="64px" height="64px"/>

[![EO principles respected here](https://www.elegantobjects.org/badge.svg)](https://www.elegantobjects.org)
[![DevOps By Rultor.com](http://www.rultor.com/b/artipie/asto)](http://www.rultor.com/p/artipie/asto)
[![We recommend IntelliJ IDEA](https://www.elegantobjects.org/intellij-idea.svg)](https://www.jetbrains.com/idea/)

[![Build Status](https://img.shields.io/travis/artipie/asto/master.svg)](https://travis-ci.org/artipie/asto)
[![Javadoc](http://www.javadoc.io/badge/com.artipie/asto.svg)](http://www.javadoc.io/doc/com.artipie/asto)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/artipie/asto/blob/master/LICENSE.txt)
[![Hits-of-Code](https://hitsofcode.com/github/artipie/asto)](https://hitsofcode.com/view/github/artipie/asto)
[![Maven Central](https://img.shields.io/maven-central/v/com.artipie/asto.svg)](https://maven-badges.herokuapp.com/maven-central/com.artipie/asto)
[![PDD status](http://www.0pdd.com/svg?name=artipie/asto)](http://www.0pdd.com/p?name=artipie/asto)

This is a simple storage, used in a few other projects.

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

Create a `hello.txt` file with `"Hello World!"` content on file-system-based
storage in blocking way:
```java
final BlockingStorage storage = new BlockingStorage(
    new FileStorage(
        Files.createTempDirectory("temp-blocking")
    )
).save(new Key.From("hello.txt"), "Hello World!".getBytes());
``` 

The same with RxJava2 way:
```java
new RxStorageWrapper(
    new FileStorage(
        Files.createTempDirectory("temp-rx")
    )
).save(
    new Key.From("hello.txt"),
    Flowable.fromArray(new ByteArray("Hello World!".getBytes()).boxedBytes())
);
```

## How to contribute

Fork repository, make changes, send us a pull request. We will review
your changes and apply them to the `master` branch shortly, provided
they don't violate our quality standards. To avoid frustration, before
sending us your pull request please run full Maven build:

```
$ mvn clean install -Pqulice
```

To avoid build errors use Maven 3.2+.

