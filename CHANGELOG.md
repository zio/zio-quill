# 0.4.0 / 19-Feb-2016

* [new sources creation mechanism](https://github.com/getquill/quill/pull/136)
* [simplified join syntax](https://github.com/getquill/quill/commit/bfcfe49fbdbda04cce7fe7e7d382fb1adbfcbd7f)
* [Comparison between Quill and other alternatives for CQL](https://github.com/getquill/quill/pull/164)
* [`contains` operator (sql `in`)](https://github.com/getquill/quill/pull/165/files#diff-04c6e90faac2675aa89e2176d2eec7d8R377)
* [unary sql queries](https://github.com/getquill/quill/pull/179/files#diff-02749abf4d0d51be99715cff7074bc9eR207)
* [query probing is now opt-in](https://github.com/getquill/quill/pull/176/files#diff-04c6e90faac2675aa89e2176d2eec7d8R453)
* [quill-cassandra: upgrade Datastax Java Driver to version 3.0.0](https://github.com/getquill/quill/pull/171)
* [support implicit quotations with type parameters](https://github.com/getquill/quill/pull/163)
* [quill-cassandra: UUID support](https://github.com/getquill/quill/pull/142)
* [quill-async: more reasonable numeric type decodes](https://github.com/getquill/quill/pull/139)

# 0.3.1 / 01-Feb-2016

* [fix #134 - ignore the property `queryProbing` when creating the hikari data source](https://github.com/getquill/quill/issues/134)

# 0.3.0 / 26-Jan-2016

* [quill-cassandra: first version of the module featuring async and sync sources](https://github.com/getquill/quill/#cassandra-sources)
* [quill-cassandra: reactive streams support via Monix](https://github.com/getquill/quill/#cassandra-sources)
* [quill-core: updates using table columns](https://github.com/getquill/quill/commit/0681b21aad8d75cb7793840c4f905b80645872cc#diff-04c6e90faac2675aa89e2176d2eec7d8R458)
* [quill-core: explicit inner joins](https://github.com/getquill/quill/commit/902eb858e0e844f41978f8179156da9c69f2d847#diff-2e097508346e0e431a36abcb2c1cc4cbR270)
* [quill-core: configuration option to disable the compile-time query probing](https://github.com/getquill/quill/commit/130919d62a1f852c2d26203c035361ccb3284e53#diff-04c6e90faac2675aa89e2176d2eec7d8L840)
* [quill-core: `if/`else` support (sql `case`/`when`)](https://github.com/getquill/quill/commit/16674ba77fdc880a64af719d150560351ac6a8f6#diff-2e097508346e0e431a36abcb2c1cc4cbR598)
* [quill-async: uuid encoding](https://github.com/getquill/quill/commit/743227aaa3ec76cefcffb405ac658069d90118fc#diff-7bfbe03bba9c515d3f16f88115eb2f9fR24)
* [quill-core: custom ordering](https://github.com/getquill/quill/commit/2fe7556279c5919aa9c1e22bf9c8caf4c67e53e7#diff-04c6e90faac2675aa89e2176d2eec7d8R257)
* [quill-core: expressions in sortBy](https://github.com/getquill/quill/commit/0dbb492de7334cb8ad34dc5c6246ec6908d328bc#diff-2e097508346e0e431a36abcb2c1cc4cbR107)

# 0.2.1 / 28-Dec-2015

* [expire and close compile-time sources automatically](https://github.com/getquill/quill/issues/10)
* [Aggregation sum should return an Option](https://github.com/getquill/quill/pull/69)
* [Changed min/max implicit from Numeric to Ordering](https://github.com/getquill/quill/pull/70)
* [provide implicit to query case class companion objects directly](https://github.com/getquill/quill/pull/73)
* [don't fuse multiple `sortBy`s](https://github.com/getquill/quill/issues/71)
* [actions now respect the naming strategy](https://github.com/getquill/quill/issues/74)

# 0.2.0 / 24-Dec-2015

* [Insert/update using case class instances](https://github.com/getquill/quill/commit/aed630bdb514b3d71a3a3cc47299ff28c0472023)
* [Better IntelliJ IDEA support](https://github.com/getquill/quill/issues/23)
* [Implicit quotations](https://github.com/getquill/quill/commit/1991d694a2bdad645d6d169acefba51f90acde62#diff-6858983f3617753cfb9852426edaa121R491)
* [`like` operator](https://github.com/getquill/quill/commit/f05763ff6cfbe850d7cab2e15d570603cad194c4#diff-04c6e90faac2675aa89e2176d2eec7d8R420)
* [string interpolation support](https://github.com/getquill/quill/commit/c510ee8a6daa98caf45743fd7fc75230cbb3d71e#diff-6858983f3617753cfb9852426edaa121R290)
* [Finagle pool configuration](https://github.com/getquill/quill/pull/60)
* [Allow empty password in Finagle Mysql client](https://github.com/getquill/quill/pull/59)
* Bug fixes:
	* https://github.com/getquill/quill/issues/53
	* https://github.com/getquill/quill/issues/62
	* https://github.com/getquill/quill/issues/63

# 0.1.0 / 27-Nov-2015

* Initial release
