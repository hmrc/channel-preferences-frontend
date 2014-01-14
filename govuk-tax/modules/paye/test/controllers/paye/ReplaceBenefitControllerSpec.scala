package controllers.paye

import org.scalatest.mock.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import org.joda.time.{LocalDate => JLocalDate}
import play.api.test._
import org.mockito.Mockito._
import play.api.test.Helpers._
import uk.gov.hmrc.common.microservice.paye.domain.{CarBenefit, Employment}
import scala.concurrent.Future
import controllers.DateFieldsHelper
import controllers.paye.CarBenefitFormFields._
import org.mockito.Matchers._
import org.mockito.Matchers
import scala.Some
import play.api.test.FakeApplication
import uk.gov.hmrc.common.microservice.paye.domain.CarAndFuel
import uk.gov.hmrc.common.microservice.paye.domain.TaxCode
import org.jsoup.Jsoup
import models.paye.{RemoveCarBenefitFormData, ReplaceCarBenefitFormData}


class ReplaceBenefitControllerSpec extends PayeBaseSpec with MockitoSugar with ScalaFutures with DateFieldsHelper {

  "Showing the replace car benefit form" should {

    "redirect to carBenefitHomeController if the user does not have a car benefit" in new WithApplication(FakeApplication()) with TestCase {
      setupMocksForJohnDensmore(benefits = Seq.empty)
      val result = controller.showReplaceCarBenefitFormAction(testTaxYear, 2)(johnDensmore, requestWithCorrectVersion)

      status(result) shouldBe 303
      val expectedUri = routes.CarBenefitHomeController.carBenefitHome().url
      redirectLocation(result) shouldBe Some(expectedUri)
    }

    "show the replace car benefit form" in new WithApplication(FakeApplication()) with TestCase {
      setupMocksForJohnDensmore()
      val result = controller.showReplaceCarBenefitFormAction(testTaxYear, 2)(johnDensmore, requestWithCorrectVersion)

      status(result) shouldBe 200
    }

    "show the prepopulated add car benefit form if the keystore returns some data" in new WithApplication(FakeApplication()) with TestCase {
      val employmentSeqNumberOne = johnDensmoresEmployments(0).sequenceNumber
      setupMocksForJohnDensmore()
      when(mockKeyStoreService.getEntry[ReplaceCarBenefitFormData](
        Matchers.eq("ReplaceCarBenefitFormData"), Matchers.eq("paye"), Matchers.eq("replace_benefit"), Matchers.eq(false))(any(), any())).thenReturn(Some(johnDensmoresReplaceCarBenefitData))
      val result = controller.showReplaceCarBenefitFormAction(testTaxYear, 2)(johnDensmore, requestWithCorrectVersion)

      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      val addTable = doc.select("#add-car-benefit-fields")
      addTable.select("[id~=providedFrom]").select("[id~=day-29]").attr("selected") shouldBe "selected"
      addTable.select("[id~=providedFrom]").select("[id~=month-7]").attr("selected") shouldBe "selected"
      addTable.select("[id~=providedFrom]").select(s"[id~=year-$testTaxYear]").attr("selected") shouldBe "selected"
      addTable.select("#listPrice").attr("value") shouldBe "1000"
      addTable.select("#privateUsePayment-true").attr("checked") shouldBe "checked"
      addTable.select("#privateUsePaymentAmount").attr("value") shouldBe "999"
      addTable.select("[id~=carRegistrationDate]").select("[id~=day-13]").attr("selected") shouldBe "selected"
      addTable.select("[id~=carRegistrationDate]").select("[id~=month-9]").attr("selected") shouldBe "selected"
      addTable.select("[id~=carRegistrationDate]").select("[id~=year]").attr("value") shouldBe "1950"
      addTable.select("#fuelType-electricity").attr("checked") shouldBe "checked"
      addTable.select("#engineCapacity-2000").attr("checked") shouldBe "checked"
      addTable.select("#employerPayFuel-false").attr("checked") shouldBe "checked"
      addTable.select("#employeeContributes-true").attr("checked") shouldBe "checked"
      addTable.select("#employeeContribution").attr("value") shouldBe "100"
    }

    "show the prepopulated remove car benefit form if the keystore returns some data" in new WithApplication(FakeApplication()) with TestCase {
      val employmentSeqNumberOne = johnDensmoresEmployments(0).sequenceNumber
      setupMocksForJohnDensmore()
      when(mockKeyStoreService.getEntry[ReplaceCarBenefitFormData](
        Matchers.eq("ReplaceCarBenefitFormData"), Matchers.eq("paye"), Matchers.eq("replace_benefit"), Matchers.eq(false))(any(), any())).thenReturn(Some(johnDensmoresReplaceCarBenefitData))
      val result = controller.showReplaceCarBenefitFormAction(testTaxYear, 2)(johnDensmore, requestWithCorrectVersion)

      status(result) shouldBe 200
      val doc = Jsoup.parse(contentAsString(result))
      val removeTable = doc.select("#remove-car-benefit-fields")
      removeTable.select("[id~=withdrawDate]").select("[id~=day-8]").attr("selected") shouldBe "selected"
      removeTable.select("[id~=withdrawDate]").select("[id~=month-1]").attr("selected") shouldBe "selected"
      removeTable.select("[id~=withdrawDate]").select(s"[id~=year-2014]").attr("selected") shouldBe "selected"
      removeTable.select("#carUnavailable-true").attr("checked") shouldBe "checked"
      removeTable.select("#numberOfDaysUnavailable").attr("value") shouldBe "10"
      removeTable.select("#removeEmployeeContributes-true").attr("checked") shouldBe "checked"
      removeTable.select("#removeEmployeeContribution").attr("value") shouldBe "2000"
      removeTable.select("#fuelRadio-differentDateFuel").attr("checked") shouldBe "checked"
      removeTable.select("[id~=fuelWithdrawDate]").select("[id~=day-8]").attr("selected") shouldBe "selected"
      removeTable.select("[id~=fuelWithdrawDate]").select("[id~=month-2]").attr("selected") shouldBe "selected"
      removeTable.select("[id~=fuelWithdrawDate]").select("[id~=year-2013]").attr("selected") shouldBe "selected"
    }

    "Save the form values to the keystore when the next button is pressed." in new WithApplication(FakeApplication()) with TestCase {

      import Matchers.{eq => is}

      val employmentSeqNumberOne = johnDensmoresEmployments(0).sequenceNumber
      setupMocksForJohnDensmore()
      val result = controller.requestReplaceCarAction(testTaxYear, 2)(johnDensmore, requestBenefitReplacementFormSubmission(removeEmployeeContributesVal = "true", removeEmployeeContributionVal = "200", employeeContributesVal = Some("true"), employeeContributionVal = Some("500")))

      status(result) shouldBe 200

      verify(mockKeyStoreService).addKeyStoreEntry(is("ReplaceCarBenefitFormData"), is("paye"), is("replace_benefit"), is(ReplaceCarBenefitFormData(RemoveCarBenefitFormData(LocalDate(2013, 5, 1), Some(false), None, Some(true), Some(200), Some("sameDateFuel"), None),
        CarBenefitDataBuilder(employeeContributes = Some(true), employeeContribution = Some(500)))), any())(any(), any())

    }
  }

