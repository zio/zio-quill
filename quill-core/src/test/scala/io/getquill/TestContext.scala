package io.getquill

import io.getquill.context.mirror.MirrorContext

object testContext extends TestContextTemplate with QueryProbing

class TestContextTemplate extends MirrorContext with TestEntities
