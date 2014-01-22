package controllers.paye

import scala.concurrent.Future

import play.api.mvc.SimpleResult
import play.api.i18n.Messages

import play.api.test.{FakeRequest, WithApplication}
import play.api.test.Helpers._
import play.api.test.FakeApplication

import org.joda.time.LocalDate
import org.joda.time.chrono.ISOChronology

import org.jsoup.Jsoup
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest.TestData

import uk.gov.hmrc.common.microservice.paye.domain._
import BenefitTypes._
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import uk.gov.hmrc.common.microservice.paye.{domain, PayeConnector}
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector
import uk.gov.hmrc.common.microservice.txqueue.domain.TxQueueTransaction
import models.paye.CarBenefitData

import controllers.DateFieldsHelper
import controllers.paye.CarBenefitFormFields._
import controllers.common.actions.HeaderCarrier
import controllers.paye.validation.BenefitFlowHelper
import controllers.common.SessionKeys


class AddCarBenefitControllerSpec extends PayeBaseSpec with DateFieldsHelper with TaxYearSupport {

  import CarBenefitDataBuilder._
  import Matchers.{any, eq => is}

  val mockKeyStoreService = mock[KeyStoreConnector]
  val mockPayeConnector = mock[PayeConnector]
  val mockTxQueueConnector = mock[TxQueueConnector]
  val mockAuthConnector = mock[AuthConnector]
  val mockAuditConnector = mock[AuditConnector]

  private lazy val controller = new AddCarBenefitController(mockKeyStoreService, mockAuditConnector, mockAuthConnector)(mockPayeConnector, mockTxQueueConnector) with StubTaxYearSupport {
    override def timeSource() = CarBenefitDataBuilder.now
  }

  override protected def beforeEach(testData: TestData) {
    super.beforeEach(testData)

    reset(mockKeyStoreService)
    reset(mockPayeConnector)
    reset(mockTxQueueConnector)
    reset(mockAuthConnector)
    reset(mockAuditConnector)
  }

  "calling start add car benefit" should {

    "return 200 and show the add car benefit form with the required fields and no values filled in" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()

      val result = controller.startAddCarBenefitAction(johnDensmore, requestWithCorrectVersion, taxYear, employmentSeqNumberOne)

      status(result) shouldBe 200

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("[id~=providedFrom]").select("[id~=day]") should not be empty
      doc.select("#listPrice") should not be empty
      doc.select("#listPrice").attr("value") shouldBe empty

      doc.select("#employeeContributes-false") should not be empty

      doc.select("#employeeContributes-true") should not be empty
      doc.select("#employeeContributes-true").attr("checked") shouldBe empty

      doc.select("#employeeContribution") should not be empty
      doc.select("#employeeContribution").attr("value") shouldBe empty

      doc.select("#privateUsePayment-false") should not be empty
      doc.select("#privateUsePayment-true") should not be empty
      doc.select("#privateUsePaymentAmount") should not be empty
      doc.select("#privateUsePaymentAmount").attr("value") shouldBe empty


      doc.select("[id~=carRegistrationDate]").select("[id~=day]") should not be empty
      doc.select("[id~=carRegistrationDate]").select("[id~=year]").attr("value") shouldBe empty

      doc.select("#fuelType-diesel") should not be empty
      doc.select("#fuelType-diesel").attr("checked") shouldBe empty

      doc.select("#fuelType-electricity") should not be empty
      doc.select("#fuelType-other") should not be empty
      doc.select("#engineCapacity-1400") should not be empty
      doc.select("#engineCapacity-1400").attr("checked") shouldBe empty

      doc.select("#engineCapacity-2000") should not be empty
      doc.select("#engineCapacity-9999") should not be empty
      doc.select("#employerPayFuel-true") should not be empty
      doc.select("#employerPayFuel-true").attr("checked") shouldBe empty
      doc.select("#employerPayFuel-false") should not be empty

      doc.select("#co2Figure") should not be empty
      doc.select("#co2NoFigure") should not be empty
      doc.select("#co2NoFigure").attr("checked") shouldBe empty

    }

    "return 400 when employer for sequence number does not exist" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()

      val result = controller.startAddCarBenefitAction(johnDensmore, requestWithCorrectVersion, taxYear, 5)

