package org.tribbloid.spookystuff.example.network

import org.tribbloid.spookystuff.{dsl, SpookyContext}
import org.tribbloid.spookystuff.actions._
import org.tribbloid.spookystuff.example.ExampleCore
import dsl._

/**
 * Created by peng on 9/7/14.
 */
object Useragentstring extends ExampleCore {

  override def doMain(spooky: SpookyContext) = {
    import spooky._

    //    spooky.driverFactory = TorDriverFactory()

    noInput
      .fetch(
        Wget("http://www.useragentstring.com/pages/Browserlist/")
      )
      .select($"li a".text > 'agent_string)
      .asSchemaRDD()
  }
}
