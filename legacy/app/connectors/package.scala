/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import play.api.libs.json.{ JsString, _ }

import java.net.{ URI, URL }

package object connectors {

  implicit def urlWrites = new Writes[URL] {
    override def writes(url: URL): JsValue = JsString(url.toExternalForm)
  }

  implicit def uriWrites = new Writes[URI] {
    override def writes(uri: URI): JsValue = JsString(uri.toASCIIString)
  }

  implicit def uriReads = new Reads[URI] {
    override def reads(json: JsValue): JsResult[URI] = json.validate[String].map(URI.create)
  }

}
