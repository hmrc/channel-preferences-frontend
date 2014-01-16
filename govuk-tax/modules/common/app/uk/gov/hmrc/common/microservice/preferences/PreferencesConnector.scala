package uk.gov.hmrc.common.microservice.preferences

import uk.gov.hmrc.common.microservice.{Connector, MicroServiceConfig}

import uk.gov.hmrc.domain.SaUtr
import controllers.common.actions.HeaderCarrier
import scala.concurrent.Future
import java.net.URI


class PreferencesConnector extends Connector {

  override val serviceUrl = MicroServiceConfig.preferencesServiceUrl

  def savePreferences(utr: SaUtr, digital: Boolean, email: Option[String] = None)(implicit hc: HeaderCarrier): Future[Option[FormattedUri]] = {
    httpPostF[FormattedUri, UpdateEmail](s"/preferences/sa/individual/$utr/print-suppression", Some(UpdateEmail(digital, email)))
  }

  def getPreferences(utr: SaUtr)(implicit headerCarrier: HeaderCarrier): Future[Option[SaPreference]] = {
    httpGetF[SaPreference](s"/preferences/sa/individual/$utr/print-suppression")
  }

  def savePreferencesUnsecured(utr: String, digital: Boolean, email: Option[String] = None)(implicit hc: HeaderCarrier) =
    httpPostF(s"/portal/preferences/sa/individual/$utr/print-suppression", Some(SaPreferenceSimplified(digital, email)))

  def getPreferencesUnsecured(utr: String)(implicit hc: HeaderCarrier): Future[Option[SaPreferenceSimplified]] =
    httpGetF[SaPreferenceSimplified](s"/portal/preferences/sa/individual/$utr/print-suppression")

  def updateEmailValidationStatusUnsecured(token: String)(implicit hc: HeaderCarrier): Future[EmailVerificationLinkResponse.Value] = {
    httpPost("/preferences/sa/verify-email", ValidateEmail(token)) {
      _.status match {
        case s if OK to MULTI_STATUS contains s => EmailVerificationLinkResponse.OK
        case GONE => EmailVerificationLinkResponse.EXPIRED
        case _ => EmailVerificationLinkResponse.ERROR
      }
    }
  }

}

case class SaPreferenceSimplified(digital: Boolean, email: Option[String] = None)

object EmailVerificationLinkResponse extends Enumeration {
  type EmailVerificationLinkResponse = Value

  val OK, EXPIRED, ERROR = Value
}

case class UpdateEmail(digital: Boolean, email: Option[String])

case class SaEmailPreference(email: String, status: String, message: Option[String] = None)

object SaEmailPreference {

  object Status {
    val pending = "pending"
    val bounced = "bounced"
    val verified = "verified"
  }

}

case class SaPreference(digital: Boolean, email: Option[SaEmailPreference] = None)

case class ValidateEmail(token: String)

case class FormattedUri(uri: URI)