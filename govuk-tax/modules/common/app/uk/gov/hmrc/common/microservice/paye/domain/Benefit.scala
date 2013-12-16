package uk.gov.hmrc.common.microservice.paye.domain

import org.joda.time.LocalDate
import scala._


case class Benefit(benefitType: Int,
                   taxYear: Int,
                   grossAmount: BigDecimal,
                   employmentSequenceNumber: Int,
                   costAmount: Option[BigDecimal] = None,
                   amountMadeGood: Option[BigDecimal] = None,
                   cashEquivalent: Option[BigDecimal]= None,
                   expensesIncurred: Option[BigDecimal]= None,
                   amountOfRelief: Option[BigDecimal]= None,
                   paymentOrBenefitDescription: Option[String]= None,
                   dateWithdrawn: Option[LocalDate]= None,
                   car: Option[Car]= None,
                   actions: Map[String, String]= Map.empty,
                   calculations: Map[String, String]= Map.empty,
                   benefitAmount: Option[BigDecimal] = None,
                   forecastAmount: Option[Int] = None) {

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
  require(carBenefit.car.isDefined, "Car benefit does not have a Car part defined")
  require(carBenefit.benefitType == BenefitTypes.CAR, s"Car benefit has incorrect type ${carBenefit.benefitType}, should be ${BenefitTypes.CAR}")
  require(!fuelBenefit.exists(_.benefitType != BenefitTypes.FUEL))

  def toSeq: Seq[Benefit] = Seq(Some(carBenefit), fuelBenefit).flatten

  // our constraints say that carBenefit.car must be Some, so naked get
  // should be okay. If it isn't it means something is wrong, so exception
  // is the only way out
  def isActive: Boolean = carBenefit.car.map(_.dateCarWithdrawn.isEmpty).get
}