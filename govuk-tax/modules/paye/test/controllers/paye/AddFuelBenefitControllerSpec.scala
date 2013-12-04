package controllers.paye

import play.api.test.{FakeRequest, WithApplication}
import scala.concurrent._
import org.jsoup.Jsoup
import play.api.test.Helpers._
import uk.gov.hmrc.common.microservice.paye.domain._
import org.joda.time.LocalDate
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import org.scalatest.matchers.{MatchResult, Matcher}
import org.mockito.Mockito._
import FuelBenefitFormFields._
import controllers.DateFieldsHelper
import play.api.i18n.Messages
import org.mockito.{ArgumentCaptor, Matchers}
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import uk.gov.hmrc.utils.TaxYearResolver
import controllers.common.actions.HeaderCarrier
import play.api.mvc.SimpleResult
import uk.gov.hmrc.common.microservice.paye.domain.Car
import play.api.test.FakeApplication
import uk.gov.hmrc.common.microservice.paye.domain.TaxCode
import uk.gov.hmrc.common.microservice.paye.domain.TransactionId
import uk.gov.hmrc.common.microservice.paye.domain.NewBenefitCalculationResponse
import uk.gov.hmrc.common.microservice.paye.domain.AddBenefitResponse
import controllers.paye.AddFuelBenefitController.FuelBenefitDataWithGrossBenefit
import org.scalatest.concurrent.ScalaFutures
import controllers.paye.validation.{BenefitFlowHelper, AddBenefitFlow}

class AddFuelBenefitControllerSpec extends PayeBaseSpec with DateFieldsHelper with ScalaFutures {

  private val employmentSeqNumberOne = 1

  "calling start add fuel benefit" should {
    "return 200 and show the fuel page with the employer s name" in new TestCaseIn2012 {

      setupMocksForJohnDensmore()

      when(mockKeyStoreService.getEntry[FuelBenefitDataWithGrossBenefit](generateKeystoreActionId(testTaxYear, employmentSeqNumberOne), "paye", "AddFuelBenefitForm", false)).thenReturn(None)

      val result = controller.startAddFuelBenefitAction(johnDensmore, requestWithCorrectVersion, testTaxYear, employmentSeqNumberOne)

      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#company-name").text shouldBe "Weyland-Yutani Corp"
      doc.select("#heading").text should include("company fuel")

      verify(mockKeyStoreService).getEntry[FuelBenefitDataWithGrossBenefit](generateKeystoreActionId(testTaxYear, employmentSeqNumberOne), "paye", "AddFuelBenefitForm", false)
    }

    "return 200 and show the fuel page with the employer s name and previously populated data. EmployerPayFuelTrue and no dateWithdrawn specified" in new TestCaseIn2012 {

      setupMocksForJohnDensmore()

      val fuelBenefitData = FuelBenefitData(Some("true"), None)
      when(mockKeyStoreService.getEntry[FuelBenefitDataWithGrossBenefit](generateKeystoreActionId(testTaxYear, employmentSeqNumberOne), "paye", "AddFuelBenefitForm", false)).thenReturn(Some((fuelBenefitData, 0)))

      val result = controller.startAddFuelBenefitAction(johnDensmore, requestWithCorrectVersion, testTaxYear, employmentSeqNumberOne)

      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))

