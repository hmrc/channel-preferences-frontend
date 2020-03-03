/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

package controllers.auth

import org.joda.time.DateTime
import play.api.Logger
import play.api.mvc.{ Cookies, Request, Result, Results }
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ ExecutionContext, Future }

trait WithAuthRetrievals extends AuthorisedFunctions {
  def withAuthenticatedRequest[A](block: AuthenticatedRequest[A] => HeaderCarrier => Future[Result])(
    implicit request: Request[A],
    ec: ExecutionContext) = {
    implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
    authorised().retrieve(Retrievals.name and Retrievals.loginTimes and Retrievals.nino and Retrievals.saUtr) {
      case name ~ retLoginTimes ~ nino ~ utr => {
        val previousLoginTime: Option[DateTime] = retLoginTimes.previousLogin
        val fullName = name.map { n =>
          (n.name, n.lastName) match {
            case (None, None)       => None
            case (None, Some(l))    => Some(l)
            case (Some(n), None)    => Some(n)
            case (Some(n), Some(l)) => Some(s"$n $l")
          }
        }.flatten
        block(AuthenticatedRequest[A](request, fullName, previousLoginTime, nino, utr))(hc)
      }
      case _ => Future.successful(Results.Unauthorized)
    }
  }.recover {
    case _: InsufficientConfidenceLevel => Results.Unauthorized
    case _: UnsupportedAffinityGroup    => Results.Unauthorized
    case _: UnsupportedCredentialRole   => Results.Unauthorized
    case _: UnsupportedAuthProvider     => Results.Unauthorized
    case _: BearerTokenExpired          => Results.Unauthorized
    case _: MissingBearerToken          => Results.Unauthorized
    case _: InvalidBearerToken          => Results.Unauthorized
    case _: SessionRecordNotFound       => Results.Unauthorized
    case _: IncorrectCredentialStrength => Results.Unauthorized
    case _: InsufficientEnrolments      => Results.Unauthorized
    case e                              => throw e
  }

}
