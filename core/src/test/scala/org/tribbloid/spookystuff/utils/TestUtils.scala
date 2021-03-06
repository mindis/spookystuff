package org.tribbloid.spookystuff.utils

import org.scalatest.FunSuite
import org.tribbloid.spookystuff.entity.{KeyLike, PageRow, Key}

/**
 * Created by peng on 11/1/14.
 */
class TestUtils extends FunSuite {

  test("Clean ?:$&#"){
    val url = Utils.canonizeUrn("http://abc.com?re#k2$si")
    assert(url === "http/abc.com/re/k2/si")
  }

  test("interpolate") {
    val someMap = Map[KeyLike, Any](Key("abc") -> 1, Key("def") -> 2.2)
    val result = PageRow(someMap).replaceInto("rpk'{abc}aek'{def}")
    assert(result === Some("rpk1aek2.2"))
  }

  test("interpolate returns None when key not found") {

    val someMap = Map[KeyLike, Any](Key("abc") -> 1, Key("rpk") -> 2.2)
    val result = PageRow(someMap).replaceInto("rpk'{abc}aek'{def}")
    assert(result === None)
  }

  test("formatNullString") {assert (PageRow(Map()).replaceInto(null) === None)}

  test("formatEmptyString") {assert (PageRow(Map()).replaceInto("") === Some(""))}
}
