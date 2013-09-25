package controllers.paye

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.MockMicroServicesForTests
import play.api.test.{FakeRequest, WithApplication}
import play.api.test.Helpers._
import org.jsoup.Jsoup
import uk.gov.hmrc.common.microservice.paye.domain._
import org.mockito.Mockito._
import org.mockito.{Mockito, Matchers}
import uk.gov.hmrc.common.microservice.paye.domain.Employment
import org.joda.time.{DateTimeZone, LocalDate}
import org.scalatest.TestData
import CarBenefitFormFields._
import uk.gov.hmrc.microservice.txqueue.TxQueueTransaction
import uk.gov.hmrc.common.microservice.paye.domain.Car
import play.api.test.FakeApplication
import uk.gov.hmrc.common.microservice.paye.domain.TaxCode
import uk.gov.hmrc.utils.DateConverter
import play.api.mvc.Result
import uk.gov.hmrc.common.microservice.keystore.KeyStoreMicroService
import CarBenefitDataBuilder._

class CarBenefitHomeControllerSpec extends PayeBaseSpec with MockitoSugar with DateConverter {

  val mockKeyStoreService = mock[KeyStoreMicroService]

  private lazy val controller = new CarBenefitHomeController(timeSource = () => now.toDateTimeAtCurrentTime(DateTimeZone.UTC), mockKeyStoreService) with MockMicroServicesForTests

  override protected def beforeEach(testData: TestData) {
    super.beforeEach(testData)

    controller.resetAll()
    Mockito.reset(mockKeyStoreService)
  }

  val carBenefitEmployer1 = Benefit(benefitType = 31, taxYear = 2013, grossAmount = 321.42, employmentSequenceNumber = 1, null, null, null, null, null, null,
    car = Some(Car(Some(new LocalDate(2012, 12, 12)), None, Some(new LocalDate(2012, 12, 12)), 0, 2, 124, 1, "B", BigDecimal("12343.21"))), actions("AB123456C", 2013, 1), Map.empty)

  val fuelBenefitEmployer1 = Benefit(benefitType = 29, taxYear = 2013, grossAmount = 22.22, employmentSequenceNumber = 1, null, null, null, null, null, null,
    car = None, actions("AB123456C", 2013, 1), Map.empty)

  val johnDensmoresBenefitsForEmployer1 = Seq(
    carBenefitEmployer1,
    fuelBenefitEmployer1)

  val taxYear = 2013
  val employmentSeqNumber = 1

  "calling carBenefitHome" should {

    "return 500 if we cannot find a primary employment for the customer" in {
      val employments = Seq(
        Employment(sequenceNumber = 1, startDate = new LocalDate(2013, 7, 2), endDate = Some(new LocalDate(2013, 10, 8)), taxDistrictNumber = "898", payeNumber = "9900112", employerName = Some("Weyland-Yutani Corp"), 2),
        Employment(sequenceNumber = 2, startDate = new LocalDate(2013, 10, 14), endDate = None, taxDistrictNumber = "899", payeNumber = "1212121", employerName = None, 2))

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, employments, Seq.empty, List.empty, List.empty)

      val result = controller.carBenefitHomeAction(johnDensmore, FakeRequest())

      status(result) should be(500)
    }

