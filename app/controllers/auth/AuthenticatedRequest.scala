/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package controllers.auth

import org.joda.time.DateTime
import play.api.mvc.{ Request, WrappedRequest }
import uk.gov.hmrc.auth.core.{ AffinityGroup, ConfidenceLevel }

case class AuthenticatedRequest[A](
  request: Request[A],
  fullName: Option[String],
  previousLoginTime: Option[DateTime],
  nino: Option[String],
  saUtr: Option[String],
  affinityGroup: Option[AffinityGroup] = None,
  confidenceLevel: Option[ConfidenceLevel] = None
) extends WrappedRequest[A](request)
