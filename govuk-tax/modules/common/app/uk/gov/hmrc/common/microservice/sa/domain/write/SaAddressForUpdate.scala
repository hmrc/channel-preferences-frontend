package uk.gov.hmrc.common.microservice.sa.domain.write

case class SaAddressForUpdate(
  additionalDeliveryInfo: Option[String],
  addressLine1: String,
  addressLine2: String,
  addressLine3: Option[String],
  addressLine4: Option[String],
  postcode: Option[String])
