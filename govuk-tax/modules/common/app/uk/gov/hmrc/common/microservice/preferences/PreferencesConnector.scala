package uk.gov.hmrc.common.microservice.preferences

import uk.gov.hmrc.microservice.{Connector, MicroServiceConfig}

import controllers.common.domain.Transform._
import play.api.libs.json.Json
import uk.gov.hmrc.domain.SaUtr
import controllers.common.actions.HeaderCarrier
import scala.concurrent.Future


class PreferencesConnector extends Connector {

  override val serviceUrl = MicroServiceConfig.preferencesServiceUrl

  def savePreferences(utr: SaUtr, digital: Boolean, email: Option[String] = None)(implicit hc: HeaderCarrier) {
    httpPostAndForget(s"/preferences/sa/individual/$utr/print-suppression", Json.parse(toRequestBody(SaPreference(digital, email))))
  }

  def getPreferences(utr: SaUtr)(implicit headerCarrier:HeaderCarrier): Future[Option[SaPreference]] = {
    httpGetF[SaPreference](s"/preferences/sa/individual/$utr/print-suppression")
  }
}

case class ValidateEmail(token: String)
case class SaPreference(digital: Boolean, email: Option[String] = None)

