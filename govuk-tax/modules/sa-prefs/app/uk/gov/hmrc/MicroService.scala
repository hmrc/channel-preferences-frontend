package uk.gov.hmrc

import scala.concurrent.duration.Duration
import uk.gov.hmrc.Transform._
import play.api.Play
import scala.concurrent.Future
import play.api.libs.json.Json
import java.net.URLEncoder
import uk.gov.hmrc.EmailVerificationLinkResponse.EmailVerificationLinkResponse
import uk.gov.hmrc.common.microservice.{MicroServiceConfig, MicroServiceException, Connector}
import controllers.common.actions.HeaderCarrier


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
      .map(_.orElse(throw new RuntimeException(s"Access to resource: '/portal/preferences/sa/individual/$utr/print-suppression' gave an inconsistent response")))
      .recover { case MicroServiceException(errorMessage, response) if response.status == 404 => None}

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

class EmailConnector() extends Connector {

  protected val serviceUrl = MicroServiceConfig.emailServiceUrl

  def validateEmailAddress(emailAddress: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    httpGetF[ValidateEmailResponse](s"/validate-email-address?email=${URLEncoder.encode(emailAddress, "UTF-8")}") map
      (_.getOrElse(throw new RuntimeException(s"Access to resource: '/validate-email-address' gave an invalid response")).valid)
  }
}

case class ValidateEmailResponse(valid: Boolean)

case class ValidateEmail(token: String)

case class SaPreference(digital: Boolean, email: Option[String] = None)

