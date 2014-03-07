package config

import scala.io.Source

object RefData {

  lazy val data: Map[String, Map[Int, String]] = load()

  def load() = {
    val csv = Source.fromInputStream(getClass.getResourceAsStream("/refData.csv"))
    csv.getLines().drop(1).toList.map(line => {
      val tokens = line.split("\\|")
      (tokens(0), tokens(1).toInt -> tokens(2))
    })
      .groupBy(_._1)
      .mapValues(value => Map[Int, String]() ++ value.map(_._2))
  }

  def iabdTypeFor(code: Int) = {
    dataFor("IABDTYPE", code)
  }

  private def dataFor(key: String, code: Int) = data(key).getOrElse(code, "Unknown")

}
