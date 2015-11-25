![quill](https://raw.githubusercontent.com/getquill/quill/master/quill.png)
# Quill
Compile-time Language Integrated Query for Scala

[![Join the chat at https://gitter.im/getquill/quill](https://img.shields.io/badge/gitter-join%20chat-green.svg)](https://gitter.im/getquill/quill?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status](https://img.shields.io/travis/getquill/quill.svg)](https://api.travis-ci.org/getquill/quill.svg?branch=master)
[![Codacy Badge](https://img.shields.io/codacy/36ab84c7ff43480489df9b7312a4bdc1.svg)](https://www.codacy.com/app/fwbrasil/quill)
[![codecov.io](https://img.shields.io/codecov/c/github/getquill/quill.svg)](http://codecov.io/github/getquill/quill?branch=master)

# Overview #

Quill provides a Quoted Domain Specific Language (QDSL) to express queries in Scala and execute them in a target query language. The library's core is designed to support multiple target languages but the current version only supports the generation of Structured Language Queries (SQL) for interaction with relational databases.

![example](https://raw.githubusercontent.com/getquill/quill/master/example.gif)

1. **Boilerplate-free mapping**: The database's schema is expressed using simple case classes.
2. **Quoted DSL**: Queries are defined inside a `quote` block. Quill parses each quoted block of code (quotation) at compile time and translates them to an internal Abstract Syntax Tree (AST)
3. **Compile time SQL generation**: The `db.run` call reads the quotation's AST and translates it to the target language at compile time, emitting the SQL string as a compilation message. As the query string is known at compile time, the runtime overhead is very low and similar to using database driver directly.
4. **Compile-time query validation**: If configured, the query is verified against the database at compile time and the compilation fails if it is not valid.

# Getting started #

Quill is distributed via maven central. Click on the badge for more information on how to add the dependency to your project:

[![maven](https://img.shields.io/maven-central/v/io.getquill/quill_2.11.svg)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22io.getquill%22)

# Queries #

## Quotation ##

## Joins ##



# Sources #

## Probing ##

# Dynamic queries #

# Extending quill #

## Custom encoding ##

## Infix ##

# Internals #

# Known limitations #

# Acknowledgments #

# Contributing #

# License #

See the [LICENSE](https://github.com/getquill/quill/blob/master/LICENSE.txt) file for details.