package org.tribbloid.spookystuff.expressions

import scala.reflect.ClassTag

/**
 * Created by peng on 10/11/14.
 * This is the preferred way of defining extraction
 * entry point for all "query lambda"
 */

class ExprView[T: ClassTag](self: Expr[T]) {

  def andMap[A](g: T => A): Expr[A] = self.andThen(_.map(v => g(v)))

  def andMap[A](g: T => A, name: String): Expr[A] = self.andThen(NamedFunction1(_.map(v => g(v)), name))

  def andFlatMap[A](g: T => Option[A]): Expr[A] = self.andThen(_.flatMap(v => g(v)))

  def andFlatMap[A](g: T => Option[A], name: String): Expr[A] = self.andThen(NamedFunction1(_.flatMap(v => g(v)), name))

//  def defaultToHrefExpr = (self match {
//    case expr: Expr[Unstructured] => expr.href
//    case expr: Expr[Seq[Unstructured]] => expr.hrefs
//    case _ => self
//  }) > Symbol(Const.joinExprKey)

//  def defaultToTextExpr = (this match {
//    case expr: Expr[Unstructured] => expr.text
//    case expr: Expr[Seq[Unstructured]] => expr.texts
//    case _ => this
//  }) as Symbol(Const.joinExprKey)
}
