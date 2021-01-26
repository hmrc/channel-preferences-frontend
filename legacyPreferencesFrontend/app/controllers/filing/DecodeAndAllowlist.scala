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

package controllers.filing

import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import play.api.Logger
import play.api.mvc._

import java.net.URLDecoder
import scala.concurrent.Future

private[filing] object DecodeAndAllowlist extends Results {
  def apply(encodedReturnUrl: String)(action: (Uri => Action[AnyContent]))(implicit allowedDomains: Set[String]) =
    Action.async { request: Request[AnyContent] =>
      val decodedReturnUrl: Uri = URLDecoder.decode(encodedReturnUrl, "UTF-8")

      if (decodedReturnUrl.host.exists(h => allowedDomains.exists(h.endsWith)))
        action(decodedReturnUrl)(request)
      else {
        Logger.debug(s"Return URL '$encodedReturnUrl' was invalid as it was not on the allowlist")
        Future.successful(BadRequest)
      }
    }
}
