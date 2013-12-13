package controllers.paye

import uk.gov.hmrc.common.microservice.paye.domain._
import BenefitTypes._
import play.api.test.WithApplication
import play.api.test.Helpers._
import org.jsoup.Jsoup
import uk.gov.hmrc.common.microservice.paye.domain._
import org.joda.time.LocalDate
import uk.gov.hmrc.utils.DateConverter
import controllers.DateFieldsHelper
import uk.gov.hmrc.common.microservice.paye.domain.Employment._
import org.scalatest.matchers.{MatchResult, Matcher}
import views.html.paye.{car_benefit_home, error_no_data_car_benefit_home}
import scala.Some
import uk.gov.hmrc.common.microservice.paye.domain.Car
import play.api.test.FakeApplication
import uk.gov.hmrc.common.microservice.paye.domain.TaxYearData
import models.paye.EmploymentViews

class CarBenefitHomeTemplateSpec extends PayeBaseSpec with DateConverter with DateFieldsHelper {


  val removeFuelLinkId = "rm-fuel-link"
  val removeCarLinkId = "rm-car-link"
  val addCarLinkId = "add-car-link"
  val addFuelLinkId = "add-fuel-link"
  val taxYear = 2013
  val employmentSeqNumber = 1
  val removeActionName = "removed"
  val addActionName = "added"
  val carBenefitName = "car"
  val fuelBenefitName = "fuel"
  val carAndFuelBenefitName = "car and fuel"
  val todaysDate = "2 December 2012"

  val benefitTypes = Set(BenefitTypes.CAR, BenefitTypes.FUEL)

  "the error page template" should {
    "render with an error message when HomePageParams are not available" in new WithApplication(FakeApplication()) with BaseData {
      private val errorMessage: String = "there was an error"

      val result = error_no_data_car_benefit_home(errorMessage)(johnDensmore)
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#error-message").text() shouldBe errorMessage
    }
  }

  trait BaseData {
    def employments = johnDensmoresOneEmployment()

    def carBenefit = carBenefitEmployer1

    def fuelBenefit: Option[Benefit] = None

    def taxCodes = johnDensmoresTaxCodes

    def transactionHistory = Seq(acceptedRemovedCarTransaction, completedRemovedFuelTransaction)

    def employmentViews = EmploymentViews.createEmploymentViews(employments, taxCodes, testTaxYear,
      benefitTypes, transactionHistory)

    def companyName: Option[String] = Some("Weyland-Yutani Corp")

    def activeCar: Option[CarAndFuel] = Some(CarAndFuel(carBenefit, fuelBenefit))

    def previousCars = Seq.empty[CarAndFuel]

    def params =
      HomePageParams(activeCar, companyName, 0, testTaxYear, employmentViews, previousCars)
  }

