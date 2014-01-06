package uk.gov.hmrc.common.microservice.preferences

import uk.gov.hmrc.common.microservice.{Connector, MicroServiceConfig}

import uk.gov.hmrc.domain.SaUtr
import controllers.common.actions.HeaderCarrier
import scala.concurrent.Future
import java.net.URI


class PreferencesConnector extends Connector {

  override val serviceUrl = MicroServiceConfig.preferencesServiceUrl

  def savePreferences(utr: SaUtr, digital: Boolean, email: Option[String] = None)(implicit hc: HeaderCarrier) : Future[Option[FormattedUri]] = {
    httpPostF[UpdateEmail, FormattedUri](s"/preferences/sa/individual/$utr/print-suppression", UpdateEmail(digital, email))
  }

  def getPreferences(utr: SaUtr)(implicit headerCarrier:HeaderCarrier): Future[Option[SaPreference]] = {
    httpGetF[SaPreference](s"/preferences/sa/individual/$utr/print-suppression")
  }
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