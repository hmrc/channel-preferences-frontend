package controllers.paye

import org.scalatest.mock.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import org.joda.time.{LocalDate, DateTime}
import org.joda.time.chrono.ISOChronology
import play.api.test._
import org.mockito.Mockito._
import play.api.test.Helpers._
import uk.gov.hmrc.common.microservice.paye.domain.{TaxCode, CarBenefit, Employment, CarAndFuel}
import scala.concurrent.Future
import play.api.test.FakeApplication
import scala.Some
import controllers.DateFieldsHelper
import controllers.paye.CarBenefitFormFields._
import scala.Some
import play.api.test.FakeApplication
import uk.gov.hmrc.common.microservice.paye.domain.CarAndFuel
import uk.gov.hmrc.common.microservice.paye.domain.TaxCode
import akka.actor.FSM.->


class ReplaceBenefitControllerSpec extends PayeBaseSpec with MockitoSugar with ScalaFutures with DateFieldsHelper {

  "Showing the replace cart benefit form" should {

    "redirect to carBenefitHomeController if the user does not have a car benefit" in new WithApplication(FakeApplication()) with TestCase {
      setupMocksForJohnDensmore(benefits = Seq.empty)
      val result = controller.showReplaceCarBenefitFormAction(johnDensmore, requestWithCorrectVersion, testTaxYear, 2)

      status(result) shouldBe 303
      val expectedUri = routes.CarBenefitHomeController.carBenefitHome().url
      redirectLocation(result) shouldBe Some(expectedUri)
    }

    "return replace car benefit form" in new WithApplication(FakeApplication()) with TestCase {
      setupMocksForJohnDensmore()
      val result = controller.showReplaceCarBenefitFormAction(johnDensmore, requestWithCorrectVersion, testTaxYear, 2)

      status(result) shouldBe 200
    }
  }

  "Requesting replace car " should {
    implicit val user = johnDensmore

    "return bad request if the remove car form contains errors"  in new WithApplication(FakeApplication()) with TestCase {
      implicit val request = requestBenefitReplacementFormSubmission(dateReturned = None)
      setupMocksForJohnDensmore()
      val result = controller.requestReplaceCarAction(testTaxYear, 2)
      status(result) shouldBe 400
    }

    "return bad request if the new car form contains errors"  in new WithApplication(FakeApplication()) with TestCase {
      implicit val request = requestBenefitReplacementFormSubmission(carRegistrationDateVal = None)
      setupMocksForJohnDensmore()
      val result = controller.requestReplaceCarAction(testTaxYear, 2)
      status(result) shouldBe 400
    }

    "return the confirmation page if both the forms are valid"  in new WithApplication(FakeApplication()) with TestCase {
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

    private lazy val dateToday: DateTime = new DateTime(2013, 12, 8, 12, 30, ISOChronology.getInstanceUTC)

    lazy val controller = new ReplaceBenefitController(mockKeyStoreService, mock[AuthConnector], mock[AuditConnector])(mockPayeConnector, mockTxQueueConnector) with MockedTaxYearSupport {
      override def now = () => dateToday
    }

    def setupMocksForJohnDensmore(employments: Seq[Employment] = johnDensmoresEmployments, benefits: Seq[CarBenefit] = johnDensmoresBenefits, taxCodes: Seq[TaxCode] = johnDensmoresTaxCodes) {
      when(mockPayeConnector.linkedResource[Seq[TaxCode]](s"/paye/AB123456C/tax-codes/$testTaxYear")).thenReturn(Some(taxCodes))
      when(mockPayeConnector.linkedResource[Seq[CarAndFuel]](s"/paye/AB123456C/benefit-cars/$testTaxYear")).thenReturn(Future.successful(Some(benefits.map(c => CarAndFuel(c.toBenefits(0), c.toBenefits.drop(1).headOption)))))
      when(mockPayeConnector.linkedResource[Seq[Employment]](s"/paye/AB123456C/employments/$testTaxYear")).thenReturn(Future.successful(Some(johnDensmoresEmployments)))
    }

    import CarBenefitDataBuilder._

    def requestBenefitReplacementFormSubmission(dateReturned: Option[LocalDate] = Some(new LocalDate(testTaxYear, 5, 1)), carUnavailable: String = "false", employeeContributes: String = "false", fuelGiveUp: String = "sameDateFuel",
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
                                               path: String = "")  = {

      requestWithCorrectVersion.withFormUrlEncodedBody(Seq(
        "agreement" -> "true",
        "carUnavailable" -> carUnavailable,
        "employeeContributes" -> employeeContributes,
        "fuelRadio" -> fuelGiveUp,
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
        ++ buildDateFormField("withdrawDate", Some(localDateToTuple(dateReturned)))
        ++ buildDateFormField(providedFrom, providedFromVal)
        ++ buildDateFormField(dateFuelWithdrawn, dateFuelWithdrawnVal)
        ++ buildDateFormField(carRegistrationDate, carRegistrationDateVal): _*)
    }
  }


}
