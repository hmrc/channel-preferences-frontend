/*
 * Copyright 2021 HM Revenue & Customs
 *
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

  lazy val redirectDomainAllowlist = configuration
    .getOptional[Seq[String]](s"govuk-tax.${runMode.env}.portal.redirectDomainAllowlist")
    .getOrElse(List())
    .toSet
  implicit val wl: Set[String] = redirectDomainAllowlist
  implicit val config = UriConfig(encoder = percentEncode)

  def redirectWithEmailAddress(
    encryptedToken: String,
    encodedReturnUrl: String,
    emailAddressToPrefill: Option[Encrypted[EmailAddress]]) =
    DecodeAndAllowlist(encodedReturnUrl) { returnUrl =>
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
