package uk.gov.hmrc.common.microservice.paye.domain

import org.joda.time.LocalDate
import scala._
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import org.joda.time.format.DateTimeFormat

@deprecated("Use CarBenefit and FuelBenefit instead", "20/12/2013")
case class Benefit(benefitType: Int,
                   taxYear: Int,
                   grossAmount: BigDecimal,
                   employmentSequenceNumber: Int,
                   costAmount: Option[BigDecimal] = None,
                   amountMadeGood: Option[BigDecimal] = None,
                   cashEquivalent: Option[BigDecimal] = None,
                   expensesIncurred: Option[BigDecimal] = None,
                   amountOfRelief: Option[BigDecimal] = None,
                   paymentOrBenefitDescription: Option[String] = None,
                   dateWithdrawn: Option[LocalDate] = None,
                   car: Option[Car] = None,
                   actions: Map[String, String] = Map.empty,
                   calculations: Map[String, String] = Map.empty,
                   benefitAmount: Option[BigDecimal] = None,
                   forecastAmount: Option[Int] = None) {

  def getStartDate(startOfCurrentTaxYear: LocalDate): LocalDate = {
    val dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd")
    val dateRegex = """(\d\d\d\d-\d\d-\d\d)""".r

    val pathIncludingStartDate = calculations.get(PayeConnector.calculationWithdrawKey).getOrElse("")

    val benefitStartDate = dateRegex.findFirstIn(pathIncludingStartDate) map {
      dateFormat.parseLocalDate
    }

    benefitStartDate match {
      case Some(dateOfBenefitStart) if dateOfBenefitStart.isAfter(startOfCurrentTaxYear) => dateOfBenefitStart
      case _ => startOfCurrentTaxYear
    }
  }
}

object Benefit {
  def findByTypeAndEmploymentNumber(benefits: Seq[Benefit], employmentSequenceNumber: Int, benefitType: Int): Option[Benefit] = {
    benefits.find(b => b.employmentSequenceNumber == employmentSequenceNumber && b.benefitType == benefitType)
  }

}

@deprecated("Use CarBenefit instead", "20/12/2013")
case class Car(dateCarMadeAvailable: Option[LocalDate] = None,
               dateCarWithdrawn: Option[LocalDate] = None,
               dateCarRegistered: Questionable[LocalDate],
               employeeCapitalContribution: Option[BigDecimal] = None,
               fuelType: Option[String] = None,
               co2Emissions: Option[Int] = None,
               engineSize: Option[Int] = None,
               mileageBand: Option[String] = None,
               carValue: Option[BigDecimal] = None,
               employeePayments: Option[BigDecimal] = None,
               daysUnavailable: Option[Int] = None)

case class Questionable[A](value: A, displayable: Boolean)

object BenefitTypes {
  val FUEL = 29
  val CAR = 31
  val TELEPHONE = 32
}

@deprecated("Use CarBenefit and FuelBenefit instead", "20/12/2013")
case class CarAndFuel(carBenefit: Benefit, fuelBenefit: Option[Benefit] = None) {
  require(carBenefit.car.isDefined, "Car benefit does not have a Car part defined")
  require(carBenefit.benefitType == BenefitTypes.CAR, s"Car benefit has incorrect type ${carBenefit.benefitType}, should be ${BenefitTypes.CAR}")
  require(!fuelBenefit.exists(_.benefitType != BenefitTypes.FUEL))

  def toSeq: Seq[Benefit] = Seq(Some(carBenefit), fuelBenefit).flatten

  def isActive: Boolean = carBenefit.car.isDefined && !carBenefit.car.get.dateCarWithdrawn.isDefined

  def hasActiveFuel: Boolean = fuelBenefit.isDefined && !fuelBenefit.get.dateWithdrawn.isDefined
}