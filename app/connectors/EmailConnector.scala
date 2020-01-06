/*
 * Copyright 2020 HM Revenue & Customs
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

package connectors

import config.ServicesCircuitBreaker
import javax.inject.{ Inject, Singleton }
import play.api.libs.json._
import play.api.{ Configuration, Logger }
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.{ RunMode, ServicesConfig }
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ ExecutionContext, Future }

@Singleton
class EmailConnector @Inject()(http: HttpClient, config: Configuration, runMode: RunMode)(implicit ec: ExecutionContext)
    extends ServicesConfig(config, runMode) with ServicesCircuitBreaker {
  protected def serviceUrl: String = baseUrl("email")

  override val externalServiceName = "email"

  def isValid(emailAddress: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    implicit val readValidBoolean = (__ \ "valid").read[Boolean]
    withCircuitBreaker(
      http.POST[UpdateEmail, Boolean](s"$serviceUrl/validate-email-address", UpdateEmail(emailAddress)) recover {
        case e => {
          Logger.error(s"Could not contact EMAIL service and validate email address for $emailAddress: ${e.getMessage}")
          false
        }
      })
  }
}
