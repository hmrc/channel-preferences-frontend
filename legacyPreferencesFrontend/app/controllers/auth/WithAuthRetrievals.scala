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

package controllers.auth

import org.joda.time.DateTime
import play.api.mvc.{ Request, Result, Results }
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ ExecutionContext, Future }

trait WithAuthRetrievals extends AuthorisedFunctions {
  def withAuthenticatedRequest[A](
    block: AuthenticatedRequest[A] => HeaderCarrier => Future[Result]
  )(implicit request: Request[A], ec: ExecutionContext) = {
    implicit val hc = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
    authorised().retrieve(
      Retrievals.name and Retrievals.loginTimes and Retrievals.nino and Retrievals.saUtr and
        Retrievals.affinityGroup and Retrievals.confidenceLevel
    ) {
      case name ~ retLoginTimes ~ nino ~ utr ~ affinityGroup ~ confidenceLevel =>
        val previousLoginTime: Option[DateTime] = retLoginTimes.previousLogin
        val fullName = name.map { n =>
          (n.name, n.lastName) match {
            case (None, None)       => None
            case (None, Some(l))    => Some(l)
            case (Some(n), None)    => Some(n)
            case (Some(n), Some(l)) => Some(s"$n $l")
          }
        }.flatten
        block(
          AuthenticatedRequest[A](request, fullName, previousLoginTime, nino, utr, affinityGroup, Some(confidenceLevel))
        )(hc)
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
