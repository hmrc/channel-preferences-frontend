package uk.gov.hmrc.common.microservice.paye

import org.scalatest.mock.MockitoSugar
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.libs.json.JsValue
import org.joda.time.LocalDate
import play.api.test.WithApplication
import org.mockito.ArgumentCaptor
import controllers.common.domain.Transform
import uk.gov.hmrc.common.BaseSpec
import uk.gov.hmrc.common.microservice.paye.domain._
import uk.gov.hmrc.common.microservice.paye.domain.NewBenefitCalculationResponse
import uk.gov.hmrc.common.microservice.paye.domain.Car
import play.api.test.FakeApplication
import uk.gov.hmrc.common.microservice.paye.domain.AddBenefitResponse
import org.joda.time.chrono.ISOChronology
import controllers.common.actions.HeaderCarrier
import org.scalatest.concurrent.ScalaFutures
import scala.concurrent.Future
import scala.Some

class PayeConnectorSpec extends BaseSpec with ScalaFutures {

  val carBenefit = Benefit(31, 2013, 321.42, 2, None, None, None, None, None, None, None,
    Some(Car(None, Some(new LocalDate(2012, 6, 1)), Some(new LocalDate(2012, 12, 12)), Some(0), Some("diesel"), Some(124), Some(1400), Some("A"), Some(BigDecimal("12343.21")), None, None)),
    actions("AB123456C", 2013, 1), Map("withdraw" -> "someUrl/{withdrawDate}"))

  "Addition of new benefits" should {

    "delegate correctly to the paye service" in new WithApplication(FakeApplication()) {
      val service = new HttpMockedPayeConnector
      val uri = ""
      val version = 0
      val employmentSeqNumber = 1
      val benefit = Benefit(benefitType = 31, taxYear = 2013, grossAmount = 0, employmentSequenceNumber = 1, costAmount = None, amountMadeGood = None, cashEquivalent = None,
        expensesIncurred = None, amountOfRelief = None, paymentOrBenefitDescription = None, dateWithdrawn = None, car = None, actions = Map.empty[String, String], calculations = Map.empty[String, String])

      val capturedBody = ArgumentCaptor.forClass(classOf[JsValue])

      when(service.httpWrapper.postF[AddBenefitResponse](Matchers.eq(uri), capturedBody.capture, Matchers.any())).
        thenReturn(Some(AddBenefitResponse(TransactionId("24242t"), Some("456TR"), Some(12345))))
      val response = service.addBenefits(uri, version, employmentSeqNumber, Seq(benefit))
      response.get.newTaxCode shouldBe Some("456TR")
      response.get.netCodedAllowance shouldBe Some(12345)

      val capturedAddedBenefit = Transform.fromResponse[AddBenefit](capturedBody.getValue.toString())
      capturedAddedBenefit should have(
        'version(version),
        'employmentSequence(employmentSeqNumber),
        'benefits(Seq(benefit))
      )
    }
  }

  def localDate(year: Int, month: Int, day: Int) = new LocalDate(year, month, day, ISOChronology.getInstanceUTC)

  "Calculation of benefit addition" should {

    "delegate correctly to the paye service" in new WithApplication(FakeApplication()) {
      val service = new HttpMockedPayeConnector
      val uri: String = "/paye/AB123456C/benefits/2013/1/add"

      val car = Car(dateCarMadeAvailable = Some(localDate(2013, 7, 1)), dateCarWithdrawn = Some(localDate(2014, 2, 1)), dateCarRegistered = Some(localDate(1988, 1, 1)),
        employeeCapitalContribution = Some(9000), fuelType = Some("diesel"), co2Emissions = Some(200), engineSize = Some(1200),
        mileageBand = None, carValue = Some(25000), employeePayments = Some(250), daysUnavailable = None)

      val carBenefit = Benefit(benefitType = BenefitTypes.CAR, taxYear = 2013,
        grossAmount = 0, employmentSequenceNumber = 1, costAmount = Some(25000),
        amountMadeGood = None, cashEquivalent = None, expensesIncurred = None,
        amountOfRelief = None, paymentOrBenefitDescription = None,
        dateWithdrawn = Some(localDate(2014, 2, 1)), car = Some(car),
        actions = Map.empty, calculations = Map.empty)
      val fuelBenefit = Benefit(benefitType = BenefitTypes.FUEL, taxYear = 2013,
        grossAmount = 0, employmentSequenceNumber = 1, costAmount = None,
        amountMadeGood = None, cashEquivalent = None, expensesIncurred = None,
        amountOfRelief = None, paymentOrBenefitDescription = None,
        dateWithdrawn = None, car = None,
        actions = Map.empty, calculations = Map.empty)
      val carAndFuel = CarAndFuel(carBenefit, Some(fuelBenefit))

      when(service.httpWrapper.postF[NewBenefitCalculationResponse](Matchers.eq(uri), Matchers.any[JsValue], Matchers.any[Map[String, String]])).
        thenReturn(Future.successful(Some(NewBenefitCalculationResponse(Some(123), Some(456), Some(1234), Some(3456)))))

      val response = service.calculateBenefitValue(uri, carAndFuel)

      val capturedBody = ArgumentCaptor.forClass(classOf[JsValue])
      verify(service.httpWrapper).postF(Matchers.eq(uri), capturedBody.capture, Matchers.any[Map[String, String]])

      val capturedAddedBenefit = Transform.fromResponse[CarAndFuel](capturedBody.getValue.toString())
      capturedAddedBenefit shouldBe carAndFuel

      response.get.carBenefitValue shouldBe Some(123)
      response.get.fuelBenefitValue shouldBe Some(456)
      response.get.carBenefitForecastValue shouldBe Some(1234)
      response.get.fuelBenefitForecastValue shouldBe Some(3456)
    }
  }

  private def actions(nino: String, year: Int, esn: Int): Map[String, String] = {
    Map("removeCar" -> s"/paye/$nino/benefits/$year/$esn/remove/")
  }

}

class HttpMockedPayeConnector extends PayeConnector with MockitoSugar {

  val httpWrapper = mock[HttpWrapper]

  override def httpGetF[A](uri: String)(implicit m: Manifest[A], hc: HeaderCarrier): Future[Option[A]] = httpWrapper.getF[A](uri)

  override def httpPostF[A](uri: String, body: JsValue, headers: Map[String, String])(implicit m: Manifest[A], headerCarrier: HeaderCarrier): Future[Option[A]] =
    httpWrapper.postF[A](uri, body, headers)

  class HttpWrapper {
    def getF[T](uri: String): Future[Option[T]] = Future.successful(None)

    def postF[T](uri: String, body: JsValue, headers: Map[String, String]): Future[Option[T]] = Future.successful(None)
  }

}