  "car benefit home page template" should {
    "render with correct car details for user with a company car and no fuel" in new WithApplication(FakeApplication()) with BaseData {
      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#company-name").text shouldBe "Company car provided by Weyland-Yutani Corp"
      doc.select("#car-benefit-engine").text shouldBe "1,400cc or less"
      doc.select("#car-benefit-fuel-type").text shouldBe "Diesel"
      doc.select("#car-benefit-date-available").text shouldBe "12 December 2012"
      doc.select("#car-benefit-amount").text shouldBe "£321"
      doc.select("#fuel-benefit-amount").text shouldBe ""
      doc.select("#no-car-benefit-container").text shouldBe ""
      doc.select("#private-fuel").text shouldBe "No"
    }

    "render previous cars in the tax year" in new WithApplication(FakeApplication()) with BaseData {
      val previousCar = Benefit(31, 2013, 1234, 1, car = Some(Car()))
      val previousCar2 = Benefit(31, 2013, 5678, 1, car = Some(Car(dateCarWithdrawn = Some(new LocalDate(2011, 3, 5)))))
      val previousCarAndFuel1 = CarAndFuel(previousCar)
      val previousCarAndFuel2 = CarAndFuel(previousCar2)

      override val previousCars = Seq(previousCarAndFuel1, previousCarAndFuel2)

      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#company-name-0").text shouldBe "Company car provided by Weyland-Yutani Corp"
      doc.select("#car-benefit-amount-0").text shouldBe "£1,234"
      doc.select("#company-name-1").text shouldBe "Company car provided by Weyland-Yutani Corp"
      doc.select("#car-benefit-amount-1").text shouldBe "£5,678"
      doc.select("#car-benefit-date-withdrawn-1").text shouldBe "5 March 2011"
    }

    "show car details for user with a company car and fuel benefit" in new WithApplication(FakeApplication()) with BaseData {
      override val fuelBenefit = Some(fuelBenefitEmployer1)

      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#company-name").text shouldBe "Company car provided by Weyland-Yutani Corp"
      doc.select("#car-benefit-engine").text shouldBe "1,400cc or less"
      doc.select("#car-benefit-fuel-type").text shouldBe "Diesel"
      doc.select("#car-benefit-date-available").text shouldBe "12 December 2012"
      doc.select("#car-benefit-amount").text shouldBe "£321"
      doc.select("#fuel-benefit-amount").text shouldBe "£22"
      doc.select("#no-car-benefit-container").text shouldBe ""
      doc.select("#private-fuel").text shouldBe "Yes, private fuel is available when you use the car"
    }

    "show car details for a user with a company car and a fuel benefit that has been withdrawn" in new WithApplication(FakeApplication()) with BaseData {
      override val fuelBenefit = Some(fuelBenefitEmployer1.copy(dateWithdrawn = Some(new LocalDate(2013, 6, 6))))

      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#company-name").text shouldBe "Company car provided by Weyland-Yutani Corp"
      doc.select("#car-benefit-engine").text shouldBe "1,400cc or less"
      doc.select("#car-benefit-fuel-type").text shouldBe "Diesel"
      doc.select("#car-benefit-date-available").text shouldBe "12 December 2012"
      doc.select("#car-benefit-amount").text shouldBe "£321"
      doc.select("#fuel-benefit-amount").text shouldBe "£22"
      doc.select("#no-car-benefit-container").text shouldBe ""
      doc.select("#private-fuel").text shouldBe "Weyland-Yutani Corp did pay for fuel for private travel, but stopped paying on 6 June 2013"
    }

    "show car details for user with a company car where the employer name is unknown" in new WithApplication(FakeApplication()) with BaseData {
      override val fuelBenefit = Some(fuelBenefitEmployer1)

      override val companyName = None

      override val employments = Seq(
        Employment(sequenceNumber = 1, startDate = new LocalDate(2013, 7, 2), endDate = Some(new LocalDate(2013, 10, 8)), taxDistrictNumber = "898", payeNumber = "9900112", employerName = None, employmentType = Employment.primaryEmploymentType),
        Employment(sequenceNumber = 2, startDate = new LocalDate(2013, 10, 14), endDate = None, taxDistrictNumber = "899", payeNumber = "1212121", employerName = None, employmentType = 1))


      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#company-name").text shouldBe "Company car provided by Your employer"
      doc.select("#car-benefit-engine").text shouldBe "1,400cc or less"
      doc.select("#car-benefit-fuel-type").text shouldBe "Diesel"
      doc.select("#car-benefit-date-available").text shouldBe "12 December 2012"
      doc.select("#car-benefit-amount").text shouldBe "£321"
      doc.select("#fuel-benefit-amount").text shouldBe "£22"
      doc.select("#no-car-benefit-container").text shouldBe ""
    }

    "show an Add Car link for a user without a company car and do not show the add fuel link" in new WithApplication(FakeApplication()) with BaseData {
      override val activeCar = None

      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById(addCarLinkId) should not be (null)
      doc.getElementById(addCarLinkId).text should include("add a company car")
      doc.getElementById(addFuelLinkId) shouldBe (null)
    }

    "show an Add Fuel link for a user with a car benefit but no fuel benefit" in new WithApplication(FakeApplication()) with BaseData {
      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById(addFuelLinkId) should not be (null)
      doc.getElementById(addCarLinkId) shouldBe (null)
    }

    "not show an add Car or add Fuel links for a user with company car and fuel benefits" in new WithApplication(FakeApplication()) with BaseData {
      override val fuelBenefit = Some(fuelBenefitEmployer1)

      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById(addCarLinkId) should be(null)
      doc.getElementById(addFuelLinkId) should be(null)
    }

    "not show an Add Fuel Link if the user has a company car with fuel of type electricity" in new WithApplication(FakeApplication()) with BaseData {


      override val carBenefit = Benefit(31, testTaxYear, 321.42, 1, None, None, None, None, None, None, None,
        Some(Car(Some(new LocalDate(testTaxYear - 1, 12, 12)), None, Some(new LocalDate(testTaxYear - 1, 12, 12)), Some(0), Some("electricity"), Some(124), Some(1400), None, Some(BigDecimal("12343.21")), None, None)), actions("AB123456C", testTaxYear, 1), Map.empty)


      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById(addFuelLinkId) shouldBe null
    }

    "show a remove car link and not show a remove fuel link for a user who has a car without a fuel benefit" in new WithApplication(FakeApplication()) with BaseData {
      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById(removeFuelLinkId) should be(null)
      doc.getElementById(removeCarLinkId) should not be (null)
    }

    "show a remove car and remove fuel link for a user who has a car and fuel benefit" in new WithApplication(FakeApplication()) with BaseData {
      override val fuelBenefit = Some(fuelBenefitEmployer1)

      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById(removeFuelLinkId) should not be (null)
      doc.getElementById(removeCarLinkId) should not be (null)
    }

    "show an add Car link for a user without a company car" in new WithApplication(FakeApplication()) with BaseData {
      override val activeCar = None

      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById(addCarLinkId) should not be (null)
    }

    "not show a remove car or remove fuel link for a user with no car benefit" in new WithApplication(FakeApplication()) with BaseData {
      override val activeCar = None
      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))

