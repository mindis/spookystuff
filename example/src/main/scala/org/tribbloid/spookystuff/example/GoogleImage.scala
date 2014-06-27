package org.tribbloid.spookystuff.example

import org.apache.spark.{SparkContext, SparkConf}
import org.tribbloid.spookystuff.Conf
import org.tribbloid.spookystuff.entity._
import org.tribbloid.spookystuff.SpookyContext._

/**
 * Created by peng on 10/06/14.
 */
object GoogleImage extends SparkSubmittable {

  override def doMain() {

    //    val nameRDD = sc.textFile("/home/peng/Documents/affiliation.txt").distinct(16)
    val names = (sc.parallelize(Seq("dummy")) +>
      Visit("http://www.utexas.edu/world/univ/alpha/") !)
      //      .flatMap(page => page.text("div.box2 a", limit = Int.MaxValue, distinct = true)) +>
      .flatMap(_.text("div.box2 a", limit = 10, distinct = true))

    names.persist()
    names.saveAsTextFile("s3n://college-logo/list")

    val searchPages = names.repartition(5) +>
      Visit("http://images.google.com/") +>
      DelayFor("form[action=\"/search\"]",50) +>
      TextInput("input[name=\"q\"]","#{_} Logo") +>
      Submit("input[name=\"btnG\"]") +>
      DelayFor("div#search",50) !

    searchPages.persist()
    searchPages.save("#{_}.html", "s3n://college-logo-search-page")

    searchPages
      .wgetJoin("div#search img",1,"src")
      .save("#{_}", "s3n://college-logo")
      .foreach(println(_))
  }
}
