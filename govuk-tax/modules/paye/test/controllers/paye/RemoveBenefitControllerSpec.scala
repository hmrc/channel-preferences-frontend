package controllers.paye

import org.scalatest.mock.MockitoSugar
import org.scalatest.TestData
import org.joda.time.{DateTime, LocalDate}
import play.api.test.{WithApplication, FakeRequest}
import play.api.test.Helpers._
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import org.mockito.Mockito._
import org.mockito.{Matchers, Mockito, ArgumentMatcher}
import Matchers._
import uk.gov.hmrc.common.microservice.paye.domain._
import org.jsoup.Jsoup
import uk.gov.hmrc.common.microservice.paye.domain.Employment._
import uk.gov.hmrc.utils.TaxYearResolver
import controllers.DateFieldsHelper
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import org.joda.time.format.DateTimeFormat
import concurrent.Future
import org.joda.time.chrono.ISOChronology
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector
import BenefitTypes._
import controllers.common.actions.HeaderCarrier
import org.scalatest.concurrent.ScalaFutures
import controllers.paye.validation.BenefitFlowHelper
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import scala.Some
import uk.gov.hmrc.common.microservice.paye.domain.Car
import uk.gov.hmrc.common.microservice.domain.User
import play.api.test.FakeApplication
import uk.gov.hmrc.common.microservice.paye.domain.TaxCode
import uk.gov.hmrc.common.microservice.paye.domain.TransactionId
import uk.gov.hmrc.common.microservice.paye.domain.RemoveBenefitResponse
import uk.gov.hmrc.common.microservice.txqueue.domain.TxQueueTransaction
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import uk.gov.hmrc.common.microservice.paye.domain.WithdrawnBenefitRequest
import models.paye.{RemoveFuelBenefitFormData, RemoveCarBenefitFormData}
import controllers.common.SessionKeys

class RemoveBenefitControllerSpec extends PayeBaseSpec with MockitoSugar with DateFieldsHelper with ScalaFutures {
  import Matchers.{any, eq => is}
  import controllers.domain.AuthorityUtils._

  val mockKeyStoreService = mock[KeyStoreConnector]
  val mockPayeConnector = mock[PayeConnector]
  val mockTxQueueConnector = mock[TxQueueConnector]
  val mockAuthConnector = mock[AuthConnector]
  val mockAuditConnector = mock[AuditConnector]

  private lazy val controller = new RemoveBenefitController(mockKeyStoreService, mockAuthConnector, mockAuditConnector)(mockPayeConnector, mockTxQueueConnector) with MockedTaxYearSupport {
    override def now = () => dateToday
  }

  private lazy val formController = new ShowRemoveBenefitFormController(mockKeyStoreService, mockAuthConnector, mockAuditConnector)(mockPayeConnector, mockTxQueueConnector) with MockedTaxYearSupport {
    override def now = () => dateToday
  }


  private lazy val dateToday: DateTime = new DateTime(2013, 12, 8, 12, 30, ISOChronology.getInstanceUTC)

  override protected def beforeEach(testData: TestData) {
    super.beforeEach(testData)
    Mockito.reset(mockKeyStoreService)
    Mockito.reset(mockPayeConnector)
    Mockito.reset(mockTxQueueConnector)
    Mockito.reset(mockAuthConnector)
    Mockito.reset(mockAuditConnector)
  }

  val isBenefitOfType = (benefType: Int) =>
    new ArgumentMatcher[Benefit] {
      def matches(benefit: Any) = benefit != null && benefit.asInstanceOf[Benefit].benefitType == benefType
    }

  private def setupMocksForJohnDensmore(taxCodes: Seq[TaxCode], employments: Seq[Employment], cars: Seq[CarBenefit]) {
    implicit val hc = HeaderCarrier()
    when(mockPayeConnector.linkedResource[Seq[TaxCode]](is("/paye/AB123456C/tax-codes/2013"))(any(), any())).thenReturn(Some(taxCodes))
    when(mockPayeConnector.linkedResource[Seq[Employment]](is("/paye/AB123456C/employments/2013"))(any(), any())).thenReturn(Some(employments))
    when(mockPayeConnector.version(is("/paye/AB123456C/version"))(any())).thenReturn(Future.successful(johnDensmoreVersionNumber))

    val benefits = cars.map(c => CarAndFuel(c.toBenefits(0), c.toBenefits.drop(1).headOption))
    when(mockPayeConnector.linkedResource[Seq[CarAndFuel]](is("/paye/AB123456C/benefit-cars/2013"))(any(),any())).thenReturn(Some(benefits))


    val withdrawDate = new LocalDate(2013, 12, 8)
    val formData: RemoveCarBenefitFormData = RemoveCarBenefitFormData(withdrawDate, Some(true), Some(11), Some(true), Some(250), Some("differentDateFuel"), Some(withdrawDate))
    when(mockKeyStoreService.getEntry[RemoveCarBenefitFormData](is(RemovalUtils.benefitFormDataActionId), is("paye"), is("remove_benefit"), is(false))(any(),any())).thenReturn(Some(formData))
    when(mockKeyStoreService.getEntry[RemoveFuelBenefitFormData](any(),any(),any(),any())(any(), any())).thenReturn(None)


    when(mockKeyStoreService.addKeyStoreEntry(any(), any(), any(), any(), any())(any(), any())).
      thenReturn(Future.successful(None))
  }

