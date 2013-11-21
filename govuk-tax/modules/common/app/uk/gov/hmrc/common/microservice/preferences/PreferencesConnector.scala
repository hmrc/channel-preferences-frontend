package uk.gov.hmrc.common.microservice.preferences

import uk.gov.hmrc.microservice.{MicroServiceException, Connector, MicroServiceConfig}

import controllers.common.domain.Transform._
import play.api.libs.json.Json
import uk.gov.hmrc.domain.SaUtr
import controllers.common.actions.HeaderCarrier


class PreferencesConnector extends Connector {

  override val serviceUrl = MicroServiceConfig.preferencesServiceUrl

  def savePreferences(utr: SaUtr, digital: Boolean, email: Option[String] = None) {
    httpPostAndForget(s"/preferences/sa/individual/$utr/print-suppression", Json.parse(toRequestBody(SaPreference(digital, email))))
  }

  def getPreferences(utr: SaUtr)(implicit headerCarrier:HeaderCarrier): Option[SaPreference] = {
    httpGetHC[SaPreference](s"/preferences/sa/individual/$utr/print-suppression")
  }
//
//  def updateEmailValidationStatus(token : String) : Boolean = {
//    val response = httpPostSynchronous("/preferences/sa/verifyEmailAndSuppressPrint", Json.parse(toRequestBody(ValidateEmail(token))))
//    return response.status < 300 && response.status >= 200
//  }
}

case class ValidateEmail(token: String)
case class SaPreference(digital: Boolean, email: Option[String] = None)

