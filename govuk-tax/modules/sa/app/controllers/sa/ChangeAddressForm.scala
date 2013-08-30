package controllers.sa

import uk.gov.hmrc.common.microservice.sa.domain.write.SaAddressForUpdate

case class ChangeAddressForm(
    addressLine1: Option[String],
    addressLine2: Option[String],
    addressLine3: Option[String],
    addressLine4: Option[String],
    postcode: Option[String],
    additionalDeliveryInformation: Option[String]) {

  lazy val toUpdateAddress = {
    if (addressLine1 == None) throw new IllegalStateException("Address line 1 is missing")
    if (addressLine2 == None) throw new IllegalStateException("Address line 2 is missing")

    SaAddressForUpdate(
      addressLine1 = addressLine1.get,
      addressLine2 = addressLine2.get,
      addressLine3 = addressLine3,
      addressLine4 = addressLine4,
      postcode = postcode,
      additionalDeliveryInformation = additionalDeliveryInformation
    )
  }
}