package controllers.paye

import play.api.test.{FakeRequest, WithApplication}
import play.api.test.Helpers._
import org.jsoup.Jsoup
import uk.gov.hmrc.common.microservice.paye.domain._
import org.joda.time.LocalDate
import controllers.DateFieldsHelper
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import org.mockito.{Matchers, ArgumentCaptor}
import org.mockito.Mockito._
import controllers.paye.CarBenefitFormFields._
import CarBenefitDataBuilder._
import play.api.i18n.Messages
import uk.gov.hmrc.common.microservice.paye.{domain, PayeConnector}
import org.scalatest.matchers.{MatchResult, Matcher}
import concurrent.Future
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector
import BenefitTypes._
import uk.gov.hmrc.common.microservice.txqueue.domain.TxQueueTransaction
import uk.gov.hmrc.common.microservice.paye.domain.AddBenefitResponse
import play.api.mvc.SimpleResult
import uk.gov.hmrc.common.microservice.paye.domain.Car
import play.api.test.FakeApplication
import uk.gov.hmrc.common.microservice.paye.domain.TaxCode
import uk.gov.hmrc.common.microservice.paye.domain.TransactionId
import uk.gov.hmrc.common.microservice.paye.domain.NewBenefitCalculationResponse
import org.joda.time.chrono.ISOChronology
import models.paye.{CarAndFuelBuilder, CarBenefitData, CarBenefitDataAndCalculations}
import org.scalatest.TestData
import controllers.common.actions.HeaderCarrier

class AddCarBenefitControllerSpec extends PayeBaseSpec with DateFieldsHelper {

  val mockKeyStoreService = mock[KeyStoreConnector]
  val mockPayeConnector = mock[PayeConnector]
  val mockTxQueueConnector = mock[TxQueueConnector]
  val mockAuthConnector = mock[AuthConnector]
  val mockAuditConnector = mock[AuditConnector]

  private lazy val controller = new AddCarBenefitController(mockKeyStoreService, mockAuditConnector, mockAuthConnector)(mockPayeConnector, mockTxQueueConnector) with MockedTaxYearSupport {
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
    "return 200 and show the add car benefit form with the employers name  " in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()

      val result = Future.successful(controller.startAddCarBenefitAction(johnDensmore, FakeRequest(), taxYear, employmentSeqNumberOne))

      result should haveStatus(200)
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#company-name").text shouldBe "Weyland-Yutani Corp"
    }

    "return 200 and show the add car benefit form with the required fields and no values filled in" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()

      val result = Future.successful(controller.startAddCarBenefitAction(johnDensmore, FakeRequest(), taxYear, employmentSeqNumberOne))

      result should haveStatus(200)

      verify(mockKeyStoreService).getEntry[CarBenefitDataAndCalculations](s"AddCarBenefit:$johnDensmoreOid:$taxYear:$employmentSeqNumberOne", "paye", "AddCarBenefitForm")
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("[id~=providedFrom]").select("[id~=day]") should not be empty
      doc.select("#carUnavailable-true")  should not be empty
      doc.select("#carUnavailable-true").attr("checked") shouldBe empty

      doc.select("#numberOfDaysUnavailable")  should not be empty
      doc.select("#numberOfDaysUnavailable").attr("value")  shouldBe empty

      doc.select("#carUnavailable-false")  should not be empty
      doc.select("#giveBackThisTaxYear-true")  should not be empty
      doc.select("#giveBackThisTaxYear-true").attr("checked") shouldBe empty

      doc.select("#giveBackThisTaxYear-false")  should not be empty
      doc.select("[id~=providedTo]").select("[id~=day]") should not be empty
      doc.select("#listPrice")  should not be empty
      doc.select("#listPrice").attr("value")  shouldBe empty

      doc.select("#employeeContributes-false")  should not be empty

      doc.select("#employeeContributes-true")  should not be empty
      doc.select("#employeeContributes-true").attr("checked") shouldBe empty

      doc.select("#employeeContribution")  should not be empty
      doc.select("#employeeContribution").attr("value")  shouldBe empty

      doc.select("#employerContributes-false")  should not be empty
      doc.select("#employerContributes-true")  should not be empty
      doc.select("#employerContribution")  should not be empty
      doc.select("#employerContribution").attr("value")  shouldBe empty


      doc.select("[id~=carRegistrationDate]").select("[id~=day]") should not be empty
      doc.select("[id~=carRegistrationDate]").select("[id~=year]").attr("value") shouldBe empty

      doc.select("#fuelType-diesel")  should not be empty
      doc.select("#fuelType-diesel").attr("checked") shouldBe empty

      doc.select("#fuelType-electricity")  should not be empty
      doc.select("#fuelType-other")  should not be empty
      doc.select("#engineCapacity-none")  should not be empty
      doc.select("#engineCapacity-1400")  should not be empty
      doc.select("#engineCapacity-1400").attr("checked") shouldBe empty

      doc.select("#engineCapacity-2000")  should not be empty
      doc.select("#engineCapacity-9999")  should not be empty
      doc.select("#employerPayFuel-true")  should not be empty
      doc.select("#employerPayFuel-false")  should not be empty
      doc.select("#employerPayFuel-again")  should not be empty
      doc.select("#employerPayFuel-date")  should not be empty
      doc.select("#employerPayFuel-date").attr("checked") shouldBe empty

