[![EO principles respected here](https://www.elegantobjects.org/badge.svg)](https://www.elegantobjects.org)
[![DevOps By Rultor.com](http://www.rultor.com/b/yegor256/asto)](http://www.rultor.com/p/yegor256/asto)
[![We recommend IntelliJ IDEA](https://www.elegantobjects.org/intellij-idea.svg)](https://www.jetbrains.com/idea/)

[![Build Status](https://img.shields.io/travis/yegor256/asto/master.svg)](https://travis-ci.org/yegor256/asto)
[![Javadoc](http://www.javadoc.io/badge/com.yegor256/asto.svg)](http://www.javadoc.io/doc/com.yegor256/asto)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/yegor256/asto/blob/master/LICENSE.txt)
[![Hits-of-Code](https://hitsofcode.com/github/yegor256/asto)](https://hitsofcode.com/view/github/yegor256/asto)
[![Maven Central](https://img.shields.io/maven-central/v/com.yegor256/asto.svg)](https://maven-badges.herokuapp.com/maven-central/com.yegor256/asto)
[![PDD status](http://www.0pdd.com/svg?name=yegor256/asto)](http://www.0pdd.com/p?name=yegor256/asto)

This is a simple storage, used in a few other projects.

This is the dependency you need:

```xml
<dependency>
  <groupId>com.yegor256</groupId>
  <artifactId>asto</artifactId>
  <version>[...]</version>
</dependency>
```

Read the [Javadoc](http://www.javadoc.io/doc/com.yegor256/asto)
for more technical details.

## How to contribute

Fork repository, make changes, send us a pull request. We will review
your changes and apply them to the `master` branch shortly, provided
they don't violate our quality standards. To avoid frustration, before
sending us your pull request please run full Maven build:

```
$ mvn clean install -Pqulice
```

To avoid build errors use Maven 3.2+.
