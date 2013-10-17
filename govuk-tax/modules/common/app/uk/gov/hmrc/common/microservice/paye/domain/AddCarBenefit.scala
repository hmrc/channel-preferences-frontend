package uk.gov.hmrc.common.microservice.paye.domain

import org.joda.time.LocalDate


case class AddCarBenefit(carRegisteredBefore98: Boolean, fuelType: String, co2Emission: Option[Int], engineCapacity: Option[Int])

case class AddCarBenefitResponse(percentage: Int)

case class AddCarBenefitConfirmationData(employerName: Option[String],
                                         providedFrom: LocalDate,
                                         listPrice: Int,
                                         fuelType: String,
                                         co2Figure: Option[Int],
                                         engineCapacity: Option[String],
                                         employerPayFuel: Option[String],
                                         dateFuelWithdrawn: Option[LocalDate],
                                         carBenefitValue: Option[BenefitValue],
                                         carFuelBenefitValue: Option[BenefitValue])

case class BenefitValue(taxableValue: Int) {
  val basicRateValue = (taxableValue * 0.2).toInt
  val higherRateValue = (taxableValue * 0.4).toInt
  val additionalRateValue = (taxableValue * 0.45).toInt
}


