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

package controllers.filing

import com.netaporter.uri.config.UriConfig
import com.netaporter.uri.dsl._
import com.netaporter.uri.encoding._
import connectors.EntityResolverConnector
import javax.inject.Inject
import model.Encrypted
import play.api.Configuration
import play.api.mvc._
import uk.gov.hmrc.crypto.PlainText
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import uk.gov.hmrc.play.bootstrap.config.RunMode
import uk.gov.hmrc.play.bootstrap.controller.FrontendController

import scala.concurrent.ExecutionContext

class FilingInterceptController @Inject()(
  entityResolverConnector: EntityResolverConnector,
  configuration: Configuration,
  runMode: RunMode,
  decryptAndValidate: DecryptAndValidate,
  tokenEncryption: TokenEncryption,
  mcc: MessagesControllerComponents)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) {

  lazy val redirectDomainWhiteList = configuration
    .getOptional[Seq[String]](s"govuk-tax.${runMode.env}.portal.redirectDomainWhiteList")
    .getOrElse(List())
    .toSet
  implicit val wl: Set[String] = redirectDomainWhiteList
  implicit val config = UriConfig(encoder = percentEncode)

  def redirectWithEmailAddress(
    encryptedToken: String,
    encodedReturnUrl: String,
    emailAddressToPrefill: Option[Encrypted[EmailAddress]]) =
    DecodeAndWhitelist(encodedReturnUrl) { returnUrl =>
      decryptAndValidate(encryptedToken, returnUrl) { token =>
        Action.async { implicit request =>
          implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers)
          val utr = token.utr
          entityResolverConnector.getEmailAddress(utr) map {
            case Some(emailAddress) =>
              Redirect(returnUrl ? ("email" -> tokenEncryption.encrypt(PlainText(emailAddress)).value))
            case _ =>
              Redirect(returnUrl)
          }
        }
      }
    }
}
