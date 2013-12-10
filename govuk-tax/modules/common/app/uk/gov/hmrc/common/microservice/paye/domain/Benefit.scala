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

case class Car(dateCarMadeAvailable: Option[LocalDate] = None,
               dateCarWithdrawn: Option[LocalDate] = None,
               dateCarRegistered: Option[LocalDate] = None,
               employeeCapitalContribution: Option[BigDecimal] = None,
               fuelType: Option[String] = None,
               co2Emissions: Option[Int] = None,
               engineSize: Option[Int] = None,
               mileageBand: Option[String] = None,
               carValue: Option[BigDecimal] = None,
               employeePayments: Option[BigDecimal] = None,
               daysUnavailable: Option[Int] = None)

object BenefitTypes {
  val FUEL = 29
  val CAR = 31
  val TELEPHONE = 32
}

case class CarAndFuel(carBenefit: Benefit, fuelBenefit: Option[Benefit] = None) {
  require(carBenefit.car.isDefined && carBenefit.benefitType == BenefitTypes.CAR && !fuelBenefit.exists(_.benefitType != BenefitTypes.FUEL))

  def toSeq: Seq[Benefit] = Seq(Some(carBenefit), fuelBenefit).flatten

  def isActive: Boolean = carBenefit.dateWithdrawn.isEmpty
}