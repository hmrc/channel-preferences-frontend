package controllers.paye

import org.scalatest.concurrent.ScalaFutures
import controllers.DateFieldsHelper
import models.paye.{CarBenefitData, RemoveCarBenefitFormData, ReplaceCarBenefitFormData}
import org.joda.time.LocalDate
import uk.gov.hmrc.common.BaseSpec

import uk.gov.hmrc.common.microservice.paye.domain.BenefitTypes

class ReplaceCarBenefitConfirmControllerSpec extends BaseSpec with ScalaFutures with DateFieldsHelper {

  "buildRequest" should {
    import controllers.paye.ReplaceCarBenefitConfirmController.buildRequest
    import testData._

    val request = buildRequest(version, formData, taxYear, employmentSequenceNumber)

    "populate the WithdrawnBenefitRequest version correctly" in {
      request.withdrawBenefitRequest.version shouldBe version
    }

    {
      val car = request.withdrawBenefitRequest.car

      "car should be present" in {
        car shouldNot be(None)
      }

      "populate the WithdrawnCarBenefit car withdrawDate correctly" in {
        car.get.withdrawDate shouldBe dateCarWithdrawn
      }

      "populate the WithdrawnCarBenefit numberOfDaysUnavailable correctly" in {
        car.get.numberOfDaysUnavailable shouldBe Some(numberOfDaysUnavailable)
      }

      "populate the WithdrawnCarBenefit removeEmployeeContribution correctly" in {
        car.get.employeeContribution shouldBe Some(removeEmployeeContribution)
      }
    }

    {
      val fuel = request.withdrawBenefitRequest.fuel

      "fuel should be present" in {
        fuel shouldNot be(None)
      }

      "populate the WithdrawnBenefitRequest fuel withdrawDate correctly from the date car withdrawn" in {
        fuel.get.withdrawDate shouldBe dateCarWithdrawn
      }
    }

    {
      val addBenefit = request.addBenefit

      "populate the addBenefit version correctly" in {
        addBenefit.version shouldBe version
      }

      "populate the addBenefit employment sequence number correctly" in {
        addBenefit.employmentSequence shouldBe employmentSequenceNumber
      }

      "populate the addBenefit benefits with one Benefit object" in {
        addBenefit.benefits.length shouldBe 1
      }

      "populate the addBenefit benefits with a car Benefit" in {
        addBenefit.benefits(0).benefitType shouldBe BenefitTypes.CAR
      }
    }
  }

  object testData {
    val version = 101
    val taxYear: Int = 202
    val employmentSequenceNumber = 303
    val listPrice = 404
    val numberOfDaysUnavailable = 505

    val removeEmployeeContribution = 606

    val startDate = new LocalDate(1)
    val dateMadeAvailable = new LocalDate(2)
    val dateCarWithdrawn = new LocalDate(3)
    val dateCarRegistered = new LocalDate(4)

    val benefitAmount = BigDecimal(1001)
    val grossAmount = BigDecimal(2002)
    val employeePayments = BigDecimal(4004)
    val employeeCapitalContribution = BigDecimal(5005)

    val fuelType = "xyxyxyx"

    val removeFormData = RemoveCarBenefitFormData(withdrawDate = dateCarWithdrawn, carUnavailable = None,
      numberOfDaysUnavailable = Some(numberOfDaysUnavailable), removeEmployeeContributes = Some(true),
      removeEmployeeContribution = Some(removeEmployeeContribution), fuelDateChoice = None, fuelWithdrawDate = None)

    val carBenefitData = CarBenefitData(
      providedFrom = None, carRegistrationDate = Some(dateCarRegistered), listPrice = Some(listPrice), employeeContributes = None,
      employeeContribution = None, employerContributes = None, employerContribution = None, fuelType = Some(fuelType),
      co2Figure = None, co2NoFigure = None, engineCapacity = None, employerPayFuel = None, dateFuelWithdrawn = None)
    val formData = ReplaceCarBenefitFormData(removeFormData, carBenefitData)
  }

}