  "Requesting replace car " should {
    implicit val user = johnDensmore

    "return bad request if the remove car form contains errors" in new WithApplication(FakeApplication()) with TestCase {
      implicit val request = requestBenefitReplacementFormSubmission(dateReturnedVal = None)
      setupMocksForJohnDensmore()
      val result = controller.requestReplaceCarAction(testTaxYear, 2)
      status(result) shouldBe 400
    }

    "return bad request if the new car form contains errors" in new WithApplication(FakeApplication()) with TestCase {
      implicit val request = requestBenefitReplacementFormSubmission(carRegistrationDateVal = None)
      setupMocksForJohnDensmore()
      val result = controller.requestReplaceCarAction(testTaxYear, 2)
      status(result) shouldBe 400
    }

    "return the confirmation page if both the forms are valid" in new WithApplication(FakeApplication()) with TestCase {
      implicit val request = requestBenefitReplacementFormSubmission()
      setupMocksForJohnDensmore()
      val result = controller.requestReplaceCarAction(testTaxYear, 2)
      status(result) shouldBe 200
    }
  }

  trait TestCase {

    val mockKeyStoreService = mock[KeyStoreConnector]
    val mockPayeConnector = mock[PayeConnector]
    val mockTxQueueConnector = mock[TxQueueConnector]

    lazy val controller = new ReplaceBenefitController(mockKeyStoreService, mock[AuthConnector], mock[AuditConnector])(mockPayeConnector, mockTxQueueConnector) with StubTaxYearSupport

    def setupMocksForJohnDensmore(employments: Seq[Employment] = johnDensmoresEmployments, benefits: Seq[CarBenefit] = johnDensmoresBenefits, taxCodes: Seq[TaxCode] = johnDensmoresTaxCodes) {
      when(mockPayeConnector.linkedResource[Seq[TaxCode]](Matchers.eq(s"/paye/AB123456C/tax-codes/$testTaxYear"))(any(), any())).thenReturn(Some(taxCodes))
      when(mockPayeConnector.linkedResource[Seq[CarAndFuel]](Matchers.eq(s"/paye/AB123456C/benefit-cars/$testTaxYear"))(any(), any())).thenReturn(Future.successful(Some(benefits.map(c => CarAndFuel(c.toBenefits(0), c.toBenefits.drop(1).headOption)))))
      when(mockPayeConnector.linkedResource[Seq[Employment]](Matchers.eq(s"/paye/AB123456C/employments/$testTaxYear"))(any(), any())).thenReturn(Future.successful(Some(johnDensmoresEmployments)))

      when(mockKeyStoreService.getEntry[ReplaceCarBenefitFormData](Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(None)
      when(mockKeyStoreService.addKeyStoreEntry(Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any(), Matchers.any())(Matchers.any(), Matchers.any())).thenReturn(Future.successful(None))
    }

    import CarBenefitDataBuilder._


    def requestBenefitReplacementFormSubmission(dateReturnedVal: Option[JLocalDate] = Some(LocalDate(testTaxYear, 5, 1)),
                                                carUnavailableVal: String = "false",
                                                removeEmployeeContributesVal: String = "false",
                                                removeEmployeeContributionVal: String = "0",
                                                fuelGiveUpVal: String = "sameDateFuel",
                                                providedFromVal: Option[(String, String, String)] = Some(localDateToTuple(Some(defaultProvidedFrom))),
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
      requestWithCorrectVersion.withFormUrlEncodedBody(Seq(
        agreement -> "true",
        carUnavailable -> carUnavailableVal,
        removeEmployeeContributes -> removeEmployeeContributesVal,
        removeEmployeeContribution -> removeEmployeeContributionVal,
        fuelRadio -> fuelGiveUpVal,
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
        ++ buildDateFormField(withdrawDate, Some(localDateToTuple(dateReturnedVal)))
        ++ buildDateFormField(providedFrom, providedFromVal)
        ++ buildDateFormField(dateFuelWithdrawn, dateFuelWithdrawnVal)
        ++ buildDateFormField(carRegistrationDate, carRegistrationDateVal): _*)
    }
  }

}