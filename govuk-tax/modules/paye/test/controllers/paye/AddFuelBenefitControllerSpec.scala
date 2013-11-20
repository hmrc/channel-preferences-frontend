package controllers.paye

import play.api.test.{FakeRequest, WithApplication}
import scala.concurrent.{ExecutionContext, Await, Future}
import org.jsoup.Jsoup
import play.api.test.Helpers._
import uk.gov.hmrc.common.microservice.paye.domain._
import org.joda.time.LocalDate
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import org.scalatest.matchers.{MatchResult, Matcher}
import scala.Some
import play.api.mvc.SimpleResult
import play.api.test.FakeApplication
import org.mockito.Mockito._
import FuelBenefitFormFields._
import controllers.DateFieldsHelper
import play.api.i18n.Messages
import org.mockito.{Mockito, ArgumentCaptor, Matchers}
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import ExecutionContext.Implicits.global
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector

class AddFuelBenefitControllerSpec  extends PayeBaseSpec with DateFieldsHelper{

  override lazy val testTaxYear = 2012
  private val employmentSeqNumberOne = 1

  val mockPayeConnector = mock[PayeConnector]
  val mockTxQueueConnector = mock[TxQueueConnector]
  val mockAuthConnector = mock[AuthConnector]
  val mockAuditConnector = mock[AuditConnector]
  val mockKeyStoreService = mock[KeyStoreConnector]

  before {
    Mockito.reset(mockKeyStoreService)
  }

  private lazy val controller = new AddFuelBenefitController(mockKeyStoreService, mockAuditConnector, mockAuthConnector)(mockPayeConnector, mockTxQueueConnector)  with MockedTaxYearSupport {
    override def currentTaxYear = testTaxYear
  }

  "calling start add fuel benefit" should {
    "return 200 and show the fuel page with the employer s name" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore()

      val result = Future.successful(controller.startAddFuelBenefitAction(johnDensmore, FakeRequest(), testTaxYear, employmentSeqNumberOne))

      result should haveStatus(200)
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#company-name").text shouldBe "Weyland-Yutani Corp"
      doc.select("#heading").text should include("company fuel")
    }

    "return 200 and show the add fuel benefit form with the required fields and no values filled in" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore()

      val result = Future.successful(controller.startAddFuelBenefitAction(johnDensmore, FakeRequest(), testTaxYear, employmentSeqNumberOne))

