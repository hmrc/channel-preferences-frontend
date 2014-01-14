package uk.gov.hmrc.common.microservice.paye.domain

import org.joda.time.LocalDate


case class AddCarBenefit(carRegisteredBefore98: Boolean, fuelType: String, co2Emission: Option[Int], engineCapacity: Option[Int])

case class AddCarBenefitResponse(percentage: Int)

case class AddCarBenefitConfirmationData(employerName: String,
                                         providedFrom: LocalDate,
                                         listPrice: Int,
                                         fuelType: String,
                                         co2Figure: Option[Int],
                                         engineCapacity: Option[String],
                                         employerPayFuel: Option[Boolean],
                                         employeeContributions: Option[Int],
                                         dateRegistered: Option[LocalDate],
                                         privateUsePayments: Option[Int])

object AddCarBenefitConfirmationData {
  val fuelTypeElectric = "electricity"

  def convertEmployerPayFuel(fuelType: Option[String], employerPayFuel: Option[String]): Option[Boolean] = {
    fuelType match {
      case Some(s) if s == fuelTypeElectric => None
      case _ => employerPayFuel match {
        case Some(s) if s == "true" => Some(true)
        case Some(s) if s == "false" => Some(false)
        case _ => None
      }
    }
  }
}