      doc.select("#employerPayFuel-true").attr("checked") shouldBe "checked"
      doc.select("#employerPayFuel-date").attr("checked") shouldBe empty
      doc.select("#employerPayFuel-again").attr("checked") shouldBe empty
    }

    "return 200 and show the fuel page with the employer s name and previously populated data. EmployerPayFuelTrue and dateWithdrawn specified including a date value" in new TestCaseIn2013 {
      val currentTaxYear = TaxYearResolver.currentTaxYear

      setupMocksForJohnDensmore()

      val dateWithdrawn = new LocalDate(currentTaxYear, 5, 30)
      val fuelBenefitData = FuelBenefitData(Some("date"), Some(dateWithdrawn))
      when(mockKeyStoreService.getEntry[FuelBenefitDataWithGrossBenefit](generateKeystoreActionId(testTaxYear, employmentSeqNumberOne), "paye", "AddFuelBenefitForm", false)).thenReturn(Some((fuelBenefitData, 0)))

      val result = controller.startAddFuelBenefitAction(johnDensmore, requestWithCorrectVersion, currentTaxYear, employmentSeqNumberOne)

      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))

      doc.select("#employerPayFuel-true").attr("checked") shouldBe empty
      doc.select("#employerPayFuel-date").attr("checked") shouldBe "checked"
      doc.select("#employerPayFuel-again").attr("checked") shouldBe empty

      doc.select("[id~=dateFuelWithdrawn]").select("[id~=day-30]").attr("selected") shouldBe "selected"
      doc.select("[id~=dateFuelWithdrawn]").select("[id~=month-5]").attr("selected") shouldBe "selected"
      doc.select("[id~=dateFuelWithdrawn]").select(s"[id~=year-$currentTaxYear]").attr("selected") shouldBe "selected"
    }

    "return 200 and show the fuel page with the employer s name and previously populated data. EmployerPayFuelAgain and no dateWithdrawn specified" in new TestCaseIn2012 {

      setupMocksForJohnDensmore()

      val fuelBenefitData = FuelBenefitData(Some("again"), None)
      when(mockKeyStoreService.getEntry[FuelBenefitDataWithGrossBenefit](generateKeystoreActionId(testTaxYear, employmentSeqNumberOne), "paye", "AddFuelBenefitForm", false)).thenReturn(Some((fuelBenefitData, 0)))

      val result = controller.startAddFuelBenefitAction(johnDensmore, requestWithCorrectVersion, testTaxYear, employmentSeqNumberOne)

      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))

      doc.select("#employerPayFuel-true").attr("checked") shouldBe empty
      doc.select("#employerPayFuel-date").attr("checked") shouldBe empty
      doc.select("#employerPayFuel-again").attr("checked") shouldBe "checked"
    }


    "return 200 and show the add fuel benefit form with the required fields and no values filled in" in new TestCaseIn2012 {

      setupMocksForJohnDensmore()
      when(mockKeyStoreService.getEntry[FuelBenefitDataWithGrossBenefit](generateKeystoreActionId(testTaxYear, employmentSeqNumberOne), "paye", "AddFuelBenefitForm", false)).thenReturn(None)

      val result = controller.startAddFuelBenefitAction(johnDensmore, requestWithCorrectVersion, testTaxYear, employmentSeqNumberOne)

      status(result) shouldBe 200

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#employerPayFuel-false") shouldBe empty
      doc.select("#employerPayFuel-true") should not be empty
      doc.select("#employerPayFuel-again") should not be empty
      doc.select("#employerPayFuel-date") should not be empty
      doc.select("#employerPayFuel-date").attr("checked") shouldBe empty
    }

    "return 200 and show the page for the fuel form with default employer name message if employer name does not exist " in new TestCaseIn2012 {

      val johnDensmoresNamelessEmployments = Seq(
        Employment(sequenceNumber = employmentSeqNumberOne, startDate = new LocalDate(testTaxYear, 7, 2), endDate = Some(new LocalDate(testTaxYear, 10, 8)), taxDistrictNumber = "898", payeNumber = "9900112", employerName = None, Employment.primaryEmploymentType))

      setupMocksForJohnDensmore(employments = johnDensmoresNamelessEmployments)
      when(mockKeyStoreService.getEntry[FuelBenefitDataWithGrossBenefit](generateKeystoreActionId(testTaxYear, employmentSeqNumberOne), "paye", "AddFuelBenefitForm", false)).thenReturn(None)

      val result = controller.startAddFuelBenefitAction(johnDensmore, requestWithCorrectVersion, testTaxYear, employmentSeqNumberOne)

      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#company-name").text shouldBe "your employer"
    }

    "return 400 when employer for sequence number does not exist" in new TestCaseIn2012 {

      setupMocksForJohnDensmore()

      val result = controller.startAddFuelBenefitAction(johnDensmore, requestWithCorrectVersion, testTaxYear, 5)

      status(result) shouldBe 400
    }

    "return to the car benefit home page if the user already has a fuel benefit" in new TestCaseIn2012 {

      setupMocksForJohnDensmore(benefits = johnDensmoresBenefitsForEmployer1)

      val result = controller.startAddFuelBenefitAction(johnDensmore, requestWithCorrectVersion, testTaxYear, employmentSeqNumberOne)
      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.CarBenefitHomeController.carBenefitHome().url)
    }

    "return 400 if the requested tax year is not the current tax year" in new TestCaseIn2012 {

      setupMocksForJohnDensmore()

      val result = controller.startAddFuelBenefitAction(johnDensmore, requestWithCorrectVersion, testTaxYear + 1, employmentSeqNumberOne)

      status(result) shouldBe 400
    }

    "return 400 if the employer is not the primary employer" in new TestCaseIn2012 {

      setupMocksForJohnDensmore()

      val result = controller.startAddFuelBenefitAction(johnDensmore, requestWithCorrectVersion, testTaxYear, 2)

      status(result) shouldBe 400
    }
  }


  "clicking next on the fuel benefit data entry page" should {

    "successfully store the form values in the keystore" in new TestCaseIn2012 {
      val carBenefitStartedThisYear = Benefit(31, testTaxYear, 321.42, 1, None, None, None, None, None, None, None,
        Some(Car(Some(new LocalDate(testTaxYear, 5, 12)), None, Some(new LocalDate(testTaxYear - 1, 12, 12)), Some(0), Some("diesel"), Some(124), Some(1400), None, Some(BigDecimal("12343.21")), None, None)), actions("AB123456C", testTaxYear, 1), Map.empty)

      setupMocksForJohnDensmore(benefits = Seq(carBenefitStartedThisYear))

      val fuelBenefitValue = 1234
      val benefitCalculationResponse = NewBenefitCalculationResponse(None, Some(fuelBenefitValue))
      when(mockPayeConnector.calculateBenefitValue(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Some(benefitCalculationResponse))

      val dateFuelWithdrawnFormData = new LocalDate(testTaxYear, 6, 3)
      val employerPayFuelFormData = "date"
      val request = newRequestForSaveAddFuelBenefit(employerPayFuelVal =
        Some(employerPayFuelFormData),
        dateFuelWithdrawnVal = Some((testTaxYear.toString, "6", "3")))

      val result = controller.reviewAddFuelBenefitAction(johnDensmore, request, testTaxYear, employmentSeqNumberOne)

      status(result) shouldBe 200

      val keyStoreDataCaptor = ArgumentCaptor.forClass(classOf[(FuelBenefitData, Int)])

      verify(mockKeyStoreService).addKeyStoreEntry(
        Matchers.eq(generateKeystoreActionId(testTaxYear, employmentSeqNumberOne)),
        Matchers.eq("paye"),
        Matchers.eq("AddFuelBenefitForm"),
        keyStoreDataCaptor.capture(),
        Matchers.eq(false))(Matchers.any(), Matchers.any())

      val (fuelBenefitData, grossAmount) = keyStoreDataCaptor.getValue

      fuelBenefitData.dateFuelWithdrawn shouldBe Some(dateFuelWithdrawnFormData)
      fuelBenefitData.employerPayFuel shouldBe Some(employerPayFuelFormData)
      grossAmount shouldBe fuelBenefitValue
    }

    "return 200 for employerpayefuel of type date with a correct date withdrawn and display the details in a table" in new TestCaseIn2012 {
      val carBenefitStartedThisYear = Benefit(31, testTaxYear, 321.42, 1, None, None, None, None, None, None, None,
        Some(Car(Some(new LocalDate(testTaxYear, 5, 12)), None, Some(new LocalDate(testTaxYear - 1, 12, 12)), Some(0), Some("diesel"), Some(124), Some(1400), None, Some(BigDecimal("12343.21")), None, None)), actions("AB123456C", testTaxYear, 1), Map.empty)

      setupMocksForJohnDensmore(benefits = Seq(carBenefitStartedThisYear))
      setupCalculationMock(calculationResult = 1234)

      val dateFuelWithdrawnFormData = new LocalDate(testTaxYear, 6, 3)
      val employerPayFuelFormData = "date"
      val request = newRequestForSaveAddFuelBenefit(employerPayFuelVal = Some(employerPayFuelFormData), dateFuelWithdrawnVal = Some((testTaxYear.toString, "6", "3")))

      val result = controller.reviewAddFuelBenefitAction(johnDensmore, request, testTaxYear, employmentSeqNumberOne)

      status(result) shouldBe 200

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#second-heading").text should include("Check your private fuel details")
      doc.select("#private-fuel").text should include(s"3 June $testTaxYear")
      doc.select("#provided-from").text should include(s"12 May $testTaxYear")
      doc.select("#fuelBenefitTaxableValue").text should include("£1,234")

    }

    "return 200 and show start date as beginning of the tax year if carMadeAvailable is earlier" in new TestCaseIn2012 {

      setupMocksForJohnDensmore(benefits = Seq(carBenefitEmployer1))


      val fuelBenefitValue = 1234
      val benefitCalculationResponse = NewBenefitCalculationResponse(None, Some(fuelBenefitValue))
      when(mockPayeConnector.calculateBenefitValue(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Some(benefitCalculationResponse))

      val request = newRequestForSaveAddFuelBenefit(employerPayFuelVal = Some("true"))

      val result = controller.reviewAddFuelBenefitAction(johnDensmore, request, testTaxYear, employmentSeqNumberOne)

      status(result) shouldBe 200

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#second-heading").text should include("Check your private fuel details")
      doc.select("#provided-from").text should include(s"6 April $testTaxYear")
      doc.select("#private-fuel").text should include(s"Yes, private fuel is available when you use the car")
    }

    "show the users recalculated tax code" in new TestCaseIn2012 {

      setupMocksForJohnDensmore(benefits = Seq(carBenefitEmployer1))
      setupCalculationMock(calculationResult = 1234)

      val request = newRequestForSaveAddFuelBenefit(employerPayFuelVal = Some("true"))

      val result = controller.reviewAddFuelBenefitAction(johnDensmore, request, testTaxYear, employmentSeqNumberOne)

      status(result) shouldBe 200

      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#fuelBenefitTaxableValue").text shouldBe "£1,234"
    }

    "return to the car benefit home page if the user already has a fuel benefit" in new TestCaseIn2012 {
      setupMocksForJohnDensmore(benefits = johnDensmoresBenefitsForEmployer1)

      val result = controller.reviewAddFuelBenefitAction(johnDensmore, newRequestForSaveAddFuelBenefit(), testTaxYear, employmentSeqNumberOne)
      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.CarBenefitHomeController.carBenefitHome().url)
    }

    "ignore invalid withdrawn date if employerpayfuel is not date" in new TestCaseIn2012 {
      setupMocksForJohnDensmore(benefits = Seq(carBenefitEmployer1))
      setupCalculationMock(calculationResult = 1234)

      val result = controller.reviewAddFuelBenefitAction(johnDensmore, newRequestForSaveAddFuelBenefit(employerPayFuelVal = Some("again"), dateFuelWithdrawnVal = Some(("isdufgpsiuf", "6", "3"))), testTaxYear, employmentSeqNumberOne)
      status(result) shouldBe 200
    }

    "return 400 and display error when values form data fails validation" in new TestCaseIn2012 {
      setupMocksForJohnDensmore()

      val request = newRequestForSaveAddFuelBenefit(employerPayFuelVal = Some("date"), dateFuelWithdrawnVal = Some(("jkhasgdkhsa", "05", "30")))
      val result = controller.reviewAddFuelBenefitAction(johnDensmore, request, testTaxYear, employmentSeqNumberOne)
      status(result) shouldBe 400
      val doc = Jsoup.parse(contentAsString(result))
      doc.select("#employerPayFuel-date").attr("checked") shouldBe "checked"
      doc.select("[id~=dateFuelWithdrawn]").select("[id~=day-30]").attr("selected") shouldBe "selected"

      verifyNoSaveToKeyStore()
    }

    "return 400 if the year submitted is not the current tax year" in new TestCaseIn2012 {
      setupMocksForJohnDensmore()

      val result = controller.reviewAddFuelBenefitAction(johnDensmore, newRequestForSaveAddFuelBenefit(), testTaxYear + 1, employmentSeqNumberOne)

      status(result) shouldBe 400
      verifyNoSaveToKeyStore()
    }

    "return 400 if the submitting employment number is not the primary employment" in new TestCaseIn2012 {
      setupMocksForJohnDensmore()

      val result = controller.reviewAddFuelBenefitAction(johnDensmore, newRequestForSaveAddFuelBenefit(), testTaxYear, 2)

      status(result) shouldBe 400
      verifyNoSaveToKeyStore()
    }

    "return 200 if the user selects again for the EMPLOYER PAY FUEL" in new TestCaseIn2012 {
      setupMocksForJohnDensmore(benefits = Seq(carBenefitEmployer1))
      setupCalculationMock(calculationResult = 1234)

      val request = newRequestForSaveAddFuelBenefit(employerPayFuelVal = Some("again"))
      val result = controller.reviewAddFuelBenefitAction(johnDensmore, request, testTaxYear, employmentSeqNumberOne)
      status(result) shouldBe 200
    }

    "return 400 if the user does not select any option for the EMPLOYER PAY FUEL question" in new TestCaseIn2012 {
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddFuelBenefit(employerPayFuelVal = None)
      val result = controller.reviewAddFuelBenefitAction(johnDensmore, request, testTaxYear, employmentSeqNumberOne)
      status(result) shouldBe 400
      val doc = Jsoup.parse(contentAsString(result))
      doc.select(".error-notification").text should be(Messages("error.paye.answer_mandatory"))
    }

    "return 400 if the user sends an invalid value for the EMPLOYER PAY FUEL question" in new TestCaseIn2012 {
      setupMocksForJohnDensmore()
      val request = newRequestForSaveAddFuelBenefit(employerPayFuelVal = Some("hacking!"))
      val result = controller.reviewAddFuelBenefitAction(johnDensmore, request, testTaxYear, employmentSeqNumberOne)
      status(result) shouldBe 400

      verifyNoSaveToKeyStore()
    }

    "return with an error (tbd) when a car benefit is not found" in new TestCaseIn2013 {
      setupMocksForJohnDensmore()

      val request = newRequestForSaveAddFuelBenefit(employerPayFuelVal = Some("true"))

      evaluating {
        await(controller.reviewAddFuelBenefitAction(johnDensmore, request, testTaxYear, employmentSeqNumberOne))
      } should produce[StaleHodDataException]

      verifyNoSaveToKeyStore()
    }
  }

  "clicking submit on the fuel benefit review page" should {
    "submit the corresponding keystore data to the paye service and then show the success page when successful" in new TestCaseIn2012 {
      val grossFuelBenefit = 1000
      val carBenefitStartedThisYear = Benefit(31, testTaxYear, 321.42, 1, None, None, None, None, None, None, None,
        Some(Car(Some(new LocalDate(testTaxYear, 5, 12)), None, Some(new LocalDate(testTaxYear - 1, 12, 12)), Some(0), Some("diesel"), Some(124), Some(1400), None, Some(BigDecimal("12343.21")), None, None)), actions("AB123456C", testTaxYear, 1), Map.empty)
      setupMocksForJohnDensmore(benefits = Seq(carBenefitStartedThisYear), taxCodes = Seq(TaxCode(employmentSeqNumberOne, Some(1), testTaxYear, "oldTaxCode", List.empty)))
      val fuelBenefitData = FuelBenefitData(Some("true"), None)
      when(mockKeyStoreService.getEntry[FuelBenefitDataWithGrossBenefit](generateKeystoreActionId(testTaxYear, employmentSeqNumberOne), "paye", "AddFuelBenefitForm", false)).thenReturn(Some((fuelBenefitData, grossFuelBenefit)))
      val benefitsCapture = ArgumentCaptor.forClass(classOf[Seq[Benefit]])
      val addBenefitResponse = AddBenefitResponse(TransactionId("anOid"), Some("newTaxCode"), Some(5))
      when(mockPayeConnector.addBenefits(Matchers.eq("/paye/AB123456C/benefits/2012"), Matchers.eq(johnDensmore.getPaye.version), Matchers.eq(employmentSeqNumberOne), benefitsCapture.capture())(Matchers.any())).thenReturn(Some(addBenefitResponse))

      val resultF = controller.confirmAddFuelBenefitAction(johnDensmore, requestWithCorrectVersion, testTaxYear, employmentSeqNumberOne)

      whenReady(resultF) { result =>

        val benefitsSentToPaye = benefitsCapture.getValue
        benefitsSentToPaye should have length 1
        val expectedFuelBenefit = Some(Benefit(29, 2012, grossFuelBenefit, 1, None, None, None, None, None, None, None, carBenefitStartedThisYear.car, Map(), Map()))
        Some(benefitsSentToPaye.head) shouldBe expectedFuelBenefit

        status(result) shouldBe 200
        val doc = Jsoup.parse(contentAsString(result))
        doc.select("#old-tax-code").text shouldBe "oldTaxCode"
        doc.select("#new-tax-code").text shouldBe "newTaxCode"
        doc.select("#personal-allowance").text shouldBe "£5"
        doc.select("#start-date").text shouldBe "6 Apr 2012"
        doc.select("#end-date").text shouldBe "5 Apr 2013"
      }
    }

    "show an error if the keystore data cannot be found" in new TestCaseIn2012 {
      setupMocksForJohnDensmore(benefits = Seq.empty, taxCodes = Seq(TaxCode(employmentSeqNumberOne, Some(1), testTaxYear, "oldTaxCode", List.empty)))
      when(mockKeyStoreService.getEntry[FuelBenefitDataWithGrossBenefit](generateKeystoreActionId(testTaxYear, employmentSeqNumberOne), "paye", "AddFuelBenefitForm", false)).thenReturn(None)

      val actualMessage = (evaluating {
        await(controller.confirmAddFuelBenefitAction(johnDensmore, requestWithCorrectVersion, testTaxYear, employmentSeqNumberOne))
      } should produce[IllegalStateException]).getMessage

      actualMessage shouldBe "No value was returned from the keystore for AddFuelBenefit:jdensmore:2012:1"

    }

    "show an error if the user does not have a car benefit" in new TestCaseIn2012 {
      setupMocksForJohnDensmore(benefits = Seq.empty)
      val fuelBenefitData = FuelBenefitData(Some("true"), None)
      when(mockKeyStoreService.getEntry[FuelBenefitDataWithGrossBenefit](generateKeystoreActionId(testTaxYear, employmentSeqNumberOne), "paye", "AddFuelBenefitForm", false)).thenReturn(Some((fuelBenefitData, 10)))

      val addBenefitResponse = AddBenefitResponse(TransactionId("anOid"), Some("newTaxCode"), Some(5))
      when(mockPayeConnector.addBenefits(Matchers.eq("/paye/AB123456C/benefits/2012"), Matchers.eq(johnDensmore.getPaye.version), Matchers.eq(employmentSeqNumberOne), Matchers.any(classOf[Seq[Benefit]]))(Matchers.any())).thenReturn(Some(addBenefitResponse))

      val actualMessage = (evaluating {
        await(controller.confirmAddFuelBenefitAction(johnDensmore, requestWithCorrectVersion, testTaxYear, employmentSeqNumberOne))
      } should produce[StaleHodDataException]).getMessage

      actualMessage shouldBe "No Car benefit found!"

    }

    "propagates any exceptions thrown by the paye microservice" in new TestCaseIn2012 {
      val carBenefitStartedThisYear = Benefit(31, testTaxYear, 321.42, 1, None, None, None, None, None, None, None,
        Some(Car(Some(new LocalDate(testTaxYear, 5, 12)), None, Some(new LocalDate(testTaxYear - 1, 12, 12)), Some(0), Some("diesel"), Some(124), Some(1400), None, Some(BigDecimal("12343.21")), None, None)), actions("AB123456C", testTaxYear, 1), Map.empty)
      setupMocksForJohnDensmore(benefits = Seq(carBenefitStartedThisYear), taxCodes = Seq(TaxCode(employmentSeqNumberOne, Some(1), testTaxYear, "oldTaxCode", List.empty)))
      val fuelBenefitData = FuelBenefitData(Some("true"), None)
      when(mockKeyStoreService.getEntry[FuelBenefitDataWithGrossBenefit](generateKeystoreActionId(testTaxYear, employmentSeqNumberOne), "paye", "AddFuelBenefitForm", false)).thenReturn(Some((fuelBenefitData, 10)))
      val addBenefitResponse = AddBenefitResponse(TransactionId("anOid"), Some("newTaxCode"), Some(5))

      when(mockPayeConnector.addBenefits(Matchers.eq("/paye/AB123456C/benefits/2012"), Matchers.eq(johnDensmore.getPaye.version), Matchers.eq(employmentSeqNumberOne), Matchers.any(classOf[Seq[Benefit]]))(Matchers.any())).thenThrow(new RuntimeException("Timeout!"))

      val actualMessage = (evaluating {
        await(controller.confirmAddFuelBenefitAction(johnDensmore, requestWithCorrectVersion, testTaxYear, employmentSeqNumberOne))
      } should produce[RuntimeException]).getMessage

      actualMessage shouldBe "Timeout!"
    }
  }

  private def generateKeystoreActionId(taxYear: Int, employmentSequenceNumber: Int) = {
    s"AddFuelBenefit:$taxYear:$employmentSequenceNumber"
  }

  private def newRequestForSaveAddFuelBenefit(employerPayFuelVal: Option[String] = None, dateFuelWithdrawnVal: Option[(String, String, String)] = None, path: String = "") =
    FakeRequest("GET", path).withFormUrlEncodedBody(Seq(employerPayFuel -> employerPayFuelVal.getOrElse("")) ++ buildDateFormField(dateFuelWithdrawn, dateFuelWithdrawnVal): _*).
      withSession((BenefitFlowHelper.npsVersionKey, johnDensmoreVersionNumber.toString))

  private def haveStatus(expectedStatus: Int) = new Matcher[Future[SimpleResult]] {
    def apply(response: Future[SimpleResult]) = {
      val actualStatus = status(response)
      MatchResult(actualStatus == expectedStatus, s"Expected result with status $expectedStatus but was $actualStatus.", s"Expected result with status other than $expectedStatus, but was actually $actualStatus.")
    }
  }
}

