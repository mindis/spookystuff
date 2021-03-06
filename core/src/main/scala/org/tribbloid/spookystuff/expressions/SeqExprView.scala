package org.tribbloid.spookystuff.expressions

import org.tribbloid.spookystuff.dsl

import scala.reflect.ClassTag

/**
 * Created by peng on 11/29/14.
 */
class SeqExprView[T: ClassTag](self: Expr[Seq[T]]) {

  import dsl._

  def head: Expr[T] = self.andFlatMap(_.headOption, "head")

  def last: Expr[T] = self.andFlatMap(_.lastOption, "last")

  def get(i: Int): Expr[T] = self.andFlatMap({
    seq =>
      val realIdx = if (i >= 0) i
      else seq.size - i

      if (realIdx>=seq.size || realIdx<0) None
      else Some(seq(realIdx))
  },
  s"get($i)")

  def size: Expr[Int] = self.andMap(_.size, "size")

  def mkString(sep: String): Expr[String] = self.andMap(_.mkString(sep), s"mkString($sep)")

  //TODO: handle exception
  //  def only: Expr[T] =
  //    expr.andThen(NamedFunction1("only", _.map{
  //      seq =>
  //        assert(seq.size == 1)
  //        seq.head
  //    }))
}