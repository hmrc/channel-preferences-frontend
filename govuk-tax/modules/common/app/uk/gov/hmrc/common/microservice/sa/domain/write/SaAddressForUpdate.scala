package uk.gov.hmrc.common.microservice.sa.domain.write

case class SaAddressForUpdate(
  addressLine1: String,
  addressLine2: String,
  addressLine3: Option[String],
  addressLine4: Option[String],
  postcode: Option[String],
  additionalDeliveryInformation: Option[String])
