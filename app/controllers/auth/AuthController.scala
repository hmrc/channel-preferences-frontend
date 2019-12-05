/*
 * Copyright 2018 HM Revenue & Customs
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

import akka.actor.ActorSystem
import com.typesafe.config.Config
import org.joda.time.DateTime
import play.api.Mode.Mode
import play.api.mvc.{Request, Result, Results, _}
import play.api.{Configuration, Play}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.{Retrievals, ~}
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, HttpPost, HttpPut, Request => _, _}
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.config.{AppName, RunMode, ServicesConfig}
import uk.gov.hmrc.play.http.ws.{WSDelete, WSGet, WSPost, WSPut}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait AuthController extends AuthorisedFunctions with AuthAction {
  def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))
    authorised().retrieve(Retrievals.name and Retrievals.loginTimes and Retrievals.nino and Retrievals.saUtr) {
      case name ~ retLoginTimes ~ nino ~ utr => {
        val previousLoginTime: Option[DateTime] = retLoginTimes.previousLogin
        val fullName = (name.name, name.lastName) match {
          case (None, None) => None
          case (None, Some(l)) => Some(l)
          case (Some(n), None) => Some(n)
          case (Some(n), Some(l)) => Some(s"${n} ${l}")
        }
        block(AuthenticatedRequest(request, fullName, previousLoginTime, nino, utr))
      }
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
      case e => throw e
    }
  }
}

object AuthController extends AuthController {
  val authConnector = AuthConnector
}

object AuthConnector extends PlayAuthConnector with ServicesConfig {

  val serviceUrl: String =baseUrl("auth")
  def http: CorePost = WSHttp

  override protected def mode: Mode = Play.current.mode

  override protected def runModeConfiguration: Configuration = Play.current.configuration
}

trait WSHttp extends HttpGet with WSGet with HttpPut with WSPut with HttpPost with WSPost with HttpDelete with WSDelete with AppName with HttpAuditing with RunMode


object WSHttp extends WSHttp {
  override val auditConnector = config.Audit
  override val hooks = Seq(AuditingHook)

  override protected def actorSystem: ActorSystem = Play.current.actorSystem

  override protected def configuration: Option[Config] = Some(Play.current.configuration.underlying)

  override protected def appNameConfiguration: Configuration = Play.current.configuration

  override protected def mode: Mode = Play.current.mode

  override protected def runModeConfiguration: Configuration = Play.current.configuration
}

trait AuthAction extends ActionBuilder[AuthenticatedRequest] with ActionFunction[Request, AuthenticatedRequest]


case class AuthenticatedRequest[A](request: Request[A], fullName: Option[String], previousLoginTime: Option[DateTime], nino: Option[String], saUtr: Option[String]) extends WrappedRequest[A](request)

class MCIUserException extends Exception