    "show car details for user with a company car and no fuel" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq(carBenefitEmployer1), List.empty, List.empty)

      val result = controller.carBenefitHomeAction(johnDensmore, FakeRequest())

      status(result) should be(200)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#company-name").text shouldBe "Weyland-Yutani Corp"
      doc.select("#car-benefit-engine").text shouldBe "0-1400 cc"
      doc.select("#car-benefit-fuel-type").text shouldBe "Bi-Fuel"
      doc.select("#car-benefit-date-available").text shouldBe "December 12, 2012"
      doc.select("#car-benefit-amount").text shouldBe "£321.42"
      doc.select("#fuel-benefit-amount").text shouldBe ""
      doc.select("#no-car-benefit-container").text shouldBe ""
    }

    "show car details for user with a company car and fuel benefit" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefitsForEmployer1, List.empty, List.empty)

      val result = controller.carBenefitHomeAction(johnDensmore, FakeRequest())

      status(result) should be(200)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#company-name").text shouldBe "Weyland-Yutani Corp"
      doc.select("#car-benefit-engine").text shouldBe "0-1400 cc"
      doc.select("#car-benefit-fuel-type").text shouldBe "Bi-Fuel"
      doc.select("#car-benefit-date-available").text shouldBe "December 12, 2012"
      doc.select("#car-benefit-amount").text shouldBe "£321.42"
      doc.select("#fuel-benefit-amount").text shouldBe "£22.22"
      doc.select("#no-car-benefit-container").text shouldBe ""
    }

    "show car details for user with a company car an no employer name" in new WithApplication(FakeApplication()) {
      val johnDensmoresEmploymentsWithoutName = Seq(
        Employment(sequenceNumber = 1, startDate = new LocalDate(2013, 7, 2), endDate = Some(new LocalDate(2013, 10, 8)), taxDistrictNumber = "898", payeNumber = "9900112", employerName = None, Employment.primaryEmploymentType),
        Employment(sequenceNumber = 2, startDate = new LocalDate(2013, 10, 14), endDate = None, taxDistrictNumber = "899", payeNumber = "1212121", employerName = None, 1))

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmploymentsWithoutName, johnDensmoresBenefitsForEmployer1, List.empty, List.empty)

      val result = controller.carBenefitHomeAction(johnDensmore, FakeRequest())

      status(result) should be(200)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#company-name").text shouldBe "Your employer"
      doc.select("#car-benefit-engine").text shouldBe "0-1400 cc"
      doc.select("#car-benefit-fuel-type").text shouldBe "Bi-Fuel"
      doc.select("#car-benefit-date-available").text shouldBe "December 12, 2012"
      doc.select("#car-benefit-amount").text shouldBe "£321.42"
      doc.select("#fuel-benefit-amount").text shouldBe "£22.22"
      doc.select("#no-car-benefit-container").text shouldBe ""
    }

    "show Add Car link for a user without a company car" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq.empty, List.empty, List.empty)

      val result = controller.carBenefitHomeAction(johnDensmore, FakeRequest())

      status(result) should be(200)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#no-car-benefit-container").text should not be ""
      doc.select("#company-name").text shouldBe ""
      doc.select("#car-benefit-engine").text shouldBe ""
      doc.select("#car-benefit-fuel-type").text shouldBe ""
      doc.select("#car-benefit-date-available").text shouldBe ""
      doc.select("#car-benefit-amount").text shouldBe ""
      doc.select("#fuel-benefit-amount").text shouldBe ""
    }
  }

  "calling start add car benefit" should {
    "return 200 and show the add car benefit form with the employers name  " in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq.empty, List.empty, List.empty)

      val result = controller.startAddCarBenefitAction(johnDensmore, FakeRequest(), 2013, 1)

      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#company-name").text shouldBe "Weyland-Yutani Corp"
    }
    "return 200 and show the add car benefit form and show your company if the employer name does not exist " in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq.empty, List.empty, List.empty)

      val result = controller.startAddCarBenefitAction(johnDensmore, FakeRequest(), 2013, 2)

      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#company-name").text shouldBe "Your employer"
    }
    "return 400 when employer for sequence number does not exist" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq.empty, List.empty, List.empty)

      val result = controller.startAddCarBenefitAction(johnDensmore, FakeRequest(), 2013, 5)

      status(result) shouldBe 400
    }
  }

  "submitting add car benefit" should {

    "return 200 when values form data validates successfully" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq.empty, List.empty, List.empty)

      assertSuccessfulDatesSubmit(Some(inTwoDaysTime), false,  None, false,  None)
      assertSuccessfulDatesSubmit(Some(inTwoDaysTime), true,  Some("15"), false,  None)
      assertSuccessfulDatesSubmit(Some(inTwoDaysTime), false,  None, true,  Some(endOfTaxYearMinusOne))
      assertSuccessfulDatesSubmit(None, false,  None, false,  None)
      assertSuccessfulDatesSubmit(None, false,  None, true,  Some(endOfTaxYearMinusOne))
      assertSuccessfulDatesSubmit(None, true,  Some("30"), true,  Some(endOfTaxYearMinusOne))
    }
    "ignore invalid values and return 200 when fields are not required" in new WithApplication(FakeApplication()) {
       setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq.empty, List.empty, List.empty)

      assertSuccessfulDatesSubmitWithTuple(None, false, Some("llama"), false, Some("donkey", "", ""))
    }

    "return 400 and display error when values form data fails validation" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq.empty, List.empty, List.empty)

      assertFailedDatesSubmit(Some(("2013", "9", "18")), Some("true"), Some("150"), Some("true"), Some("2013","12","31"), "error_q_2", "Car cannot be unavailable for longer than the total time you have a company car for.")
      assertFailedDatesSubmit(Some("2012","1","1"), Some("false"), None, Some("false"), None, "error_q_1", "You must specify a date within the current tax year.")
      assertFailedDatesSubmit(None, Some("false"), None, Some("true"), Some("2013","4","5"), "error_q_3", "You must specify a date within the current tax year.")
      assertFailedDatesSubmit(None, Some("false"), None, Some("true"),  Some("2014","4","6"), "error_q_3", "You must specify a date within the current tax year.")
      assertFailedDatesSubmit(None, Some("false"), None, Some("true"), None, "error_q_3", "You must specify when you will be returning your company car.")
      assertFailedDatesSubmit(Some(localDateToTuple(Some(now.plusDays(8)))), Some("false"), None, Some("false"),  None, "error_q_1", "You must specify a date, which is not more than 7 days in future from today.")
      assertFailedDatesSubmit(Some("2013","9","18"), Some("true"), Some("250"), Some("false"), None, "error_q_2", "Car cannot be unavailable for longer than the total time you have a company car for.")
      assertFailedDatesSubmit(Some("2013","9","18"), Some("true"), None, Some("true"), Some("2013","9","17"), "error_q_3", "You cannot return your car before you get it.")
      assertFailedDatesSubmit(None, Some("true"), Some("300"), Some("true"), Some("2013","12","31"), "error_q_2", "Car cannot be unavailable for longer than the total time you have a company car for.")
      assertFailedDatesSubmit(None, Some("true"), Some("367"), Some("true"), None, "error_q_2", "Car cannot be unavailable for longer than the total time you have a company car for.")
      assertFailedDatesSubmit(None, Some("true"), None, Some("false"), None, "error_q_2", "You must specify the number of consecutive days the car has been unavailable.")

      assertFailedDatesSubmit(None, Some("true"), Some("1367"), Some("false"), None, "error_q_2", "Please use whole numbers only, not decimals or other characters.")
      assertFailedDatesSubmit(None, Some("true"), Some("9.5"), Some("false"), None, "error_q_2", "Please use whole numbers only, not decimals or other characters.")
      assertFailedDatesSubmit(None, Some("true"), Some("&@^adsf"), Some("false"), None, "error_q_2", "Please use whole numbers only, not decimals or other characters.")

      assertFailedDatesSubmit(Some(("2012","1","1")), None, None, Some("false"), None, "error_q_2", "Please answer this question.")
      assertFailedDatesSubmit(Some(("2012","1","1")), Some("false"), None, None, None, "error_q_3", "Please answer this question.")

      assertFailedDatesSubmit(Some(("2013","5", "")), Some("true"), None, Some("true"), Some("2013","10","17"), "error_q_1", "You must specify a valid date")
      assertFailedDatesSubmit(Some(("2013","","1")), Some("true"), None, Some("true"), Some("2013","10","17"), "error_q_1", "You must specify a valid date")
      assertFailedDatesSubmit(Some(("","5","1")), Some("true"), None, Some("true"), Some("2013","10","17"), "error_q_1", "You must specify a valid date")
      assertFailedDatesSubmit(Some(("2013","10","32")), Some("true"), None, Some("true"), Some("2013","10","17"), "error_q_1", "You must specify a valid date")
      assertFailedDatesSubmit(Some(("2013","13","1")), Some("true"), None, Some("true"), Some("2013","10","17"), "error_q_1", "You must specify a valid date")
      assertFailedDatesSubmit(Some(("2asd","","")), Some("true"), None, Some("true"), Some("2013","10","17"), "error_q_1", "You must specify a valid date")

      assertFailedDatesSubmit(Some(("2013","10","18")), Some("true"), None, Some("true"), Some("2013","10",""), "error_q_3", "You must specify a valid date")
      assertFailedDatesSubmit(Some(("2013","10","18")), Some("true"), None, Some("true"), Some("2013","","22"), "error_q_3", "You must specify a valid date")
      assertFailedDatesSubmit(Some(("2013","10","18")), Some("true"), None, Some("true"), Some("","22","10"), "error_q_3", "You must specify a valid date")
      assertFailedDatesSubmit(Some(("2013","10","18")), Some("true"), None, Some("true"), Some("2013","13","2"), "error_q_3", "You must specify a valid date")
      assertFailedDatesSubmit(Some(("2013","10","18")), Some("true"), None, Some("true"), Some("2013","12","32"), "error_q_3", "You must specify a valid date")
      assertFailedDatesSubmit(Some(("2013","10","18")), Some("true"), None, Some("true"), Some("adssda","",""), "error_q_3", "You must specify a valid date")
    }

    "return 200 when listPrice form data validates successfully" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq.empty, List.empty, List.empty)

      assertSuccessfulListPriceSubmit(Some(1000))
      assertSuccessfulListPriceSubmit(Some(25000))
      assertSuccessfulListPriceSubmit(Some(99999))
    }

    "return 400 and display error when listPrice form data fails validation" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq.empty, List.empty, List.empty)

      assertFailedListPriceSubmit(None, "error_q_5", "You must specify the list price of your company car.")
      assertFailedListPriceSubmit(Some("999"), "error_q_5", "List price must be greater than £1000.")
      assertFailedListPriceSubmit(Some("10000.1"), "error_q_5", "Please use whole numbers only, not decimals or other characters.")
      assertFailedListPriceSubmit(Some("Ten thousand1"), "error_q_5", "Please use whole numbers only, not decimals or other characters.")
      assertFailedListPriceSubmit(Some("I own @ cat"), "error_q_5", "Please use whole numbers only, not decimals or other characters.")
      assertFailedListPriceSubmit(Some("100000"), "error_q_5", "List price must not be higher than £99999.")
    }

    "return 200 when employeeContribution form data validates successfully" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq.empty, List.empty, List.empty)

      assertSuccessfulEmployeeContributionSubmit(Some(false), None)
      assertSuccessfulEmployeeContributionSubmit(Some(true), Some(1000))
      assertSuccessfulEmployeeContributionSubmit(Some(true), Some(9999))
    }

    "return 400 and display error when employeeContribution form data fails validation" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq.empty, List.empty, List.empty)

      assertFailedEmployeeContributionSubmit(Some("true"), None, "error_q_6", "You must specify how much you paid towards the cost of the car.")
      assertFailedEmployeeContributionSubmit(Some("true"), Some("100.25"), "error_q_6", "Please use whole numbers only, not decimals or other characters.")
      assertFailedEmployeeContributionSubmit(Some("true"), Some("Ten thousand"), "error_q_6", "Please use whole numbers only, not decimals or other characters.")
      assertFailedEmployeeContributionSubmit(Some("true"), Some("I own @ cat"), "error_q_6", "Please use whole numbers only, not decimals or other characters.")
      assertFailedEmployeeContributionSubmit(Some("true"), Some("10000"), "error_q_6", "Capital contribution must not be higher than £9999.")
      assertFailedEmployeeContributionSubmit(None, None, "error_q_6", "Please answer this question.")
    }

    "return 200 when employers form data validates successfully" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq.empty, List.empty, List.empty)

      assertSuccessfulEmployerContributionSubmit(Some(false), None)
      assertSuccessfulEmployerContributionSubmit(Some(true), Some(1000))
      assertSuccessfulEmployerContributionSubmit(Some(true), Some(99999))
    }

    "return 400 and display error when employerContribution form data fails validation" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq.empty, List.empty, List.empty)

      assertFailedEmployerContributionSubmit(Some("true"), None, "error_q_7", "You must specify how much you pay your employer towards the cost of the car.")
      assertFailedEmployerContributionSubmit(Some("true"), Some("1000.25"), "error_q_7", "Please use whole numbers only, not decimals or other characters.")
      assertFailedEmployerContributionSubmit(Some("true"), Some("Ten thousand"), "error_q_7", "Please use whole numbers only, not decimals or other characters.")
      assertFailedEmployerContributionSubmit(Some("true"), Some("I own @ cat"), "error_q_7", "Please use whole numbers only, not decimals or other characters.")
      assertFailedEmployerContributionSubmit(Some("true"), Some("100000"), "error_q_7", "Employer contribution must not be higher than £99999.")
      assertFailedEmployerContributionSubmit(None, None, "error_q_7", "Please answer this question.")
    }

    def assertFailedDatesSubmit(providedFromVal: Option[(String, String, String)],
                           carUnavailableVal:  Option[String],
                           numberOfDaysUnavailableVal: Option[String],
                           giveBackThisTaxYearVal: Option[String],
                           providedToVal: Option[(String, String, String)],
                           errorId: String,
                           errorMessage: String) {

      val result = controller.saveAddCarBenefitAction(johnDensmore, newRequestForSaveAddCarBenefit(
        providedFromVal, carUnavailableVal, numberOfDaysUnavailableVal,
        giveBackThisTaxYearVal, providedToVal), 2013, 1)

      assertFailure(result, errorId, errorMessage)
    }

    def assertSuccessfulDatesSubmit(providedFromVal : Option[LocalDate],
                           carUnavailableVal:  Boolean,
                           numberOfDaysUnavailableVal: Option[String],
                           giveBackThisTaxYearVal: Boolean,
                           providedToVal: Option[LocalDate]) {

      assertSuccessfulDatesSubmitWithTuple(providedFromVal, carUnavailableVal, numberOfDaysUnavailableVal, giveBackThisTaxYearVal, Some(localDateToTuple(providedToVal)))
    }

    def assertSuccessfulDatesSubmitWithTuple(providedFromVal : Option[LocalDate],
                           carUnavailableVal:  Boolean,
                           numberOfDaysUnavailableVal: Option[String],
                           giveBackThisTaxYearVal: Boolean,
                           providedToVal: Option[(String, String, String)]) {

      val result = controller.saveAddCarBenefitAction(johnDensmore, newRequestForSaveAddCarBenefit(
        Some(localDateToTuple(providedFromVal)),
        Some(carUnavailableVal).map(_.toString), numberOfDaysUnavailableVal,
        Some(giveBackThisTaxYearVal).map(_.toString), providedToVal), 2013, 1)

      val daysUnavailable = try {numberOfDaysUnavailableVal.map(_.toInt)} catch { case _ => None}

      val expectedStoredData = CarBenefitDataBuilder(providedFrom = providedFromVal, carUnavailable = Some(carUnavailableVal),
        numberOfDaysUnavailable = daysUnavailable,
        giveBackThisTaxYear = Some(giveBackThisTaxYearVal), providedTo = tupleToLocalDate(providedToVal) )
      assertSuccess(result, expectedStoredData)
    }

    def assertFailedListPriceSubmit(listPriceVal : Option[String], errorId: String, errorMessage: String) {

      val result = controller.saveAddCarBenefitAction(johnDensmore, newRequestForSaveAddCarBenefit(listPriceVal = listPriceVal), taxYear, employmentSeqNumber)

      assertFailure(result, errorId, errorMessage)
    }

    def assertSuccessfulListPriceSubmit(listPriceVal : Option[Int]) {

      val result = controller.saveAddCarBenefitAction(johnDensmore, newRequestForSaveAddCarBenefit(listPriceVal = listPriceVal.map(_.toString)), taxYear, employmentSeqNumber)

      val expectedStoredData = CarBenefitDataBuilder(listPrice = listPriceVal)

      assertSuccess(result, expectedStoredData)

    }

    def assertFailedEmployeeContributionSubmit(employeeContributesVal: Option[String], employeeContributionVal : Option[String], errorId: String, errorMessage: String) {

      val result = controller.saveAddCarBenefitAction(johnDensmore, newRequestForSaveAddCarBenefit(employeeContributesVal = employeeContributesVal, employeeContributionVal = employeeContributionVal), 2013, 1)

      assertFailure(result, errorId, errorMessage)
    }

    def assertSuccessfulEmployeeContributionSubmit(employeeContributesVal: Option[Boolean], employeeContributionVal : Option[Int]) {

      val result = controller.saveAddCarBenefitAction(johnDensmore, newRequestForSaveAddCarBenefit(employeeContributesVal = employeeContributesVal.map(_.toString), employeeContributionVal = employeeContributionVal.map(_.toString)), 2013, 1)

      val expectedStoredData = CarBenefitDataBuilder(employeeContributes = employeeContributesVal, employeeContribution = employeeContributionVal)

      assertSuccess(result, expectedStoredData)
    }

     def assertFailedEmployerContributionSubmit(employerContributesVal: Option[String], employerContributionVal : Option[String], errorId: String, errorMessage: String) {

      val result = controller.saveAddCarBenefitAction(johnDensmore, newRequestForSaveAddCarBenefit(employerContributesVal = employerContributesVal, employerContributionVal = employerContributionVal), 2013, 1)

      assertFailure(result, errorId, errorMessage)
    }

    def assertSuccessfulEmployerContributionSubmit(employerContributesVal: Option[Boolean], employerContributionVal : Option[Int]) {

      val result = controller.saveAddCarBenefitAction(johnDensmore, newRequestForSaveAddCarBenefit(employerContributesVal = employerContributesVal.map(_.toString), employerContributionVal = employerContributionVal.map(_.toString)), 2013, 1)

      val expectedStoredData = CarBenefitDataBuilder(employerContributes = employerContributesVal, employerContribution = employerContributionVal)

      assertSuccess(result, expectedStoredData)
    }

    def assertSuccess(result: Result, collectedData: CarBenefitData)  {
      status(result) shouldBe 200
      verify(mockKeyStoreService).addKeyStoreEntry(s"AddCarBenefit:${johnDensmore.oid}:$taxYear:$employmentSeqNumber", "paye", "AddCarBenefitForm", collectedData)
    }

    def assertFailure(result: Result, errorId: String, errorMessage: String) {
      status(result) shouldBe 400
      contentAsString(result) should include(errorMessage)
      verifyZeroInteractions(mockKeyStoreService)
     // TODO: uncomment
     // val doc = Jsoup.parse(contentAsString(result))
     // doc.select(errorId).text should include(errorMessage)
    }
  }

  private def setupMocksForJohnDensmore(taxCodes: Seq[TaxCode], employments: Seq[Employment], benefits: Seq[Benefit],
                                        acceptedTransactions: List[TxQueueTransaction], completedTransactions: List[TxQueueTransaction]) {
    when(controller.payeMicroService.linkedResource[Seq[TaxCode]]("/paye/AB123456C/tax-codes/2013")).thenReturn(Some(taxCodes))
    when(controller.payeMicroService.linkedResource[Seq[Employment]]("/paye/AB123456C/employments/2013")).thenReturn(Some(employments))
    when(controller.payeMicroService.linkedResource[Seq[Benefit]]("/paye/AB123456C/benefits/2013")).thenReturn(Some(benefits))
    when(controller.txQueueMicroService.transaction(Matchers.matches("^/txqueue/current-status/paye/AB123456C/ACCEPTED/.*"))).thenReturn(Some(acceptedTransactions))
    when(controller.txQueueMicroService.transaction(Matchers.matches("^/txqueue/current-status/paye/AB123456C/COMPLETED/.*"))).thenReturn(Some(completedTransactions))
  }

  private def newRequestForSaveAddCarBenefit(providedFromVal : Option[(String, String, String)] = Some(localDateToTuple(defaultProvidedFrom)),
                                       carUnavailableVal:  Option[String] = Some(defaultCarUnavailable.toString),
                                       numberOfDaysUnavailableVal: Option[String] = defaultNumberOfDaysUnavailable,
                                       giveBackThisTaxYearVal: Option[String] = Some(defaultGiveBackThisTaxYear.toString),
                                       providedToVal: Option[(String, String, String)] = defaultProvidedTo,
                                       listPriceVal : Option[String] = Some(defaultListPrice.toString),
                                       employeeContributesVal: Option[String] = Some(defaultEmployeeContributes.toString),
                                       employeeContributionVal : Option[String] = defaultEmployeeContribution,
                                       employerContributesVal: Option[String] = Some(defaultEmployerContributes.toString),
                                       employerContributionVal : Option[String] = defaultEmployerContribution) = {

        FakeRequest().withFormUrlEncodedBody(Seq(
          carUnavailable -> carUnavailableVal.getOrElse("").toString,
          numberOfDaysUnavailable -> numberOfDaysUnavailableVal.getOrElse(""),
          giveBackThisTaxYear -> giveBackThisTaxYearVal.getOrElse("").toString,
          listPrice -> listPriceVal.getOrElse(""),
          employeeContributes -> employeeContributesVal.getOrElse(""),
          employeeContribution -> employeeContributionVal.getOrElse(""),
          employerContributes -> employerContributesVal.getOrElse(""),
          employerContribution -> employerContributionVal.getOrElse(""))
          ++ buildDateFormField(providedFrom, providedFromVal)
          ++ buildDateFormField(providedTo, providedToVal) : _*)
  }


    def localDateToTuple(date : Option[LocalDate]) : (String, String, String) = {
      (date.map(_.getYear.toString).getOrElse(""),date.map(_.getMonthOfYear.toString).getOrElse(""),date.map(_.getDayOfMonth.toString).getOrElse(""))
    }

    def tupleToLocalDate(tupleDate : Option[(String,String,String)]) : Option[LocalDate] = {
      tupleDate match {
        case Some((y,m,d)) => try {
          Some(new LocalDate(y.toInt, m.toInt, d.toInt))
        } catch {
          case _ => None
        }
        case _ => None
      }
    }

    def buildDateFormField(fieldName: String, value : Option[(String, String, String)] ) : Seq[(String, String)] = {
      Seq((fieldName + "." + "day" -> value.map(_._3).getOrElse("")),
        (fieldName + "." + "month" -> value.map(_._2).getOrElse("")),
        (fieldName + "." + "year" -> value.map(_._1).getOrElse("")))
    }
}

