package controllers.paye

import org.scalatest.mock.MockitoSugar
import controllers.common.CookieEncryption
import org.scalatest.TestData
import org.joda.time.{DateTime, LocalDate}
import play.api.test.{WithApplication, FakeRequest}
import play.api.test.Helpers._
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import org.mockito.Mockito._
import org.mockito.{Mockito, ArgumentMatcher, Matchers}
import uk.gov.hmrc.common.microservice.paye.domain._
import org.jsoup.Jsoup
import uk.gov.hmrc.common.microservice.paye.domain.Employment._
import uk.gov.hmrc.utils.TaxYearResolver
import controllers.DateFieldsHelper
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.common.microservice.paye.domain.Car
import uk.gov.hmrc.common.microservice.domain.User
import play.api.test.FakeApplication
import uk.gov.hmrc.common.microservice.paye.domain.TaxCode
import uk.gov.hmrc.common.microservice.paye.domain.TransactionId
import uk.gov.hmrc.common.microservice.paye.domain.RevisedBenefit
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import org.joda.time.format.DateTimeFormat
import concurrent.Future
import org.joda.time.chrono.ISOChronology
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector
import uk.gov.hmrc.common.microservice.txqueue.domain.TxQueueTransaction
import BenefitTypes._
import controllers.common.actions.HeaderCarrier
import org.scalatest.concurrent.ScalaFutures
import controllers.paye.validation.BenefitFlowHelper
import controllers.paye.RemovalUtils.RemoveBenefitData

class RemoveBenefitControllerSpec extends PayeBaseSpec with MockitoSugar with CookieEncryption with DateFieldsHelper with ScalaFutures {

  import controllers.domain.AuthorityUtils._

  val mockKeyStoreService = mock[KeyStoreConnector]
  val mockPayeConnector = mock[PayeConnector]
  val mockTxQueueConnector = mock[TxQueueConnector]
  val mockAuthConnector = mock[AuthConnector]
  val mockAuditConnector = mock[AuditConnector]

  private lazy val controller = new RemoveBenefitController(mockKeyStoreService, mockAuthConnector, mockAuditConnector)(mockPayeConnector, mockTxQueueConnector) with MockedTaxYearSupport {
    override def now = () => dateToday
  }

  private lazy val formController = new DisplayRemoveBenefitFormController(mockKeyStoreService, mockAuthConnector, mockAuditConnector)(mockPayeConnector, mockTxQueueConnector) with MockedTaxYearSupport {
    override def now = () => dateToday
  }

  private def generateKeystoreActionId(benefitTypes: String, taxYear: Int, employmentSequenceNumber: Int) = {
    s"RemoveBenefit:$benefitTypes:$taxYear:$employmentSequenceNumber"
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

  private def setupMocksForJohnDensmore(taxCodes: Seq[TaxCode], employments: Seq[Employment], benefits: Seq[Benefit]) {
    implicit val hc = HeaderCarrier()
    when(mockPayeConnector.linkedResource[Seq[TaxCode]]("/paye/AB123456C/tax-codes/2013")).thenReturn(Some(taxCodes))
    when(mockPayeConnector.linkedResource[Seq[Employment]]("/paye/AB123456C/employments/2013")).thenReturn(Some(employments))
    when(mockPayeConnector.linkedResource[Seq[Benefit]]("/paye/AB123456C/benefit-car/2013")).thenReturn(Some(benefits))
  }

  "Removing FUEL benefit only" should {

    "notify the user the fuel benefit will be removed for benefit with no company name" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)
      val result = formController.benefitRemovalFormAction(johnDensmore, requestWithCorrectVersion, FUEL.toString, 2013, 2)

      val doc = Jsoup.parse(contentAsString(result))

      doc.select(".benefit-type").text shouldBe "Your old company fuel benefit"
      doc.select("label[for=removeCar]").text should include("I would also like to remove my car benefit.")
    }