      doc.select("[id~=dateFuelWithdrawn]").select("[id~=day]") should not be empty
      doc.select("#co2Figure")  should not be empty
      doc.select("#co2NoFigure")  should not be empty
      doc.select("#co2NoFigure").attr("checked") shouldBe empty

    }

    "return 200 and show the add car benefit form and show your company if the employer name does not exist " in new WithApplication(FakeApplication()) {
      val johnDensmoresNamelessEmployments = Seq(
        Employment(sequenceNumber = employmentSeqNumberOne, startDate = new LocalDate(taxYear, 7, 2), endDate = Some(new LocalDate(taxYear, 10, 8)), taxDistrictNumber = "898", payeNumber = "9900112", employerName = None, Employment.primaryEmploymentType))

      setupMocksForJohnDensmore(employments = johnDensmoresNamelessEmployments)

      val result = Future.successful(controller.startAddCarBenefitAction(johnDensmore, FakeRequest(), taxYear, employmentSeqNumberOne))

      result should haveStatus(200)
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#company-name").text shouldBe "your employer"
    }
    "return 400 when employer for sequence number does not exist" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()

      val result = Future.successful(controller.startAddCarBenefitAction(johnDensmore, FakeRequest(), taxYear, 5))

      result should haveStatus(400)
    }
    "return to the car benefit home page if the user already has a car benefit" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(benefits = johnDensmoresBenefitsForEmployer1)

      val result = Future.successful(controller.startAddCarBenefitAction(johnDensmore, FakeRequest(), taxYear, employmentSeqNumberOne))
      result should haveStatus(303)
      redirectLocation(result) shouldBe Some(routes.CarBenefitHomeController.carBenefitHome.url)
    }
    "return 400 if the requested tax year is not the current tax year" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()

      val result = Future.successful(controller.startAddCarBenefitAction(johnDensmore, FakeRequest(), taxYear+1, employmentSeqNumberOne))

      result should haveStatus(400)
    }
    "return 400 if the employer is not the primary employer" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()

      val result = Future.successful(controller.startAddCarBenefitAction(johnDensmore, FakeRequest(), taxYear, 2))

      result should haveStatus(400)
    }
  }

  "submitting add car benefit" should {

    "return to the car benefit home page if the user already has a car benefit" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(benefits = johnDensmoresBenefitsForEmployer1)

      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, newRequestForSaveAddCarBenefit(), taxYear, employmentSeqNumberOne))
      result should haveStatus(303)
      redirectLocation(result) shouldBe Some(routes.CarBenefitHomeController.carBenefitHome.url)
    }

    "return 200 when values form data validates successfully" in new WithApplication(FakeApplication()) {      
      assertSuccessfulDatesSubmitJohnDensmore(Some(inTwoDaysTime), false,  None, false,  None)
      assertSuccessfulDatesSubmitJohnDensmore(Some(inTwoDaysTime), true,  Some("15"), false,  None)
      assertSuccessfulDatesSubmitJohnDensmore(Some(inTwoDaysTime), false,  None, true,  Some(endOfTaxYearMinusOne))
      assertSuccessfulDatesSubmitJohnDensmore(None, false,  None, false,  None)
      assertSuccessfulDatesSubmitJohnDensmore(None, false,  None, true,  Some(endOfTaxYearMinusOne))
      assertSuccessfulDatesSubmitJohnDensmore(None, true,  Some("30"), true,  Some(endOfTaxYearMinusOne))

    }

    "return 200 for a successful combination of fields" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore()

      val now = Some(new LocalDate)
      val registrationDate = new LocalDate().withYear(1995)
      val request = newRequestForSaveAddCarBenefit(
        carRegistrationDateVal = Some(localDateToTuple(Some(registrationDate))),
        fuelTypeVal = Some("diesel"),
        co2FigureVal = Some("20"),
        co2NoFigureVal = Some("false"),
        engineCapacityVal= Some("1400"),
        employerPayFuelVal = Some("date"),
        dateFuelWithdrawnVal = Some(localDateToTuple(now))
      )

      val keyStoreDataCaptor = ArgumentCaptor.forClass(classOf[CarBenefitDataAndCalculations])

      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne))
      result should haveStatus(200)

      verify(mockKeyStoreService).addKeyStoreEntry(
        Matchers.any,
        Matchers.any,
        Matchers.any,
        keyStoreDataCaptor.capture()) (Matchers.any())

        val data = keyStoreDataCaptor.getValue.carBenefitData
        data.carRegistrationDate shouldBe Some(registrationDate)
        data.fuelType shouldBe Some("diesel")
        data.co2Figure shouldBe Some(20)
        data.co2NoFigure shouldBe Some(false)
        data.engineCapacity shouldBe Some("1400")
        data.employerPayFuel shouldBe Some("date")
        data.dateFuelWithdrawn shouldBe now
    }

    "return 200 for a successful combination of fields including engine capacity is not available" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore()

      val now = Some(new LocalDate)
      val registrationDate = new LocalDate().withYear(2000)
      val request = newRequestForSaveAddCarBenefit(
        carRegistrationDateVal = Some(localDateToTuple(Some(registrationDate))),
        fuelTypeVal = Some("electricity"),
        co2FigureVal = None,
        co2NoFigureVal = None,
        engineCapacityVal= Some("none"),
        employerPayFuelVal = Some("false")
      )

      val keyStoreDataCaptor = ArgumentCaptor.forClass(classOf[CarBenefitDataAndCalculations])

      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne))
      result should haveStatus(200)

      verify(mockKeyStoreService).addKeyStoreEntry(
        Matchers.any,
        Matchers.any,
        Matchers.any,
        keyStoreDataCaptor.capture()) (Matchers.any())

      val data = keyStoreDataCaptor.getValue.carBenefitData
      data.carRegistrationDate shouldBe Some(registrationDate)
      data.fuelType shouldBe Some("electricity")
      data.co2Figure shouldBe None
      data.co2NoFigure shouldBe None
      data.engineCapacity shouldBe Some("none")
      data.employerPayFuel shouldBe Some("false")
    }

    "ignore invalid values and return 200 when fields are not required" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()

      assertSuccessfulDatesSubmitWithTuple(None, false, Some("llama"), false, Some(("donkey", "", "")))
    }

    "return 400 and display error when values form data fails validation" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()

      assertFailedDatesSubmit(Some((s"$taxYear", "9", "18")), Some("true"), Some("150"), Some("true"), Some((s"$taxYear","12","31")), "error_q_2", "Car cannot be unavailable for longer than the total time you have a company car for.")
      assertFailedDatesSubmit(Some(("2012","1","1")), Some("false"), None, Some("false"), None, "error_q_1", "You must specify a date within the current tax year.")
      assertFailedDatesSubmit(None, Some("false"), None, Some("true"), Some((s"$taxYear","4","5")), "error_q_3", "You must specify a date within the current tax year.")
      assertFailedDatesSubmit(None, Some("false"), None, Some("true"),  Some((s"${taxYear+1}","4","6")), "error_q_3", "You must specify a date within the current tax year.")
      assertFailedDatesSubmit(None, Some("false"), None, Some("true"), None, "error_q_3", "You must specify when you will be returning your company car.")
      assertFailedDatesSubmit(Some(localDateToTuple(Some(now.plusDays(8)))), Some("false"), None, Some("false"),  None, "error_q_1", "You must specify a date, which is not more than 7 days in future from today.")
      assertFailedDatesSubmit(Some((s"$taxYear","9","18")), Some("true"), Some("250"), Some("false"), None, "error_q_2", "Car cannot be unavailable for longer than the total time you have a company car for.")
      assertFailedDatesSubmit(Some((s"$taxYear","9","18")), Some("true"), None, Some("true"), Some((s"$taxYear","9","17")), "error_q_3", "You cannot return your car before you get it.")
      assertFailedDatesSubmit(None, Some("true"), Some("300"), Some("true"), Some((s"$taxYear","12","31")), "error_q_2", "Car cannot be unavailable for longer than the total time you have a company car for.")
      assertFailedDatesSubmit(None, Some("true"), Some("367"), Some("true"), None, "error_q_2", "Car cannot be unavailable for longer than the total time you have a company car for.")
      assertFailedDatesSubmit(None, Some("true"), None, Some("false"), None, "error_q_2", "You must specify the number of consecutive days the car has been unavailable.")

      assertFailedDatesSubmit(None, Some("true"), Some("1367"), Some("false"), None, "error_q_2", "Please enter a number of 3 characters or less.")
      assertFailedDatesSubmit(None, Some("true"), Some("9.5"), Some("false"), None, "error_q_2", "Please use whole numbers only, not decimals or other characters.")
      assertFailedDatesSubmit(None, Some("true"), Some("&@^adsf"), Some("false"), None, "error_q_2", "Please use whole numbers only, not decimals or other characters.")

      assertFailedDatesSubmit(Some(("2012","1","1")), None, None, Some("false"), None, "error_q_2", "Please answer this question.")
      assertFailedDatesSubmit(Some(("2012","1","1")), Some("false"), None, None, None, "error_q_3", "Please answer this question.")

      assertFailedDatesSubmit(Some((s"$taxYear","5", "")), Some("true"), None, Some("true"), Some((s"$taxYear","10","17")), "error_q_1", "You must specify a valid date")
      assertFailedDatesSubmit(Some((s"$taxYear","","1")), Some("true"), None, Some("true"), Some((s"$taxYear","10","17")), "error_q_1", "You must specify a valid date")
      assertFailedDatesSubmit(Some(("","5","1")), Some("true"), None, Some("true"), Some((s"$taxYear","10","17")), "error_q_1", "You must specify a valid date")
      assertFailedDatesSubmit(Some((s"$taxYear","10","32")), Some("true"), None, Some("true"), Some((s"$taxYear","10","17")), "error_q_1", "You must specify a valid date")
      assertFailedDatesSubmit(Some((s"$taxYear","13","1")), Some("true"), None, Some("true"), Some((s"$taxYear","10","17")), "error_q_1", "You must specify a valid date")
      assertFailedDatesSubmit(Some(("2asd","","")), Some("true"), None, Some("true"), Some((s"$taxYear","10","17")), "error_q_1", "You must specify a valid date")

      assertFailedDatesSubmit(Some((s"$taxYear","10","18")), Some("true"), None, Some("true"), Some((s"$taxYear","10","")), "error_q_3", "You must specify a valid date")
      assertFailedDatesSubmit(Some((s"$taxYear","10","18")), Some("true"), None, Some("true"), Some((s"$taxYear","","22")), "error_q_3", "You must specify a valid date")
      assertFailedDatesSubmit(Some((s"$taxYear","10","18")), Some("true"), None, Some("true"), Some(("","22","10")), "error_q_3", "You must specify a valid date")
      assertFailedDatesSubmit(Some((s"$taxYear","10","18")), Some("true"), None, Some("true"), Some((s"$taxYear","13","2")), "error_q_3", "You must specify a valid date")
      assertFailedDatesSubmit(Some((s"$taxYear","10","18")), Some("true"), None, Some("true"), Some((s"$taxYear","12","32")), "error_q_3", "You must specify a valid date")
      assertFailedDatesSubmit(Some((s"$taxYear","10","18")), Some("true"), None, Some("true"), Some(("adssda","","")), "error_q_3", "You must specify a valid date")
    }

    "return 200 when listPrice form data validates successfully" in new WithApplication(FakeApplication()) {
      assertSuccessfulListPriceSubmit(Some(1000))
      assertSuccessfulListPriceSubmit(Some(25000))
      assertSuccessfulListPriceSubmit(Some(99999))
    }

    "return 400 and display error when listPrice form data fails validation" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()

      assertFailedListPriceSubmit(None, "error_q_5", "You must specify the list price of your company car.")
      assertFailedListPriceSubmit(Some("999"), "error_q_5", "List price must be greater than or equal to £1,000.")
      assertFailedListPriceSubmit(Some("10000.1"), "error_q_5", "Please use whole numbers only, not decimals or other characters.")
      assertFailedListPriceSubmit(Some("Ten thousand1"), "error_q_5", "Please use whole numbers only, not decimals or other characters.")
      assertFailedListPriceSubmit(Some("I own @ cat"), "error_q_5", "Please use whole numbers only, not decimals or other characters.")
      assertFailedListPriceSubmit(Some("100000"), "error_q_5", "List price must not be higher than £99,999.")
    }

    "return 200 when employeeContribution form data validates successfully" in new WithApplication(FakeApplication()) {

      assertSuccessfulEmployeeContributionSubmit(Some(false), None, None)
      assertSuccessfulEmployeeContributionSubmit(Some(true), Some("1000"), Some(1000))
      assertSuccessfulEmployeeContributionSubmit(Some(true), Some("9999"), Some(9999))
      assertSuccessfulEmployeeContributionSubmit(Some(false), Some("0"), None)
      assertSuccessfulEmployeeContributionSubmit(Some(false), Some("5.5"), None)
    }

    "return 400 and display error when employeeContribution form data fails validation" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()

      assertFailedEmployeeContributionSubmit(Some("true"), None, "error_q_6", "You must specify how much you paid towards the cost of the car.")
      assertFailedEmployeeContributionSubmit(Some("true"), Some("100.25"), "error_q_6", "Please use whole numbers only, not decimals or other characters.")
      assertFailedEmployeeContributionSubmit(Some("true"), Some("Ten thousand"), "error_q_6", "Please use whole numbers only, not decimals or other characters.")
      assertFailedEmployeeContributionSubmit(Some("true"), Some("I own @ cat"), "error_q_6", "Please use whole numbers only, not decimals or other characters.")
      assertFailedEmployeeContributionSubmit(Some("true"), Some("10000"), "error_q_6", "Capital contribution must not be higher than £9,999.")
      assertFailedEmployeeContributionSubmit(None, None, "error_q_6", "Please answer this question.")
      assertFailedEmployeeContributionSubmit(Some("true"), Some("0"), "error_q_6", "Capital contribution must be greater than zero if you have selected yes.")
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

      assertFailedEmployerContributionSubmit(Some("true"), None, "error_q_7", "You must specify how much you pay your employer towards the cost of the car.")
      assertFailedEmployerContributionSubmit(Some("true"), Some("1000.25"), "error_q_7", "Please use whole numbers only, not decimals or other characters.")
      assertFailedEmployerContributionSubmit(Some("true"), Some("Ten thousand"), "error_q_7", "Please use whole numbers only, not decimals or other characters.")
      assertFailedEmployerContributionSubmit(Some("true"), Some("I own @ cat"), "error_q_7", "Please use whole numbers only, not decimals or other characters.")
      assertFailedEmployerContributionSubmit(Some("true"), Some("100000"), "error_q_7", "Employee payment must not be higher than £99,999.")
      assertFailedEmployerContributionSubmit(None, None, "error_q_7", "Please answer this question.")
      assertFailedEmployerContributionSubmit(Some("true"), Some("0"), "error_q_7", "Payment towards private use must be greater than zero if you have selected yes.")
    }

    "return 400 if the submitting is for year that is not the current tax year" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()

      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, newRequestForSaveAddCarBenefit(), taxYear+1, employmentSeqNumberOne))

      result should haveStatus(400)
    }

    "return 400 if the submitting is for employment number that is not the primary employment" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()

      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, newRequestForSaveAddCarBenefit(), taxYear, 2))

      result should haveStatus(400)
    }

    "return 200 if the user submits selects an option for the registered before 98 question" in new WithApplication(FakeApplication()){
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(carRegistrationDateVal = Some(localDateToTuple(Some(LocalDate.now.withYear(1996)))))
      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne))
      result should haveStatus(200)
    }

    "return 400 if the user does not select any option for the registered before 98 question" in new WithApplication(FakeApplication()){
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(carRegistrationDateVal = None)
      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne))
      result should haveStatus(400)
    }

    "return 400 if the user sends an invalid value for the registered before 98 question" in new WithApplication(FakeApplication()){
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(carRegistrationDateVal  = Some(("hacking!", "garbage", "Hotdogs!")))
      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne))
      result should haveStatus(400)
    }

    "return 400 if the user sends a car registered date before 1900" in new WithApplication(FakeApplication()){
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(carRegistrationDateVal  = Some(("1899", "7", "1")))
      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne))
      result should haveStatus(400)
    }

    "keep the selected option in the car registered date question if the validation fails due to another reason" in new WithApplication(FakeApplication()){
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(carRegistrationDateVal = Some((taxYear.toString,"5","29")), carUnavailableVal = None)
      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne))
      result should haveStatus(400)
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("[id~=carRegistrationDate]").select("[id~=day-29]").attr("selected") shouldBe "selected"
      doc.select("[id~=carRegistrationDate]").select("[id~=month-5]").attr("selected") shouldBe "selected"
      doc.select("[id~=carRegistrationDate]").select(s"[id~=year").attr("value") shouldBe taxYear.toString

    }

    "return 200 if the user selects an option for the FUEL TYPE question" in new WithApplication(FakeApplication()){
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(fuelTypeVal = Some("electricity"), engineCapacityVal = None, co2NoFigureVal = None)
      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne))
      result should haveStatus(200)
    }

    "return 400 if the user does not select any option for the FUEL TYPE question" in new WithApplication(FakeApplication()){
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(fuelTypeVal = None)
      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne))
      result should haveStatus(400)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".error-notification").text should include(Messages("error.paye.answer_mandatory"))
    }

    "return 400 if the user sends an invalid value for the FUEL TYPE question" in new WithApplication(FakeApplication()){
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(fuelTypeVal = Some("hacking!"))
      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne))
      result should haveStatus(400)
    }

    "keep the selected option in the FUEL TYPE question if the validation fails due to another reason" in new WithApplication(FakeApplication()){
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(fuelTypeVal = Some("electricity"), carUnavailableVal = None)
      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne))
      result should haveStatus(400)
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#fuelType-electricity").attr("checked") shouldBe "checked"
    }

    "return 200 if the user enters a valid integer for the CO2 FIGURE question" in new WithApplication(FakeApplication()){
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(co2FigureVal = Some("123"), co2NoFigureVal = Some("false"))
      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne))
      result should haveStatus(200)
    }

    "return 400 if the user sends an invalid value for the CO2 FIGURE question" in new WithApplication(FakeApplication()){
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(co2FigureVal = Some("hacking!"))
      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne))
      result should haveStatus(400)
    }

    "keep the selected option in the CO2 FIGURE question if the validation fails due to another reason" in new WithApplication(FakeApplication()){
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(co2FigureVal = Some("123"), carUnavailableVal = None)
      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne))
      result should haveStatus(400)
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#co2Figure").attr("value") shouldBe "123"
    }

    "return 200 if the user selects the option for the CO2 NO FIGURE" in new WithApplication(FakeApplication()){
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(co2NoFigureVal = Some("true"))
      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne))
      result should haveStatus(200)
    }

    "return 400 if the user sends an invalid value for the option CO2 NO VALUE" in new WithApplication(FakeApplication()){
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(co2NoFigureVal = Some("hacking!"))
      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne))
      result should haveStatus(400)
    }

    "keep the checkbox elected for the CO2 NO VALUE option if the validation fails due to another reason" in new WithApplication(FakeApplication()){
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(co2NoFigureVal = Some("true"), carUnavailableVal = None)
      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne))
      result should haveStatus(400)
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#co2NoFigure").attr("value") shouldBe "true"
      doc.select("#co2NoFigure").attr("checked") shouldBe "checked"
    }

    "return 200 if the user selects an option for the ENGINE CAPACITY question" in new WithApplication(FakeApplication()){
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(engineCapacityVal = Some("2000"))
      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne))
      result should haveStatus(200)
    }

    "return 400 if the user sends an invalid value for the ENGINE CAPACITY question" in new WithApplication(FakeApplication()){
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(engineCapacityVal = Some("hacking!"))
      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne))
      result should haveStatus(400)
    }

    "keep the selected option in the ENGINE CAPACITY question if the validation fails due to another reason" in new WithApplication(FakeApplication()){
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(engineCapacityVal = Some("none"), carUnavailableVal = None)
      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne))
      result should haveStatus(400)
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#engineCapacity-none").attr("checked") shouldBe "checked"
    }

    "return 200 if the user selects an option for the EMPLOYER PAY FUEL question" in new WithApplication(FakeApplication()){
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(employerPayFuelVal = Some("again"))
      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne))
      result should haveStatus(200)
    }

    "return 400 if the user does not select any option for the EMPLOYER PAY FUEL question" in new WithApplication(FakeApplication()){
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(employerPayFuelVal = None)
      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne))
      result should haveStatus(400)
      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".error-notification").text should be(Messages("error.paye.answer_mandatory"))
    }

    "return 400 if the user sends an invalid value for the EMPLOYER PAY FUEL question" in new WithApplication(FakeApplication()){
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(employerPayFuelVal = Some("hacking!"))
      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne))
      result should haveStatus(400)
    }

    "keep the selected option in the EMPLOYER PAY FUEL question if the validation fails due to another reason" in new WithApplication(FakeApplication()){
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(employerPayFuelVal = Some("date"), dateFuelWithdrawnVal = Some((taxYear.toString,"05","30")), carUnavailableVal = None)
      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne))
      result should haveStatus(400)
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#employerPayFuel-date").attr("checked") shouldBe "checked"
      doc.select("[id~=dateFuelWithdrawn]").select("[id~=day-30]").attr("selected") shouldBe "selected"
    }

    "store the values the user has entered and the benefit calculations " in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddCarBenefit(engineCapacityVal = Some("2000"))
      val carBenefitValue = 100
      val fuelBenefitValue = 500
      when(mockPayeConnector.calculateBenefitValue(Matchers.anyString(), Matchers.any[CarAndFuel])).thenReturn(Some(NewBenefitCalculationResponse(Some(carBenefitValue),Some(fuelBenefitValue))))
      val keyStoreDataCaptor = ArgumentCaptor.forClass(classOf[CarBenefitDataAndCalculations])
      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, request, taxYear, employmentSeqNumberOne))
      result should haveStatus(200)
      verify(mockKeyStoreService).addKeyStoreEntry(Matchers.eq(s"AddCarBenefit:${johnDensmore.oid}:$taxYear:$employmentSeqNumberOne"),
        Matchers.eq("paye"), Matchers.eq("AddCarBenefitForm"), keyStoreDataCaptor.capture())(Matchers.eq(manifest[CarBenefitDataAndCalculations]))
      val returnedData : CarBenefitDataAndCalculations = keyStoreDataCaptor.getValue
      returnedData.carBenefitValue shouldBe carBenefitValue
      returnedData.fuelBenefitValue shouldBe Some(fuelBenefitValue)
      returnedData.carBenefitData shouldBe CarBenefitDataBuilder(engineCapacity = Some("2000") )
    }


    def assertFailedDatesSubmit(providedFromVal: Option[(String, String, String)],
                                carUnavailableVal:  Option[String],
                                numberOfDaysUnavailableVal: Option[String],
                                giveBackThisTaxYearVal: Option[String],
                                providedToVal: Option[(String, String, String)],
                                errorId: String,
                                errorMessage: String) {

      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, newRequestForSaveAddCarBenefit(
        providedFromVal, carUnavailableVal, numberOfDaysUnavailableVal,
        giveBackThisTaxYearVal, providedToVal), taxYear, employmentSeqNumberOne))

      assertFailure(result, errorId, errorMessage)
    }

    def assertSuccessfulDatesSubmitJohnDensmore(providedFromVal : Option[LocalDate],
                                    carUnavailableVal:  Boolean,
                                    numberOfDaysUnavailableVal: Option[String],
                                    giveBackThisTaxYearVal: Boolean,
                                    providedToVal: Option[LocalDate]) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq.empty, List.empty, List.empty)
      assertSuccessfulDatesSubmitWithTuple(providedFromVal, carUnavailableVal, numberOfDaysUnavailableVal, giveBackThisTaxYearVal, Some(localDateToTuple(providedToVal)))
    }

    def assertSuccessfulDatesSubmitWithTuple(providedFromVal : Option[LocalDate],
                                             carUnavailableVal:  Boolean,
                                             numberOfDaysUnavailableVal: Option[String],
                                             giveBackThisTaxYearVal: Boolean,
                                             providedToVal: Option[(String, String, String)]) {

      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, newRequestForSaveAddCarBenefit(
        Some(localDateToTuple(providedFromVal)),
        Some(carUnavailableVal).map(_.toString), numberOfDaysUnavailableVal,
        Some(giveBackThisTaxYearVal).map(_.toString), providedToVal), taxYear, employmentSeqNumberOne))

      val daysUnavailable = try {numberOfDaysUnavailableVal.map(_.toInt)} catch { case _:Throwable => None}

      val expectedStoredData = CarBenefitDataBuilder(providedFrom = providedFromVal, carUnavailable = Some(carUnavailableVal),
        numberOfDaysUnavailable = daysUnavailable,
        giveBackThisTaxYear = Some(giveBackThisTaxYearVal), providedTo = tupleToLocalDate(providedToVal) )
      assertSuccess(result, expectedStoredData)
    }

    def assertFailedListPriceSubmit(listPriceVal : Option[String], errorId: String, errorMessage: String) {

      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, newRequestForSaveAddCarBenefit(listPriceVal = listPriceVal), taxYear, employmentSeqNumberOne))

      assertFailure(result, errorId, errorMessage)
    }

    def assertSuccessfulListPriceSubmit(listPriceVal : Option[Int]) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq.empty, List.empty, List.empty)

      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, newRequestForSaveAddCarBenefit(listPriceVal = listPriceVal.map(_.toString)), taxYear, employmentSeqNumberOne))

      val expectedStoredData = CarBenefitDataBuilder(listPrice = listPriceVal)

      assertSuccess(result, expectedStoredData)

    }

    def assertFailedEmployeeContributionSubmit(employeeContributesVal: Option[String], employeeContributionVal : Option[String], errorId: String, errorMessage: String) {

      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, newRequestForSaveAddCarBenefit(employeeContributesVal = employeeContributesVal, employeeContributionVal = employeeContributionVal), taxYear, employmentSeqNumberOne))

      assertFailure(result, errorId, errorMessage)
    }


    def assertSuccessfulEmployeeContributionSubmit(employeeContributesVal: Option[Boolean], employeeContributionVal : Option[String], expectedContributionVal : Option[Int]) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq.empty, List.empty, List.empty)

      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, newRequestForSaveAddCarBenefit(employeeContributesVal = employeeContributesVal.map(_.toString), employeeContributionVal = employeeContributionVal), taxYear, employmentSeqNumberOne))

      val expectedStoredData = CarBenefitDataBuilder(employeeContributes = employeeContributesVal, employeeContribution = expectedContributionVal)

      assertSuccess(result, expectedStoredData)
    }

    def assertFailedEmployerContributionSubmit(employerContributesVal: Option[String], employerContributionVal : Option[String], errorId: String, errorMessage: String) {

      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, newRequestForSaveAddCarBenefit(employerContributesVal = employerContributesVal, employerContributionVal = employerContributionVal), taxYear, employmentSeqNumberOne))

      assertFailure(result, errorId, errorMessage)
    }


    def assertSuccessfulEmployerContributionSubmit(employerContributesVal: Option[Boolean], employerContributionVal : Option[String], expectedContributionVal : Option[Int]) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq.empty, List.empty, List.empty)

      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, newRequestForSaveAddCarBenefit(employerContributesVal = employerContributesVal.map(_.toString), employerContributionVal = employerContributionVal), taxYear, employmentSeqNumberOne))

      val expectedStoredData = CarBenefitDataBuilder(employerContributes = employerContributesVal, employerContribution = expectedContributionVal)

      assertSuccess(result, expectedStoredData)
    }

    def assertSuccess(result: Future[SimpleResult], collectedData: CarBenefitData)  {
      val carAndFuel = CarAndFuelBuilder(carBenefitDataAndCalculations = CarBenefitDataAndCalculations(collectedData, 0, Some(0)), taxYear = 2013, employmentSequenceNumber = 1)
      result should haveStatus(200)
      val carAndFuelCaptor = ArgumentCaptor.forClass(classOf[CarAndFuel])
      verify(mockPayeConnector).calculateBenefitValue(Matchers.eq("/calculation/paye/benefit/new/value-calculation"), carAndFuelCaptor.capture())
      carAndFuelCaptor.getValue shouldBe carAndFuel
      reset(mockKeyStoreService)
      reset(mockPayeConnector)
    }

    def assertFailure(result: Future[SimpleResult], errorId: String, errorMessage: String) {
      result should haveStatus(400)
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
      when(mockPayeConnector.calculateBenefitValue("/calculation/paye/benefit/new/value-calculation", carAndFuel)) thenReturn Some(NewBenefitCalculationResponse(Some(999), None))
       val result =  Future.successful(controller.reviewAddCarBenefitAction(johnDensmore,
         newRequestForSaveAddCarBenefit(carRegistrationDateVal = Some(localDateToTuple(Some(carRegistrationDate))),
         providedFromVal = None,
         providedToVal = None,
         fuelTypeVal = Some(fuelType),
         engineCapacityVal = None,
         co2NoFigureVal = None,
         employeeContributesVal = Some("true"),
         employeeContributionVal = Some(userContribution.toString),
         listPriceVal = Some(listPrice.toString))
         , taxYear, employmentSeqNumberOne))


      verify(mockPayeConnector).calculateBenefitValue("/calculation/paye/benefit/new/value-calculation", carAndFuel)

      result should haveStatus(200)
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#carBenefitTaxableValue").text shouldBe "£999"
      doc.select("#fuelBenefitTaxableValue").isEmpty shouldBe true
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
      val uri = s"paye/car-benefit/$taxYear/$employmentSeqNumberOne/add"
      val updatedCar = car.copy(dateCarRegistered = Some(carRegistrationDate), fuelType=Some("diesel"), co2Emissions = Some(50), engineSize=Some(1400))
      val updatedCarAndFuel = carAndFuel.copy(carBenefit= carBenefit.copy(car = Some(updatedCar)))
      when(mockPayeConnector.calculateBenefitValue("/calculation/paye/benefit/new/value-calculation", updatedCarAndFuel)) thenReturn Some(NewBenefitCalculationResponse(Some(999), Some(444)))

      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore,
        newRequestForSaveAddCarBenefit(carRegistrationDateVal = Some(localDateToTuple(Some(carRegistrationDate))),
          providedFromVal = None,
          providedToVal = None,
          fuelTypeVal = Some(fuelType),
          engineCapacityVal = Some(engineCapacity.toString),
          co2NoFigureVal = None,
          co2FigureVal = Some(co2Emission.toString),
          employeeContributesVal = Some("true"),
          employeeContributionVal = Some(userContribution.toString),
          listPriceVal = Some(listPrice.toString),
          path = uri)
        , taxYear, employmentSeqNumberOne))
      result should haveStatus(200)

      verify(mockPayeConnector).calculateBenefitValue("/calculation/paye/benefit/new/value-calculation", updatedCarAndFuel)
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#carBenefitTaxableValue").text shouldBe "£999"
      doc.select("#fuelBenefitTaxableValue").text shouldBe "£444"
      doc.select("#edit-data").text shouldBe "This information is wrong"
      doc.select("#edit-data").attr("href") shouldBe uri
    }

    "handle the case where no provided from or provided to dates are returned from the key store."  in new WithApplication(FakeApplication()){
      setupMocksForJohnDensmore()

      val employmentSeqNumberOne = johnDensmoresEmployments(0).sequenceNumber
      val taxYear = controller.currentTaxYear
      val carBenefitData = new CarBenefitData(providedFrom = None,
        carUnavailable = Some(true), numberOfDaysUnavailable = Some(1),
        giveBackThisTaxYear = Some(true), carRegistrationDate = Some(new LocalDate(1950, 9, 13)), providedTo = None , listPrice = Some(1000),
        employeeContributes = Some(true),
        employeeContribution = Some(50),
        employerContributes = Some(true),
        employerContribution = Some(999),
        fuelType = Some("diesel"),
        co2Figure = None,
        co2NoFigure = Some(true),
        engineCapacity = Some("1400"),
        employerPayFuel = Some("date"),
        dateFuelWithdrawn = Some(new LocalDate(taxYear, 8, 29)))

      when(mockKeyStoreService.getEntry[CarBenefitData](s"AddCarBenefit:$johnDensmoreOid:$taxYear:$employmentSeqNumberOne", "paye", "AddCarBenefitForm")).thenReturn(Some(carBenefitData))

      val result = Future.successful(controller.startAddCarBenefitAction(johnDensmore, FakeRequest(), taxYear, employmentSeqNumberOne))

      result should haveStatus(200)

      verify(mockKeyStoreService).getEntry[CarBenefitDataAndCalculations](s"AddCarBenefit:$johnDensmoreOid:$taxYear:$employmentSeqNumberOne", "paye", "AddCarBenefitForm")
    }

    "allow the user to reedit the form with other fuel type and show it with values already filled" in new WithApplication(FakeApplication()){

      setupMocksForJohnDensmore()

      val employmentSeqNumberOne = johnDensmoresEmployments(0).sequenceNumber
      val taxYear = controller.currentTaxYear
      val carBenefitData = new CarBenefitData(providedFrom = Some(new LocalDate(taxYear, 7, 29)),
        carUnavailable = Some(true), numberOfDaysUnavailable = Some(1),
        giveBackThisTaxYear = Some(true), carRegistrationDate = Some(new LocalDate(1950, 9, 13)), providedTo = Some(new LocalDate(taxYear, 8, 30)) , listPrice = Some(1000),
        employeeContributes = Some(false),
        employeeContribution = None,
        employerContributes = Some(true),
        employerContribution = Some(999),
        fuelType = Some("other"),
        co2Figure = None,
        co2NoFigure = Some(true),
        engineCapacity = Some("1400"),
        employerPayFuel = Some("date"),
        dateFuelWithdrawn = Some(new LocalDate(taxYear, 8, 29)))

      when(mockKeyStoreService.getEntry[CarBenefitDataAndCalculations](s"AddCarBenefit:$johnDensmoreOid:$taxYear:$employmentSeqNumberOne", "paye", "AddCarBenefitForm")).thenReturn(Some(CarBenefitDataAndCalculations(carBenefitData, 0, None)))

      val result = Future.successful(controller.startAddCarBenefitAction(johnDensmore, FakeRequest(), taxYear, employmentSeqNumberOne))

      result should haveStatus(200)
      verify(mockKeyStoreService).getEntry[CarBenefitDataAndCalculations](s"AddCarBenefit:$johnDensmoreOid:$taxYear:$employmentSeqNumberOne", "paye", "AddCarBenefitForm")

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("[id~=providedFrom]").select("[id~=day-29]").attr("selected") shouldBe "selected"
      doc.select("[id~=providedFrom]").select("[id~=month-7]").attr("selected") shouldBe "selected"
      doc.select("[id~=providedFrom]").select(s"[id~=year-$taxYear]").attr("selected") shouldBe "selected"
      doc.select("#carUnavailable-true").attr("checked") shouldBe "checked"
      doc.select("#numberOfDaysUnavailable").attr("value")  shouldBe "1"
      doc.select("#giveBackThisTaxYear-true").attr("checked") shouldBe "checked"
      doc.select("[id~=providedTo]").select("[id~=day-30]").attr("selected") shouldBe "selected"
      doc.select("[id~=providedTo]").select("[id~=month-8]").attr("selected") shouldBe "selected"
      doc.select("[id~=providedTo]").select(s"[id~=year-$taxYear]").attr("selected") shouldBe "selected"
      doc.select("#listPrice").attr("value")  shouldBe "1000"
      doc.select("#employeeContributes-true").attr("checked") shouldBe empty
      doc.select("#employeeContribution").attr("value")  shouldBe empty
      doc.select("#employerContributes-true").attr("checked") shouldBe "checked"
      doc.select("#employerContribution").attr("value")  shouldBe "999"

      doc.select("[id~=carRegistrationDate]").select("[id~=day-13]").attr("selected") shouldBe "selected"
      doc.select("[id~=carRegistrationDate]").select("[id~=month-9]").attr("selected") shouldBe "selected"
      doc.select("[id~=carRegistrationDate]").select("[id~=year]").attr("value") shouldBe "1950"
      doc.select("#fuelType-other").attr("checked") shouldBe "checked"
      doc.select("#engineCapacity-1400").attr("checked") shouldBe "checked"
      doc.select("#employerPayFuel-date").attr("checked") shouldBe "checked"
      doc.select("[id~=dateFuelWithdrawn]").select("[id~=day-29]").attr("selected") shouldBe "selected"
      doc.select("[id~=dateFuelWithdrawn]").select("[id~=month-8]").attr("selected") shouldBe "selected"
      doc.select("[id~=dateFuelWithdrawn]").select(s"[id~=year-$taxYear]").attr("selected") shouldBe "selected"
      doc.select("#co2NoFigure").attr("checked") shouldBe "checked"
    }

    "allow the user to reedit the form with electric fuel type and show it with values already filled" in new WithApplication(FakeApplication()){

      setupMocksForJohnDensmore()

      val employmentSeqNumberOne = johnDensmoresEmployments(0).sequenceNumber
      val taxYear = controller.currentTaxYear
      val carBenefitData = new CarBenefitData(providedFrom = Some(new LocalDate(taxYear, 7, 29)),
        carUnavailable = Some(false), numberOfDaysUnavailable = None,
        giveBackThisTaxYear = Some(false), carRegistrationDate = Some(new LocalDate(1950, 9, 13)), providedTo = None , listPrice = Some(1000),
        employeeContributes = Some(true),
        employeeContribution = Some(100),
        employerContributes = Some(true),
        employerContribution = Some(999),
        fuelType = Some("electricity"),
        co2Figure = None,
        co2NoFigure = None,
        engineCapacity = Some("none"),
        employerPayFuel = Some("false"),
        dateFuelWithdrawn = None)

      when(mockKeyStoreService.getEntry[CarBenefitDataAndCalculations](s"AddCarBenefit:$johnDensmoreOid:$taxYear:$employmentSeqNumberOne", "paye", "AddCarBenefitForm")).thenReturn(Some(CarBenefitDataAndCalculations(carBenefitData, 0, None)))

      val result = Future.successful(controller.startAddCarBenefitAction(johnDensmore, FakeRequest(), taxYear, employmentSeqNumberOne))

      result should haveStatus(200)
      verify(mockKeyStoreService).getEntry[CarBenefitDataAndCalculations](s"AddCarBenefit:$johnDensmoreOid:$taxYear:$employmentSeqNumberOne", "paye", "AddCarBenefitForm")

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("[id~=providedFrom]").select("[id~=day-29]").attr("selected") shouldBe "selected"
      doc.select("[id~=providedFrom]").select("[id~=month-7]").attr("selected") shouldBe "selected"
      doc.select("[id~=providedFrom]").select(s"[id~=year-$taxYear]").attr("selected") shouldBe "selected"
      doc.select("#carUnavailable-false").attr("checked") shouldBe "checked"
      doc.select("#numberOfDaysUnavailable").attr("value")  shouldBe ""
      doc.select("#giveBackThisTaxYear-false").attr("checked") shouldBe "checked"
      doc.select("#listPrice").attr("value")  shouldBe "1000"
      doc.select("#employeeContributes-true").attr("checked") shouldBe "checked"
      doc.select("#employeeContribution").attr("value")  shouldBe "100"
      doc.select("#employerContributes-true").attr("checked") shouldBe "checked"
      doc.select("#employerContribution").attr("value")  shouldBe "999"

      doc.select("[id~=carRegistrationDate]").select("[id~=day-13]").attr("selected") shouldBe "selected"
      doc.select("[id~=carRegistrationDate]").select("[id~=month-9]").attr("selected") shouldBe "selected"
      doc.select("[id~=carRegistrationDate]").select("[id~=year]").attr("value") shouldBe "1950"
      doc.select("#fuelType-electricity").attr("checked") shouldBe "checked"
      doc.select("#engineCapacity-none").attr("checked") shouldBe "checked"
      doc.select("#employerPayFuel-false").attr("checked") shouldBe "checked"
    }


    "allow the user to confirm the addition of the car benefit" in  new WithApplication(FakeApplication()){
      setupMocksForJohnDensmore()

      when(mockPayeConnector.calculateBenefitValue(Matchers.any(), Matchers.any())).thenReturn(Some(NewBenefitCalculationResponse(Some(999), Some(444))))

      val result = Future.successful(controller.reviewAddCarBenefitAction(johnDensmore, newRequestForSaveAddCarBenefit(), taxYear, employmentSeqNumberOne))

      result should haveStatus(200)

      val expectedUri = routes.AddCarBenefitController.confirmAddingBenefit(taxYear, employmentSeqNumberOne).url
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("form").attr("action") shouldBe expectedUri

    }
  }

  "confirm submission of add car benefit" should {

    val carBenefitDataAndCalculations = CarBenefitDataAndCalculations(CarBenefitData(providedFrom = None,
      carUnavailable = Some(true), numberOfDaysUnavailable = Some(1),
      giveBackThisTaxYear = Some(true), carRegistrationDate = Some(new LocalDate(1950, 9, 13)), providedTo = None , listPrice = Some(1000),
      employeeContributes = Some(true),
      employeeContribution = Some(50),
      employerContributes = Some(true),
      employerContribution = Some(999),
      fuelType = Some("diesel"),
      co2Figure = None,
      co2NoFigure = Some(true),
      engineCapacity = Some("1400"),
      employerPayFuel = Some("date"),
      dateFuelWithdrawn = Some(new LocalDate(taxYear, 8, 29))), 87, Some(12))

    "remove the saved data for the car benefit form values" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore()
      when(mockKeyStoreService.getEntry[CarBenefitDataAndCalculations](s"AddCarBenefit:$johnDensmoreOid:$taxYear:$employmentSeqNumberOne", "paye", "AddCarBenefitForm")).thenReturn(Some(carBenefitDataAndCalculations))
      when(mockPayeConnector.addBenefits(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Some(AddBenefitResponse(transaction = TransactionId("aTransactionId"), newTaxCode = Some("bla2"), netCodedAllowance = Some(123))))

      val result = Future.successful(controller.confirmAddingBenefitAction(johnDensmore, FakeRequest(), taxYear, employmentSeqNumberOne))
      result should haveStatus(200)

      verify(mockKeyStoreService).deleteKeyStore(s"AddCarBenefit:$johnDensmoreOid:$taxYear:$employmentSeqNumberOne", "paye")
    }

    "show the user a confirmation page"  in new WithApplication(FakeApplication()){
      setupMocksForJohnDensmore()
      when(mockKeyStoreService.getEntry[CarBenefitDataAndCalculations](s"AddCarBenefit:$johnDensmoreOid:$taxYear:$employmentSeqNumberOne", "paye", "AddCarBenefitForm")).thenReturn(Some(carBenefitDataAndCalculations))
      when(mockPayeConnector.addBenefits(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Some(AddBenefitResponse(transaction = TransactionId("aTransactionId"), newTaxCode = Some("bla2"), netCodedAllowance = Some(123))))
      val result = Future.successful(controller.confirmAddingBenefitAction(johnDensmore, FakeRequest(), taxYear, employmentSeqNumberOne))
      result should haveStatus(200)

      val doc = Jsoup.parse(contentAsString(result))
      doc.title should include("Your Tax - Benefits Added")
    }

    "call the paye microservice to add a new benefit for a car only" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore()

      val carBenefitData: CarBenefitData = CarBenefitData(providedFrom = None,
        carUnavailable = Some(true), numberOfDaysUnavailable = Some(1),
        giveBackThisTaxYear = Some(true), carRegistrationDate = Some(new LocalDate(1950, 9, 13)), providedTo = None, listPrice = Some(1000),
        employeeContributes = Some(true),
        employeeContribution = Some(50),
        employerContributes = Some(true),
        employerContribution = Some(999),
        fuelType = Some("diesel"),
        co2Figure = None,
        co2NoFigure = Some(true),
        engineCapacity = Some("1400"),
        employerPayFuel = Some("false"),
        dateFuelWithdrawn = Some(new LocalDate(taxYear, 8, 29)))
      val fuelBenefitGrossAmount  = None
      val carGrossAmount = 204
      val carBenefitDataAndCalculation = CarBenefitDataAndCalculations(carBenefitData, carGrossAmount, None)

      when(mockPayeConnector.addBenefits(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Some(AddBenefitResponse(transaction = TransactionId("aTransactionId"), newTaxCode = Some("bla2"), netCodedAllowance = Some(123))))
      when(mockKeyStoreService.getEntry[CarBenefitDataAndCalculations](s"AddCarBenefit:$johnDensmoreOid:$taxYear:$employmentSeqNumberOne", "paye", "AddCarBenefitForm")).thenReturn(Some(carBenefitDataAndCalculation))

      val result = Future.successful(controller.confirmAddingBenefitAction(johnDensmore, FakeRequest(), taxYear, employmentSeqNumberOne))
      result should haveStatus(200)

      val car = Car(dateCarMadeAvailable = carBenefitData.providedFrom,
        dateCarWithdrawn = carBenefitData.providedTo,
        dateCarRegistered = carBenefitData.carRegistrationDate,
        employeeCapitalContribution = carBenefitData.employeeContribution.map(BigDecimal(_)),
        fuelType = carBenefitData.fuelType,
        co2Emissions = carBenefitData.co2Figure,
        engineSize = carBenefitData.engineCapacity.map(_.toInt),
        mileageBand = None,
        carValue = carBenefitData.listPrice.map(BigDecimal(_)),
        employeePayments = carBenefitData.employerContribution.map(BigDecimal(_)),
        daysUnavailable = carBenefitData.numberOfDaysUnavailable
      )
      val benefit = Benefit(benefitType = CAR,
        taxYear = taxYear,
        grossAmount = carGrossAmount,
        employmentSequenceNumber = employmentSeqNumberOne,
        costAmount = None,
        amountMadeGood = None,
        cashEquivalent = None,
        expensesIncurred = None,
        amountOfRelief = None,
        paymentOrBenefitDescription = None,
        dateWithdrawn = carBenefitData.providedTo,
        car = Some(car),
        actions = Map.empty[String, String],
        calculations = Map.empty[String, String])

      verify(mockPayeConnector).addBenefits(s"/paye/${johnDensmore.getPaye.nino}/benefits/2013", johnDensmore.getPaye.version, employmentSeqNumberOne, Seq(benefit))
    }

    "call the paye microservice to add new benefits for both car and fuel" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore()

      val carBenefitData = CarBenefitData(providedFrom = None,
        carUnavailable = Some(true), numberOfDaysUnavailable = Some(1),
        giveBackThisTaxYear = Some(true), carRegistrationDate = Some(new LocalDate(1950, 9, 13)), providedTo = None , listPrice = Some(1000),
        employeeContributes = Some(true),
        employeeContribution = Some(50),
        employerContributes = Some(true),
        employerContribution = Some(999),
        fuelType = Some("diesel"),
        co2Figure = None,
        co2NoFigure = Some(true),
        engineCapacity = Some("1400"),
        employerPayFuel = Some("date"),
        dateFuelWithdrawn = Some(new LocalDate(taxYear, 8, 29)))
      val carBenefitGrossAmount: Int = 764
      val fuelBenefitGrossAmount: Int = 85
      val carBenefitDataAndCalculations =   CarBenefitDataAndCalculations(carBenefitData, carBenefitGrossAmount, Some(fuelBenefitGrossAmount))

      when(mockPayeConnector.addBenefits(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Some(AddBenefitResponse(transaction = TransactionId("aTransactionId"), newTaxCode = Some("bla2"), netCodedAllowance = Some(123))))
      when(mockKeyStoreService.getEntry[CarBenefitDataAndCalculations](s"AddCarBenefit:$johnDensmoreOid:$taxYear:$employmentSeqNumberOne", "paye", "AddCarBenefitForm")).thenReturn(Some(carBenefitDataAndCalculations))

      val result = Future.successful(controller.confirmAddingBenefitAction(johnDensmore, FakeRequest(), taxYear, employmentSeqNumberOne))
      result should haveStatus(200)

      val car = Car(dateCarMadeAvailable = carBenefitData.providedFrom,
        dateCarWithdrawn = carBenefitData.providedTo,
        dateCarRegistered = carBenefitData.carRegistrationDate,
        employeeCapitalContribution = carBenefitData.employeeContribution.map(BigDecimal(_)),
        fuelType = carBenefitData.fuelType,
        co2Emissions = carBenefitData.co2Figure,
        engineSize = carBenefitData.engineCapacity.map(_.toInt),
        mileageBand = None,
        carValue = carBenefitData.listPrice.map(BigDecimal(_)),
        employeePayments = carBenefitData.employerContribution.map(BigDecimal(_)),
        daysUnavailable = carBenefitData.numberOfDaysUnavailable
      )
      val carBenefit = Benefit(benefitType = CAR,
        taxYear = taxYear,
        grossAmount = carBenefitGrossAmount,
        employmentSequenceNumber = employmentSeqNumberOne,
        costAmount = None,
        amountMadeGood = None,
        cashEquivalent = None,
        expensesIncurred = None,
        amountOfRelief = None,
        paymentOrBenefitDescription = None,
        dateWithdrawn = carBenefitData.providedTo,
        car = Some(car),
        actions = Map.empty[String, String],
        calculations = Map.empty[String, String])

      val fuelBenefit = Benefit(benefitType = FUEL,
        taxYear = taxYear,
        grossAmount = fuelBenefitGrossAmount,
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
        calculations = Map.empty[String, String])

      verify(mockPayeConnector).addBenefits(s"/paye/${johnDensmore.getPaye.nino}/benefits/2013", johnDensmore.getPaye.version, employmentSeqNumberOne, Seq(carBenefit,fuelBenefit))
    }

    "show the confirmation page with the correct tax codes and allowance" in new WithApplication(FakeApplication())  {
      setupMocksForJohnDensmore()
      val carBenefitDataAndCalculations = CarBenefitDataAndCalculations(CarBenefitData(providedFrom = None,
        carUnavailable = Some(true), numberOfDaysUnavailable = Some(1),
        giveBackThisTaxYear = Some(true), carRegistrationDate = Some(new LocalDate(1950, 9, 13)), providedTo = None , listPrice = Some(1000),
        employeeContributes = Some(true),
        employeeContribution = Some(50),
        employerContributes = Some(true),
        employerContribution = Some(999),
        fuelType = Some("diesel"),
        co2Figure = None,
        co2NoFigure = Some(true),
        engineCapacity = Some("1400"),
        employerPayFuel = Some("date"),
        dateFuelWithdrawn = Some(new LocalDate(taxYear, 8, 29))), 43, Some(9))


      when(mockPayeConnector.addBenefits(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())).thenReturn(Some(AddBenefitResponse(transaction = TransactionId("aTransactionId"), newTaxCode = Some("aNewTaxCoe"), netCodedAllowance = Some(123))))
      when(mockKeyStoreService.getEntry[CarBenefitDataAndCalculations](s"AddCarBenefit:$johnDensmoreOid:$taxYear:$employmentSeqNumberOne", "paye", "AddCarBenefitForm")).thenReturn(Some(carBenefitDataAndCalculations))
      val result = Future.successful(controller.confirmAddingBenefitAction(johnDensmore, FakeRequest(), taxYear, employmentSeqNumberOne))
      result should haveStatus(200)
      print(contentAsString(result))
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#old-tax-code").text shouldBe "430L"
      doc.select("#new-tax-code").text shouldBe "aNewTaxCoe"
      doc.select("#personal-allowance").text shouldBe "£123"
    }
  }

  private def haveStatus(expectedStatus:Int) = new Matcher[Future[SimpleResult]]{
    def apply(response:Future[SimpleResult]) = {
      val actualStatus = status(response)
      MatchResult(actualStatus == expectedStatus , s"Expected result with status $expectedStatus but was $actualStatus.", s"Expected result with status other than $expectedStatus, but was actually $actualStatus.")
    }
  }

  private def setupMocksForJohnDensmore(taxCodes: Seq[TaxCode] = johnDensmoresTaxCodes, employments: Seq[Employment] = johnDensmoresEmployments, benefits: Seq[Benefit] = Seq.empty,
                                        acceptedTransactions: List[TxQueueTransaction] = List.empty, completedTransactions: List[TxQueueTransaction] =  List.empty) {

    implicit val hc = HeaderCarrier()
    when(mockPayeConnector.linkedResource[Seq[TaxCode]](s"/paye/AB123456C/tax-codes/$taxYear")).thenReturn(Some(taxCodes))
    when(mockPayeConnector.linkedResource[Seq[Employment]](s"/paye/AB123456C/employments/$taxYear")).thenReturn(Some(employments))
    when(mockPayeConnector.linkedResource[Seq[Benefit]](s"/paye/AB123456C/benefits/$taxYear")).thenReturn(Some(benefits))
    when(mockTxQueueConnector.transaction(Matchers.matches("^/txqueue/current-status/paye/AB123456C/ACCEPTED/.*"))).thenReturn(Some(acceptedTransactions))
    when(mockTxQueueConnector.transaction(Matchers.matches("^/txqueue/current-status/paye/AB123456C/COMPLETED/.*"))).thenReturn(Some(completedTransactions))
    when(mockKeyStoreService.getEntry[CarBenefitData](Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(None)
    when(mockPayeConnector.calculateBenefitValue(Matchers.anyString(), Matchers.any[CarAndFuel])).thenReturn(Some(NewBenefitCalculationResponse(Some(1000),None)))
  }

  private def newRequestForSaveAddCarBenefit(providedFromVal : Option[(String, String, String)] = Some(localDateToTuple(Some(defaultProvidedFrom))),
                                             carUnavailableVal:  Option[String] = Some(defaultCarUnavailable.toString),
                                             numberOfDaysUnavailableVal: Option[String] = defaultNumberOfDaysUnavailable,
                                             giveBackThisTaxYearVal: Option[String] = Some(defaultGiveBackThisTaxYear.toString),
                                             providedToVal: Option[(String, String, String)] = defaultProvidedTo,
                                             listPriceVal : Option[String] = Some(defaultListPrice.toString),
                                             employeeContributesVal: Option[String] = Some(defaultEmployeeContributes.toString),
                                             employeeContributionVal : Option[String] = defaultEmployeeContribution,
                                             employerContributesVal: Option[String] = Some(defaultEmployerContributes.toString),
                                             employerContributionVal : Option[String] = defaultEmployerContribution,
                                             carRegistrationDateVal: Option[(String, String, String)] =  Some(localDateToTuple(Some(defaultCarRegistrationDate))),
                                             fuelTypeVal:Option[String]= Some(defaultFuelType.toString),
                                             co2FigureVal: Option[String] = defaultCo2Figure,
                                             co2NoFigureVal: Option[String] = Some(defaultCo2NoFigure.toString),
                                             engineCapacityVal: Option[String] = Some(defaultEngineCapacity.toString),
                                             employerPayFuelVal: Option[String] = Some(defaultEmployerPayFuel.toString),
                                             dateFuelWithdrawnVal: Option[(String, String, String)] = Some(localDateToTuple(defaultDateFuelWithdrawn)),
                                              path:String = "") = {

    FakeRequest("GET", path).withFormUrlEncodedBody(Seq(
      carUnavailable -> carUnavailableVal.getOrElse(""),
      numberOfDaysUnavailable -> numberOfDaysUnavailableVal.getOrElse(""),
      giveBackThisTaxYear -> giveBackThisTaxYearVal.getOrElse(""),
      listPrice -> listPriceVal.getOrElse(""),
      employeeContributes -> employeeContributesVal.getOrElse(""),
      employeeContribution -> employeeContributionVal.getOrElse(""),
      employerContributes -> employerContributesVal.getOrElse(""),
      employerContribution -> employerContributionVal.getOrElse(""),
      fuelType -> fuelTypeVal.getOrElse(""),
      co2Figure -> co2FigureVal.getOrElse(""),
      co2NoFigure -> co2NoFigureVal.getOrElse(""),
      engineCapacity -> engineCapacityVal.getOrElse(""),
      employerPayFuel -> employerPayFuelVal.getOrElse(""))
      ++ buildDateFormField(providedFrom, providedFromVal)
      ++ buildDateFormField(dateFuelWithdrawn, dateFuelWithdrawnVal)
      ++ buildDateFormField(providedTo, providedToVal)
      ++ buildDateFormField(carRegistrationDate, carRegistrationDateVal) : _*)
  }
}

object CarBenefitDataBuilder {
  val taxYear = 2013
  val employmentSeqNumberOne = 1

  val now: LocalDate = new LocalDate(taxYear, 10, 3)
  val inTwoDaysTime = now.plusDays(2)
  val inThreeDaysTime = now.plusDays(3)
  val endOfTaxYearMinusOne = new LocalDate(taxYear+1, 4, 4)
  val defaultListPrice = 1000
  val defaultEmployeeContributes = false
  val defaultEmployeeContribution = None
  val defaultEmployerContributes = false
  val defaultEmployerContribution = None
  val defaultCarUnavailable = false
  val defaultNumberOfDaysUnavailable = None
  val defaultGiveBackThisTaxYear = false
  val defaultFuelType = "diesel"
  val defaultProvidedTo = None
  val defaultProvidedFrom = now.plusDays(2)
  val defaultCarRegistrationDate = now.minusYears(1)
  val defaultCo2Figure = None
  val defaultCo2NoFigure = true
  val defaultEngineCapacity = 1400
  val defaultEmployerPayFuel = "false"
  val defaultDateFuelWithdrawn = None

  def apply(providedFrom: Option[LocalDate] = Some(defaultProvidedFrom),
            carUnavailable: Option[Boolean] = Some(defaultCarUnavailable),
            numberOfDaysUnavailable: Option[Int] = defaultNumberOfDaysUnavailable,
            giveBackThisTaxYear: Option[Boolean] = Some(defaultGiveBackThisTaxYear),
            carRegistrationDate: Option[LocalDate] = Some(defaultCarRegistrationDate),
            providedTo: Option[LocalDate] = defaultProvidedTo,
            listPrice: Option[Int] = Some(defaultListPrice),
            employeeContributes: Option[Boolean] = Some(defaultEmployeeContributes),
            employeeContribution: Option[Int] = defaultEmployeeContribution,
            employerContributes: Option[Boolean] = Some(defaultEmployerContributes),
            employerContribution: Option[Int] = defaultEmployerContribution,
            fuelType:Option[String] = Some(defaultFuelType),
            co2Figure: Option[Int] = defaultCo2Figure,
            co2NoFigure: Option[Boolean] = Some(defaultCo2NoFigure),
            engineCapacity: Option[String] = Some(defaultEngineCapacity.toString),
            employerPayFuel: Option[String] = Some(defaultEmployerPayFuel),
            dateFuelWithdrawn: Option[LocalDate] = defaultDateFuelWithdrawn) = {

    CarBenefitData(providedFrom = providedFrom,
      carUnavailable = carUnavailable,
      numberOfDaysUnavailable = numberOfDaysUnavailable,
      giveBackThisTaxYear = giveBackThisTaxYear,
      carRegistrationDate = carRegistrationDate,
      providedTo = providedTo,
      listPrice = listPrice,
      employeeContributes = employeeContributes,
      employeeContribution = employeeContribution,
      employerContributes = employerContributes,
      employerContribution = employerContribution,
      fuelType = fuelType,
      co2Figure = co2Figure,
      co2NoFigure = co2NoFigure,
      engineCapacity = engineCapacity,
      employerPayFuel = employerPayFuel,
      dateFuelWithdrawn = dateFuelWithdrawn)
  }
}

private trait WithCarAndFuelBenefit {
  val car = Car(dateCarMadeAvailable = None,dateCarWithdrawn = None,dateCarRegistered = Some(localDate(2011, 10, 3)),
    employeeCapitalContribution = Some(100),fuelType = Some("electricity"),
    co2Emissions = None,engineSize = None,mileageBand = None,
    carValue = Some(9999),employeePayments = None,daysUnavailable = None)

  val carBenefit = Benefit(benefitType = BenefitTypes.CAR, taxYear = 2013,
    grossAmount = 0, employmentSequenceNumber = 1, costAmount = None,
    amountMadeGood = None, cashEquivalent = None, expensesIncurred = None,
    amountOfRelief = None, paymentOrBenefitDescription = None,
    dateWithdrawn = None, car = Some(car),
    actions = Map.empty, calculations = Map.empty)
  val fuelBenefit = None
  val carAndFuel = domain.CarAndFuel(carBenefit, fuelBenefit)
  def localDate(year : Int, month : Int, day : Int) = new LocalDate(year, month, day, ISOChronology.getInstanceUTC)
}