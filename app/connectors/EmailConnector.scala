/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package connectors

import config.ServicesCircuitBreaker
import play.api.libs.json._
import play.api.{ Configuration, Environment, Logger }
import uk.gov.hmrc.http.{ HeaderCarrier, HttpClient }
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class EmailConnector @Inject() (http: HttpClient, config: Configuration, env: Environment)(implicit
  ec: ExecutionContext
) extends ServicesConfig(config) with ServicesCircuitBreaker {
  protected def serviceUrl: String = baseUrl("email")

  override val externalServiceName = "email"

  def isValid(emailAddress: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    implicit val readValidBoolean = (__ \ "valid").read[Boolean]
    withCircuitBreaker(
      http.POST[UpdateEmail, Boolean](s"$serviceUrl/validate-email-address", UpdateEmail(emailAddress)) recover {
        case e =>
          Logger.error(s"Could not contact EMAIL service and validate email address for $emailAddress: ${e.getMessage}")
          false
      }
    )
  }
}
