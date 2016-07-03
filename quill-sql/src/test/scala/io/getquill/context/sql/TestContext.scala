package io.getquill.context.sql

import io.getquill.TestEntities
import io.getquill.context.sql.mirror.SqlMirrorContext
import io.getquill.naming.Literal

object testContext extends TestContextTemplate

class TestContextTemplate extends SqlMirrorContext[Literal] with TestEntities
