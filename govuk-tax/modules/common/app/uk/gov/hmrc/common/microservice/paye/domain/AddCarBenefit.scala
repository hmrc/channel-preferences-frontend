package uk.gov.hmrc.common.microservice.paye.domain


case class AddCarBenefit(carRegisteredBefore98: Boolean, fuelType: String, co2Emission: Option[Int], engineCapacity: Option[Int])

case class AddCarBenefitResponse(percentage: Int)