package uk.gov.hmrc.microservice.sa

import play.Logger
import uk.gov.hmrc.microservice.sa.domain.{ TransactionId, SaRoot, SaPerson }
import uk.gov.hmrc.microservice.{ MicroService, MicroServiceConfig }
import uk.gov.hmrc.microservice.auth.domain.Utr
import uk.gov.hmrc.microservice.sa.domain.TransactionId
import play.api.libs.json.Json
import controllers.common.domain.Transform._
import uk.gov.hmrc.microservice.sa.domain.SaPerson
import uk.gov.hmrc.microservice.sa.domain.SaRoot
import uk.gov.hmrc.microservice.auth.domain.Utr
import uk.gov.hmrc.microservice.sa.domain.TransactionId

case class MainAddress(additionalDeliveryInfo: Option[String], addressLine1: Option[String], addressLine2: Option[String],
  addressLine3: Option[String], addressLine4: Option[String], postcode: Option[String])

class SaMicroService extends MicroService {

  override val serviceUrl = MicroServiceConfig.saServiceUrl

  def root(uri: String): SaRoot = httpGet[SaRoot](uri).getOrElse(throw new IllegalStateException(s"Expected SA root not found at URI '$uri'"))
  def person(uri: String): Option[SaPerson] = httpGet[SaPerson](uri)

  def linkedResource[T](uri: String)(implicit m: Manifest[T]) = {
    Logger.debug(s"Loading linked sa resource, uri: $uri")
    httpGet[T](uri)
  }

  def updateMainAddress(updateAddressUri: String, addtionalDeliveryInfo: Option[String], addressLine1: String, addressLine2: String,
    addressLine3: Option[String], addressLine4: Option[String], postcode: Option[String]): Option[TransactionId] = {

    httpPost[TransactionId](
      uri = updateAddressUri,
      body = Json.parse(
        toRequestBody(
          MainAddress(
            additionalDeliveryInfo = addtionalDeliveryInfo,
            addressLine1 = Some(addressLine1),
            addressLine2 = Some(addressLine2),
            addressLine3 = addressLine3,
            addressLine4 = addressLine4,
            postcode = postcode
          )
        )
      )
    )

  }
}
