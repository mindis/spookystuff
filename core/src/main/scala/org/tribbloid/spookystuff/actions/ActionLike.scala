package org.tribbloid.spookystuff.actions

import org.tribbloid.spookystuff.entity.{Page, PageRow}
import org.tribbloid.spookystuff.factory.PageBuilder

/**
 * Created by peng on 11/7/14.
 */
//can't extend function, will override toString() of case classes
trait ActionLike
  extends Serializable
  with Product {

  final def interpolate(pr: PageRow): Option[this.type] = {
    val result = Option[this.type](this.doInterpolate(pr))
    result.foreach(_.inject(this))
    result
  }

  def doInterpolate(pageRow: PageRow): this.type = this //TODO: return Option as well

  def inject(same: this.type ): Unit

  //used to determine if snapshot needs to be appended or if possible to be executed lazily
  final def mayExport: Boolean = outputs.nonEmpty

  def outputs: Set[String]

  //the minimal equivalent action that can be put into backtrace
  def trunk: Option[this.type]

  def apply(session: PageBuilder): Seq[Page]
}