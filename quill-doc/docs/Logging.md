---
id: Logging
title: Logging
---

## Compile-time

To disable logging of queries during compilation use `quill.macro.log` option:
```
sbt -Dquill.macro.log=false
```
## Runtime

Quill uses SLF4J for logging. Each context logs queries which are currently executed.
It also logs the list of parameters that are bound into a prepared statement if any.
To enable that use `quill.binds.log` option:
```
java -Dquill.binds.log=true -jar myapp.jar
```

## Pretty Printing

Quill can pretty print compile-time produced queries by leveraging a great library
produced by [@vertical-blank](https://github.com/vertical-blank) which is compatible
with both Scala and ScalaJS. To enable this feature use the `quill.macro.log.pretty` option:
```
sbt -Dquill.macro.log.pretty=true
```
Before:
```
[info] /home/me/project/src/main/scala/io/getquill/MySqlTestPerson.scala:20:18: SELECT p.id, p.name, p.age, a.ownerFk, a.street, a.state, a.zip FROM Person p INNER JOIN Address a ON a.ownerFk = p.id
```

After:
```
[info] /home/me/project/src/main/scala/io/getquill/MySqlTestPerson.scala:20:18: 
[info]   | SELECT
[info]   |   p.id,
[info]   |   p.name,
[info]   |   p.age,
[info]   |   a.ownerFk,
[info]   |   a.street,
[info]   |   a.state,
[info]   |   a.zip
[info]   | FROM
[info]   |   Person p
[info]   |   INNER JOIN Address a ON a.ownerFk = p.id
```
