package io.getquill

import io.getquill.context.mirror.MirrorContext

object testContext extends TestContextTemplate

class TestContextTemplate extends MirrorContext with TestEntities