      doc.getElementById(removeFuelLinkId) should be(null)
      doc.getElementById(removeCarLinkId) should be(null)
    }

    "show a remove car link and not show a remove fuel link for a user with a car benefit but no fuel benefit" in new WithApplication(FakeApplication()) with BaseData {

      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))

      doc.getElementById(removeFuelLinkId) should be(null)
      doc.getElementById(removeCarLinkId) should not be null
    }

    "do not show an add company car benefit link if the user has a car benefit" in new WithApplication(FakeApplication()) with BaseData {
      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById(addCarLinkId) shouldBe null
    }

    "display recent transactions for John Densmore" in new WithApplication(FakeApplication()) with BaseData {
      override val fuelBenefit = Some(fuelBenefitEmployer1)
      override val transactionHistory = Seq(
        acceptedRemovedCarTransaction, completedRemovedCarTransaction,
        acceptedRemovedFuelTransaction, completedRemovedFuelTransaction,
        acceptedAddFuelTransaction, completedAddFuelTransaction)

      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))
      val recentChanges = doc.select(".overview__actions__done").text
      val employerName = companyName.get

      recentChanges should include(s"On $todaysDate you $removeActionName your $carBenefitName from $employerName. This is being processed. HMRC will write to you to confirm your new tax code within 7 days.")
      recentChanges should include(s"On $todaysDate you $removeActionName your $carBenefitName from $employerName. This has been completed.")

      recentChanges should include(s"On $todaysDate you $removeActionName your $fuelBenefitName from $employerName. This is being processed. HMRC will write to you to confirm your new tax code within 7 days.")
      recentChanges should include(s"On $todaysDate you $removeActionName your $fuelBenefitName from $employerName. This has been completed.")

      recentChanges should include(s"On $todaysDate you $addActionName your $fuelBenefitName from $employerName. This is being processed. HMRC will write to you to confirm your new tax code within 7 days.")
      recentChanges should include(s"On $todaysDate you $addActionName your $fuelBenefitName from $employerName. This has been completed.")

      doc.select(".no_actions") shouldBe empty
    }

    "only display recent car or fuel transactions for John Densmore" in new WithApplication(FakeApplication()) with BaseData {
      override val fuelBenefit = Some(fuelBenefitEmployer1)
      override val transactionHistory = Seq(completedAddCarTransaction, acceptedAddCarTransaction)

      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))
      val recentChanges = doc.select(".overview__actions__done").text
      val employerName = companyName.get

      recentChanges should include(s"On $todaysDate you $addActionName your $carBenefitName from $employerName. This is being processed. HMRC will write to you to confirm your new tax code within 7 days.")
      recentChanges should include(s"On $todaysDate you $addActionName your $carBenefitName from $employerName. This has been completed.")

      recentChanges should containSentences(5)
    }

    def containSentences(n: Int) = {
      new Matcher[String] {
        def apply(s: String) = {
          MatchResult(s.split( """\.""").length == n,
            s"""`$s` did not contain $n sentences.""",
            s"""`$s` did contain $n sentences.""")
        }
      }
    }


    "display recent transactions for John Densmore when both car and fuel benefit have been removed and added " in new WithApplication(FakeApplication()) with BaseData {
      val removeCar1AndFuel1CompletedTransaction = transactionWithTags(List("paye", "test", "message.code.removeBenefits"), Map("benefitTypes" -> s"$CAR,$FUEL"), mostRecentStatus = "completed")
      val addCar2AndFuel2CompletedTransaction = transactionWithTags(List("paye", "test", "message.code.addBenefits"), Map("benefitTypes" -> s"$CAR,$FUEL"), mostRecentStatus = "completed")
      val removeCar2AndFuel2AcceptedTransaction = transactionWithTags(List("paye", "test", "message.code.removeBenefits"), Map("benefitTypes" -> s"$CAR,$FUEL"), mostRecentStatus =  "accepted")
      val addCar3AndFuel4AcceptedTransaction = transactionWithTags(List("paye", "test", "message.code.addBenefits"), Map("benefitTypes" -> s"$CAR,$FUEL"), mostRecentStatus = "accepted")
      val employerName1: String = "Weyland-Yutani Corp"

      override val fuelBenefit = Some(fuelBenefitEmployer1)
      override val transactionHistory = Seq(removeCar2AndFuel2AcceptedTransaction, addCar3AndFuel4AcceptedTransaction, removeCar1AndFuel1CompletedTransaction, addCar2AndFuel2CompletedTransaction)

      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))
      val recentChanges = doc.select(".overview__actions__done").text


      recentChanges should include(s"On $todaysDate you $addActionName your $carAndFuelBenefitName from $employerName1. This is being processed. HMRC will write to you to confirm your new tax code within 7 days.")
      recentChanges should include(s"On $todaysDate you $removeActionName your $carAndFuelBenefitName from $employerName1. This is being processed. HMRC will write to you to confirm your new tax code within 7 days.")

      recentChanges should include(s"On $todaysDate you $addActionName your $carAndFuelBenefitName from $employerName1. This has been completed.")
      recentChanges should include(s"On $todaysDate you $removeActionName your $carAndFuelBenefitName from $employerName1. This has been completed.")
    }

    "display no recent changes for John Densmore if there are no transactions" in new WithApplication(FakeApplication()) with BaseData {
      override val fuelBenefit = Some(fuelBenefitEmployer1)
      override val transactionHistory = Seq()


      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".no_actions") shouldBe empty
      doc.select(".no_actions").text should not include "no changes"
      doc.select(".overview__actions").text shouldBe ""
    }
  }
}