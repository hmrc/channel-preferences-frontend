/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers.auth

import org.joda.time.DateTime
import play.api.mvc.{ Request, WrappedRequest }

case class AuthenticatedRequest[A](
  request: Request[A],
  fullName: Option[String],
  previousLoginTime: Option[DateTime],
  nino: Option[String],
  saUtr: Option[String])
    extends WrappedRequest[A](request)