object CarBenefitDataBuilder {

  val now: LocalDate = new LocalDate(2013, 10, 3)
  val inTwoDaysTime = now.plusDays(2)
  val inThreeDaysTime = now.plusDays(3)
  val endOfTaxYearMinusOne = new LocalDate(2014, 4, 4)
  val defaultListPrice = 1000
  val defaultEmployeeContributes = false
  val defaultEmployeeContribution = None
  val defaultEmployerContributes = false
  val defaultEmployerContribution = None
  val defaultCarUnavailable = false
  val defaultNumberOfDaysUnavailable = None
  val defaultGiveBackThisTaxYear = false
  val defaultProvidedTo = None
  val defaultProvidedFrom = Some(now.plusDays(2))

  def apply(providedFrom: Option[LocalDate] = defaultProvidedFrom,
            carUnavailable: Option[Boolean] = Some(defaultCarUnavailable),
            numberOfDaysUnavailable: Option[Int] = defaultNumberOfDaysUnavailable,
            giveBackThisTaxYear: Option[Boolean] = Some(defaultGiveBackThisTaxYear),
            providedTo: Option[LocalDate] = defaultProvidedTo,
            listPrice: Option[Int] = Some(defaultListPrice),
            employeeContributes: Option[Boolean] = Some(defaultEmployeeContributes),
            employeeContribution: Option[Int] = defaultEmployeeContribution,
            employerContributes: Option[Boolean] = Some(defaultEmployerContributes),
            employerContribution: Option[Int] = defaultEmployerContribution ) = {

    CarBenefitData(providedFrom = providedFrom,
                  carUnavailable = carUnavailable,
                  numberOfDaysUnavailable = numberOfDaysUnavailable,
                  giveBackThisTaxYear = giveBackThisTaxYear,
                  providedTo = providedTo,
                  listPrice = Some(defaultListPrice),
                  employeeContributes = Some(defaultEmployeeContributes),
                  employeeContribution = defaultEmployeeContribution,
                  employerContributes = Some(defaultEmployerContributes),
                  employerContribution = defaultEmployerContribution)
  }
}