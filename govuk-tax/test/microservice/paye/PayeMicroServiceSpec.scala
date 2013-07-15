package microservice.paye

import test.BaseSpec
import org.scalatest.mock.MockitoSugar
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.libs.json.JsValue
import microservice.paye.domain.{ Car, Benefit }
import org.joda.time.LocalDate
import play.api.test.{ FakeApplication, WithApplication }

class PayeMicroServiceSpec extends BaseSpec {

  val carBenefit = Benefit(benefitType = 31, taxYear = 2013, grossAmount = 321.42, employmentSequenceNumber = 2,
    cars = List(Car(None, Some(new LocalDate(2012, 6, 1)), Some(new LocalDate(2012, 12, 12)), 0, 2, 124, 1, "B", BigDecimal("12343.21"))),
    actions("AB123456C", 2013, 1), Map("withdraw" -> "someUrl/{withdrawDate}"))

  "Remove a benefit" should {

    "perform a post to the paye service with the correct uri" in new WithApplication(FakeApplication()) {

      val service = new HttpMockedPayeMicroService
      val uri = "/paye/AB123456C/benefits/2013/1/update/car"

      when(service.httpWrapper.post[Map[String, String]](org.mockito.Matchers.eq(uri), any[JsValue], any[Map[String, String]])).thenReturn(Some(Map("message" -> "Yeah!")))
      val result: Option[Map[String, String]] = service.removeCarBenefit("AB123456C", 22, carBenefit, new LocalDate(2013, 7, 18))

      verify(service.httpWrapper, times(1)).post(org.mockito.Matchers.eq(uri), any[JsValue], any[Map[String, String]])
      result.get("message") mustBe "Yeah!"
    }

    "forward the version as a Version header" in new WithApplication(FakeApplication()) {

      val service = new HttpMockedPayeMicroService

      val headers: Map[String, String] = Map("Version" -> "22")
      service.removeCarBenefit("AB123456C", 22, carBenefit, new LocalDate(2013, 7, 18))

      verify(service.httpWrapper, times(1)).post(any[String], any[JsValue], org.mockito.Matchers.eq(headers))

    }

    "alter correctly the benefit for the post to paye service" in new WithApplication(FakeApplication()) {
      (pending)
    }

  }

  "Calculations" should {

    "return a value when a request for to Calculate Withdraw Benefit is made" in new WithApplication(FakeApplication()) {
      val service = new HttpMockedPayeMicroService

      val stubbedCalculationResult = Option(new CalculationResult(Map("2013" -> BigDecimal(1234.56), "2014" -> BigDecimal(0))))
      when(service.httpWrapper.get[CalculationResult]("someUrl/2013-07-18")).thenReturn(stubbedCalculationResult)

      val calculationResult = service.calculateWithdrawBenefit(carBenefit, new LocalDate(2013, 7, 18))
      calculationResult.result.get("2013") mustBe Some(BigDecimal(1234.56))
      calculationResult.result.get("2014") mustBe Some(BigDecimal(0))
    }
  }

  private def actions(nino: String, year: Int, esn: Int): Map[String, String] = {
    Map("updateCar" -> s"/paye/$nino/benefits/$year/$esn/update/car")
  }

}

class HttpMockedPayeMicroService extends PayeMicroService with MockitoSugar {

  val httpWrapper = mock[HttpWrapper]

  override def httpGet[A](uri: String)(implicit m: Manifest[A]): Option[A] = httpWrapper.get[A](uri)
  override def httpPost[A](uri: String, body: JsValue, headers: Map[String, String])(implicit m: Manifest[A]): Option[A] = httpWrapper.post[A](uri, body, headers)

  class HttpWrapper {
    def get[T](uri: String): Option[T] = None
    def post[T](uri: String, body: JsValue, headers: Map[String, String]): Option[T] = None
  }
}

