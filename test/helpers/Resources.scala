/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package helpers
import play.api.libs.json.{JsValue, Json}

import scala.io.Source

object Resources {
  def readJson(fileName: String): JsValue = {
    val resource = Source.fromURL(getClass.getResource("/" + fileName))
    val json = Json.parse(resource.mkString)
    resource.close()
    json
  }
}