      result should haveStatus(200)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#employerPayFuel-false") shouldBe empty
      doc.select("#employerPayFuel-true") should not be empty
      doc.select("#employerPayFuel-again") should not be empty
      doc.select("#employerPayFuel-date") should not be empty
      doc.select("#employerPayFuel-date").attr("checked") shouldBe empty
    }

    "return 200 and show the page for the fuel form with default employer name message if employer name does not exist " in new WithApplication(FakeApplication()) {

      val johnDensmoresNamelessEmployments = Seq(
        Employment(sequenceNumber = employmentSeqNumberOne, startDate = new LocalDate(testTaxYear, 7, 2), endDate = Some(new LocalDate(testTaxYear, 10, 8)), taxDistrictNumber = "898", payeNumber = "9900112", employerName = None, Employment.primaryEmploymentType))

      setupMocksForJohnDensmore(employments = johnDensmoresNamelessEmployments)

      val result = Future.successful(controller.startAddFuelBenefitAction(johnDensmore, FakeRequest(), testTaxYear, employmentSeqNumberOne))

      result should haveStatus(200)
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#company-name").text shouldBe "your employer"
    }

    "return 400 when employer for sequence number does not exist" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore()

      val result = Future.successful(controller.startAddFuelBenefitAction(johnDensmore, FakeRequest(), testTaxYear, 5))

      result should haveStatus(400)
    }

    "return to the car benefit home page if the user already has a fuel benefit" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore(benefits = johnDensmoresBenefitsForEmployer1)

      val result = Future.successful(controller.startAddFuelBenefitAction(johnDensmore, FakeRequest(), testTaxYear, employmentSeqNumberOne))
      result should haveStatus(303)
      redirectLocation(result) shouldBe Some(routes.CarBenefitHomeController.carBenefitHome.url)
    }

    "return 400 if the requested tax year is not the current tax year" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore()

      val result = Future.successful(controller.startAddFuelBenefitAction(johnDensmore, FakeRequest(), testTaxYear+1, employmentSeqNumberOne))

      result should haveStatus(400)
    }

    "return 400 if the employer is not the primary employer" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore()

      val result = Future.successful(controller.startAddFuelBenefitAction(johnDensmore, FakeRequest(), testTaxYear, 2))

      result should haveStatus(400)
    }
  }


  "submitting add fuel benefit" should {

    "successfully store the form values in the keystore" in new WithApplication(FakeApplication()) {
      val carBenefitStartedThisYear = Benefit(31, testTaxYear, 321.42, 1, None, None, None, None, None, None, None,
        Some(Car(Some(new LocalDate(testTaxYear, 5, 12)), None, Some(new LocalDate(testTaxYear - 1, 12, 12)), Some(0), Some("diesel"), Some(124), Some(1400), None, Some(BigDecimal("12343.21")), None, None)), actions("AB123456C", testTaxYear, 1), Map.empty)

      setupMocksForJohnDensmore(benefits = Seq(carBenefitStartedThisYear))

      val fuelBenefitValue = 1234
      val benefitCalculationResponse = NewBenefitCalculationResponse(None, Some(fuelBenefitValue))
      when(mockPayeConnector.calculateBenefitValue(Matchers.any(), Matchers.any())).thenReturn(Some(benefitCalculationResponse))

      val dateFuelWithdrawnFormData = new LocalDate(testTaxYear, 6, 3)
      val employerPayFuelFormData = "date"
      val request = newRequestForSaveAddFuelBenefit(employerPayFuelVal = Some(employerPayFuelFormData), dateFuelWithdrawnVal = Some(testTaxYear.toString, "6", "3"))

      val result = Future.successful(controller.reviewAddFuelBenefitAction(johnDensmore, request, testTaxYear, employmentSeqNumberOne))

      result should haveStatus(200)

      val keyStoreDataCaptor = ArgumentCaptor.forClass(classOf[FuelBenefitData])

      verify(mockKeyStoreService).addKeyStoreEntry(
        Matchers.eq(s"AddFuelBenefit:${johnDensmoreOid}:$testTaxYear:$employmentSeqNumberOne"),
        Matchers.eq("paye"),
        Matchers.eq("AddFuelBenefitForm"),
        keyStoreDataCaptor.capture()) (Matchers.any())

      keyStoreDataCaptor.getValue.dateFuelWithdrawn shouldBe Some(dateFuelWithdrawnFormData)
      keyStoreDataCaptor.getValue.employerPayFuel shouldBe Some(employerPayFuelFormData)
    }

    "return 200 for employerpayefuel of type date with a correct date withdrawn and display the details in a table" in new WithApplication(FakeApplication()) {
      val carBenefitStartedThisYear = Benefit(31, testTaxYear, 321.42, 1, None, None, None, None, None, None, None,
        Some(Car(Some(new LocalDate(testTaxYear, 5, 12)), None, Some(new LocalDate(testTaxYear - 1, 12, 12)), Some(0), Some("diesel"), Some(124), Some(1400), None, Some(BigDecimal("12343.21")), None, None)), actions("AB123456C", testTaxYear, 1), Map.empty)

      setupMocksForJohnDensmore(benefits = Seq(carBenefitStartedThisYear))

      val fuelBenefitValue = 1234
      val benefitCalculationResponse = NewBenefitCalculationResponse(None, Some(fuelBenefitValue))
      when(mockPayeConnector.calculateBenefitValue(Matchers.any(), Matchers.any())).thenReturn(Some(benefitCalculationResponse))

      val dateFuelWithdrawnFormData = new LocalDate(testTaxYear, 6, 3)
      val employerPayFuelFormData = "date"
      val request = newRequestForSaveAddFuelBenefit(employerPayFuelVal = Some(employerPayFuelFormData), dateFuelWithdrawnVal = Some(testTaxYear.toString, "6", "3"))

      val result = Future.successful(controller.reviewAddFuelBenefitAction(johnDensmore, request, testTaxYear, employmentSeqNumberOne))

      result should haveStatus(200)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#second-heading").text should include("Check your private fuel details")
      doc.select("#private-fuel").text should include(s"3 June $testTaxYear")
      doc.select("#provided-from").text should include(s"12 May ${testTaxYear}")
      //      doc.select("#fuelBenefitTaxableValue").text should include("£0")

    }

    "return 200 and show start date as beginning of the tax year if carMadeAvailable is earlier" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore(benefits = Seq(carBenefitEmployer1))

      val request = newRequestForSaveAddFuelBenefit( employerPayFuelVal = Some("true"))

      val result = Future.successful(controller.reviewAddFuelBenefitAction(johnDensmore, request, testTaxYear, employmentSeqNumberOne))

      result should haveStatus(200)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#second-heading").text should include("Check your private fuel details")
      doc.select("#provided-from").text should include(s"6 April ${testTaxYear}")
      doc.select("#private-fuel").text should include(s"Yes, private fuel is available when you use the car")
    }

    "show the users recalculated tax code" in new WithApplication(FakeApplication()) {

      val fuelBenefitValue = 1234
      val benefitCalculationResponse = NewBenefitCalculationResponse(None, Some(fuelBenefitValue))

      setupMocksForJohnDensmore(benefits = Seq(carBenefitEmployer1))
      when(mockPayeConnector.calculateBenefitValue(Matchers.any(), Matchers.any())).thenReturn(Some(benefitCalculationResponse))


      val request = newRequestForSaveAddFuelBenefit(employerPayFuelVal = Some("true"))

      val result = Future.successful(controller.reviewAddFuelBenefitAction(johnDensmore, request, testTaxYear, employmentSeqNumberOne))

      result should haveStatus(200)

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#fuelBenefitTaxableValue").text shouldBe "£1,234"
    }

    "return to the car benefit home page if the user already has a fuel benefit" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(benefits = johnDensmoresBenefitsForEmployer1)

      val result = Future.successful(controller.reviewAddFuelBenefitAction(johnDensmore, newRequestForSaveAddFuelBenefit(), testTaxYear, employmentSeqNumberOne))
      result should haveStatus(303)
      redirectLocation(result) shouldBe Some(routes.CarBenefitHomeController.carBenefitHome.url)
    }

    "ignore invalid withdrawn date if employerpayfuel is not date" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(benefits = Seq(carBenefitEmployer1))

      val result = Future.successful(controller.reviewAddFuelBenefitAction(johnDensmore, newRequestForSaveAddFuelBenefit(employerPayFuelVal = Some("again"), dateFuelWithdrawnVal = Some("isdufgpsiuf", "6", "3")), testTaxYear, employmentSeqNumberOne))
      result should haveStatus(200)
    }

    "return 400 and display error when values form data fails validation" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()
      reset(mockKeyStoreService)

      val request = newRequestForSaveAddFuelBenefit(employerPayFuelVal = Some("date"), dateFuelWithdrawnVal = Some(("jkhasgdkhsa","05","30")))
      val result = Future.successful(controller.reviewAddFuelBenefitAction(johnDensmore, request, testTaxYear, employmentSeqNumberOne))
      result should haveStatus(400)
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#employerPayFuel-date").attr("checked") shouldBe "checked"
      doc.select("[id~=dateFuelWithdrawn]").select("[id~=day-30]").attr("selected") shouldBe "selected"

      verifyNoSaveToKeyStore
    }

    "return 400 if the year submitted is not the current tax year" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()

      val result = Future.successful(controller.reviewAddFuelBenefitAction(johnDensmore, newRequestForSaveAddFuelBenefit(), testTaxYear+1, employmentSeqNumberOne))

      result should haveStatus(400)
      verifyNoSaveToKeyStore
    }

    "return 400 if the submitting employment number is not the primary employment" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore()

      val result = Future.successful(controller.reviewAddFuelBenefitAction(johnDensmore, newRequestForSaveAddFuelBenefit(), testTaxYear, 2))

      result should haveStatus(400)
      verifyNoSaveToKeyStore
    }

    "return 200 if the user selects again for the EMPLOYER PAY FUEL" in new WithApplication(FakeApplication()){
      setupMocksForJohnDensmore(benefits = Seq(carBenefitEmployer1))
      val request = newRequestForSaveAddFuelBenefit(employerPayFuelVal = Some("again"))
      val result = Future.successful(controller.reviewAddFuelBenefitAction(johnDensmore, request, testTaxYear, employmentSeqNumberOne))
      result should haveStatus(200)
    }

    "return 400 if the user does not select any option for the EMPLOYER PAY FUEL question" in new WithApplication(FakeApplication()){
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddFuelBenefit(employerPayFuelVal = None)
      val result = Future.successful(controller.reviewAddFuelBenefitAction(johnDensmore, request, testTaxYear, employmentSeqNumberOne))
      result should haveStatus(400)
      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".error-notification").text should be(Messages("error.paye.answer_mandatory"))
    }

    "return 400 if the user sends an invalid value for the EMPLOYER PAY FUEL question" in new WithApplication(FakeApplication()){
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddFuelBenefit(employerPayFuelVal = Some("hacking!"))
      val result = Future.successful(controller.reviewAddFuelBenefitAction(johnDensmore, request, testTaxYear, employmentSeqNumberOne))
      result should haveStatus(400)

      verifyNoSaveToKeyStore
    }

    "return with an error (tbd) when a car benefit is not found" in {
      when(mockPayeConnector.linkedResource[Seq[Benefit]](s"/paye/AB123456C/benefits/$testTaxYear")).thenReturn(Some(Seq.empty))

      val request = newRequestForSaveAddFuelBenefit( employerPayFuelVal = Some("true"))

      val result = Future(controller.reviewAddFuelBenefitAction(johnDensmore, request, testTaxYear, employmentSeqNumberOne))

      val ex = Await.result(result.failed, Duration(3, TimeUnit.SECONDS))
      ex shouldBe a [StaleHodDataException]

      verifyNoSaveToKeyStore
    }

    def verifyNoSaveToKeyStore {
      verify(mockKeyStoreService, never()).addKeyStoreEntry(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any())
    }
  }

  private def newRequestForSaveAddFuelBenefit(employerPayFuelVal:Option[String] = None, dateFuelWithdrawnVal:Option[(String, String,String)] = None, path:String = "") = FakeRequest("GET", path).withFormUrlEncodedBody(Seq(
    employerPayFuel -> employerPayFuelVal.getOrElse(""))
    ++ buildDateFormField(dateFuelWithdrawn, dateFuelWithdrawnVal) : _*)

  private def haveStatus(expectedStatus:Int) = new Matcher[Future[SimpleResult]]{
    def apply(response:Future[SimpleResult]) = {
      val actualStatus = status(response)
      MatchResult(actualStatus == expectedStatus , s"Expected result with status $expectedStatus but was $actualStatus.", s"Expected result with status other than $expectedStatus, but was actually $actualStatus.")
    }
  }

  private def setupMocksForJohnDensmore(taxCodes: Seq[TaxCode] = johnDensmoresTaxCodes, employments: Seq[Employment] = johnDensmoresEmployments, benefits: Seq[Benefit] = Seq.empty) {
    when(mockPayeConnector.linkedResource[Seq[TaxCode]](s"/paye/AB123456C/tax-codes/$testTaxYear")).thenReturn(Some(taxCodes))
    when(mockPayeConnector.linkedResource[Seq[Employment]](s"/paye/AB123456C/employments/$testTaxYear")).thenReturn(Some(employments))
    when(mockPayeConnector.linkedResource[Seq[Benefit]](s"/paye/AB123456C/benefits/$testTaxYear")).thenReturn(Some(benefits))
  }
}
