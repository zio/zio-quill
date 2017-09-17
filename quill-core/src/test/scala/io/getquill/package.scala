package io

package object getquill {

  object testContext extends MirrorContext(MirrorIdiom, Literal) with TestEntities
  object testAsyncContext extends AsyncMirrorContext(MirrorIdiom, Literal) with TestEntities

}
