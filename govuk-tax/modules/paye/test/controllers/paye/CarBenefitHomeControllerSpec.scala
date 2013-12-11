package controllers.paye

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.paye.domain._
import org.mockito.Mockito._
import uk.gov.hmrc.utils.DateConverter
import controllers.DateFieldsHelper
import play.api.test.Helpers._
import org.scalautils.Fail
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import play.api.test.{WithApplication, FakeApplication}
import org.scalatest.concurrent.ScalaFutures
import models.paye.EmploymentViews
import play.api.mvc.Session

//TODO: Create a separate test case for the public method (carBenefitHome)
class CarBenefitHomeControllerSpec extends PayeBaseSpec with MockitoSugar with DateConverter with DateFieldsHelper with ScalaFutures {
  implicit val payeConnector = mock[PayeConnector]
  implicit val txMs = mock[TxQueueConnector]

  private lazy val controller = new CarBenefitHomeController(mock[AuditConnector], mock[AuthConnector]) {
    override def currentTaxYear = testTaxYear
  }

  val removeFuelLinkId = "rmFuelLink"
  val removeCarLinkId = "rmCarLink"
  val addCarLinkId = "addCarLink"
  val addFuelLinkId = "addFuelLink"
  val employmentSeqNumber = 1

  "calling buildHomePageResponse " should {
    implicit val user = johnDensmore
    "return a status 200 (OK) when HomePageParams are available" in new WithApplication(FakeApplication()) {
      val homePageParams = HomePageParams(activeCarBenefit = None, employerName = None,
        employmentSequenceNumber = employmentSeqNumber, currentTaxYear = testTaxYear, employmentViews = Seq.empty, previousCarBenefits = Seq.empty)

      val actualResponse = controller.buildHomePageResponse(Some(homePageParams))
      status(actualResponse) should be(200)
    }

    "return a status 500 (InternalServerError) when HomePageParams are not available" in new WithApplication(FakeApplication()) {
      val actualResponse = controller.buildHomePageResponse(None)
      status(actualResponse) should be(500)
    }
  }

  "calling assembleCarBenefitData" should {
    "return a populated CarBenefitDetails with values returned from Paye Connector calls" in {
      val mockPayeRoot = mock[PayeRoot]

      val employments = johnDensmoresOneEmployment()
      val carBenefit = carBenefitEmployer1
      val taxYearData = TaxYearData(Seq(CarAndFuel(carBenefit)), employments)
      val taxCodes = johnDensmoresTaxCodes
      val acceptedTransactions = Seq(removedCarTransaction)
      val completedTransactions = Seq(removedFuelTransaction)

      when(mockPayeRoot.fetchTaxYearData(testTaxYear)).thenReturn(taxYearData)
      when(mockPayeRoot.fetchRecentAcceptedTransactions).thenReturn(acceptedTransactions)
      when(mockPayeRoot.fetchRecentCompletedTransactions).thenReturn(completedTransactions)
      when(mockPayeRoot.fetchTaxCodes(testTaxYear)).thenReturn(taxCodes)
      when(mockPayeRoot.fetchEmployments(testTaxYear)).thenReturn(employments)

      val actualCarBenefitDetails = controller.assembleCarBenefitData(mockPayeRoot, testTaxYear)

      whenReady(actualCarBenefitDetails) { carBenefitData =>
        carBenefitData match {
          case Some(actualBenefitData) => {
            actualBenefitData.acceptedTransactions shouldBe acceptedTransactions
            actualBenefitData.completedTransactions shouldBe completedTransactions
            actualBenefitData.employments shouldBe employments
            actualBenefitData.taxCodes shouldBe taxCodes
            actualBenefitData.employment shouldBe employments.head
            actualBenefitData.taxYear shouldBe testTaxYear
          }
          case _ => Fail("Car Benefit Data was not as expected!")
        }
      }
    }
  }

  "calling buildHomePageParams" should {
    "return a populated HomePageParams with the expected values" in {

      val benefitTypes = Set(BenefitTypes.CAR, BenefitTypes.FUEL)

      val employments = johnDensmoresOneEmployment()
      val carBenefit = carBenefitEmployer1
      val taxYearData = TaxYearData(Seq(CarAndFuel(carBenefit)), employments)
      val taxCodes = johnDensmoresTaxCodes
      val acceptedTransactions = Seq(removedCarTransaction)
      val completedTransactions = Seq(removedFuelTransaction)

      val benefitDetails = CarBenefitDetails(employments, testTaxYear, taxCodes, acceptedTransactions,
        completedTransactions, taxYearData, employments.head)

      val actualHomePageParams = controller.buildHomePageParams(benefitDetails, benefitTypes, testTaxYear)

      actualHomePageParams should have(
        'activeCarBenefit(Some(CarAndFuel(carBenefit))),
        'currentTaxYear(testTaxYear),
        'employerName(Some("Weyland-Yutani Corp")),
        'sequenceNumber(1)
      )

      val expectedEmploymentViews = EmploymentViews.createEmploymentViews(employments, taxCodes, testTaxYear,
        benefitTypes, acceptedTransactions, completedTransactions)

      actualHomePageParams.employmentViews.head shouldBe expectedEmploymentViews.head
    }
  }

  "sessionWithNpsVersion" should {
    val stubSession: Session = Session(Map("foo" -> "bar"))

    "stash the nps version from the user on the session" in {
      val session = controller.sessionWithNpsVersion(stubSession)(johnDensmore)

      session.get("nps-version") shouldBe Some("22")
    }

    "not trash other session properties" in {
      val session = controller.sessionWithNpsVersion(stubSession)(johnDensmore)

      session.get("foo") shouldBe Some("bar")
    }
  }
}