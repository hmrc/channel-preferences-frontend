/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

import play.api.libs.json.{ JsString, _ }

import java.net.{ URI, URL }

package object connectors {

  implicit def urlWrites =
    new Writes[URL] {
      override def writes(url: URL): JsValue = JsString(url.toExternalForm)
    }

  implicit def uriWrites =
    new Writes[URI] {
      override def writes(uri: URI): JsValue = JsString(uri.toASCIIString)
    }

  implicit def uriReads =
    new Reads[URI] {
      override def reads(json: JsValue): JsResult[URI] = json.validate[String].map(URI.create)
    }

}