class TestCase(protected val taxYear: Int = 2012) extends WithApplication(FakeApplication()) with PayeBaseSpec {

  override lazy val testTaxYear = taxYear

  val mockPayeConnector = mock[PayeConnector]
  val mockTxQueueConnector = mock[TxQueueConnector]
  val mockAuthConnector = mock[AuthConnector]
  val mockAuditConnector = mock[AuditConnector]
  val mockKeyStoreService = mock[KeyStoreConnector]

  def verifyNoSaveToKeyStore() {
    verify(mockKeyStoreService, never()).addKeyStoreEntry(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())
  }

  def setupMocksForJohnDensmore(taxCodes: Seq[TaxCode] = johnDensmoresTaxCodes, employments: Seq[Employment] = johnDensmoresEmployments, benefits: Seq[Benefit] = Seq.empty) {

    implicit val hc = HeaderCarrier()
    when(mockPayeConnector.linkedResource[Seq[TaxCode]](s"/paye/AB123456C/tax-codes/$taxYear")).thenReturn(Some(taxCodes))
    when(mockPayeConnector.linkedResource[Seq[Employment]](s"/paye/AB123456C/employments/$taxYear")).thenReturn(Some(employments))
    when(mockPayeConnector.linkedResource[Seq[Benefit]](s"/paye/AB123456C/benefits/$taxYear")).thenReturn(Some(benefits))
  }

  def setupCalculationMock(calculationResult: Int) = {
    val benefitCalculationResponse = NewBenefitCalculationResponse(None, Some(calculationResult))
    when(mockPayeConnector.calculateBenefitValue(Matchers.any(), Matchers.any())(Matchers.any())).thenReturn(Some(benefitCalculationResponse))
  }
}

class TestCaseIn2012 extends TestCase {
  lazy val controller = new AddFuelBenefitController(mockKeyStoreService, mockAuditConnector, mockAuthConnector)(mockPayeConnector, mockTxQueueConnector) with MockedTaxYearSupport {
    override def currentTaxYear = taxYear
  }
}

class TestCaseIn2013 extends TestCase(2013) {
  lazy val controller = new AddFuelBenefitController(mockKeyStoreService, mockAuditConnector, mockAuthConnector)(mockPayeConnector, mockTxQueueConnector) with MockedTaxYearSupport
}