  "Removing FUEL benefit only" should {
    "notify the user the fuel benefit will be removed for benefit with no company name" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)
      when(mockKeyStoreService.getEntry(anyString, anyString, anyString, anyBoolean)(any(), any())).thenReturn(None)

      val result = formController.showRemoveFuelBenefitFormAction(johnDensmore, requestWithCorrectVersion, 2013, 2)

      val doc = Jsoup.parse(contentAsString(result))

      doc.select(".benefit-type").text shouldBe "Your old company fuel benefit"
    }
  }

  "Removing Car benefit " should {

    "not display car removal checkbox" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)
      when(mockKeyStoreService.getEntry(anyString, anyString, anyString, anyBoolean)(any(), any())).thenReturn(None)
      val result = formController.showRemoveCarBenefitFormAction(johnDensmore, requestWithCorrectVersion, 2013, 2)

      val doc = Jsoup.parse(contentAsString(result))

      doc.select("label[for=removeCar]").text shouldBe ""
    }
  }

  "Removing your car prior to its start date" should {

    "Not validate the view and redirect with correct error" in new WithApplication(FakeApplication()) {

      val benefitStartDate = currentTestDate.toLocalDate
      val startDateToString = DateTimeFormat.forPattern("yyyy-MM-dd").print(benefitStartDate)
      val calculation: String = "/calculation/paye/thenino/benefit/withdraw/3333/" + startDateToString + "/withdrawDate"

      val carBenefitStartedThisYear = Benefit(31, 2013, 321.42, 2, None, None, None, None, None, None, None,
        Some(Car(Some(benefitStartDate), None, Some(new LocalDate(2012, 12, 12)), Some(0), Some("diesel"), Some(124), Some(1400), None, Some(BigDecimal("12343.21")), None, None)), Map.empty, Map("withdraw" -> calculation))

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq(CarBenefit(carBenefitStartedThisYear)))

      val withdrawDate = benefitStartDate.minusDays(2)

      def requestBenefitRemovalFormSubmission(date: Option[LocalDate], agreed: Boolean, removeFuel: Boolean) =
        requestWithCorrectVersion.withFormUrlEncodedBody(Seq(
          "agreement" -> agreed.toString.toLowerCase)
          ++ buildDateFormField("withdrawDate", Some(localDateToTuple(Some(withdrawDate)))): _*)

      val result = controller.requestRemoveCarBenefitAction(2013, 2)(johnDensmore, requestBenefitRemovalFormSubmission(Some(withdrawDate), agreed = true, removeFuel = true))

      status(result) shouldBe BAD_REQUEST

      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".error-notification").text should include("Invalid date: Return date cannot be before benefit was made available")
    }
  }

  "Given a user who has car and fuel benefits, removing fuel and then separately car benefit " should {

    def requestBenefitRemovalFormSubmission(date: Option[LocalDate], carUnavailable: String = "false", removeEmployeeContributes: String = "false") =
      requestWithCorrectVersion.withFormUrlEncodedBody(Seq(
        "agreement" -> "true",
        "carUnavailable" -> carUnavailable,
        "removeEmployeeContributes" -> removeEmployeeContributes)
        ++ buildDateFormField("withdrawDate", Some(localDateToTuple(date))): _*)

    "allow the user to remove fuel first without showing error" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val fuelWithdrawDate = new LocalDate(2013, 12, 8)

      val result = controller.requestRemoveFuelBenefitAction(2013, 2)(johnDensmore, requestBenefitRemovalFormSubmission(Some(fuelWithdrawDate)))

      status(result) shouldBe 200

      val doc = Jsoup.parse(contentAsString(result))
      val error = doc.select("#form-remove-fuel-benefit .error-notification").text
      error shouldBe empty
    }

    "allow the user to remove car benefit when fuel is already removed without showing error" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits.map(_.copy(fuelBenefit = None)))

      val carWithdrawDate = new LocalDate(2013, 12, 8)
      val result = controller.requestRemoveCarBenefitAction(2013, 2)(johnDensmore, requestBenefitRemovalFormSubmission(Some(carWithdrawDate)))
      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      val error = doc.select("#form-remove-fuel-benefit .error-notification").text
      error shouldBe empty
    }

  }

  "Given a user who has car and fuel benefits, removing car benefit " should {

    "In step 1, give the user the option to remove fuel benefit on the same (or different) date as the car" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)
      when(mockKeyStoreService.getEntry(anyString, anyString, anyString, anyBoolean)(any(), any())).thenReturn(None)

      val result = formController.showRemoveCarBenefitFormAction(johnDensmore, requestWithCorrectVersion, 2013, 2)
      status(result) shouldBe 200

      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".fuel-benefit-info") should not be empty
      doc.getElementById("fuelRadio-sameDateFuel") should not be null
      doc.getElementById("fuelRadio-differentDateFuel") should not be null
    }

    "in step 1, display error message if user has not selected the type of date for fuel removal" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)


      val withdrawDate = new LocalDate(2013, 12, 8)
      val result = controller.requestRemoveCarBenefitAction(2013, 2)(johnDensmore, requestBenefitRemovalFormSubmission(Some(withdrawDate), true, None))

      status(result) shouldBe BAD_REQUEST

      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".error-notification").text should include("select an option")
    }

    "in step 1, display error message if user chooses different date but dont select a fuel date" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val withdrawDate = new LocalDate()
      val result = controller.requestRemoveCarBenefitAction(2013, 2)(johnDensmore, requestBenefitRemovalFormSubmission(Some(withdrawDate), true, Some("differentDateFuel")))

      status(result) shouldBe BAD_REQUEST
      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".error-notification").text should include("Please enter a date")
    }

    "in step 1, display error message if user chooses a fuel date that is greater than the car return date" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val withdrawDate = new LocalDate()
      val fuelDate = new LocalDate().plusDays(1)

      val result = controller.requestRemoveCarBenefitAction(2013, 2)(johnDensmore, requestBenefitRemovalFormSubmission(Some(withdrawDate), true, Some("differentDateFuel"), Some(fuelDate)))

      status(result) shouldBe BAD_REQUEST
      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".error-notification").text should include("Return date for fuel cannot be after car return date")
    }

    "in step 1, display error message if user chooses a fuel date before start of tax year" in new WithApplication(FakeApplication()) {
      val benefitStartDate = new LocalDate().minusYears(3)
      val carBenefitStatedLongTimeAgo = Benefit(31, 2013, 321.42, 2, None, None, None, None, None, None, None,
        Some(Car(Some(benefitStartDate), None, Some(new LocalDate(2012, 12, 12)), Some(0), Some("diesel"), Some(124), Some(1400), None, Some(BigDecimal("12343.21")), None, None)), Map.empty, Map.empty)

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq(CarBenefit(carBenefitStatedLongTimeAgo, Some(fuelBenefit))))

      val withdrawDate = new LocalDate()
      val fuelDate = withdrawDate.minusYears(1)

      val dateintaxyear = TaxYearResolver.startOfCurrentTaxYear
      val result = controller.requestRemoveCarBenefitAction(2013, 2)(johnDensmore, requestBenefitRemovalFormSubmission(Some(withdrawDate), true, Some("differentDateFuel"), Some(fuelDate)))

      status(result) shouldBe BAD_REQUEST
      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".error-notification").text should include("Return date cannot be in previous tax years")
    }

    "display error message if user chooses a fuel date which is malformed" in new WithApplication(FakeApplication()) {
      val benefitStartDate = new LocalDate().minusYears(3)
      val carBenefitStatedLongTimeAgo = Benefit(31, 2013, 321.42, 2, None, None, None, None, None, None, None,
        Some(Car(Some(benefitStartDate), None, Some(new LocalDate(2012, 12, 12)), Some(0), Some("diesel"), Some(124), Some(1400), None, Some(BigDecimal("12343.21")), None, None)), Map.empty, Map.empty)

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq(CarBenefit(carBenefitStatedLongTimeAgo, Some(fuelBenefit))))

      val withdrawDate = new LocalDate()

      val requestBenefitRemovalForm = requestWithCorrectVersion.withFormUrlEncodedBody(Seq("agreement" -> "true", "fuelRadio" -> "differentDateFuel")
        ++ buildDateFormField("fuelWithdrawDate", Some(("aa", "bb", "cc")))
        ++ buildDateFormField("withdrawDate", Some(localDateToTuple(Some(withdrawDate)))): _*)

      val result = controller.requestRemoveCarBenefitAction(2013, 2)(johnDensmore, requestBenefitRemovalForm)

      status(result) shouldBe BAD_REQUEST
      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".error-notification").text should include("You must specify a valid date")
    }

    "in step 1, display error message if user choose a fuel date before benefit started" in new WithApplication(FakeApplication()) {

      val benefitStartDate = currentTestDate.toLocalDate
      val startDateToString = DateTimeFormat.forPattern("yyyy-MM-dd").print(benefitStartDate)
      val calculation: String = "/calculation/paye/thenino/benefit/withdraw/3333/" + startDateToString + "/withdrawDate"

      val carBenefitStartedThisYear = Benefit(31, 2013, 321.42, 2, None, None, None, None, None, None, None,
        Some(Car(Some(benefitStartDate), None, Some(new LocalDate(2012, 12, 12)), Some(0), Some("diesel"), Some(124), Some(1400), None, Some(BigDecimal("12343.21")), None, None)), Map.empty, Map("withdraw" -> calculation))


      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq(CarBenefit(carBenefitStartedThisYear, Some(fuelBenefit))))

      val withdrawDate = benefitStartDate
      val fuelDate = benefitStartDate.minusDays(1)

      val result = controller.requestRemoveCarBenefitAction(2013, 2)(johnDensmore, requestBenefitRemovalFormSubmission(Some(withdrawDate), true, Some("differentDateFuel"), Some(fuelDate)))

      status(result) shouldBe BAD_REQUEST
      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".error-notification").text should include("Return date cannot be before benefit was made available")
    }

    "in step 1, keep user choice for fuel date if a bad request redirect him to the form" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val carWithdrawDate = new LocalDate()

      val withdrawDate = new LocalDate(2013, 3, 9)
      val result = controller.requestRemoveCarBenefitAction(2013, 2)(johnDensmore, requestBenefitRemovalFormSubmission(Some(withdrawDate), true, Some("differentDateFuel"), Some(withdrawDate)))

      status(result) shouldBe BAD_REQUEST
      val doc = Jsoup.parse(contentAsString(result))

      doc.getElementById("fuelRadio-differentDateFuel").hasAttr("checked") shouldBe true
      doc.getElementById("fuelWithdrawDate.day-9").hasAttr("selected") shouldBe true
      doc.getElementById("fuelWithdrawDate.month-3").hasAttr("selected") shouldBe true
      doc.getElementById("fuelWithdrawDate.year-2013").hasAttr("selected") shouldBe true

    }

    "in step 1, display error message if user does not give an answer for car unavailable question" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val withdrawDate = new LocalDate(2013, 6, 9)
      val request = requestBenefitRemovalFormSubmission(withdrawDate = Some(withdrawDate), agreement = true, carUnavailable = "")
      val result = controller.requestRemoveCarBenefitAction(2013, 2)(johnDensmore, request)

      status(result) shouldBe BAD_REQUEST
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#form-remove-car-benefit .error-notification").text shouldBe "Please answer this question."

    }

    "in step 1, display error message if user specifies car was unavailable but he does not enter the days" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val withdrawDate = new LocalDate(2013, 6, 9)
      val request = requestBenefitRemovalFormSubmission(withdrawDate = Some(withdrawDate), agreement = true, carUnavailable = "true")
      val result = controller.requestRemoveCarBenefitAction(2013, 2)(johnDensmore, request)

      status(result) shouldBe BAD_REQUEST
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#form-remove-car-benefit .error-notification").text shouldBe "You must specify the number of consecutive days the car has been unavailable."

    }

    "in step 1, display error message if user specifies car was unavailable but he does not introduce a valid input" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val withdrawDate = new LocalDate(2013, 6, 9)
      val request = requestBenefitRemovalFormSubmission(withdrawDate = Some(withdrawDate), agreement = true, carUnavailable = "true", numberOfDaysUnavailable = "not-a-valid-input")
      val result = controller.requestRemoveCarBenefitAction(2013, 2)(johnDensmore, request)

      status(result) shouldBe BAD_REQUEST
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#form-remove-car-benefit .error-notification").text shouldBe "Please use whole numbers only, not decimals or other characters."

    }

    "in step 1, display error message if user specifies car was unavailable but he introduces a negative value" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val withdrawDate = new LocalDate(2013, 6, 9)
      val request = requestBenefitRemovalFormSubmission(withdrawDate = Some(withdrawDate), agreement = true, carUnavailable = "true", numberOfDaysUnavailable = "-3")
      val result = controller.requestRemoveCarBenefitAction(2013, 2)(johnDensmore, request)

      status(result) shouldBe BAD_REQUEST
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#form-remove-car-benefit .error-notification").text shouldBe "Days unavailable must be greater than zero if you have selected yes."

    }

    "in step 1, display error message if user specifies car was unavailable but he introduces a value that is bigger than the interval period (benefitStartDate - withdrawnDate)" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val withdrawDate = new LocalDate(2013, 6, 9)
      val request = requestBenefitRemovalFormSubmission(withdrawDate = Some(withdrawDate), agreement = true, carUnavailable = "true", numberOfDaysUnavailable = "100")
      val result = controller.requestRemoveCarBenefitAction(2013, 2)(johnDensmore, request)

      status(result) shouldBe BAD_REQUEST
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#form-remove-car-benefit .error-notification").text shouldBe "Car cannot be unavailable for longer than the total time you have a company car for."

    }

    "in step 1, display error message if user does not give an answer for employee contributes" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val withdrawDate = new LocalDate(2013, 6, 9)
      val request = requestBenefitRemovalFormSubmission(withdrawDate = Some(withdrawDate), agreement = true, removeEmployeeContributes = "")
      val result = controller.requestRemoveCarBenefitAction(2013, 2)(johnDensmore, request)

      status(result) shouldBe BAD_REQUEST
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#form-remove-car-benefit .error-notification").text shouldBe "Please answer this question."

    }

    "in step 1, display error message if user has contributed to the car but he has not entered the contribution" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val withdrawDate = new LocalDate(2013, 6, 9)
      val request = requestBenefitRemovalFormSubmission(withdrawDate = Some(withdrawDate), agreement = true, removeEmployeeContributes = "true")
      val result = controller.requestRemoveCarBenefitAction(2013, 2)(johnDensmore, request)

      status(result) shouldBe BAD_REQUEST
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#form-remove-car-benefit .error-notification").text shouldBe "You must specify how much you paid your employer for private use of the company car."

    }

    "in step 1, display error message if user has contributed to the car but he does not enter a valid value" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val withdrawDate = new LocalDate(2013, 6, 9)
      val request = requestBenefitRemovalFormSubmission(withdrawDate = Some(withdrawDate), agreement = true, removeEmployeeContributes = "true", removeEmployeeContribution = "not-a-valid-input")
      val result = controller.requestRemoveCarBenefitAction(2013, 2)(johnDensmore, request)

      status(result) shouldBe BAD_REQUEST
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#form-remove-car-benefit .error-notification").text shouldBe "Please use whole numbers only, not decimals or other characters."

    }

    "in step 1, display error message if user has contributed to the car but he has entered a negative value" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val withdrawDate = new LocalDate(2013, 6, 9)
      val request = requestBenefitRemovalFormSubmission(withdrawDate = Some(withdrawDate), agreement = true, removeEmployeeContributes = "true", removeEmployeeContribution = "-343")
      val result = controller.requestRemoveCarBenefitAction(2013, 2)(johnDensmore, request)

      status(result) shouldBe BAD_REQUEST
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#form-remove-car-benefit .error-notification").text shouldBe "Employee payment must be greater than zero if you have selected yes."
    }

    "in step 1, display error message if user has contributed to the car but he has exceded the allowed maximum value" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val withdrawDate = new LocalDate(2013, 6, 9)
      val request = requestBenefitRemovalFormSubmission(withdrawDate = Some(withdrawDate), agreement = true, removeEmployeeContributes = "true", removeEmployeeContribution = "100000")
      val result = controller.requestRemoveCarBenefitAction(2013, 2)(johnDensmore, request)

      status(result) shouldBe BAD_REQUEST
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#form-remove-car-benefit .error-notification").text shouldBe "Please enter a number of 5 characters or less."
    }

    "in step 2, display the infos provided in the form" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val carWithdrawDate = new LocalDate(2013, 12, 8)
      val fuelWithdrawDate = carWithdrawDate.minusDays(1)
      val companyCarDetails = "company-car-details"

      val result = controller.requestRemoveCarBenefitAction(2013, 2)(johnDensmore, requestBenefitRemovalFormSubmission(Some(carWithdrawDate), true, Some("differentDateFuel"), Some(fuelWithdrawDate), "true", "20", "true", "250"))

      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById(companyCarDetails) should not be null
      doc.select("#car-benefit-date-car-withdrawn").text shouldBe "8 December 2013"
      doc.select("#car-benefit-num-days-unavailable").text shouldBe "20 days"
      doc.select("#car-benefit-employee-payments").text shouldBe "£250"
    }

    "in step 2, display none for info not provided in the form" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val carWithdrawDate = new LocalDate(2013, 12, 8)
      val fuelWithdrawDate = carWithdrawDate.minusDays(1)
      val companyCarDetails = "company-car-details"

      val result = controller.requestRemoveCarBenefitAction(2013, 2)(johnDensmore, requestBenefitRemovalFormSubmission(Some(carWithdrawDate), true, Some("differentDateFuel"), Some(fuelWithdrawDate)))

      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById(companyCarDetails) should not be null
      doc.select("#car-benefit-num-days-unavailable").text shouldBe "none"
      doc.select("#car-benefit-employee-payments").text shouldBe "none"
    }
  }

  "The car benefit removal method" should {
    "In step 1, display page correctly for well formed request" in new WithApplication(FakeApplication()) {

      val car = Car(Some(new LocalDate(1994, 10, 7)), None, Some(new LocalDate(2012, 12, 12)), Some(0), Some("diesel"), Some(124), Some(1440), None, Some(BigDecimal("15000")), None, None)
      val specialCarBenefit = Benefit(31, 2013, 666, 3, None, None, None, None, None, None, None,
        Some(car), Map.empty, Map.empty)

      setupMocksForJohnDensmore(johnDensmoresTaxCodes,
        Seq(Employment(sequenceNumber = 3, startDate = new LocalDate(2013, 10, 14), endDate = None, taxDistrictNumber = "899", payeNumber = "1212121", employerName = None, employmentType = primaryEmploymentType)),
        Seq(CarBenefit(specialCarBenefit)))
      when(mockKeyStoreService.getEntry(anyString, anyString, anyString, anyBoolean)(any(), any())).thenReturn(None)

      val result = formController.showRemoveCarBenefitFormAction(johnDensmore, requestWithCorrectVersion, 2013, 3)

      status(result) shouldBe 200
    }

    "in step 1, notify the user that the fuel benefit is going to be removed with the car benefit when removing car benefit" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)
      when(mockKeyStoreService.getEntry(anyString, anyString, anyString, anyBoolean)(any(), any())).thenReturn(None)

      val result = formController.showRemoveCarBenefitFormAction(johnDensmore, requestWithCorrectVersion, 2013, 2)
      status(result) shouldBe 200

      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".fuel-benefit-info") should not be empty

    }

    "in step 1, not notify the user about fuel benefit if the user does not have one" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq(CarBenefit(carBenefit)))
      when(mockKeyStoreService.getEntry(anyString, anyString, anyString, anyBoolean)(any(), any())).thenReturn(None)

      val result = formController.showRemoveCarBenefitFormAction(johnDensmore, requestWithCorrectVersion, 2013, 2)
      status(result) shouldBe 200

      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".fuel-benefit-info") shouldBe empty

    }

    "in step 2, request removal for both fuel and car benefit when both benefits are selected and user confirms" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      when(mockPayeConnector.removeBenefits(any[String], any[WithdrawnBenefitRequest])(any())).thenReturn(Some(RemoveBenefitResponse(TransactionId("someIdForCarAndFuelRemoval"), Some("123L"), Some(9999))))

      val withdrawDate = new LocalDate(2013, 7, 18)
      val revisedAmounts = Map(carBenefit.benefitType.toString -> BigDecimal(210.17), fuelBenefit.benefitType.toString -> BigDecimal(14.1))
      private val formData: RemoveCarBenefitFormData = RemoveCarBenefitFormData(withdrawDate, Some(true), Some(11), Some(true), Some(250), Some("differentDateFuel"), Some(withdrawDate))
      when(mockKeyStoreService.getEntry[RemoveCarBenefitFormData](
        is(RemovalUtils.benefitFormDataActionId),
        is("paye"),
        is("remove_benefit"),
        is(false))(any(), any())).thenReturn(Some(formData))

      val resultF = controller.confirmCarBenefitRemovalAction(2013, 2)(johnDensmore, requestWithCorrectVersion, johnDensmoreVersionNumber)

      val requestBody = WithdrawnBenefitRequest(22, Some(WithdrawnCarBenefit(withdrawDate, Some(11), Some(250))), Some(WithdrawnFuelBenefit(withdrawDate)))

      status(resultF) shouldBe 303

      verify(mockPayeConnector, times(1)).removeBenefits(is("/paye/AB123456C/benefits/2013/1/update"), is(requestBody))(any())

      val expectedUri = routes.RemoveBenefitController.benefitRemoved(s"$CAR", 2013, 2, "someIdForCarAndFuelRemoval", Some("123L"), Some(9999)).url
      redirectLocation(resultF) shouldBe Some(expectedUri)
    }

    "in step 3, display page for confirmation of removal of both fuel and car benefit when both benefits are selected and user confirms" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val transaction: TxQueueTransaction = mock[TxQueueTransaction]
      when(mockTxQueueConnector.transaction(is("210"), any[PayeRoot])(any())).thenReturn(Some(transaction))

      val withdrawDate = new LocalDate(2013, 7, 18)

      val formData: RemoveCarBenefitFormData = RemoveCarBenefitFormData(withdrawDate, Some(true), Some(11), Some(true), Some(250), Some("differentDateFuel"), Some(withdrawDate))
      when(mockKeyStoreService.getEntry[RemoveCarBenefitFormData](
        is(RemovalUtils.benefitFormDataActionId),
        is("paye"),
        is("remove_benefit"),
        is(false))(any(), any())).thenReturn(Some(formData))

      val result = controller.benefitRemovedAction(johnDensmore, requestWithCorrectVersion, s"$CAR,$FUEL", 2013, 1, "210", Some("newTaxCode"), Some(9988))

      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#benefit-type").text should include("car and fuel")
    }

    "in step 3, do not display information about new tax code nor personal allowance if they are not available" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val transaction: TxQueueTransaction = mock[TxQueueTransaction]
      when(mockTxQueueConnector.transaction(is("210"), any[PayeRoot])(any())).thenReturn(Some(transaction))

      val withdrawDate = new LocalDate(2013, 7, 18)
      val formData: RemoveCarBenefitFormData = RemoveCarBenefitFormData(withdrawDate, Some(true), Some(11), Some(true), Some(250), Some("differentDateFuel"), Some(withdrawDate))
      when(mockKeyStoreService.getEntry[RemoveCarBenefitFormData](
        is(RemovalUtils.benefitFormDataActionId),
        is("paye"),
        is("remove_benefit"),
        is(false))(any(), any())).thenReturn(Some(formData))

      val result = controller.benefitRemovedAction(johnDensmore, requestWithCorrectVersion, s"$CAR,$FUEL", 2013, 1, "210", None, None)

      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#benefit-type").text should include("car and fuel")
      doc.select("#old-tax-code").text should include("430L")
      doc.select("#new-tax-code").size should be(0)
      doc.select("#personal-allowance").size should be(0)
    }
  }

  "The remove benefit method" should {

    "in step 1 display an error message when return date of car greater than 7 days" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val invalidWithdrawDate = dateToday.toLocalDate.plusDays(36)
      val result = Future.successful(controller.requestRemoveCarBenefitAction(2013, 2)(johnDensmore,
        requestBenefitRemovalFormSubmission(Some(invalidWithdrawDate), true)))

      status(result) shouldBe 400

      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".benefit-type").text should include("Your old company car")
      doc.select(".error-notification").text should include("Invalid date: Return date cannot be greater than 7 days from today")
    }

    "in step 1 display an error message when return date of the car is in the previous tax year" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val invalidWithdrawDate = new LocalDate(1999, 2, 1)
      val result = Future.successful(controller.requestRemoveCarBenefitAction(2013, 2)(johnDensmore,
        requestBenefitRemovalFormSubmission(Some(invalidWithdrawDate), true)))

      status(result) shouldBe 400

      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".benefit-type").text should include("Your old company car")
      doc.select(".error-notification").text should include("Invalid date: Return date cannot be in previous tax years")
    }

    "in step 1 display an error message when return date of the car is in the next tax year" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val invalidWithdrawDate = new LocalDate(2030, 2, 1)
      val result = Future.successful(controller.requestRemoveCarBenefitAction(2013, 2)(johnDensmore,
        requestBenefitRemovalFormSubmission(Some(invalidWithdrawDate), true)))

      status(result) shouldBe 400
      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".benefit-type").text should include("Your old company car")
      doc.select(".error-notification").text should include("Invalid date: Return date cannot be in next tax years")
    }

    "in step 1 display an error message when return date is not set" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val result = controller.requestRemoveCarBenefitAction(2013, 2)(johnDensmore, requestBenefitRemovalFormSubmission(None, true))

      status(result) shouldBe 400
      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".benefit-type").text should include("Your old company car")
      doc.select(".error-notification").text should include("Please enter a date")
    }


    "in step 1 display an error message when return date and agreement response is misformed" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val requestBenefitRemovalForm = requestWithCorrectVersion.withFormUrlEncodedBody(buildDateFormField("withdrawDate", Some(("A", "b", "2013"))): _*)
      val result = controller.requestRemoveFuelBenefitAction(2013, 2)(johnDensmore, requestBenefitRemovalForm)

      status(result) shouldBe 400
      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".benefit-type").text should include("Your old company")
      doc.select(".error-notification").text should include("You must specify a valid date")
    }


    "in step 2 save the withdrawDate to the keystore" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq(CarBenefit(carBenefit)))

      val withdrawDate = new LocalDate(2013, 12, 8)

      val resultF = controller.requestRemoveCarBenefitAction(2013, 2)(johnDensmore, requestBenefitRemovalFormSubmission(Some(withdrawDate), true))

      whenReady(resultF) { result =>
        verify(mockKeyStoreService, times(1)).addKeyStoreEntry(is(RemovalUtils.benefitFormDataActionId), is("paye"), is("remove_benefit"), is(RemoveCarBenefitFormData(withdrawDate, Some(false), None, Some(false), None, Some("sameDateFuel"), None)), is(false))(any(), any())
      }
    }

    "in step 2 call the paye service to remove the benefit and render the success page" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      when(mockPayeConnector.removeBenefits(any[String], any[WithdrawnBenefitRequest])(any())).thenReturn(Some(RemoveBenefitResponse(TransactionId("someId"), Some("123L"), Some(9999))))

      val withdrawDate = new LocalDate(2013, 7, 18)
      when(mockKeyStoreService.getEntry[RemoveFuelBenefitFormData](
        is(RemovalUtils.benefitFormDataActionId),
        is("paye"),
        is("remove_benefit"),
        is(false))(any(), any())).thenReturn(Some(RemoveFuelBenefitFormData(withdrawDate)))

      val result = controller.confirmFuelBenefitRemovalAction(2013, 2)(johnDensmore, requestWithCorrectVersion)

      status(result) shouldBe 303
      val expectedUri = routes.RemoveBenefitController.benefitRemoved(s"$FUEL", 2013, 2, "someId", Some("123L"), Some(9999)).url
      redirectLocation(result) shouldBe Some(expectedUri)

      val requestBody = WithdrawnBenefitRequest(22, None, Some(WithdrawnFuelBenefit(withdrawDate)))
      verify(mockPayeConnector, times(1)).removeBenefits(is("/paye/AB123456C/benefits/2013/1/update"), is(requestBody))(any())
    }

    "in step 3 return 404 if the transaction does not exist" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      when(mockTxQueueConnector.transaction(is("123"), any[PayeRoot])(any())).thenReturn(None)
      when(mockKeyStoreService.getEntry[RemoveCarBenefitFormData](
        is(RemovalUtils.benefitFormDataActionId),
        is("paye"),
        is("remove_benefit"),
        is(false))(any(), any())).thenReturn(Some(RemoveCarBenefitFormData(withdrawDate, Some(false), None, Some(false), None, Some("sameDateFuel"), None)))

      val withdrawDate = new LocalDate(2013, 7, 18)
      val result = controller.benefitRemovedAction(johnDensmore, requestWithCorrectVersion, s"$CAR", 2013, 1, "123", Some("newCode"), Some(9998))

      status(result) shouldBe 404

    }

    "return the updated benefits list page if the user has gone back in the browser and resubmitted and the benefit has already been removed" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val result = formController.showRemoveCarBenefitFormAction(johnDensmore, requestWithCorrectVersion, 2013, 1)
      status(result) shouldBe 303

      val expectedUri = routes.CarBenefitHomeController.carBenefitHome().url
      redirectLocation(result) shouldBe Some(expectedUri)
    }

    "return to the benefits list page if the user modifies the url to include an incorrect employment sequence number" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val result = formController.showRemoveCarBenefitFormAction(johnDensmore, requestWithCorrectVersion, 2013, 3)
      status(result) shouldBe 303
      val expectedUri = routes.CarBenefitHomeController.carBenefitHome().url
      redirectLocation(result) shouldBe Some(expectedUri)
    }
  }

  "benefitRemoved" should {
    "render a view with correct elements" in new WithApplication(FakeApplication()) {

      val car = Car(Some(new LocalDate(testTaxYear, 3, 6, ISOChronology.getInstanceUTC)), None, Some(new LocalDate(2000, 5, 8, ISOChronology.getInstanceUTC)),
                    Some(BigDecimal(10)), Some("diesel"), Some(1), Some(1400), None, Some(BigDecimal("1432")), None, None)

      val versionNumber = 1
      val payeRoot = new PayeRoot("CE927349E", "Mr", "Will", None, "Shakespeare", "Will Shakespeare", "1983-01-02", Map(), Map(), Map()) {
        override def fetchEmployments(taxYear: Int)(implicit payeConnector: PayeConnector, headerCarrier: HeaderCarrier): Future[Seq[Employment]] = {
          Seq(Employment(1, new LocalDate(), Some(new LocalDate()), "123", "123123", None, primaryEmploymentType))
        }

        override def fetchCars(taxYear: Int)(implicit payeConnector: PayeConnector, headerCarrier: HeaderCarrier): Future[Seq[CarBenefit]] = {
          Future.successful(
            Seq(CarBenefit(Benefit(31, 2013, BigDecimal("3"), 1, Some(BigDecimal("4")), Some(BigDecimal("5")), Some(BigDecimal("6")),
              Some(BigDecimal("7")), Some(BigDecimal("8")), Some("payment"), None, Some(car), Map[String, String](),
              Map[String, String]()))))
        }
      }

      when(mockKeyStoreService.getEntry[RemoveCarBenefitFormData](
        is(RemovalUtils.benefitFormDataActionId),
        is("paye"),
        is("remove_benefit"),
        is(false))(any(), any())).thenReturn(None)

      val user = User("wshakespeare", payeAuthority("someId", "CE927349E"), RegimeRoots(paye = Some(payeRoot)), None, None)

      val request = FakeRequest().
        withFormUrlEncodedBody("withdrawDate" -> "2013-07-13", "agreement" -> "true").
        withSession(SessionKeys.npsVersion -> versionNumber.toString)

      val result = controller.requestRemoveCarBenefitAction(2013, 1)(user, request)
      status(result) shouldBe 400
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("h2").first().text should be("no longer supply a company car.")
    }

    "Contain correct employee names" in new WithApplication(FakeApplication()) {
      val car = Car(Some(new LocalDate(testTaxYear, 3, 6, ISOChronology.getInstanceUTC)), None, Some(new LocalDate(2000, 1, 23, ISOChronology.getInstanceUTC)), Some(BigDecimal(10)), Some("diesel"), Some(10), Some(1400), None, Some(BigDecimal("1432")), None, None)
      val versionNumber = 1
      val payeRoot = new PayeRoot("CE927349E", "Mr", "Will", None, "Shakespeare", "Will Shakespeare", "1983-01-02", Map(), Map(), Map()) {
        override def fetchEmployments(taxYear: Int)(implicit payeConnector: PayeConnector, headerCarrier: HeaderCarrier): Future[Seq[Employment]] = {
          Seq(Employment(1, new LocalDate(), Some(new LocalDate()), "123", "123123", Some("Sainsburys"), primaryEmploymentType))
        }

        override def fetchCars(taxYear: Int)(implicit payeConnector: PayeConnector, headerCarrier: HeaderCarrier): Future[Seq[CarBenefit]] = {
          Future.successful(Seq(CarBenefit(Benefit(31, 2013, BigDecimal("3"), 1, Some(BigDecimal("4")), Some(BigDecimal("5")),
            Some(BigDecimal("6")), Some(BigDecimal("7")), Some(BigDecimal("8")), Some("payment"), None, Some(car),
            Map[String, String](), Map[String, String]()))))
        }
      }

      val user = User("wshakespeare", payeAuthority("someId", "CE927349E"), RegimeRoots(paye = Some(payeRoot)), None, None)

      val request: play.api.mvc.Request[_] = FakeRequest().withFormUrlEncodedBody("withdrawDate" -> "2013-07-13", "agreement" -> "true").
        withSession(SessionKeys.npsVersion -> versionNumber.toString)

      when(mockKeyStoreService.getEntry(anyString, anyString, anyString, anyBoolean)(any(), any())).thenReturn(None)

      val result = formController.showRemoveCarBenefitFormAction(user, request, 2013, 1)
      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".checkbox").text should not include "Some("
    }

  }

  def requestBenefitRemovalFormSubmission(withdrawDate: Option[LocalDate],
                                          agreement: Boolean,
                                          fuelRadio: Option[String] = Some("sameDateFuel"),
                                          fuelWithdrawDate: Option[LocalDate] = None,
                                          carUnavailable: String = "false",
                                          numberOfDaysUnavailable: String = "",
                                          removeEmployeeContributes: String = "false",
                                          removeEmployeeContribution: String = "") =
    requestWithCorrectVersion.withFormUrlEncodedBody(Seq(
      "agreement" -> agreement.toString.toLowerCase,
      "fuelRadio" -> fuelRadio.getOrElse(""),
      "carUnavailable" -> carUnavailable,
      "numberOfDaysUnavailable" -> numberOfDaysUnavailable,
      "removeEmployeeContributes" -> removeEmployeeContributes,
      "removeEmployeeContribution" -> removeEmployeeContribution)
      ++ buildDateFormField("fuelWithdrawDate", Some(localDateToTuple(fuelWithdrawDate)))
      ++ buildDateFormField("withdrawDate", Some(localDateToTuple(withdrawDate))): _*)
}
