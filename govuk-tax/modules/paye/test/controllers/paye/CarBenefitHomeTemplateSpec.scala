package controllers.paye

import uk.gov.hmrc.common.microservice.paye.domain._
import BenefitTypes._
import play.api.test.WithApplication
import play.api.test.Helpers._
import org.jsoup.Jsoup
import uk.gov.hmrc.common.microservice.paye.domain._
import org.joda.time.LocalDate
import uk.gov.hmrc.utils.{TaxYearResolver, DateConverter}
import controllers.DateFieldsHelper
import org.scalatest.matchers.{MatchResult, Matcher}
import views.html.paye.{car_benefit_home, error_no_data_car_benefit_home}
import uk.gov.hmrc.common.microservice.paye.domain.Car
import play.api.test.FakeApplication
import models.paye.EmploymentViews
import java.text.SimpleDateFormat
import views.formatting.Dates

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
  val currentTaxYear = TaxYearResolver.currentTaxYear

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

    def carGrossAmountBenefitValue: Option[BenefitValue]  = Some(BenefitValue(carGrossAmount))
    def fuelGrossAmountBenefitValue: Option[BenefitValue] = Some(BenefitValue(fuelGrossAmount))

    def params =
      HomePageParams(activeCar, companyName, 0, testTaxYear, employmentViews, previousCars, carGrossAmountBenefitValue, fuelGrossAmountBenefitValue)

    val dateFormatter = new SimpleDateFormat("d  yyyy")
  }

  "car benefit home page template" should {
    "render with correct car details for user with a company car and no fuel" in new WithApplication(FakeApplication()) with BaseData {
      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#company-name").text shouldBe "Company car provided by Weyland-Yutani Corp"
      doc.select("#car-benefit-date-available").text shouldBe "12 December 2012"
      doc.select("#car-benefit-date-registered").text shouldBe "12 December 2012"
      doc.select("#car-benefit-engine").text shouldBe "1,400cc or less"
      doc.select("#car-benefit-fuel-type").text shouldBe "Diesel"
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
      doc.select("#company-name").text shouldBe "Company car provided by Weyland-Yutani Corp"
      doc.select("#company-name-0").text shouldBe "Company car provided by Weyland-Yutani Corp"
      doc.select("#company-name-1").text shouldBe "Company car provided by Weyland-Yutani Corp"
      doc.select("#car-benefit-date-withdrawn-1").text shouldBe "5 March 2011"
    }

    "show car details for user with a company car and fuel benefit" in new WithApplication(FakeApplication()) with BaseData {
      override val fuelBenefit = Some(fuelBenefitEmployer1)

      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#company-name").text shouldBe "Company car provided by Weyland-Yutani Corp"
      doc.select("#car-benefit-date-available").text shouldBe "12 December 2012"
      doc.select("#car-benefit-date-registered").text shouldBe "12 December 2012"
      doc.select("#car-benefit-car-value").text shouldBe "£12,343"
      doc.select("#car-benefit-employee-capital-contribution").text shouldBe "£0"
      doc.select("#car-benefit-employee-payments").text shouldBe "£120"
      doc.select("#private-fuel").text shouldBe "Yes, private fuel is available when you use the car"
      doc.select("#car-benefit-fuel-type").text shouldBe "Diesel"
      doc.select("#car-benefit-engine").text shouldBe "1,400cc or less"
      doc.select("#no-car-benefit-container").text shouldBe ""
    }

    "show car details for a user with a company car and a fuel benefit that has been withdrawn" in new WithApplication(FakeApplication()) with BaseData {
      override val fuelBenefit = Some(fuelBenefitEmployer1.copy(dateWithdrawn = Some(new LocalDate(2013, 6, 6))))

      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#company-name").text shouldBe "Company car provided by Weyland-Yutani Corp"
      doc.select("#car-benefit-engine").text shouldBe "1,400cc or less"
      doc.select("#car-benefit-fuel-type").text shouldBe "Diesel"
      doc.select("#car-benefit-date-available").text shouldBe "12 December 2012"
      doc.select("#no-car-benefit-container").text shouldBe ""
      doc.select("#private-fuel").text shouldBe "Weyland-Yutani Corp did pay for fuel for private travel, but stopped paying on 6 June 2013"
    }

    "not display the remove fuel benefit link when the car has an inactive fuel benefit" in new WithApplication(FakeApplication()) with BaseData {
      override val fuelBenefit = Some(Benefit(BenefitTypes.FUEL, 2013, 0.0, 1, dateWithdrawn = Some(new LocalDate)))

      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById(removeFuelLinkId) shouldBe null
    }

    "display the remove fuel benefit link when the car has an active fuel benefit" in new WithApplication(FakeApplication()) with BaseData {
      override val fuelBenefit = Some(Benefit(BenefitTypes.FUEL, 2013, 0.0, 1))

      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById(removeFuelLinkId) shouldNot be (null)
    }

    "display the add fuel benefit link when the car has an inactive fuel benefit" in new WithApplication(FakeApplication()) with BaseData {
      override val fuelBenefit = Some(Benefit(BenefitTypes.FUEL, 2013, 0.0, 1, dateWithdrawn = Some(new LocalDate)))

      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById(addFuelLinkId) shouldNot be(null)
    }

    "not display the add fuel benefit link when the car has an active fuel benefit" in new WithApplication(FakeApplication()) with BaseData {
      override val fuelBenefit = Some(Benefit(BenefitTypes.FUEL, 2013, 0.0, 1))

      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById(addFuelLinkId) shouldBe null
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
      doc.select("#no-car-benefit-container").text shouldBe ""
    }

    "show benefits details for the tax year including gross amount for a user with an active car and multiple previous cars" in new WithApplication(FakeApplication()) with BaseData {
      val currentTaxYear = TaxYearResolver.currentTaxYear
      override val fuelBenefit = Some(fuelBenefitEmployer1)

      val fuelBenefit2 = Some(fuelBenefitEmployer1.copy(benefitAmount = Some(7)))
      val fuelBenefit3 = Some(fuelBenefitEmployer1.copy(benefitAmount = Some(10)))

      val carBenefit2 = carBenefit.copy(benefitAmount = Some(13), car = Some(carBenefit.car.get.copy(dateCarMadeAvailable = Some(new LocalDate(2012, 10, 10)), dateCarWithdrawn = Some(new LocalDate(2012, 10, 11)))))
      val carBenefit3 = carBenefit.copy(benefitAmount = Some(44), car = Some(carBenefit.car.get.copy(dateCarMadeAvailable = Some(new LocalDate(2012, 11, 11)), dateCarWithdrawn = Some(new LocalDate(2012, 11, 12)))))

      val previousCarAndFuel1 = CarAndFuel(carBenefit2, fuelBenefit2)
      val previousCarAndFuel2 = CarAndFuel(carBenefit3, fuelBenefit3)

      override val previousCars = Seq(previousCarAndFuel1, previousCarAndFuel2)

      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))
      val c = doc.select("#car-name")
      doc.select("#car-name").text shouldBe "Current car"
      doc.select("#car-available").text shouldBe s"12 December 2012 to ${Dates.formatDate(TaxYearResolver.endOfCurrentTaxYear)}"
      doc.select("#car-benefit-amount").text shouldBe "£264"
      doc.select("#fuel-benefit-amount").text shouldBe "£5"

      doc.select("#car-name-0").text shouldBe "Previous car"
      doc.select("#car-available-0").text shouldBe s"10 October 2012 to 11 October 2012"
      doc.select("#car-benefit-amount-0").text shouldBe "£13"
      doc.select("#fuel-benefit-amount-0").text shouldBe "£7"

      doc.select("#car-name-1").text shouldBe "Previous car"
      doc.select("#car-available-1").text shouldBe s"11 November 2012 to 12 November 2012"
      doc.select("#car-benefit-amount-1").text shouldBe "£44"
      doc.select("#fuel-benefit-amount-1").text shouldBe "£10"

      doc.select("#total-amounts").text shouldBe s"Total for the ${currentTaxYear}-${currentTaxYear + 1} tax year"
      doc.select("#total-car-amount").text shouldBe s"£321"
      doc.select("#total-fuel-amount").text shouldBe s"£22"
    }

    "show benefit summary details for the tax year excluding the total row for a user with only an active car" in new WithApplication(FakeApplication()) with BaseData {
      override val fuelBenefit = Some(fuelBenefitEmployer1)

      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#car-name").text shouldBe "Current car"
      doc.select("#car-available").text shouldBe s"12 December 2012 to ${Dates.formatDate(TaxYearResolver.endOfCurrentTaxYear)}"
      doc.select("#car-benefit-amount").text shouldBe "£264"
      doc.select("#fuel-benefit-amount").text shouldBe "£5"

      doc.select("#total-amounts").text shouldBe ""
    }

    "show benefit summary details for the tax year excluding the total row for a user with no active car and only one previous car" in new WithApplication(FakeApplication()) with BaseData {
      override val activeCar = None
      val carBenefit2 = carBenefit.copy(benefitAmount = Some(13), car = Some(carBenefit.car.get.copy(dateCarMadeAvailable = Some(new LocalDate(2012, 10, 10)), dateCarWithdrawn = Some(new LocalDate(2012, 10, 11)))))
      val previousCarAndFuel1 = CarAndFuel(carBenefit2, None)
      override val previousCars = Seq(previousCarAndFuel1)

      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#car-name-0").text shouldBe "Previous car"
      doc.select("#total-amounts").text shouldBe ""
    }

    "show benefit summary details for the tax year for the inactive cars when the user has several deactivated cars and no active car" in new WithApplication(FakeApplication()) with BaseData {
      override val activeCar = None

      val fuelBenefit2 = Some(fuelBenefitEmployer1.copy(benefitAmount = Some(7)))
      val fuelBenefit3 = Some(fuelBenefitEmployer1.copy(benefitAmount = Some(10)))

      val carBenefit2 = carBenefit.copy(benefitAmount = Some(13), car = Some(carBenefit.car.get.copy(dateCarMadeAvailable = Some(new LocalDate(2012, 10, 10)), dateCarWithdrawn = Some(new LocalDate(2012, 10, 11)))))
      val carBenefit3 = carBenefit.copy(benefitAmount = Some(44), car = Some(carBenefit.car.get.copy(dateCarMadeAvailable = Some(new LocalDate(2012, 11, 11)), dateCarWithdrawn = Some(new LocalDate(2012, 11, 12)))))

      val previousCarAndFuel1 = CarAndFuel(carBenefit2, fuelBenefit2)
      val previousCarAndFuel2 = CarAndFuel(carBenefit3, fuelBenefit3)

      override val previousCars = Seq(previousCarAndFuel1, previousCarAndFuel2)

      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#car-name").text shouldBe ""

      doc.select("#car-name-0").text shouldBe "Previous car"
      doc.select("#car-available-0").text shouldBe s"10 October 2012 to 11 October 2012"
      doc.select("#car-benefit-amount-0").text shouldBe "£13"
      doc.select("#fuel-benefit-amount-0").text shouldBe "£7"

      doc.select("#car-name-1").text shouldBe "Previous car"
      doc.select("#car-available-1").text shouldBe s"11 November 2012 to 12 November 2012"
      doc.select("#car-benefit-amount-1").text shouldBe "£44"
      doc.select("#fuel-benefit-amount-1").text shouldBe "£10"

      doc.select("#total-amounts").text shouldBe s"Total for the ${currentTaxYear}-${currentTaxYear + 1} tax year"
      doc.select("#total-car-amount").text shouldBe s"£321"
      doc.select("#total-fuel-amount").text shouldBe s"£22"
    }

    "should not display the fuel column on the benefit summary details table when user has no gross fuel benefit amount" in new WithApplication(FakeApplication()) with BaseData {
      override val fuelGrossAmountBenefitValue = None

      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))
      withClue("Benefits summary table should not contain the fuel column: ") {
        doc.getElementById("benefits-summary-table-fuel-header") shouldBe null
        doc.getElementById("fuel-benefit-amount") shouldBe null
        doc.getElementById("fuel-benefit-amount-0") shouldBe null
        doc.getElementById("fuel-benefit-amount-1") shouldBe null
        doc.getElementById("total-fuel-amount") shouldBe null
      }
    }

    "should not display the private fuel row in the tax liability table when user has no gross fuel benefit amount" in new WithApplication(FakeApplication()) with BaseData {
      override val fuelGrossAmountBenefitValue = None

      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))
      withClue("Tax you'll pay table should not contain the fuel row: ") {
        doc.getElementById("tax-liability-summary-table-fuel-row") shouldBe null
      }
    }

    "should not display the total row in the tax liability table when user has no gross fuel benefit amount" in new WithApplication(FakeApplication()) with BaseData {
      override val fuelGrossAmountBenefitValue = None

      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))
      withClue("Tax you'll pay table should not contain the total row: ") {
        doc.getElementById("tax-liability-summary-table-total-row") shouldBe null
      }
    }

    "should not display the car row in the tax liability table when user has no gross car benefit amount" in new WithApplication(FakeApplication()) with BaseData {
      override val carGrossAmountBenefitValue = None

      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))
      withClue("Tax you'll pay table should not contain the car row: ") {
        doc.getElementById("tax-liability-summary-table-car-row") shouldBe null
      }
    }

    "should not display the total row in the tax liability table when user has no gross car benefit amount" in new WithApplication(FakeApplication()) with BaseData {
      override val carGrossAmountBenefitValue = None

      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))
      withClue("Tax you'll pay table should not contain the total row: ") {
        doc.getElementById("tax-liability-summary-table-total-row") shouldBe null
      }
    }

    "should display the tax liability table with all car and fuel values with user has both a car and fuel gross amount" in new WithApplication(FakeApplication()) with BaseData {
      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))
      withClue("Tax you'll pay table should be displayed with all values") {
        doc.getElementById("tax-liability-summary-table-fuel-row") should not be null
        doc.getElementById("tax-liability-summary-table-fuel-header").text shouldBe "Private fuel"
        doc.getElementById("tax-liability-summary-table-fuel-taxable-value").text shouldBe "£22"

        doc.getElementById("tax-liability-summary-table-car-row") should not be null
        doc.getElementById("tax-liability-summary-table-car-header").text shouldBe "Company car"
        doc.getElementById("tax-liability-summary-table-car-taxable-value").text shouldBe "£321"

        doc.getElementById("tax-liability-summary-table-total-row") should not be null
        doc.getElementById("tax-liability-summary-table-total-header").text shouldBe "Total"
        doc.getElementById("tax-liability-summary-table-total-taxable-value").text shouldBe "£343"
      }
    }

    "hide car and fuel benefit details for the tax year when the user does not have any car benefits"  in new WithApplication(FakeApplication()) with BaseData {
      override val activeCar = None

      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))

      withClue("Benefits summary table should not be shown: ") {
        doc.getElementById("car-and-fuel-benefits-summary-table") shouldBe null
      }
      withClue("How much tax you'll pay table should not be shown: ") {
        doc.getElementById("tax-liability-summary-table") shouldBe null
      }
    }

    "show an Add Car link for a user without a company car and do not show the add fuel link" in new WithApplication(FakeApplication()) with BaseData {
      override val activeCar = None

      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById(addCarLinkId) should not be null
      doc.getElementById(addCarLinkId).text should include("add a company car")
      doc.getElementById(addFuelLinkId) shouldBe null
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

    // List of transactions is temporarily disabled on the home page as per-requriements
    "display recent transactions for John Densmore" ignore new WithApplication(FakeApplication()) with BaseData {
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

    // List of transactions is temporarily disabled on the home page as per-requriements
    "not display transactions for John Densmore even when passed to the template" in new WithApplication(FakeApplication()) with BaseData {
      override val fuelBenefit = Some(fuelBenefitEmployer1)
      override val transactionHistory = Seq(
        acceptedRemovedCarTransaction)

      val result = car_benefit_home(params)(johnDensmore)

      val doc = Jsoup.parse(contentAsString(result))
      val recentChanges = doc.select(".overview__actions__done")
      recentChanges shouldBe empty
    }

    // List of transactions is temporarily disabled on the home page as per-requriements
    "only display recent car or fuel transactions for John Densmore" ignore new WithApplication(FakeApplication()) with BaseData {
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

    // List of transactions is temporarily disabled on the home page as per-requriements
    "display recent transactions for John Densmore when both car and fuel benefit have been removed and added " ignore new WithApplication(FakeApplication()) with BaseData {
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
