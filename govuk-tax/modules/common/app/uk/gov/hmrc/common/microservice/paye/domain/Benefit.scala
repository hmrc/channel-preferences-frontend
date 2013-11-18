package uk.gov.hmrc.common.microservice.paye.domain

import org.joda.time.LocalDate
import scala._


case class Benefit(benefitType: Int,
                   taxYear: Int,
                   grossAmount: BigDecimal,
                   employmentSequenceNumber: Int,
                   costAmount: Option[BigDecimal],
                   amountMadeGood: Option[BigDecimal],
                   cashEquivalent: Option[BigDecimal],
                   expensesIncurred: Option[BigDecimal],
                   amountOfRelief: Option[BigDecimal],
                   paymentOrBenefitDescription: Option[String],
                   dateWithdrawn: Option[LocalDate],
                   car: Option[Car],
                   actions: Map[String, String],
                   calculations: Map[String, String]) {

}

object Benefit {
  def findByTypeAndEmploymentNumber(benefits: Seq[Benefit], employmentSequenceNumber: Int, benefitType: Int): Option[Benefit] = {
    benefits.find(b => b.employmentSequenceNumber == employmentSequenceNumber && b.benefitType == benefitType)
  }

}

case class Car(dateCarMadeAvailable: Option[LocalDate],
               dateCarWithdrawn: Option[LocalDate],
               dateCarRegistered: Option[LocalDate],
               employeeCapitalContribution: Option[BigDecimal],
               fuelType: Option[String],
               co2Emissions: Option[Int],
               engineSize: Option[Int],
               mileageBand: Option[String],
               carValue: Option[BigDecimal],
               employeePayments: Option[BigDecimal],
               daysUnavailable: Option[Int]
                )

object BenefitTypes {

  val FUEL = 29
  val CAR = 31
  val TELEPHONE = 32

}

case class CarAndFuel(carBenefit  :Benefit , fuelBenefit : Option[Benefit]) {
  require(carBenefit.car.isDefined && carBenefit.benefitType == BenefitTypes.CAR && !fuelBenefit.exists(_.benefitType != BenefitTypes.FUEL))
}