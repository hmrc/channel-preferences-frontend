package uk.gov.hmrc.common.microservice.domain

case class Address(addressLine1: String = "",
  addressLine2: Option[String] = None,
  addressLine3: Option[String] = None,
  addressLine4: Option[String] = None,
  postcode: Option[String] = None)