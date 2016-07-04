package io.getquill.context.sql

import io.getquill.TestEntities
import io.getquill.SqlMirrorContext
import io.getquill.Literal

object testContext extends TestContextTemplate

class TestContextTemplate extends SqlMirrorContext[Literal] with TestEntities
