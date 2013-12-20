package controllers.paye

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.paye.domain._
import uk.gov.hmrc.utils.DateConverter
import controllers.DateFieldsHelper
import play.api.test.Helpers._
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import play.api.test.WithApplication
import org.scalatest.concurrent.ScalaFutures
import models.paye.EmploymentViews
import play.api.mvc.Session
import org.joda.time.LocalDate
import play.api.test.FakeApplication
import uk.gov.hmrc.common.microservice.paye.domain.CarAndFuel

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
        employmentSequenceNumber = employmentSeqNumber, currentTaxYear = testTaxYear, employmentViews = Seq.empty,
        previousCarBenefits = Seq.empty, Some(BenefitValue(carGrossAmount)),
        Some(BenefitValue(fuelGrossAmount)))

      val actualResponse = controller.buildHomePageResponse(Some(homePageParams))
      status(actualResponse) should be(200)
    }

    "return a status 500 (InternalServerError) when HomePageParams are not available" in new WithApplication(FakeApplication()) {
      val actualResponse = controller.buildHomePageResponse(None)
      status(actualResponse) should be(500)
    }
  }

  "calling buildHomePageParams" should {
    "return a populated HomePageParams with the expected values" in {

      val benefitTypes = Set(BenefitTypes.CAR, BenefitTypes.FUEL)

      val employments = johnDensmoresEmployments
      val carBenefit = carBenefitEmployer1
      val cars: Seq[CarBenefit] = Seq(CarBenefit(carBenefit))
      val taxCodes = johnDensmoresTaxCodes
      val transactionHistory = Seq(acceptedRemovedCarTransaction, completedRemovedFuelTransaction)

      val benefitDetails = RawTaxData(testTaxYear, cars, employments, taxCodes, transactionHistory)

      val actualHomePageParams = controller.buildHomePageParams(benefitDetails, benefitTypes, testTaxYear)

      actualHomePageParams shouldNot be(None)

      actualHomePageParams.get should have(
        'activeCarBenefit(Some(CarAndFuel(carBenefit))),
        'currentTaxYear(testTaxYear),
        'employerName(Some("Weyland-Yutani Corp")),
        'employmentSequenceNumber(1),
        'carGrossAmount(Some(BenefitValue(BigDecimal(321.42)))),
        'fuelGrossAmount(None)
      )

      val expectedEmploymentViews = EmploymentViews.createEmploymentViews(employments, taxCodes, testTaxYear,
        benefitTypes, transactionHistory)

      actualHomePageParams.get.employmentViews.head shouldBe expectedEmploymentViews.head
    }

    "return none when there is no active employment" in {
      val benefitTypes = Set(BenefitTypes.CAR, BenefitTypes.FUEL)

      val employments = Seq(
        Employment(sequenceNumber = 2, startDate = new LocalDate(testTaxYear, 7, 2),
          endDate = Some(new LocalDate(testTaxYear, 10, 8)), taxDistrictNumber = "898",
          payeNumber = "9900112", employerName = Some("Weyland-Yutani Corp"), employmentType = 2))

      val carBenefit = carBenefitEmployer1
      val cars: Seq[CarBenefit] = Seq(CarBenefit(carBenefit))
      val taxCodes = johnDensmoresTaxCodes
      val transactionHistory = Seq(acceptedRemovedCarTransaction, completedRemovedFuelTransaction)

      val benefitDetails = RawTaxData(testTaxYear, cars, employments, taxCodes, transactionHistory)

      val actualHomePageParams = controller.buildHomePageParams(benefitDetails, benefitTypes, testTaxYear)

      actualHomePageParams shouldBe None
    }

    "add all car and fuel gross amounts correctly" in {

      val benefitTypes = Set(BenefitTypes.CAR, BenefitTypes.FUEL)

      val carBenefit = carBenefitEmployer1.copy(grossAmount = BigDecimal(600))
      val carBenefit2 = carBenefitEmployer1.copy(grossAmount = BigDecimal(600), car = Some(carBenefitEmployer1.car.get.copy(dateCarWithdrawn = Some(new LocalDate(2012, 10, 10)))))
      val carBenefit3 = carBenefitEmployer1.copy(grossAmount = BigDecimal(600), car = Some(carBenefitEmployer1.car.get.copy(dateCarWithdrawn = Some(new LocalDate(2012, 10, 10)))))
      val carBenefit4 = carBenefitEmployer1.copy(grossAmount = BigDecimal(600), car = Some(carBenefitEmployer1.car.get.copy(dateCarWithdrawn = Some(new LocalDate(2012, 10, 10)))))

      val fuelBenefit = fuelBenefitEmployer1.copy(grossAmount = BigDecimal(55.21))
      val fuelBenefit2 = fuelBenefitEmployer1.copy(grossAmount = BigDecimal(55.21))
      val fuelBenefit3 = fuelBenefitEmployer1.copy(grossAmount = BigDecimal(55.21))

      val cars: Seq[CarBenefit] = Seq(CarBenefit(carBenefit, Some(fuelBenefit)),
        CarBenefit(carBenefit2, Some(fuelBenefit2)),
        CarBenefit(carBenefit3, Some(fuelBenefit3)),
        CarBenefit(carBenefit4, None))

      val benefitDetails = RawTaxData(testTaxYear, cars, johnDensmoresEmployments, Seq(), Seq())

      val actualHomePageParams = controller.buildHomePageParams(benefitDetails, benefitTypes, testTaxYear)

      actualHomePageParams shouldNot be(None)

      actualHomePageParams.get should have(
        'carGrossAmount(Some(BenefitValue(BigDecimal(600)))),
        'fuelGrossAmount(Some(BenefitValue(BigDecimal(55.21))))
      )

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

  trait BaseData {
    def carBenefit = carBenefitEmployer1

    def cars: Seq[CarBenefit] = Seq(CarBenefit(carBenefit))

    def employments = johnDensmoresOneEmployment(1)

    def rawTaxData = RawTaxData(2013, cars, employments, Seq.empty, Seq.empty)

  }

  "carBenefitHomeAction" should {
    implicit val user = johnDensmore
    "return car_benefit_home view when user is accepted for beta (HMTB-2250)" in new WithApplication(FakeApplication()) with BaseData {
      val response = controller.carBenefitHomeAction(rawTaxData)

      status(response) shouldBe 200
    }

    "return failure view when user has multiple employments" in new WithApplication(FakeApplication()) with BaseData {
      override val employments = johnDensmoresEmployments

      val response = controller.carBenefitHomeAction(rawTaxData)

      status(response) shouldBe 303
      redirectLocation(response).get shouldBe routes.CarBenefitHomeController.cannotPlayInBeta.url
    }
    "return failure view when user has no employments" in new WithApplication(FakeApplication()) with BaseData {
      override val employments = Seq.empty

      val response = controller.carBenefitHomeAction(rawTaxData)

      status(response) shouldBe 303
      redirectLocation(response).get shouldBe routes.CarBenefitHomeController.cannotPlayInBeta.url
    }
    "return failure view when user has multiple active cars" in new WithApplication(FakeApplication()) with BaseData {
      override val cars = Seq(CarBenefit(carBenefit), CarBenefit(carBenefit))

      val response = controller.carBenefitHomeAction(rawTaxData)

      status(response) shouldBe 303
      redirectLocation(response).get shouldBe routes.CarBenefitHomeController.cannotPlayInBeta.url
    }
  }
}
