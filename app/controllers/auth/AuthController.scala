/*
 * Copyright 2019 HM Revenue & Customs
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

package controllers.auth

import javax.inject.Inject
import org.joda.time.DateTime
import play.api.mvc.{ Request, Result, Results, _ }
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals

import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.{ HeaderCarrier, Request => _ }
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ ExecutionContext, Future }

class PreferenceFrontendAuthActionImpl @Inject()(
  override val authConnector: AuthConnector,
  val parser: BodyParsers.Default)(implicit val executionContext: ExecutionContext)
    extends PreferenceFrontendAuthAction with AuthorisedFunctions {
  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromHeadersAndSession(request.headers)
    authorised()
      .retrieve(Retrievals.name and Retrievals.loginTimes and Retrievals.nino and Retrievals.saUtr) {
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
          block(AuthenticatedRequest(request, fullName, previousLoginTime, nino, utr))
        }
        case _ => Future.successful(Results.Unauthorized)
      } recover {
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
}

trait PreferenceFrontendAuthAction
    extends ActionBuilder[AuthenticatedRequest, AnyContent] with ActionFunction[Request, AuthenticatedRequest]

trait AuthAction
    extends ActionBuilder[AuthenticatedRequest, AnyContent] with ActionFunction[Request, AuthenticatedRequest]

case class AuthenticatedRequest[A](
  request: Request[A],
  fullName: Option[String],
  previousLoginTime: Option[DateTime],
  nino: Option[String],
  saUtr: Option[String])
    extends WrappedRequest[A](request)

class MCIUserException extends Exception