      status(result) shouldBe 400
    }

    "return to the car benefit home page if the user already has a car benefit" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(benefits = johnDensmoresBenefitsForEmployer1)

      val result = controller.startAddCarBenefitAction(johnDensmore, requestWithCorrectVersion, taxYear, employmentSeqNumberOne)
      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.CarBenefitHomeController.carBenefitHome().url)
    }

    "return 400 if the requested tax year is not the current tax year" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()

      val result = controller.startAddCarBenefitAction(johnDensmore, requestWithCorrectVersion, taxYear + 1, employmentSeqNumberOne)

      status(result) shouldBe 400
    }

    "return 400 if the employer is not the primary employer" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()

      val result = controller.startAddCarBenefitAction(johnDensmore, requestWithCorrectVersion, taxYear, 2)

      status(result) shouldBe 400
    }
  }

  "submitting add car benefit" should {

    "return to the car benefit home page if the user already has a car benefit" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(benefits = johnDensmoresBenefitsForEmployer1)

      val result = controller.reviewAddCarBenefitAction(johnDensmore, newRequestForSaveAddCarBenefit(), taxYear, employmentSeqNumberOne)
      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.CarBenefitHomeController.carBenefitHome().url)
    }

    "return 200 when values form data validates successfully" in new WithApplication(FakeApplication()) {
      assertSuccessfulDatesSubmitJohnDensmore(Some(inTwoDaysTime))
      assertSuccessfulDatesSubmitJohnDensmore(None)
    }


    "ignore invalid values and return 200 when fields are not required" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()

      assertSuccessfulDatesSubmitWithTuple(None)
    }

    "return 400 and display error when values form data fails validation" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()

      assertFailedDatesSubmit(Some(("2012", "1", "1")), Some("false"), None, Some("false"), None, "error_q_1", s"Enter a date between 6 April $currentTaxYear and 5 April ${currentTaxYear+1}.")
      assertFailedDatesSubmit(Some(localDateToTuple(Some(now.plusDays(8)))), Some("false"), None, Some("false"), None, "error_q_1", "Enter a date that’s no more than 7 days from today - you can’t report a change more than 7 days in advance.")

      assertFailedDatesSubmit(Some((s"$taxYear", "5", "")), Some("true"), None, Some("true"), Some((s"$taxYear", "10", "17")), "error_q_1", "You must specify a valid date")
      assertFailedDatesSubmit(Some((s"$taxYear", "", "1")), Some("true"), None, Some("true"), Some((s"$taxYear", "10", "17")), "error_q_1", "You must specify a valid date")
      assertFailedDatesSubmit(Some(("", "5", "1")), Some("true"), None, Some("true"), Some((s"$taxYear", "10", "17")), "error_q_1", "You must specify a valid date")
      assertFailedDatesSubmit(Some((s"$taxYear", "10", "32")), Some("true"), None, Some("true"), Some((s"$taxYear", "10", "17")), "error_q_1", "You must specify a valid date")
      assertFailedDatesSubmit(Some((s"$taxYear", "13", "1")), Some("true"), None, Some("true"), Some((s"$taxYear", "10", "17")), "error_q_1", "You must specify a valid date")
      assertFailedDatesSubmit(Some(("2asd", "", "")), Some("true"), None, Some("true"), Some((s"$taxYear", "10", "17")), "error_q_1", "You must specify a valid date")

    }

    "return 200 when listPrice form data validates successfully" in new WithApplication(FakeApplication()) {
      assertSuccessfulListPriceSubmit(Some(1000))
      assertSuccessfulListPriceSubmit(Some(25000))
      assertSuccessfulListPriceSubmit(Some(99999))
    }

    "return 400 and display error when listPrice form data fails validation" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()

      assertFailedListPriceSubmit(None, "error_q_5", "Enter the car’s list price (including VAT and accessories).")
      assertFailedListPriceSubmit(Some("999"), "error_q_5", "Enter a number between £1,000 and £99,999.")
      assertFailedListPriceSubmit(Some("10000.1"), "error_q_5", "Please use whole numbers only, not decimals or other characters.")
      assertFailedListPriceSubmit(Some("Ten thousand1"), "error_q_5", "Please use whole numbers only, not decimals or other characters.")
      assertFailedListPriceSubmit(Some("I own @ cat"), "error_q_5", "Please use whole numbers only, not decimals or other characters.")
      assertFailedListPriceSubmit(Some("100000"), "error_q_5", "Enter a number between £1,000 and £99,999.")
    }

    "return 200 when employeeContribution form data validates successfully" in new WithApplication(FakeApplication()) {

      assertSuccessfulEmployeeContributionSubmit(Some(false), None, None)
      assertSuccessfulEmployeeContributionSubmit(Some(true), Some("1000"), Some(1000))
      assertSuccessfulEmployeeContributionSubmit(Some(true), Some("5000"), Some(5000))
      assertSuccessfulEmployeeContributionSubmit(Some(false), Some("0"), None)
      assertSuccessfulEmployeeContributionSubmit(Some(false), Some("5.5"), None)
    }

    "return 400 and display error when employeeContribution form data fails validation" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()

      assertFailedEmployeeContributionSubmit(Some("true"), None, "error_q_6", "Enter a number between £1 and £5,000.")
      assertFailedEmployeeContributionSubmit(Some("true"), Some("100.25"), "error_q_6", "Please use whole numbers only, not decimals or other characters.")
      assertFailedEmployeeContributionSubmit(Some("true"), Some("Ten thousand"), "error_q_6", "Please use whole numbers only, not decimals or other characters.")
      assertFailedEmployeeContributionSubmit(Some("true"), Some("I own @ cat"), "error_q_6", "Please use whole numbers only, not decimals or other characters.")
      assertFailedEmployeeContributionSubmit(Some("true"), Some("5002"), "error_q_6", "Enter a number between £1 and £5,000.")
      assertFailedEmployeeContributionSubmit(None, None, "error_q_6", "Please answer this question.")
      assertFailedEmployeeContributionSubmit(Some("true"), Some("0"), "error_q_6", "Enter a number between £1 and £5,000.")
    }

    "return 200 when employers form data validates successfully" in new WithApplication(FakeApplication()) {

      assertSuccessfulEmployerContributionSubmit(Some(false), None, None)
      assertSuccessfulEmployerContributionSubmit(Some(true), Some("1000"), Some(1000))
      assertSuccessfulEmployerContributionSubmit(Some(true), Some("99999"), Some(99999))
      assertSuccessfulEmployerContributionSubmit(Some(false), Some("0"), None)
      assertSuccessfulEmployerContributionSubmit(Some(false), Some("5.5"), None)
    }

    "return 400 and display error when employerContribution form data fails validation" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()

      assertFailedEmployerContributionSubmit(Some("true"), None, "error_q_7", "Confirm how much you paid towards the car.")
      assertFailedEmployerContributionSubmit(Some("true"), Some("1000.25"), "error_q_7", "Please use whole numbers only, not decimals or other characters.")
      assertFailedEmployerContributionSubmit(Some("true"), Some("Ten thousand"), "error_q_7", "Please use whole numbers only, not decimals or other characters.")
      assertFailedEmployerContributionSubmit(Some("true"), Some("I own @ cat"), "error_q_7", "Please use whole numbers only, not decimals or other characters.")
      assertFailedEmployerContributionSubmit(Some("true"), Some("100000"), "error_q_7", "Enter a number between £1 and £99,999.")
      assertFailedEmployerContributionSubmit(None, None, "error_q_7", "Please answer this question.")
      assertFailedEmployerContributionSubmit(Some("true"), Some("0"), "error_q_7", "Enter a number between £1 and £99,999.")
    }

    "return 400 if the submitting is for year that is not the current tax year" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()

      val result = controller.reviewAddCarBenefitAction(johnDensmore, newRequestForSaveAddCarBenefit(), taxYear + 1, employmentSeqNumberOne)

      status(result) shouldBe 400
    }

    "return 400 if the submitting is for employment number that is not the primary employment" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()

      val result = controller.reviewAddCarBenefitAction(johnDensmore, newRequestForSaveAddCarBenefit(), taxYear, 2)

      status(result) shouldBe 400
    }

    "return 200 if the user submits selects an option for the registered before 98 question" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(carRegistrationDateVal = Some(localDateToTuple(Some(org.joda.time.LocalDate.now.withYear(1996)))))
      val result = controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne)
      status(result) shouldBe 200
    }

    "return 400 if the user does not select any option for the registered before 98 question" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(carRegistrationDateVal = None)
      val result = controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne)
      status(result) shouldBe 400
    }

    "return 400 if the user sends an invalid value for the registered before 98 question" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(carRegistrationDateVal = Some(("hacking!", "garbage", "Hotdogs!")))
      val result = controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne)
      status(result) shouldBe 400
    }

    "return 400 if the user sends a car registered date before 1900" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(carRegistrationDateVal = Some(("1899", "7", "1")))
      val result = controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne)
      status(result) shouldBe 400
    }

    "keep the selected option in the car registered date question if the validation fails due to another reason" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(providedFromVal = Some(("3000", "1", "1")), carRegistrationDateVal = Some((taxYear.toString, "5", "29")))
      val result = controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne)
      status(result) shouldBe 400
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("[id~=carRegistrationDate]").select("[id~=day-29]").attr("selected") shouldBe "selected"
      doc.select("[id~=carRegistrationDate]").select("[id~=month-5]").attr("selected") shouldBe "selected"
      doc.select("[id~=carRegistrationDate]").select(s"[id~=year").attr("value") shouldBe taxYear.toString

    }

    "return 200 if the user selects an option for the FUEL TYPE question" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(fuelTypeVal = Some("electricity"), engineCapacityVal = None, co2NoFigureVal = None)
      val result = controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne)
      status(result) shouldBe 200
    }

    "return 400 if the user does not select any option for the FUEL TYPE question" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(fuelTypeVal = None)
      val result = controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne)
      status(result) shouldBe 400

      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".error-notification").text should include(Messages("error.paye.answer_mandatory"))
    }

    "return 400 if the user sends an invalid value for the FUEL TYPE question" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(fuelTypeVal = Some("hacking!"))
      val result = controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne)
      status(result) shouldBe 400
    }

    "keep the selected option in the FUEL TYPE question if the validation fails due to another reason" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(fuelTypeVal = Some("electricity"))
      val result = controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne)
      status(result) shouldBe 400
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#fuelType-electricity").attr("checked") shouldBe "checked"
    }

    "return 200 if the user enters a valid integer for the CO2 FIGURE question" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(co2FigureVal = Some("123"), co2NoFigureVal = Some("false"))
      val result = controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne)
      status(result) shouldBe 200
    }

    "return 400 if the user sends an invalid value for the CO2 FIGURE question" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(co2FigureVal = Some("hacking!"))
      val result = controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne)
      status(result) shouldBe 400
    }

    "keep the selected option in the CO2 FIGURE question if the validation fails due to another reason" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(co2FigureVal = Some("123"))
      val result = controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne)
      status(result) shouldBe 400
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#co2Figure").attr("value") shouldBe "123"
    }

    "return 200 if the user selects the option for the CO2 NO FIGURE" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(co2NoFigureVal = Some("true"))
      val result = controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne)
      status(result) shouldBe 200
    }

    "return 400 if the user sends an invalid value for the option CO2 NO VALUE" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(co2NoFigureVal = Some("hacking!"))
      val result = controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne)
      status(result) shouldBe 400
    }

    "keep the checkbox selected for the CO2 NO VALUE option if the validation fails due to another reason" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(providedFromVal = Some(("3000", "1", "1")), co2NoFigureVal = Some("true"))
      val result = controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne)
      status(result) shouldBe 400
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#co2NoFigure").attr("value") shouldBe "true"
      doc.select("#co2NoFigure").attr("checked") shouldBe "checked"
    }

    "return 200 if the user selects an option for the ENGINE CAPACITY question" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(engineCapacityVal = Some("2000"))
      val result = controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne)
      status(result) shouldBe 200
    }

    "return 400 if the user sends an invalid value for the ENGINE CAPACITY question" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(engineCapacityVal = Some("hacking!"))
      val result = controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne)
      status(result) shouldBe 400
    }

    "keep the selected option in the ENGINE CAPACITY question if the validation fails due to another reason" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(listPriceVal = None, engineCapacityVal = Some("2000"))
      val result = controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne)
      status(result) shouldBe 400
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#engineCapacity-2000").attr("checked") shouldBe "checked"
    }

    "return 400 if the user does not select any option for the EMPLOYER PAY FUEL question" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(employerPayFuelVal = None)
      val result = controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne)
      status(result) shouldBe 400
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#form-add-car-benefit .error-notification").text should be(Messages("error.paye.answer_mandatory"))
    }

    "return 400 if the user sends an invalid value for the EMPLOYER PAY FUEL question" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(employerPayFuelVal = Some("hacking!"))
      val result = controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne)
      status(result) shouldBe 400
    }

    "keep the selected option in the EMPLOYER PAY FUEL question if the validation fails due to another reason" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(employerPayFuelVal = Some("true"), listPriceVal = None)
      val result = controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne)
      status(result) shouldBe 400
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#employerPayFuel-true").attr("checked") shouldBe "checked"
    }

    def assertFailedDatesSubmit(providedFromVal: Option[(String, String, String)],
                                carUnavailableVal: Option[String],
                                numberOfDaysUnavailableVal: Option[String],
                                giveBackThisTaxYearVal: Option[String],
                                providedToVal: Option[(String, String, String)],
                                errorId: String,
                                errorMessage: String) {

      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, newRequestForSaveAddCarBenefit(providedFromVal), taxYear, employmentSeqNumberOne))

      assertFailure(result, errorId, errorMessage)
    }

    def assertSuccessfulDatesSubmitJohnDensmore(providedFromVal: Option[LocalDate]) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq.empty, List.empty, List.empty)
      assertSuccessfulDatesSubmitWithTuple(providedFromVal)
    }

    def assertSuccessfulDatesSubmitWithTuple(providedFromVal: Option[LocalDate]) {

      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, newRequestForSaveAddCarBenefit(
        Some(localDateToTuple(providedFromVal))), taxYear, employmentSeqNumberOne))

      val expectedStoredData = CarBenefitDataBuilder(providedFrom = providedFromVal)
      assertSuccess(result, expectedStoredData)
    }

    def assertFailedListPriceSubmit(listPriceVal: Option[String], errorId: String, errorMessage: String) {

      val result = controller.reviewAddCarBenefitAction(johnDensmore, newRequestForSaveAddCarBenefit(listPriceVal = listPriceVal), taxYear, employmentSeqNumberOne)

      assertFailure(result, errorId, errorMessage)
    }

    def assertSuccessfulListPriceSubmit(listPriceVal: Option[Int]) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq.empty, List.empty, List.empty)

      val result = controller.reviewAddCarBenefitAction(johnDensmore, newRequestForSaveAddCarBenefit(listPriceVal = listPriceVal.map(_.toString)), taxYear, employmentSeqNumberOne)

      val expectedStoredData = CarBenefitDataBuilder(listPrice = listPriceVal)

      assertSuccess(result, expectedStoredData)

    }

    def assertFailedEmployeeContributionSubmit(employeeContributesVal: Option[String], employeeContributionVal: Option[String], errorId: String, errorMessage: String) {

      val result = controller.reviewAddCarBenefitAction(johnDensmore, newRequestForSaveAddCarBenefit(employeeContributesVal = employeeContributesVal, employeeContributionVal = employeeContributionVal), taxYear, employmentSeqNumberOne)

      assertFailure(result, errorId, errorMessage)
    }


    def assertSuccessfulEmployeeContributionSubmit(employeeContributesVal: Option[Boolean], employeeContributionVal: Option[String], expectedContributionVal: Option[Int]) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq.empty, List.empty, List.empty)

      val result = controller.reviewAddCarBenefitAction(johnDensmore, newRequestForSaveAddCarBenefit(employeeContributesVal = employeeContributesVal.map(_.toString), employeeContributionVal = employeeContributionVal), taxYear, employmentSeqNumberOne)

      val expectedStoredData = CarBenefitDataBuilder(employeeContributes = employeeContributesVal, employeeContribution = expectedContributionVal)

      assertSuccess(result, expectedStoredData)
    }

    def assertFailedEmployerContributionSubmit(employerContributesVal: Option[String], employerContributionVal: Option[String], errorId: String, errorMessage: String) {

      val result = controller.reviewAddCarBenefitAction(johnDensmore, newRequestForSaveAddCarBenefit(employerContributesVal = employerContributesVal, employerContributionVal = employerContributionVal), taxYear, employmentSeqNumberOne)

      assertFailure(result, errorId, errorMessage)
    }


    def assertSuccessfulEmployerContributionSubmit(employerContributesVal: Option[Boolean], employerContributionVal: Option[String], expectedContributionVal: Option[Int]) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq.empty, List.empty, List.empty)

      val result = controller.reviewAddCarBenefitAction(johnDensmore, newRequestForSaveAddCarBenefit(employerContributesVal = employerContributesVal.map(_.toString), employerContributionVal = employerContributionVal), taxYear, employmentSeqNumberOne)

      val expectedStoredData = CarBenefitDataBuilder(employerContributes = employerContributesVal, employerContribution = expectedContributionVal)

      assertSuccess(result, expectedStoredData)
    }

    def assertSuccess(result: Future[SimpleResult], collectedData: CarBenefitData) {
      status(result) shouldBe 200

      reset(mockKeyStoreService)
      reset(mockPayeConnector)
    }

    def assertFailure(result: Future[SimpleResult], errorId: String, errorMessage: String) {
      status(result) shouldBe 400
      contentAsString(result) should include(errorMessage)
      verifyZeroInteractions(mockKeyStoreService)
      // TODO: uncomment
      // val doc = Jsoup.parse(contentAsString(result))
      // doc.select(errorId).text should include(errorMessage)
    }
  }

  "the review add car benefit page" should {
    "render car benefit only when the user has no fuel benefit" in new WithApplication(FakeApplication()) with WithCarAndFuelBenefit {

      setupMocksForJohnDensmore()
      val carRegistrationDate = now.minusYears(2)

      val fuelType = "electricity"
      val userContribution = 100
      val listPrice = 9999
      val result = controller.reviewAddCarBenefitAction(johnDensmore,
        newRequestForSaveAddCarBenefit(carRegistrationDateVal = Some(localDateToTuple(Some(carRegistrationDate))),
          providedFromVal = None,
          fuelTypeVal = Some(fuelType),
          engineCapacityVal = None,
          co2NoFigureVal = None,
          employeeContributesVal = Some("true"),
          employeeContributionVal = Some(userContribution.toString),
          listPriceVal = Some(listPrice.toString))
        , taxYear, employmentSeqNumberOne)


      status(result) shouldBe 200

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#car-benefit-taxable-value") shouldBe empty
      doc.select("#fuel-benefit-taxable-value") shouldBe empty
      doc.text() should (not include "£999" and not include "20%" and not include "40%" and not include "45%")
    }

    "render car and fuel benefits when the user has both, car and fuel benefits and provide link to edit data" in new WithApplication(FakeApplication()) with WithCarAndFuelBenefit {
      setupMocksForJohnDensmore()
      val carRegistrationDate = new LocalDate().minusYears(2)

      val fuelType = "diesel"
      val userContribution = 100
      val listPrice = 9999
      val engineCapacity = 1400
      val co2Emission = 50
      val employmentSeqNumberOne = johnDensmoresEmployments(0).sequenceNumber
      val taxYear = controller.currentTaxYear
      val uri = s"/car-benefit/$taxYear/$employmentSeqNumberOne/add"
      val updatedCar = car.copy(dateCarRegistered = Some(carRegistrationDate), fuelType = Some("diesel"), co2Emissions = Some(50), engineSize = Some(1400))
      val updatedCarAndFuel = carAndFuel.copy(carBenefit = carBenefit.copy(car = Some(updatedCar)))

      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore,
        newRequestForSaveAddCarBenefit(carRegistrationDateVal = Some(localDateToTuple(Some(carRegistrationDate))),
          providedFromVal = None,
          fuelTypeVal = Some(fuelType),
          engineCapacityVal = Some(engineCapacity.toString),
          co2NoFigureVal = None,
          co2FigureVal = Some(co2Emission.toString),
          employeeContributesVal = Some("true"),
          employeeContributionVal = Some(userContribution.toString),
          listPriceVal = Some(listPrice.toString),
          path = uri)
        , taxYear, employmentSeqNumberOne))
      status(result) shouldBe 200

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#car-benefit-taxable-value") shouldBe empty
      doc.select("#fuel-benefit-taxable-value") shouldBe empty
      doc.text() should (not include "£999" and not include "£444" and not include "20%" and not include "40%" and not include "45%")
      doc.select("#edit-data").text shouldBe "Change your answers"
      doc.select("#edit-data").attr("href") shouldBe uri
    }

    "handle the case where no provided from or provided to dates are returned from the key store." in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()

      val employmentSeqNumberOne = johnDensmoresEmployments(0).sequenceNumber
      val carBenefitData = johnDensmoresCarBenefitData.copy(
        fuelType = Some("diesel"),
        co2NoFigure = Some(true),
        engineCapacity = Some("1400"),
        employerPayFuel = Some("date"),
        dateFuelWithdrawn = Some(new LocalDate(taxYear, 8, 29))
      )

      when(mockKeyStoreService.getEntry[CarBenefitData](
        is(generateKeystoreActionId(testTaxYear, employmentSeqNumberOne)),
        is("paye"),
        is("AddCarBenefitForm"),
        is(false))(any(), any())).thenReturn(Some(carBenefitData))

      val result = controller.startAddCarBenefitAction(johnDensmore, requestWithCorrectVersion, taxYear, employmentSeqNumberOne)

      status(result) shouldBe 200

      verify(mockKeyStoreService).getEntry[CarBenefitData](
        is(generateKeystoreActionId(taxYear, employmentSeqNumberOne)),
        is("paye"),
        is("AddCarBenefitForm"),
        is(false))(any(), any())
    }

    "allow the user to reedit the form with other fuel type and show it with values already filled" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore()

      val employmentSeqNumberOne = johnDensmoresEmployments(0).sequenceNumber
      val taxYear = controller.currentTaxYear
      val carBenefitData = johnDensmoresCarBenefitData.copy (
        employeeContributes = Some(false),
        employeeContribution = None,
        fuelType = Some("other"),
        co2NoFigure = Some(true),
        engineCapacity = Some("1400"),
        employerPayFuel = Some("true"),
        dateFuelWithdrawn = Some(new LocalDate(taxYear, 8, 29))
      )

      when(mockKeyStoreService.getEntry[CarBenefitData](
        is(generateKeystoreActionId(taxYear, employmentSeqNumberOne)),
        is("paye"),
        is("AddCarBenefitForm"),
        is(false))(any(), any())).thenReturn(Some(carBenefitData))

      val result = controller.startAddCarBenefitAction(johnDensmore, requestWithCorrectVersion, taxYear, employmentSeqNumberOne)

      status(result) shouldBe 200

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("[id~=providedFrom]").select("[id~=day-29]").attr("selected") shouldBe "selected"
      doc.select("[id~=providedFrom]").select("[id~=month-7]").attr("selected") shouldBe "selected"
      doc.select("[id~=providedFrom]").select(s"[id~=year-$taxYear]").attr("selected") shouldBe "selected"
      doc.select("#listPrice").attr("value") shouldBe "1000"
      doc.select("#employeeContributes-true").attr("checked") shouldBe empty
      doc.select("#employeeContribution").attr("value") shouldBe empty
      doc.select("#privateUsePayment-true").attr("checked") shouldBe "checked"
      doc.select("#privateUsePaymentAmount").attr("value") shouldBe "999"

      doc.select("[id~=carRegistrationDate]").select("[id~=day-13]").attr("selected") shouldBe "selected"
      doc.select("[id~=carRegistrationDate]").select("[id~=month-9]").attr("selected") shouldBe "selected"
      doc.select("[id~=carRegistrationDate]").select("[id~=year]").attr("value") shouldBe "1950"
      doc.select("#fuelType-other").attr("checked") shouldBe "checked"
      doc.select("#engineCapacity-1400").attr("checked") shouldBe "checked"
      doc.select("#employerPayFuel-true").attr("checked") shouldBe "checked"
      doc.select("#co2NoFigure").attr("checked") shouldBe "checked"
    }

    "allow the user to reedit the form with electric fuel type and show it with values already filled" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore()

      val employmentSeqNumberOne = johnDensmoresEmployments(0).sequenceNumber
      val taxYear = controller.currentTaxYear

      when(mockKeyStoreService.getEntry[CarBenefitData](
        is(generateKeystoreActionId(taxYear, employmentSeqNumberOne)),
        is("paye"),
        is("AddCarBenefitForm"),
        is(false))(any(), any())).thenReturn(Some(johnDensmoresCarBenefitData))

      val result = controller.startAddCarBenefitAction(johnDensmore, requestWithCorrectVersion, taxYear, employmentSeqNumberOne)

      status(result) shouldBe 200

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("[id~=providedFrom]").select("[id~=day-29]").attr("selected") shouldBe "selected"
      doc.select("[id~=providedFrom]").select("[id~=month-7]").attr("selected") shouldBe "selected"
      doc.select("[id~=providedFrom]").select(s"[id~=year-$taxYear]").attr("selected") shouldBe "selected"
      doc.select("#listPrice").attr("value") shouldBe "1000"
      doc.select("#employeeContributes-true").attr("checked") shouldBe "checked"
      doc.select("#employeeContribution").attr("value") shouldBe "100"
      doc.select("#privateUsePayment-true").attr("checked") shouldBe "checked"
      doc.select("#privateUsePaymentAmount").attr("value") shouldBe "999"

      doc.select("[id~=carRegistrationDate]").select("[id~=day-13]").attr("selected") shouldBe "selected"
      doc.select("[id~=carRegistrationDate]").select("[id~=month-9]").attr("selected") shouldBe "selected"
      doc.select("[id~=carRegistrationDate]").select("[id~=year]").attr("value") shouldBe "1950"
      doc.select("#fuelType-electricity").attr("checked") shouldBe "checked"
      doc.select("#engineCapacity-2000").attr("checked") shouldBe "checked"
      doc.select("#employerPayFuel-false").attr("checked") shouldBe "checked"
    }


    "allow the user to confirm the addition of the car benefit" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()


      val result = controller.reviewAddCarBenefitAction(johnDensmore, newRequestForSaveAddCarBenefit(), taxYear, employmentSeqNumberOne)

      status(result) shouldBe 200

      val expectedUri = routes.AddCarBenefitController.confirmAddingBenefit(taxYear, employmentSeqNumberOne).url
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("form").attr("action") shouldBe expectedUri

    }
  }

  "confirm submission of add car benefit" should {

    val carBenefitData = johnDensmoresCarBenefitData.copy(
      employeeContribution = Some(50),
      fuelType = Some("diesel"),
      co2NoFigure = Some(true),
      engineCapacity = Some("1400"),
      employerPayFuel = Some("date"),
      dateFuelWithdrawn = Some(new LocalDate(taxYear, 8, 29))
    )

    "remove the keystore data for the car benefit form values" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore()
      when(mockKeyStoreService.getEntry[CarBenefitData](
        is(generateKeystoreActionId(taxYear, employmentSeqNumberOne)),
        is("paye"),
        is("AddCarBenefitForm"),
        is(false))(any(), any())).thenReturn(Some(carBenefitData))
      when(mockPayeConnector.addBenefits(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Some(AddBenefitResponse(transaction = TransactionId("aTransactionId"), newTaxCode = Some("bla2"), netCodedAllowance = Some(123))))

      val result = controller.confirmAddingBenefitAction(johnDensmore, requestWithCorrectVersion, taxYear, employmentSeqNumberOne)
      status(result) shouldBe 200

      verify(mockKeyStoreService).deleteKeyStore(is(generateKeystoreActionId(taxYear, employmentSeqNumberOne)), is("paye"), is(false))(any())
    }

    "show the user a confirmation page" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()
      when(mockKeyStoreService.getEntry[CarBenefitData](
        is(generateKeystoreActionId(taxYear, employmentSeqNumberOne)),
        is("paye"),
        is("AddCarBenefitForm"),
        is(false))(any(), any())).thenReturn(Some(carBenefitData))
      when(mockPayeConnector.addBenefits(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Some(AddBenefitResponse(transaction = TransactionId("aTransactionId"), newTaxCode = Some("bla2"), netCodedAllowance = Some(123))))
      val result = controller.confirmAddingBenefitAction(johnDensmore, requestWithCorrectVersion, taxYear, employmentSeqNumberOne)
      status(result) shouldBe 200

      val doc = Jsoup.parse(contentAsString(result))
      doc.title should include("Tell HMRC about a change to your company car - Confirm your details")
    }

    "call the paye microservice to add a new benefit for an electric car with no fuel" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore()

      val carBenefitData = johnDensmoresCarBenefitData.copy (
        employeeContribution = Some(50),
        fuelType = Some("electricity"),
        co2NoFigure = None,
        engineCapacity = None,
        dateFuelWithdrawn = None
      )
      val fuelBenefitGrossAmount = None

      when(mockPayeConnector.addBenefits(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Some(AddBenefitResponse(transaction = TransactionId("aTransactionId"), newTaxCode = Some("bla2"), netCodedAllowance = Some(123))))
      when(mockKeyStoreService.getEntry[CarBenefitData](
        is(generateKeystoreActionId(taxYear, employmentSeqNumberOne)),
        is("paye"),
        is("AddCarBenefitForm"),
        is(false))(any(), any())).thenReturn(Some(carBenefitData))

      val result = controller.confirmAddingBenefitAction(johnDensmore, requestWithCorrectVersion, taxYear, employmentSeqNumberOne)
      status(result) shouldBe 200

      val car = Car(dateCarMadeAvailable = carBenefitData.providedFrom,
        dateCarWithdrawn = None,
        dateCarRegistered = carBenefitData.carRegistrationDate,
        employeeCapitalContribution = carBenefitData.employeeContribution.map(BigDecimal(_)),
        fuelType = carBenefitData.fuelType,
        co2Emissions = carBenefitData.co2Figure,
        engineSize = carBenefitData.engineCapacity.map(_.toInt),
        mileageBand = None,
        carValue = carBenefitData.listPrice.map(BigDecimal(_)),
        employeePayments = carBenefitData.privateUsePaymentAmount.map(BigDecimal(_)),
        daysUnavailable = None
      )
      val benefit = Benefit(benefitType = CAR,
        taxYear = taxYear,
        grossAmount = 0,
        employmentSequenceNumber = employmentSeqNumberOne,
        costAmount = None,
        amountMadeGood = None,
        cashEquivalent = None,
        expensesIncurred = None,
        amountOfRelief = None,
        paymentOrBenefitDescription = None,
        dateWithdrawn = None,
        car = Some(car),
        actions = Map.empty[String, String],
        calculations = Map.empty[String, String],
        benefitAmount = Some(0))

      verify(mockPayeConnector).addBenefits(
        is(s"/paye/${johnDensmore.getPaye.nino}/benefits/2013"),
        is(johnDensmoreVersionNumber),
        is(employmentSeqNumberOne),
        is(Seq(benefit)))(any())
    }
    "call the paye microservice to add a new benefit for a car only" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore()

      val carBenefitData = johnDensmoresCarBenefitData.copy (
        employeeContribution = Some(50),
        fuelType = Some("diesel"),
        co2NoFigure = Some(true),
        engineCapacity = Some("1400"),
        dateFuelWithdrawn = Some(new LocalDate(taxYear, 8, 29))
      )
      val fuelBenefitGrossAmount = None

      when(mockPayeConnector.addBenefits(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Some(AddBenefitResponse(transaction = TransactionId("aTransactionId"), newTaxCode = Some("bla2"), netCodedAllowance = Some(123))))
      when(mockKeyStoreService.getEntry[CarBenefitData](
        is(generateKeystoreActionId(taxYear, employmentSeqNumberOne)),
        is("paye"),
        is("AddCarBenefitForm"),
        is(false))(any(), any())).thenReturn(Some(carBenefitData))

      val result = controller.confirmAddingBenefitAction(johnDensmore, requestWithCorrectVersion, taxYear, employmentSeqNumberOne)
      status(result) shouldBe 200

      val car = Car(dateCarMadeAvailable = carBenefitData.providedFrom,
        dateCarWithdrawn = None,
        dateCarRegistered = carBenefitData.carRegistrationDate,
        employeeCapitalContribution = carBenefitData.employeeContribution.map(BigDecimal(_)),
        fuelType = carBenefitData.fuelType,
        co2Emissions = carBenefitData.co2Figure,
        engineSize = carBenefitData.engineCapacity.map(_.toInt),
        mileageBand = None,
        carValue = carBenefitData.listPrice.map(BigDecimal(_)),
        employeePayments = carBenefitData.privateUsePaymentAmount.map(BigDecimal(_)),
        daysUnavailable = None
      )
      val benefit = Benefit(benefitType = CAR,
        taxYear = taxYear,
        grossAmount = 0,
        employmentSequenceNumber = employmentSeqNumberOne,
        costAmount = None,
        amountMadeGood = None,
        cashEquivalent = None,
        expensesIncurred = None,
        amountOfRelief = None,
        paymentOrBenefitDescription = None,
        dateWithdrawn = None,
        car = Some(car),
        actions = Map.empty[String, String],
        calculations = Map.empty[String, String],
        benefitAmount = Some(0))

      verify(mockPayeConnector).addBenefits(
        is(s"/paye/${johnDensmore.getPaye.nino}/benefits/2013"),
        is(johnDensmoreVersionNumber),
        is(employmentSeqNumberOne),
        is(Seq(benefit)))(any())
    }

    "call the paye microservice to add new benefits for both car and fuel" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore()

      val carBenefitData = johnDensmoresCarBenefitData.copy (
        employeeContribution = Some(50),
        fuelType = Some("diesel"),
        co2NoFigure = Some(true),
        engineCapacity = Some("1400"),
        employerPayFuel = Some("date"),
        dateFuelWithdrawn = Some(new LocalDate(taxYear, 8, 29))
      )

      when(mockPayeConnector.addBenefits(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Some(AddBenefitResponse(transaction = TransactionId("aTransactionId"), newTaxCode = Some("bla2"), netCodedAllowance = Some(123))))
      when(mockKeyStoreService.getEntry[CarBenefitData](
        is(generateKeystoreActionId(taxYear, employmentSeqNumberOne)),
        is("paye"),
        is("AddCarBenefitForm"),
        is(false))(any(), any())).thenReturn(Some(carBenefitData))

      val result = controller.confirmAddingBenefitAction(johnDensmore, requestWithCorrectVersion, taxYear, employmentSeqNumberOne)
      status(result) shouldBe 200

      val car = Car(dateCarMadeAvailable = carBenefitData.providedFrom,
        dateCarWithdrawn = None,
        dateCarRegistered = carBenefitData.carRegistrationDate,
        employeeCapitalContribution = carBenefitData.employeeContribution.map(BigDecimal(_)),
        fuelType = carBenefitData.fuelType,
        co2Emissions = carBenefitData.co2Figure,
        engineSize = carBenefitData.engineCapacity.map(_.toInt),
        mileageBand = None,
        carValue = carBenefitData.listPrice.map(BigDecimal(_)),
        employeePayments = carBenefitData.privateUsePaymentAmount.map(BigDecimal(_)),
        daysUnavailable = None
      )
      val carBenefit = Benefit(benefitType = CAR,
        taxYear = taxYear,
        grossAmount = 0,
        employmentSequenceNumber = employmentSeqNumberOne,
        costAmount = None,
        amountMadeGood = None,
        cashEquivalent = None,
        expensesIncurred = None,
        amountOfRelief = None,
        paymentOrBenefitDescription = None,
        dateWithdrawn = None,
        car = Some(car),
        actions = Map.empty[String, String],
        calculations = Map.empty[String, String],
        benefitAmount = Some(0)
      )

      val fuelBenefit = Benefit(benefitType = FUEL,
        taxYear = taxYear,
        grossAmount = 0,
        employmentSequenceNumber = employmentSeqNumberOne,
        costAmount = None,
        amountMadeGood = None,
        cashEquivalent = None,
        expensesIncurred = None,
        amountOfRelief = None,
        paymentOrBenefitDescription = None,
        dateWithdrawn = carBenefitData.dateFuelWithdrawn,
        car = Some(car),
        actions = Map.empty[String, String],
        calculations = Map.empty[String, String],
        benefitAmount = Some(0)
      )

      verify(mockPayeConnector).addBenefits(
        is(s"/paye/${johnDensmore.getPaye.nino}/benefits/2013"),
        is(johnDensmoreVersionNumber),
        is(employmentSeqNumberOne),
        is(Seq(carBenefit, fuelBenefit)))(any())
    }

    "show the confirmation page with the correct tax codes and allowance" in new WithApplication(FakeApplication()) {
      // given
      setupMocksForJohnDensmore()
      val carBenefitData = johnDensmoresCarBenefitData.copy(
        employeeContribution = Some(50),
        fuelType = Some("diesel"),
        co2NoFigure = Some(true),
        engineCapacity = Some("1400"),
        employerPayFuel = Some("date"),
        dateFuelWithdrawn = Some(new LocalDate(taxYear, 8, 29))
      )

      when(mockPayeConnector.addBenefits(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Some(AddBenefitResponse(transaction = TransactionId("aTransactionId"), newTaxCode = Some("aNewTaxCoe"), netCodedAllowance = Some(123))))
      when(mockKeyStoreService.getEntry[CarBenefitData](
        is(generateKeystoreActionId(taxYear, employmentSeqNumberOne)),
        is("paye"),
        is("AddCarBenefitForm"),
        is(false))(any(), any())).thenReturn(Some(carBenefitData))

      // when
      val result = controller.confirmAddingBenefitAction(johnDensmore, requestWithCorrectVersion, taxYear, employmentSeqNumberOne)

      // then
      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      val text = doc.select("#headline").text
      text should be("Thank you")
      doc.select("#old-tax-code").text shouldBe "430L"
      doc.select("#new-tax-code").text shouldBe "aNewTaxCoe"
      doc.select("#personal-allowance") should be(empty)
      doc.select("#start-date") should be(empty)
      doc.select("#end-date") should be(empty)
      doc.select("#epilogue").text should include("HMRC will write to you to confirm your new tax code within 7 days.")
      doc.select("#home-page-link").text should include("View your updated details and estimated tax on your benefits.")
      doc.select("a#tax-codes").text should be("tax codes")
      doc.select("a#tax-codes").first.attr("href") should be("https://www.gov.uk/tax-codes")
      doc.select("a#tax-codes").first.attr("target") should be("_blank")
      doc.select("a#tax-on-company-benefits").text should be("tax on company benefits")
      doc.select("a#tax-on-company-benefits").first.attr("href") should be("https://www.gov.uk/tax-company-benefits")
      doc.select("a#tax-on-company-benefits").first.attr("target") should be("_blank")
    }
  }

  private def setupMocksForJohnDensmore(taxCodes: Seq[TaxCode] = johnDensmoresTaxCodes, employments: Seq[Employment] = johnDensmoresEmployments, benefits: Seq[CarBenefit] = Seq.empty,
                                        acceptedTransactions: List[TxQueueTransaction] = List.empty, completedTransactions: List[TxQueueTransaction] = List.empty) {

    implicit val hc = HeaderCarrier()
    val carAndFuels = benefits.map(c => CarAndFuel(c.toBenefits(0), c.toBenefits.drop(1).headOption))
    when(mockPayeConnector.linkedResource[Seq[TaxCode]](is(s"/paye/AB123456C/tax-codes/$taxYear"))(any(), any())).thenReturn(Some(taxCodes))
    when(mockPayeConnector.linkedResource[Seq[Employment]](is(s"/paye/AB123456C/employments/$taxYear"))(any(), any())).thenReturn(Some(employments))
    when(mockPayeConnector.linkedResource[Seq[CarAndFuel]](is(s"/paye/AB123456C/benefit-cars/$taxYear"))(any(), any())).thenReturn(Some(carAndFuels))
    when(mockPayeConnector.version(is("/paye/AB123456C/version"))(any())).thenReturn(johnDensmoreVersionNumber)

    when(mockTxQueueConnector.transaction(Matchers.matches("^/txqueue/current-status/paye/AB123456C/ACCEPTED/.*"))(any())).thenReturn(Some(acceptedTransactions))
    when(mockTxQueueConnector.transaction(Matchers.matches("^/txqueue/current-status/paye/AB123456C/COMPLETED/.*"))(any())).thenReturn(Some(completedTransactions))
    when(mockKeyStoreService.getEntry[CarBenefitData](Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(None)

    when(mockKeyStoreService.addKeyStoreEntry(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).
      thenReturn(Future.successful(None))
  }

  private def newRequestForSaveAddCarBenefit(providedFromVal: Option[(String, String, String)] = Some(localDateToTuple(Some(defaultProvidedFrom))),
                                             listPriceVal: Option[String] = Some(defaultListPrice.toString),
                                             employeeContributesVal: Option[String] = Some(defaultEmployeeContributes.toString),
                                             employeeContributionVal: Option[String] = defaultEmployeeContribution,
                                             employerContributesVal: Option[String] = Some(defaultEmployerContributes.toString),
                                             employerContributionVal: Option[String] = defaultEmployerContribution,
                                             carRegistrationDateVal: Option[(String, String, String)] = Some(localDateToTuple(Some(defaultCarRegistrationDate))),
                                             fuelTypeVal: Option[String] = Some(defaultFuelType.toString),
                                             co2FigureVal: Option[String] = defaultCo2Figure,
                                             co2NoFigureVal: Option[String] = Some(defaultCo2NoFigure.toString),
                                             engineCapacityVal: Option[String] = Some(defaultEngineCapacity.toString),
                                             employerPayFuelVal: Option[String] = Some(defaultEmployerPayFuel.toString),
                                             dateFuelWithdrawnVal: Option[(String, String, String)] = Some(localDateToTuple(defaultDateFuelWithdrawn)),
                                             path: String = "") = {

    FakeRequest("GET", path).withFormUrlEncodedBody(Seq(
      listPrice -> listPriceVal.getOrElse(""),
      employeeContributes -> employeeContributesVal.getOrElse(""),
      employeeContribution -> employeeContributionVal.getOrElse(""),
      privateUsePayment -> employerContributesVal.getOrElse(""),
      privateUsePaymentAmount -> employerContributionVal.getOrElse(""),
      fuelType -> fuelTypeVal.getOrElse(""),
      co2Figure -> co2FigureVal.getOrElse(""),
      co2NoFigure -> co2NoFigureVal.getOrElse(""),
      engineCapacity -> engineCapacityVal.getOrElse(""),
      employerPayFuel -> employerPayFuelVal.getOrElse(""))
      ++ buildDateFormField(providedFrom, providedFromVal)
      ++ buildDateFormField(dateFuelWithdrawn, dateFuelWithdrawnVal)
      ++ buildDateFormField(carRegistrationDate, carRegistrationDateVal): _*).
      withSession(SessionKeys.npsVersion -> johnDensmoreVersionNumber.toString)
  }

  private def generateKeystoreActionId(taxYear: Int, employmentSequenceNumber: Int) = {
    s"AddCarBenefit:$taxYear:$employmentSequenceNumber"
  }
}


private trait WithCarAndFuelBenefit {
  val car = Car(dateCarMadeAvailable = None, dateCarWithdrawn = None, dateCarRegistered = Some(localDate(2011, 10, 3)),
    employeeCapitalContribution = Some(100), fuelType = Some("electricity"),
    co2Emissions = None, engineSize = None, mileageBand = None,
    carValue = Some(9999), employeePayments = None, daysUnavailable = None)

  val carBenefit = Benefit(benefitType = BenefitTypes.CAR, taxYear = 2013,
    grossAmount = 0, employmentSequenceNumber = 1, costAmount = None,
    amountMadeGood = None, cashEquivalent = None, expensesIncurred = None,
    amountOfRelief = None, paymentOrBenefitDescription = None,
    dateWithdrawn = None, car = Some(car),
    actions = Map.empty, calculations = Map.empty, benefitAmount = Some(0), forecastAmount = Some(0))
  val fuelBenefit = None
  val carAndFuel = domain.CarAndFuel(carBenefit, fuelBenefit)

  def localDate(year: Int, month: Int, day: Int) = new LocalDate(year, month, day, ISOChronology.getInstanceUTC)
}