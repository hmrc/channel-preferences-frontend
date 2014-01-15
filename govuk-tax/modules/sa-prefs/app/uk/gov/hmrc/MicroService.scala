package uk.gov.hmrc

import scala.concurrent.Future
import play.api.libs.json.Json
import java.net.URLEncoder
import uk.gov.hmrc.EmailVerificationLinkResponse.EmailVerificationLinkResponse
import uk.gov.hmrc.common.microservice.{MicroServiceConfig, MicroServiceException, Connector}
import controllers.common.actions.HeaderCarrier
import controllers.common.domain.Transform._
import uk.gov.hmrc.common.microservice.preferences.ValidateEmail

object EmailVerificationLinkResponse extends Enumeration {
  type EmailVerificationLinkResponse = Value

  val OK, EXPIRED, ERROR = Value
}

class PreferencesConnector extends Connector {

  override val serviceUrl = MicroServiceConfig.preferencesServiceUrl

  def savePreferences(utr: String, digital: Boolean, email: Option[String] = None)(implicit hc: HeaderCarrier) =
    httpPostF(s"/portal/preferences/sa/individual/$utr/print-suppression", Some(Json.parse(toRequestBody(SaPreference(digital, email)))))

  def getPreferences(utr: String)(implicit hc: HeaderCarrier): Future[Option[SaPreference]] =
    httpGetF[SaPreference](s"/portal/preferences/sa/individual/$utr/print-suppression")
//      .map(_.orElse(throw new RuntimeException(s"Access to resource: '/portal/preferences/sa/individual/$utr/print-suppression' gave an inconsistent response")))
//      .recover { case MicroServiceException(errorMessage, response) if response.status == 404 => None}

  def updateEmailValidationStatus(token: String)(implicit hc: HeaderCarrier): Future[EmailVerificationLinkResponse] = {
    httpPost("/preferences/sa/verify-email", Json.parse(toRequestBody(ValidateEmail(token)))) {
      _.status match {
        case s if OK to MULTI_STATUS contains s => EmailVerificationLinkResponse.OK
        case GONE => EmailVerificationLinkResponse.EXPIRED
        case _ => EmailVerificationLinkResponse.ERROR
      }
    }
  }
}

case class SaPreference(digital: Boolean, email: Option[String] = None)

