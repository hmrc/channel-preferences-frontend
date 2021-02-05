/*
 * Copyright 2021 HM Revenue & Customs
 *
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
