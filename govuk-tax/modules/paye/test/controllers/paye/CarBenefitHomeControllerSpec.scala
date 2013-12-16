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
        employmentSequenceNumber = employmentSeqNumber, currentTaxYear = testTaxYear, employmentViews = Seq.empty, previousCarBenefits = Seq.empty, carGrossAmount, fuelGrossAmount)

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
      val cars: Seq[CarAndFuel] = Seq(CarAndFuel(carBenefit))
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
        'totalCarBenefitAmount(BigDecimal(321.42)),
        'totalFuelBenefitAmount(BigDecimal(0))
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
      val cars: Seq[CarAndFuel] = Seq(CarAndFuel(carBenefit))
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

      val cars: Seq[CarAndFuel] = Seq(CarAndFuel(carBenefit, Some(fuelBenefit)),
                                      CarAndFuel(carBenefit2, Some(fuelBenefit2)),
                                      CarAndFuel(carBenefit3, Some(fuelBenefit3)),
                                      CarAndFuel(carBenefit4, None))

      val benefitDetails = RawTaxData(testTaxYear, cars, johnDensmoresEmployments, Seq(), Seq())

      val actualHomePageParams = controller.buildHomePageParams(benefitDetails, benefitTypes, testTaxYear)

      actualHomePageParams shouldNot be(None)

      actualHomePageParams.get should have(
        'totalCarBenefitAmount(BigDecimal(600)),
        'totalFuelBenefitAmount(BigDecimal(55.21))
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
    def cars: Seq[CarAndFuel] = Seq(CarAndFuel(carBenefit))
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
      override val cars = Seq(CarAndFuel(carBenefit), CarAndFuel(carBenefit))

      val response = controller.carBenefitHomeAction(rawTaxData)

      status(response) shouldBe 303
      redirectLocation(response).get shouldBe routes.CarBenefitHomeController.cannotPlayInBeta.url
    }
  }

  "GetGrossAmounts" should {
    "return the car gross amount and 0 as the fuel gross amount when there are no fuel benefits" in  new WithApplication(FakeApplication()) with BaseData  {
      override val cars = Seq(CarAndFuel(carBenefit), CarAndFuel(carBenefit))
      val actualGrossAmounts = controller.getGrossAmounts(cars)
      actualGrossAmounts._1 shouldBe carGrossAmount
      actualGrossAmounts._2 shouldBe 0
    }

    "return the car amd fuel gross amounts when there is a car and a fuel benefit" in  new WithApplication(FakeApplication()) with BaseData  {
      override val cars = Seq(CarAndFuel(carBenefit), CarAndFuel(carBenefit, Some(fuelBenefitEmployer1)))
      val actualGrossAmounts = controller.getGrossAmounts(cars)
      actualGrossAmounts._1 shouldBe carGrossAmount
      actualGrossAmounts._2 shouldBe fuelGrossAmount
    }

    "return 0 for car and fuel gross amounts when there are no car and fuel benefits" in  new WithApplication(FakeApplication()) with BaseData {
      override val cars = Seq()
      val actualGrossAmounts = controller.getGrossAmounts(cars)
      actualGrossAmounts._1 shouldBe 0
      actualGrossAmounts._2 shouldBe 0
    }

    "return the car and fuel gross amounts when there are multiple cars with and without fuel" in  new WithApplication(FakeApplication()) with BaseData {
      override val cars = Seq(CarAndFuel(carBenefit), CarAndFuel(carBenefit, Some(fuelBenefitEmployer1)), CarAndFuel(carBenefit))
      val actualGrossAmounts = controller.getGrossAmounts(cars)
      actualGrossAmounts._1 shouldBe carGrossAmount
      actualGrossAmounts._2 shouldBe fuelGrossAmount
    }
  }
}