    "not show the car checkbox when the user has no car benefit" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq(fuelBenefit))
      val result = formController.benefitRemovalFormAction(johnDensmore, requestWithCorrectVersion, FUEL.toString, 2013, 2)

      val doc = Jsoup.parse(contentAsString(result))

      doc.select(".benefit-type").text shouldBe "Your old company fuel benefit"
      doc.select("label[for=removeCar]") shouldBe empty
    }

  }

  "Removing non-FUEL benefit " should {

    "not display car removal checkbox" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)
      val result = formController.benefitRemovalFormAction(johnDensmore, requestWithCorrectVersion, CAR.toString, 2013, 2)

      val doc = Jsoup.parse(contentAsString(result))

      doc.select("label[for=removeCar]").text shouldBe ""
    }
  }

  "Removing your benefit without checking the agreement checkbox in the form" should {

    "keep the previously entered date when redirected to the form for a car benefit" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val result = controller.requestBenefitRemovalAction(johnDensmore, requestWithCorrectVersion.withFormUrlEncodedBody(buildDateFormField("withdrawDate", Some(("2013", "9", "1"))): _*), CAR.toString, 2013, 2)

      status(result) shouldBe 400
      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById("withdrawDate.day-1").attr("selected") shouldBe "selected"
      doc.getElementById("withdrawDate.month-9").attr("selected") shouldBe "selected"
      doc.getElementById("withdrawDate.year-2013").attr("selected") shouldBe "selected"
    }

    "keep the previously entered date when redirected to the form for any benefit except car" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val result = controller.requestBenefitRemovalAction(johnDensmore, requestWithCorrectVersion.withFormUrlEncodedBody(buildDateFormField("withdrawDate", Some(("2013", "9", "1"))): _*), FUEL.toString, 2013, 2)

      status(result) shouldBe 400
      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".benefit-type").text should include("Your old company fuel benefit")

      doc.getElementById("withdrawDate.day-1").attr("selected") shouldBe "selected"
      doc.getElementById("withdrawDate.month-9").attr("selected") shouldBe "selected"
      doc.getElementById("withdrawDate.year-2013").attr("selected") shouldBe "selected"
    }
  }

  "Removing your car prior to its start date" should {

    "Not validate the view and redirect with correct error" in new WithApplication(FakeApplication()) {

      val benefitStartDate = new LocalDate()
      val startDateToString = DateTimeFormat.forPattern("yyyy-MM-dd").print(benefitStartDate)
      val calculation: String = "/calculation/paye/thenino/benefit/withdraw/3333/" + startDateToString + "/withdrawDate"

      val carBenefitStartedThisYear = Benefit(31, 2013, 321.42, 2, None, None, None, None, None, None, None,
        Some(Car(None, None, Some(new LocalDate(2012, 12, 12)), Some(0), Some("diesel"), Some(124), Some(1400), None, Some(BigDecimal("12343.21")), None, None)), Map.empty, Map("withdraw" -> calculation))

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq(carBenefitStartedThisYear))

      val withdrawDate = benefitStartDate.minusDays(2)

      def requestBenefitRemovalFormSubmission(date: Option[LocalDate], agreed: Boolean, removeFuel: Boolean) =
        requestWithCorrectVersion.withFormUrlEncodedBody(Seq(
          "agreement" -> agreed.toString.toLowerCase)
          ++ buildDateFormField("withdrawDate", Some(localDateToTuple(Some(withdrawDate)))): _*)

      val result = controller.requestBenefitRemovalAction(johnDensmore, requestBenefitRemovalFormSubmission(Some(withdrawDate), true, true), "31", 2013, 2)

      verify(mockPayeConnector, never).calculateWithdrawBenefit(_, _)

      status(result) shouldBe BAD_REQUEST

      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".error-notification").text should include("Invalid date: Return date cannot be before benefit was made available")
    }
  }

  "Given a user who has car and fuel benefits, removing fuel and selecting the option to remove car benefit too" should {

    "in step 2 allow the user to remove fuel and car together" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val withdrawDate = new LocalDate(2013, 12, 8)

      val carCalculationResult = RemoveBenefitCalculationResponse(Map("2013" -> BigDecimal(123.46), "2014" -> BigDecimal(0)))
      when(mockPayeConnector.calculateWithdrawBenefit(carBenefit, withdrawDate)).thenReturn(carCalculationResult)

      val fuelCalculationResult = RemoveBenefitCalculationResponse(Map("2013" -> BigDecimal(20.01), "2014" -> BigDecimal(0)))
      when(mockPayeConnector.calculateWithdrawBenefit(fuelBenefit, withdrawDate)).thenReturn(fuelCalculationResult)


      val result = controller.requestBenefitRemovalAction(johnDensmore, requestBenefitRemovalFormSubmission(Some(withdrawDate), true, None, Some(withdrawDate), Some(true)), FUEL.toString, 2013, 2)

      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#benefit-type-fuel").text should include("fuel")
      doc.select("#benefit-type-car").text should include("car")
      doc.select("#start-date-29").text shouldBe "10 September 2013"
      doc.select("#start-date-31").text shouldBe "30 May 2013"
      doc.select("#withdraw-date-29").text shouldBe "8 December 2013"
      doc.select("#withdraw-date-31").text shouldBe "8 December 2013"
      doc.select("#apportioned-value-29").text shouldBe "£20"
      doc.select("#apportioned-value-31").text shouldBe "£123"
      doc.select("#allowance-increase").text shouldBe "£200"
    }
  }

  "Given a user who has car and fuel benefits, removing fuel and then separately car benefit " should {

    def requestBenefitRemovalFormSubmission(date: Option[LocalDate]) =
      requestWithCorrectVersion.withFormUrlEncodedBody(Seq(
        "agreement" -> "true")
        ++ buildDateFormField("withdrawDate", Some(localDateToTuple(date))): _*)

    "allow the user to remove fuel first without showing error" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val fuelWithdrawDate = new LocalDate(2013, 12, 8)
      val fuelCalculationResult = RemoveBenefitCalculationResponse(Map("2013" -> BigDecimal(3.46), "2014" -> BigDecimal(0)))
      when(mockPayeConnector.calculateWithdrawBenefit(fuelBenefit, fuelWithdrawDate)).thenReturn(fuelCalculationResult)

      val result = controller.requestBenefitRemovalAction(johnDensmore, requestBenefitRemovalFormSubmission(Some(fuelWithdrawDate)), s"$FUEL", 2013, 2)

      status(result) shouldBe 200

      val doc = Jsoup.parse(contentAsString(result))
      val error = doc.select(".error-notification").text
      doc.select("#benefit-type-fuel").text should include("fuel")
      doc.select("#benefit-type-car").text should not include "car"
      doc.select("#start-date-29").text shouldBe "10 September 2013"
      doc.select("#start-date-31") shouldBe empty
      doc.select("#withdraw-date-29").text shouldBe "8 December 2013"
      doc.select("#withdraw-date-31") shouldBe empty
      doc.select("#apportioned-value-29").text shouldBe "£3"
      doc.select("#apportioned-value-31") shouldBe empty
      doc.select("#allowance-increase").text shouldBe "£18"
    }

    "allow the user to remove car benefit when fuel is already removed without showing error" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits.filterNot(_ == fuelBenefit))

      val carWithdrawDate = new LocalDate()
      val carCalculationResult = RemoveBenefitCalculationResponse(Map("2013" -> BigDecimal(123.46), "2014" -> BigDecimal(0)))
      when(mockPayeConnector.calculateWithdrawBenefit(carBenefit, carWithdrawDate)).thenReturn(carCalculationResult)

      val result = controller.requestBenefitRemovalAction(johnDensmore, requestBenefitRemovalFormSubmission(Some(carWithdrawDate)), "31", 2013, 2)
      status(result) shouldBe 200

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#benefit-type-car").text should include("car")
      doc.select("#benefit-type-fuel").text should not include "fuel"
      doc.select(".amount").text shouldBe "£197"
    }

  }

  "Given a user who has car and fuel benefits, removing car benefit " should {

    "In step 1, give the user the option to remove fuel benefit on the same (or different) date as the car" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val result = formController.benefitRemovalFormAction(johnDensmore, requestWithCorrectVersion, CAR.toString, 2013, 2)
      status(result) shouldBe 200

      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".fuel-benefit-info") should not be empty
      doc.getElementById("fuelRadio-sameDateFuel") should not be null
      doc.getElementById("fuelRadio-differentDateFuel") should not be null
    }

    "in step 1, display error message if user has not selected the type of date for fuel removal" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val withdrawDate = new LocalDate()
      val result = controller.requestBenefitRemovalAction(johnDensmore, requestBenefitRemovalFormSubmission(Some(withdrawDate), true, None), "31", 2013, 2)

      status(result) shouldBe BAD_REQUEST

      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".error-notification").text should include("select an option")

      verify(mockPayeConnector, times(0)).calculateWithdrawBenefit(Matchers.any[Benefit], Matchers.any[LocalDate]())(Matchers.eq(hc))
    }

    "in step 1, display the calculated value for removing both fuel and car benefit from the same date" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val withdrawDate = new LocalDate()

      val carCalculationResult = RemoveBenefitCalculationResponse(Map("2013" -> BigDecimal(123.46), "2014" -> BigDecimal(0)))
      when(mockPayeConnector.calculateWithdrawBenefit(carBenefit, withdrawDate)).thenReturn(carCalculationResult)

      val fuelCalculationResult = RemoveBenefitCalculationResponse(Map("2013" -> BigDecimal(10.01), "2014" -> BigDecimal(0)))
      when(mockPayeConnector.calculateWithdrawBenefit(fuelBenefit, withdrawDate)).thenReturn(fuelCalculationResult)


      val result = controller.requestBenefitRemovalAction(johnDensmore, requestBenefitRemovalFormSubmission(Some(withdrawDate), true, Some("sameDateFuel")), "31", 2013, 2)

      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("h1").text should include("car and fuel")
      doc.select(".amount").text shouldBe "£210"
    }

    "in step 1, display error message if user choose different date but dont select a fuel date" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val withdrawDate = new LocalDate()
      val result = controller.requestBenefitRemovalAction(johnDensmore, requestBenefitRemovalFormSubmission(Some(withdrawDate), true, Some("differentDateFuel")), "31", 2013, 2)

      status(result) shouldBe BAD_REQUEST
      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".error-notification").text should include("Please enter a date")

      verify(mockPayeConnector, times(0)).calculateWithdrawBenefit(Matchers.any[Benefit], Matchers.any[LocalDate]())(Matchers.eq(hc))
    }

    "in step 1, display error message if user choose a fuel date that is greater than the car return date" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val withdrawDate = new LocalDate()
      val fuelDate = new LocalDate().plusDays(1);

      val carCalculationResult = RemoveBenefitCalculationResponse(Map("2013" -> BigDecimal(123.46), "2014" -> BigDecimal(0)))
      when(mockPayeConnector.calculateWithdrawBenefit(carBenefit, withdrawDate)).thenReturn(carCalculationResult)

      val fuelCalculationResult = RemoveBenefitCalculationResponse(Map("2013" -> BigDecimal(10.01), "2014" -> BigDecimal(0)))
      when(mockPayeConnector.calculateWithdrawBenefit(fuelBenefit, withdrawDate)).thenReturn(fuelCalculationResult)


      val result = controller.requestBenefitRemovalAction(johnDensmore, requestBenefitRemovalFormSubmission(Some(withdrawDate), true, Some("differentDateFuel"), Some(fuelDate)), "31", 2013, 2)

      status(result) shouldBe BAD_REQUEST
      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".error-notification").text should include("Return date for fuel cannot be after car return date")
    }

    "in step 1, display error message if user choose a fuel date before start of tax year" in new WithApplication(FakeApplication()) {
      val benefitStartDate = new LocalDate().minusYears(3)
      val carBenefitStatedLongTimeAgo = Benefit(31, 2013, 321.42, 2, None, None, None, None, None, None, None,
        Some(Car(Some(benefitStartDate), None, Some(new LocalDate(2012, 12, 12)), Some(0), Some("diesel"), Some(124), Some(1400), None, Some(BigDecimal("12343.21")), None, None)), Map.empty, Map.empty)

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq(carBenefitStatedLongTimeAgo, fuelBenefit))

      val withdrawDate = new LocalDate()
      val fuelDate = withdrawDate.minusYears(1)

      val carCalculationResult = RemoveBenefitCalculationResponse(Map("2013" -> BigDecimal(123.46), "2014" -> BigDecimal(0)))
      when(mockPayeConnector.calculateWithdrawBenefit(carBenefit, withdrawDate)).thenReturn(carCalculationResult)

      val fuelCalculationResult = RemoveBenefitCalculationResponse(Map("2013" -> BigDecimal(10.01), "2014" -> BigDecimal(0)))
      when(mockPayeConnector.calculateWithdrawBenefit(fuelBenefit, withdrawDate)).thenReturn(fuelCalculationResult)

      val dateintaxyear = TaxYearResolver.startOfCurrentTaxYear
      val result = controller.requestBenefitRemovalAction(johnDensmore, requestBenefitRemovalFormSubmission(Some(withdrawDate), true, Some("differentDateFuel"), Some(fuelDate)), "31", 2013, 2)

      status(result) shouldBe BAD_REQUEST
      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".error-notification").text should include("Return date cannot be in previous tax years")
    }

    "display error message if user chooses a fuel date which is malformed" in new WithApplication(FakeApplication()) {
      val benefitStartDate = new LocalDate().minusYears(3)
      val carBenefitStatedLongTimeAgo = Benefit(31, 2013, 321.42, 2, None, None, None, None, None, None, None,
        Some(Car(Some(benefitStartDate), None, Some(new LocalDate(2012, 12, 12)), Some(0), Some("diesel"), Some(124), Some(1400), None, Some(BigDecimal("12343.21")), None, None)), Map.empty, Map.empty)

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq(carBenefitStatedLongTimeAgo, fuelBenefit))

      val withdrawDate = new LocalDate()

      val requestBenefitRemovalForm = requestWithCorrectVersion.withFormUrlEncodedBody(Seq("agreement" -> "true", "fuelRadio" -> "differentDateFuel")
        ++ buildDateFormField("fuelWithdrawDate", Some(("aa", "bb", "cc")))
        ++ buildDateFormField("withdrawDate", Some(localDateToTuple(Some(withdrawDate)))): _*)

      val result = controller.requestBenefitRemovalAction(johnDensmore, requestBenefitRemovalForm, "31", 2013, 2)

      status(result) shouldBe BAD_REQUEST
      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".error-notification").text should include("You must specify a valid date")
    }

    "not validate the fuelWithdrawDate if user chooses a fuel date which is malformed but specifies same fuel withdrawal date as the car withdrawal date" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val carCalculationResult = RemoveBenefitCalculationResponse(Map("2013" -> BigDecimal(123.46), "2014" -> BigDecimal(0)))
      when(mockPayeConnector.calculateWithdrawBenefit(Matchers.argThat(isBenefitOfType(31)), Matchers.any[LocalDate]())(Matchers.eq(hc))).thenReturn(carCalculationResult)

      val fuelCalculationResult = RemoveBenefitCalculationResponse(Map("2013" -> BigDecimal(10.01), "2014" -> BigDecimal(0)))
      when(mockPayeConnector.calculateWithdrawBenefit(Matchers.argThat(isBenefitOfType(29)), Matchers.any[LocalDate]())(Matchers.eq(hc))).thenReturn(fuelCalculationResult)

      val withdrawDate = new LocalDate()
      val requestBenefitRemovalForm = requestWithCorrectVersion.withFormUrlEncodedBody(Seq("agreement" -> "true", "fuelRadio" -> "sameDateFuel")
        ++ buildDateFormField("fuelWithdrawDate", Some(("aa", "bb", "cc")))
        ++ buildDateFormField("withdrawDate", Some(localDateToTuple(Some(withdrawDate)))): _*)
      val result = controller.requestBenefitRemovalAction(johnDensmore, requestBenefitRemovalForm, "31", 2013, 2)

      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("h1").text should include("car and fuel")
      doc.select(".amount").text shouldBe "£210"
    }

    "in step 1, display error message if user choose a fuel date before benefit started" in new WithApplication(FakeApplication()) {

      val benefitStartDate = new LocalDate()
      val startDateToString = DateTimeFormat.forPattern("yyyy-MM-dd").print(benefitStartDate)
      val calculation: String = "/calculation/paye/thenino/benefit/withdraw/3333/" + startDateToString + "/withdrawDate"

      val carBenefitStartedThisYear = Benefit(31, 2013, 321.42, 2, None, None, None, None, None, None, None,
        Some(Car(Some(benefitStartDate), None, Some(new LocalDate(2012, 12, 12)), Some(0), Some("diesel"), Some(124), Some(1400), None, Some(BigDecimal("12343.21")), None, None)), Map.empty, Map("withdraw" -> calculation))


      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq(carBenefitStartedThisYear, fuelBenefit))

      val withdrawDate = benefitStartDate
      val fuelDate = benefitStartDate.minusDays(1)

      val carCalculationResult = RemoveBenefitCalculationResponse(Map("2013" -> BigDecimal(123.46), "2014" -> BigDecimal(0)))
      when(mockPayeConnector.calculateWithdrawBenefit(carBenefit, withdrawDate)).thenReturn(carCalculationResult)

      val fuelCalculationResult = RemoveBenefitCalculationResponse(Map("2013" -> BigDecimal(10.01), "2014" -> BigDecimal(0)))
      when(mockPayeConnector.calculateWithdrawBenefit(fuelBenefit, withdrawDate)).thenReturn(fuelCalculationResult)


      val result = controller.requestBenefitRemovalAction(johnDensmore, requestBenefitRemovalFormSubmission(Some(withdrawDate), true, Some("differentDateFuel"), Some(fuelDate)), "31", 2013, 2)

      status(result) shouldBe BAD_REQUEST
      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".error-notification").text should include("Return date cannot be before benefit was made available")
    }

    "in step 1, keep user choice for fuel date if a bad request redirect him to the form" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val carWithdrawDate = new LocalDate()

      val withdrawDate = new LocalDate(2013, 3, 9)
      val result = controller.requestBenefitRemovalAction(johnDensmore, requestBenefitRemovalFormSubmission(Some(withdrawDate), true, Some("differentDateFuel"), Some(withdrawDate), None), "31", 2013, 2)

      status(result) shouldBe BAD_REQUEST
      val doc = Jsoup.parse(contentAsString(result))

      doc.getElementById("fuelRadio-differentDateFuel").hasAttr("checked") shouldBe true
      doc.getElementById("fuelWithdrawDate.day-9").hasAttr("selected") shouldBe true
      doc.getElementById("fuelWithdrawDate.month-3").hasAttr("selected") shouldBe true
      doc.getElementById("fuelWithdrawDate.year-2013").hasAttr("selected") shouldBe true

      verify(mockPayeConnector, times(0)).calculateWithdrawBenefit(Matchers.any[Benefit], Matchers.any[LocalDate]())(Matchers.eq(hc))
    }

    "in step 2, display the calculated value for removing fuel and car benefit on different correct values" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val carWithdrawDate = new LocalDate(2013, 12, 8)
      val fuelWithdrawDate = carWithdrawDate.minusDays(1)

      val carCalculationResult = RemoveBenefitCalculationResponse(Map("2013" -> BigDecimal(123.46), "2014" -> BigDecimal(0)))
      when(mockPayeConnector.calculateWithdrawBenefit(carBenefit, carWithdrawDate)).thenReturn(carCalculationResult)

      val fuelCalculationResult = RemoveBenefitCalculationResponse(Map("2013" -> BigDecimal(20.01), "2014" -> BigDecimal(0)))
      when(mockPayeConnector.calculateWithdrawBenefit(fuelBenefit, fuelWithdrawDate)).thenReturn(fuelCalculationResult)


      val result = controller.requestBenefitRemovalAction(johnDensmore, requestBenefitRemovalFormSubmission(Some(carWithdrawDate), true, Some("differentDateFuel"), Some(fuelWithdrawDate)), "31", 2013, 2)

      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("h1").text should include("car and fuel")
      doc.select("#start-date-31").text shouldBe "30 May 2013"
      doc.select("#start-date-29").text shouldBe "10 September 2013"
      doc.select("#withdraw-date-31").text shouldBe "8 December 2013"
      doc.select("#withdraw-date-29").text shouldBe "7 December 2013"
      doc.select("#apportioned-value-31").text shouldBe "£123"
      doc.select("#apportioned-value-29").text shouldBe "£20"
      doc.select("#allowance-increase").text shouldBe "£200"
    }
    "in step 2, display the company car details and the date the car was given back" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val carWithdrawDate = new LocalDate(2013, 12, 8)
      val fuelWithdrawDate = carWithdrawDate.minusDays(1)
      val companyCarDetails = "company-car-details"

      val carCalculationResult = RemoveBenefitCalculationResponse(Map("2013" -> BigDecimal(123.46), "2014" -> BigDecimal(0)))
      when(mockPayeConnector.calculateWithdrawBenefit(carBenefit, carWithdrawDate)).thenReturn(carCalculationResult)

      val fuelCalculationResult = RemoveBenefitCalculationResponse(Map("2013" -> BigDecimal(20.01), "2014" -> BigDecimal(0)))
      when(mockPayeConnector.calculateWithdrawBenefit(fuelBenefit, fuelWithdrawDate)).thenReturn(fuelCalculationResult)


      val result = controller.requestBenefitRemovalAction(johnDensmore, requestBenefitRemovalFormSubmission(Some(carWithdrawDate), true, Some("differentDateFuel"), Some(fuelWithdrawDate)), "31", 2013, 2)

      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      doc.getElementById(companyCarDetails) should not be null
      doc.select("#company-name").text should include("Company car provided by")
      doc.select("#car-benefit-car-value").text shouldBe "£12,343"
      doc.select("#car-benefit-engine").text shouldBe "1,400cc or less"
      doc.select("#car-benefit-fuel-type").text shouldBe "Diesel"
      doc.select("#car-benefit-date-available").text shouldBe "12 December 2012"
      doc.select("#private-fuel").text shouldBe "Yes, private fuel is available when you use the car"
    }
  }

  "The car benefit removal method" should {
    "In step 1, display page correctly for well formed request" in new WithApplication(FakeApplication()) {

      val car = Car(Some(new LocalDate(1994, 10, 7)), None, Some(new LocalDate(2012, 12, 12)), Some(0), Some("diesel"), Some(124), Some(1440), None, Some(BigDecimal("15000")), None, None)
      val specialCarBenefit = Benefit(31, 2013, 666, 3, None, None, None, None, None, None, None,
        Some(car), Map.empty, Map.empty)

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, Seq(Employment(sequenceNumber = 3, startDate = new LocalDate(2013, 10, 14), endDate = None, taxDistrictNumber = "899", payeNumber = "1212121", employerName = None, employmentType = primaryEmploymentType)), Seq(specialCarBenefit))
      val result = formController.benefitRemovalFormAction(johnDensmore, requestWithCorrectVersion, CAR.toString, 2013, 3)

      status(result) shouldBe 200
    }

    "in step 1, notify the user that the fuel benefit is going to be removed with the car benefit when removing car benefit" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val result = formController.benefitRemovalFormAction(johnDensmore, requestWithCorrectVersion, "31", 2013, 2)
      status(result) shouldBe 200

      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".fuel-benefit-info") should not be empty

    }

    "in step 1, not notify the user about fuel benefit if the user does not have one" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq(carBenefit))

      val result = formController.benefitRemovalFormAction(johnDensmore, requestWithCorrectVersion, "31", 2013, 2)
      status(result) shouldBe 200

      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".fuel-benefit-info") shouldBe empty

    }

    "in step 2, display the calculated value for removing both fuel and car benefit if the user chose to remove the car benefit" in new WithApplication(FakeApplication()) {
      val employment = johnDensmoresOneEmployment(2)
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, employment, johnDensmoresBenefits)

      val carCalculationResult = RemoveBenefitCalculationResponse(Map("2013" -> BigDecimal(123.46), "2014" -> BigDecimal(0)))
      when(mockPayeConnector.calculateWithdrawBenefit(Matchers.argThat(isBenefitOfType(31)), Matchers.any[LocalDate]())(Matchers.eq(hc))).thenReturn(carCalculationResult)

      val fuelCalculationResult = RemoveBenefitCalculationResponse(Map("2013" -> BigDecimal(10.01), "2014" -> BigDecimal(0)))
      when(mockPayeConnector.calculateWithdrawBenefit(Matchers.argThat(isBenefitOfType(29)), Matchers.any[LocalDate]())(Matchers.eq(hc))).thenReturn(fuelCalculationResult)

      val withdrawDate = new LocalDate(2013, 12, 8)
      val result = controller.requestBenefitRemovalAction(johnDensmore, requestBenefitRemovalFormSubmission(Some(withdrawDate), true, Some("sameDateFuel")), "31", 2013, 2)

      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#main-heading").text should include("car and fuel")
      doc.select("#first-section").text should include("your company car benefit from").and(include("and fuel benefit from"))
      doc.select("#start-date-31").text shouldBe "30 May 2013"
      doc.select("#start-date-29").text shouldBe "10 September 2013"
      doc.select("#withdraw-date-31").text shouldBe "8 December 2013"
      doc.select("#withdraw-date-29").text shouldBe "8 December 2013"
      doc.select("#apportioned-value-31").text shouldBe "£123"
      doc.select("#apportioned-value-29").text shouldBe "£10"
      doc.select("#allowance-increase").text shouldBe "£210"
    }

    "in step 2, display the calculated value for removing car benefit only if the user do not have fuel benefit for the same employment" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, carAndFuelBenefitWithDifferentEmploymentNumbers)

      val carCalculationResult = RemoveBenefitCalculationResponse(Map("2013" -> BigDecimal(123.46), "2014" -> BigDecimal(0)))
      when(mockPayeConnector.calculateWithdrawBenefit(Matchers.argThat(isBenefitOfType(31)), Matchers.any[LocalDate]())(Matchers.eq(hc))).thenReturn(carCalculationResult)

      val withdrawDate = dateToday.toLocalDate
      val result = controller.requestBenefitRemovalAction(johnDensmore, requestBenefitRemovalFormSubmission(Some(withdrawDate), true, Some("sameDateFuel")), "31", 2013, 2)

      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("h1").text should include("car")
      doc.select("h1").text should not include ("fuel")
      doc.select("#start-date-31").text shouldBe "30 May 2013"
      doc.select("#start-date-29") shouldBe empty
      doc.select("#withdraw-date-31").text shouldBe "8 December 2013"
      doc.select("#withdraw-date-29") shouldBe empty
      doc.select("#apportioned-value-31").text shouldBe "£123"
      doc.select("#apportioned-value-29") shouldBe empty
      doc.select("#allowance-increase").text shouldBe "£197"
    }

    "in step 2, request removal for both fuel and car benefit when both benefits are selected and user confirms" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      when(mockPayeConnector.removeBenefits(Matchers.any[String], Matchers.any[Int](), Matchers.any[Seq[RevisedBenefit]](), Matchers.any[LocalDate]())(Matchers.any())).thenReturn(Some(RemoveBenefitResponse(TransactionId("someIdForCarAndFuelRemoval"), Some("123L"), Some(9999))))

      val withdrawDate = new LocalDate(2013, 7, 18)
      val revisedAmounts = Map(carBenefit.benefitType.toString -> BigDecimal(210.17), fuelBenefit.benefitType.toString -> BigDecimal(14.1))
      when(mockKeyStoreService.getEntry[RemoveBenefitData](generateKeystoreActionId(s"${carBenefit.benefitType.toString},${fuelBenefit.benefitType.toString}", carBenefit.taxYear, carBenefit.employmentSequenceNumber), "paye", "remove_benefit", false)).thenReturn(Some(RemoveBenefitData(withdrawDate, revisedAmounts)))

      val resultF = controller.confirmBenefitRemovalAction(johnDensmore, requestWithCorrectVersion, s"$CAR,$FUEL", 2013, 2)

      val revisedBenefits = Seq(RevisedBenefit(carBenefit, BigDecimal(210.17)), RevisedBenefit(fuelBenefit, BigDecimal(14.1)))

      whenReady(resultF) { result =>
        status(result) shouldBe 303
        verify(mockPayeConnector, times(1)).removeBenefits("/paye/AB123456C/benefits/2013/1/update", 22, revisedBenefits, withdrawDate)
        val expectedUri = routes.RemoveBenefitController.benefitRemoved(s"$CAR,$FUEL", 2013, 2, "someIdForCarAndFuelRemoval", Some("123L"), Some(9999)).url
        redirectLocation(result) shouldBe Some(expectedUri)
      }
    }

    "in step 3, display page for confirmation of removal of both fuel and car benefit when both benefits are selected and user confirms" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val transaction: TxQueueTransaction = mock[TxQueueTransaction]
      when(mockTxQueueConnector.transaction(Matchers.eq("210"), Matchers.any[PayeRoot])(Matchers.eq(hc))).thenReturn(Some(transaction))

      val withdrawDate = new LocalDate(2013, 7, 18)
      val revisedAmounts = Map(carBenefit.benefitType.toString -> BigDecimal(210.17), fuelBenefit.benefitType.toString -> BigDecimal(14.1))

      when(mockKeyStoreService.getEntry[RemoveBenefitData](generateKeystoreActionId(carBenefit.benefitType.toString, carBenefit.taxYear, carBenefit.employmentSequenceNumber), "paye", "remove_benefit", false)).thenReturn(Some(RemoveBenefitData(withdrawDate, revisedAmounts)))

      val result = controller.benefitRemovedAction(johnDensmore, requestWithCorrectVersion, s"$CAR,$FUEL", 2013, 1, "210", Some("newTaxCode"), Some(9988))

      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#benefit-type").text should include("car and fuel")
      doc.select("#old-tax-code").text should include("430L")
      doc.select("#new-tax-code").text should include("newTaxCode")
      doc.select("#personal-allowance").text should include("£9,988")
    }

    "in step 3, do not display information about new tax code nor personal allowance if they are not available" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val transaction: TxQueueTransaction = mock[TxQueueTransaction]
      when(mockTxQueueConnector.transaction(Matchers.eq("210"), Matchers.any[PayeRoot])(Matchers.eq(hc))).thenReturn(Some(transaction))

      val withdrawDate = new LocalDate(2013, 7, 18)
      val revisedAmounts = Map(carBenefit.benefitType.toString -> BigDecimal(210.17), fuelBenefit.benefitType.toString -> BigDecimal(14.1))

      when(mockKeyStoreService.getEntry[RemoveBenefitData](generateKeystoreActionId(carBenefit.benefitType.toString, carBenefit.taxYear, carBenefit.employmentSequenceNumber), "paye", "remove_benefit", false)).thenReturn(Some(RemoveBenefitData(withdrawDate, revisedAmounts)))

      val result = controller.benefitRemovedAction(johnDensmore, requestWithCorrectVersion, s"$CAR,$FUEL", 2013, 1, "210", None, None)

      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#benefit-type").text should include("car and fuel")
      doc.select("#old-tax-code").text should include("430L")
      doc.select("#new-tax-code").size should be(0)
      doc.select("#personal-allowance").size should be(0)
    }

    "when confirming removal of benefits, it should return 400 for a request to remove a car benefit only (if they also have a fuel benefit)" in new WithApplication(FakeApplication()) {

      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      when(mockTxQueueConnector.transaction(Matchers.any[String])(Matchers.eq(hc))).thenReturn(Some(List.empty))

      val result = controller.confirmBenefitRemovalAction(johnDensmore, requestWithCorrectVersion, s"$CAR", 2013, 2)

      status(result) shouldBe BAD_REQUEST

      verifyZeroInteractions(mockKeyStoreService)
      verify(mockPayeConnector, never).removeBenefits(Matchers.any[String], Matchers.any[Int], Matchers.any[Seq[RevisedBenefit]], Matchers.any[LocalDate])(Matchers.any())
    }

  }

  "The remove benefit method" should {

    "in step 1 display an error message when return date of car greater than 7 days" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val invalidWithdrawDate = dateToday.toLocalDate.plusDays(36)
      val result = Future.successful(controller.requestBenefitRemovalAction(johnDensmore,
        requestBenefitRemovalFormSubmission(Some(invalidWithdrawDate), true), s"$CAR", 2013, 2))

      status(result) shouldBe 400

      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".benefit-type").text should include("Your old company car")
      doc.select(".error-notification").text should include("Invalid date: Return date cannot be greater than 7 days from today")
    }

    "in step 1 display an error message when return date of the car is in the previous tax year" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val invalidWithdrawDate = new LocalDate(1999, 2, 1)
      val result = Future.successful(controller.requestBenefitRemovalAction(johnDensmore,
        requestBenefitRemovalFormSubmission(Some(invalidWithdrawDate), true), s"$CAR", 2013, 2))

      status(result) shouldBe 400

      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".benefit-type").text should include("Your old company car")
      doc.select(".error-notification").text should include("Invalid date: Return date cannot be in previous tax years")
    }

    "in step 1 display an error message when return date of the car is in the next tax year" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val invalidWithdrawDate = new LocalDate(2030, 2, 1)
      val result = Future.successful(controller.requestBenefitRemovalAction(johnDensmore,
        requestBenefitRemovalFormSubmission(Some(invalidWithdrawDate), true), s"$CAR", 2013, 2))

      status(result) shouldBe 400
      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".benefit-type").text should include("Your old company car")
      doc.select(".error-notification").text should include("Invalid date: Return date cannot be in next tax years")
    }

    "in step 1 display an error message when return date is not set" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val result = controller.requestBenefitRemovalAction(johnDensmore, requestBenefitRemovalFormSubmission(None, true), s"$CAR", 2013, 2)

      status(result) shouldBe 400
      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".benefit-type").text should include("Your old company car")
      doc.select(".error-notification").text should include("Please enter a date")
    }


    "in step 1 display an error message when return date and agreement response is misformed" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val requestBenefitRemovalForm = requestWithCorrectVersion.withFormUrlEncodedBody(buildDateFormField("withdrawDate", Some(("A", "b", "2013"))): _*)
      val result = controller.requestBenefitRemovalAction(johnDensmore, requestBenefitRemovalForm, s"$FUEL", 2013, 2)

      status(result) shouldBe 400
      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".benefit-type").text should include("Your old company")
      doc.select(".error-notification").text should include("You must specify a valid date")
    }

    "in step 2 display the calculated value" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq(carBenefit))

      val calculationResult = RemoveBenefitCalculationResponse(Map("2013" -> BigDecimal(123.46), "2014" -> BigDecimal(0)))
      when(mockPayeConnector.calculateWithdrawBenefit(Matchers.any[Benefit](), Matchers.any[LocalDate]())(Matchers.eq(hc))).thenReturn(calculationResult)

      val withdrawDate = new LocalDate()
      val result = controller.requestBenefitRemovalAction(johnDensmore, requestBenefitRemovalFormSubmission(Some(withdrawDate), true), s"$CAR", 2013, 2)

      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".amount").text should include("£197")
    }

    "in step 2 save the withdrawDate to the keystore" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, Seq(carBenefit))

      val revisedAmount = BigDecimal(123.46)
      val withdrawDate = new LocalDate()

      val calculationResult = RemoveBenefitCalculationResponse(Map("2013" -> revisedAmount, "2014" -> BigDecimal(0)))
      when(mockPayeConnector.calculateWithdrawBenefit(Matchers.any[Benefit](), Matchers.any[LocalDate]())(Matchers.eq(hc))).thenReturn(calculationResult)

      val resultF = controller.requestBenefitRemovalAction(johnDensmore, requestBenefitRemovalFormSubmission(Some(withdrawDate), true), s"$CAR", 2013, 2)

      whenReady(resultF) { result =>
        val revisedAmounts = Map("31" -> BigDecimal(123.46))
        verify(mockKeyStoreService, times(1)).addKeyStoreEntry(generateKeystoreActionId(carBenefit.benefitType.toString, carBenefit.taxYear, carBenefit.employmentSequenceNumber), "paye", "remove_benefit", RemoveBenefitData(withdrawDate, revisedAmounts), false)
      }
    }

    "in step 2 call the paye service to remove the benefit and render the success page" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      when(mockPayeConnector.removeBenefits(Matchers.any[String], Matchers.any[Int](), Matchers.any[Seq[RevisedBenefit]](), Matchers.any[LocalDate]())(Matchers.any())).thenReturn(Some(RemoveBenefitResponse(TransactionId("someId"), Some("123L"), Some(9999))))

      val withdrawDate = new LocalDate(2013, 7, 18)
      val revisedAmounts = Map("29" -> BigDecimal(123.45))
      when(mockKeyStoreService.getEntry[RemoveBenefitData](generateKeystoreActionId(fuelBenefit.benefitType.toString, fuelBenefit.taxYear, fuelBenefit.employmentSequenceNumber), "paye", "remove_benefit", false)).thenReturn(Some(RemoveBenefitData(withdrawDate, revisedAmounts)))

      val result = controller.confirmBenefitRemovalAction(johnDensmore, requestWithCorrectVersion, s"$FUEL", 2013, 2)

      status(result) shouldBe 303
      val expectedUri = routes.RemoveBenefitController.benefitRemoved(s"$FUEL", 2013, 2, "someId", Some("123L"), Some(9999)).url
      redirectLocation(result) shouldBe Some(expectedUri)

      val revisedBenefits = Seq(RevisedBenefit(fuelBenefit, BigDecimal(123.45)))
      verify(mockPayeConnector, times(1)).removeBenefits("/paye/AB123456C/benefits/2013/1/update", 22, revisedBenefits, withdrawDate)
    }

    "When posting the benefit removal form, remove car benefit too if requested" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val carCalculationResult = RemoveBenefitCalculationResponse(Map("2013" -> BigDecimal(123.46), "2014" -> BigDecimal(0)))
      when(mockPayeConnector.calculateWithdrawBenefit(Matchers.argThat(isBenefitOfType(CAR)), Matchers.any[LocalDate]())(Matchers.eq(hc))).thenReturn(carCalculationResult)

      val fuelCalculationResult = RemoveBenefitCalculationResponse(Map("2013" -> BigDecimal(10.01), "2014" -> BigDecimal(0)))
      when(mockPayeConnector.calculateWithdrawBenefit(Matchers.argThat(isBenefitOfType(FUEL)), Matchers.any[LocalDate]())(Matchers.eq(hc))).thenReturn(fuelCalculationResult)

      val withdrawDate = new LocalDate()
      val result = controller.requestBenefitRemovalAction(johnDensmore, requestBenefitRemovalFormSubmission(Some(withdrawDate), true, removeCar = Some(true)), FUEL.toString, 2013, 2)

      status(result) shouldBe 200
      val response = Jsoup.parse(contentAsString(result))
      response.select("h1").text should include("fuel and car")
      response.select(".amount").text shouldBe "£210"
    }

    "When posting the benefit removal form, don t remove car benefit if not requested" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val fuelCalculationResult = RemoveBenefitCalculationResponse(Map("2013" -> BigDecimal(10.01), "2014" -> BigDecimal(0)))
      when(mockPayeConnector.calculateWithdrawBenefit(Matchers.argThat(isBenefitOfType(FUEL)), Matchers.any[LocalDate]())(Matchers.eq(hc))).thenReturn(fuelCalculationResult)

      val withdrawDate = new LocalDate()
      val result = controller.requestBenefitRemovalAction(johnDensmore, requestBenefitRemovalFormSubmission(Some(withdrawDate), true), FUEL.toString, 2013, 2)

      status(result) shouldBe 200
      val response = Jsoup.parse(contentAsString(result))
      response.select("h1").text should include("company fuel")
      response.select(".amount").text shouldBe "£12"
    }

    "in step 3 return 404 if the transaction does not exist" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      when(mockTxQueueConnector.transaction(Matchers.eq("123"), Matchers.any[PayeRoot])(Matchers.eq(hc))).thenReturn(None)
      val revisedAmounts = Map("31" -> BigDecimal(555))
      when(mockKeyStoreService.getEntry[RemoveBenefitData](generateKeystoreActionId(carBenefit.benefitType.toString, carBenefit.taxYear, carBenefit.employmentSequenceNumber), "paye", "remove_benefit", false)).thenReturn(Some(RemoveBenefitData(withdrawDate, revisedAmounts)))

      val withdrawDate = new LocalDate(2013, 7, 18)
      val result = controller.benefitRemovedAction(johnDensmore, requestWithCorrectVersion, s"$CAR", 2013, 1, "123", Some("newCode"), Some(9998))

      status(result) shouldBe 404

    }

    "return the updated benefits list page if the user has gone back in the browser and resubmitted and the benefit has already been removed" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val result = formController.benefitRemovalFormAction(johnDensmore, requestWithCorrectVersion, s"$CAR", 2013, 1)
      status(result) shouldBe 303

      val expectedUri = routes.CarBenefitHomeController.carBenefitHome().url
      redirectLocation(result) shouldBe Some(expectedUri)
    }

    "return the benefits list page if the user modifies the url to include a benefit type that they can not remove" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val result = formController.benefitRemovalFormAction(johnDensmore, requestWithCorrectVersion, "30", 2013, 1)
      status(result) shouldBe 303
      val expectedUri = routes.CarBenefitHomeController.carBenefitHome().url
      redirectLocation(result) shouldBe Some(expectedUri)
    }

    "return to the benefits list page if the user modifies the url to include an incorrect sequence number" in new WithApplication(FakeApplication()) {
      setupMocksForJohnDensmore(johnDensmoresTaxCodes, johnDensmoresEmployments, johnDensmoresBenefits)

      val result = formController.benefitRemovalFormAction(johnDensmore, requestWithCorrectVersion, s"$CAR", 2013, 3)
      status(result) shouldBe 303
      val expectedUri = routes.CarBenefitHomeController.carBenefitHome().url
      redirectLocation(result) shouldBe Some(expectedUri)
    }
  }

  "benefitRemoved" should {
    "render a view with correct elements" in new WithApplication(FakeApplication()) {

      val car = Car(None, Some(new LocalDate(2012, 12, 12)), None, Some(BigDecimal(10)), Some("diesel"), Some(1), Some(1400), None, Some(BigDecimal("1432")), None, None)

      val versionNumber = 1
      val payeRoot = new PayeRoot("CE927349E", versionNumber, "Mr", "Will", None, "Shakespeare", "Will Shakespeare", "1983-01-02", Map(), Map(), Map()) {
        override def fetchEmployments(taxYear: Int)(implicit payeConnector: PayeConnector, headerCarrier: HeaderCarrier): Future[Seq[Employment]] = {
          Seq(Employment(1, new LocalDate(), Some(new LocalDate()), "123", "123123", None, primaryEmploymentType))
        }

        override def fetchBenefits(taxYear: Int)(implicit payeConnector: PayeConnector, headerCarrier: HeaderCarrier): Future[Seq[Benefit]] = {
          Future.successful(
            Seq(Benefit(31, 2013, BigDecimal("3"), 1, Some(BigDecimal("4")), Some(BigDecimal("5")), Some(BigDecimal("6")),
              Some(BigDecimal("7")), Some(BigDecimal("8")), Some("payment"), None, Some(car), Map[String, String](),
              Map[String, String]())))
        }
      }

      val user = User("wshakespeare", payeAuthority("someId", "CE927349E"), RegimeRoots(paye = Some(payeRoot)), None, None)

      val request = FakeRequest().
        withFormUrlEncodedBody("withdrawDate" -> "2013-07-13", "agreement" -> "true").
        withSession((BenefitFlowHelper.npsVersionKey, versionNumber.toString))

      when(mockPayeConnector.calculateWithdrawBenefit(Matchers.any[Benefit], Matchers.any[LocalDate])(Matchers.eq(hc))).thenReturn(RemoveBenefitCalculationResponse(Map("2013" -> BigDecimal("123"))))

      val result = controller.requestBenefitRemovalAction(user, request, "31", 2013, 1)
      status(result) shouldBe 400
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("h2").first().text should be("no longer supply a company car")
    }

    "Contain correct employee names" in new WithApplication(FakeApplication()) {
      val car = Car(None, None, Some(new LocalDate()), Some(BigDecimal(10)), Some("diesel"), Some(10), Some(1400), None, Some(BigDecimal("1432")), None, None)
      val versionNumber = 1
      val payeRoot = new PayeRoot("CE927349E", versionNumber, "Mr", "Will", None, "Shakespeare", "Will Shakespeare", "1983-01-02", Map(), Map(), Map()) {
        override def fetchEmployments(taxYear: Int)(implicit payeConnector: PayeConnector, headerCarrier: HeaderCarrier): Future[Seq[Employment]] = {
          Seq(Employment(1, new LocalDate(), Some(new LocalDate()), "123", "123123", Some("Sainsburys"), primaryEmploymentType))
        }

        override def fetchBenefits(taxYear: Int)(implicit payeConnector: PayeConnector, headerCarrier: HeaderCarrier): Future[Seq[Benefit]] = {
          Future.successful(Seq(Benefit(31, 2013, BigDecimal("3"), 1, Some(BigDecimal("4")), Some(BigDecimal("5")),
            Some(BigDecimal("6")), Some(BigDecimal("7")), Some(BigDecimal("8")), Some("payment"), None, Some(car),
            Map[String, String](), Map[String, String]())))
        }
      }

      val user = User("wshakespeare", payeAuthority("someId", "CE927349E"), RegimeRoots(paye = Some(payeRoot)), None, None)

      val request: play.api.mvc.Request[_] = FakeRequest().withFormUrlEncodedBody("withdrawDate" -> "2013-07-13", "agreement" -> "true").
        withSession((BenefitFlowHelper.npsVersionKey, versionNumber.toString))

      when(mockPayeConnector.calculateWithdrawBenefit(Matchers.any[Benefit], Matchers.any[LocalDate])(Matchers.eq(hc))).thenReturn(RemoveBenefitCalculationResponse(Map("2013" -> BigDecimal("123"))))

      val result = formController.benefitRemovalFormAction(user, request, "31", 2013, 1)
      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".checkbox").text should not include "Some("
    }

  }

  def requestBenefitRemovalFormSubmission(withdrawDate: Option[LocalDate], agreement: Boolean, fuelRadio: Option[String] = Some("sameDateFuel"), fuelWithdrawDate: Option[LocalDate] = None, removeCar: Option[Boolean] = Some(false)) =
    requestWithCorrectVersion.withFormUrlEncodedBody(Seq(
      "agreement" -> agreement.toString.toLowerCase,
      "fuelRadio" -> fuelRadio.getOrElse(""),
      "removeCar" -> removeCar.getOrElse("").toString.toLowerCase)
      ++ buildDateFormField("fuelWithdrawDate", Some(localDateToTuple(fuelWithdrawDate)))
      ++ buildDateFormField("withdrawDate", Some(localDateToTuple(withdrawDate))): _*)
}
