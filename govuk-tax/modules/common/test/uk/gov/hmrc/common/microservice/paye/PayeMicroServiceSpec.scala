package uk.gov.hmrc.microservice.paye

import org.scalatest.mock.MockitoSugar
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.libs.json.JsValue
import uk.gov.hmrc.microservice.paye.domain.{ TransactionId, RemoveBenefit, Car, Benefit }
import org.joda.time.LocalDate
import play.api.test.{ FakeApplication, WithApplication }
import org.mockito.ArgumentCaptor
import controllers.common.domain.Transform
import uk.gov.hmrc.common.BaseSpec

class PayeMicroServiceSpec extends BaseSpec {

  val carBenefit = Benefit(benefitType = 31, taxYear = 2013, grossAmount = 321.42, employmentSequenceNumber = 2, null, null, null, null, null, null,
    car = Some(Car(None, Some(new LocalDate(2012, 6, 1)), Some(new LocalDate(2012, 12, 12)), 0, 2, 124, 1, "B", BigDecimal("12343.21"))),
    actions("AB123456C", 2013, 1), Map("withdraw" -> "someUrl/{withdrawDate}"))

  "Remove a benefit" should {

    "forward the version as a Version header" in new WithApplication(FakeApplication()) {

      val service = new HttpMockedPayeMicroService

      val headers: Map[String, String] = Map("Version" -> "22")
      val dateCarWithdrawn = new LocalDate(2013, 7, 18)
      val version = 22
      val grossAmount = BigDecimal(123.45)
      service.removeBenefits("/paye/AB123456C/benefits/2013/1/update/cars", "AB123456C", version, Seq(carBenefit), dateCarWithdrawn, grossAmount)

      val capturedBody = ArgumentCaptor.forClass(classOf[JsValue])
      verify(service.httpWrapper, times(1)).post(any[String], capturedBody.capture, any[Map[String, String]])

      val capturedRemovedCarBenefit = Transform.fromResponse[RemoveBenefit](capturedBody.getValue.toString())
      capturedRemovedCarBenefit.revisedAmount shouldBe grossAmount
      capturedRemovedCarBenefit.withdrawDate shouldBe dateCarWithdrawn
      capturedRemovedCarBenefit.version shouldBe version
    }

  }

  "Calculations" should {

    "return a value when a request for to Calculate Withdraw Benefit is made" in new WithApplication(FakeApplication()) {
      val service = new HttpMockedPayeMicroService

      val stubbedCalculationResult = Option(new CalculationResult(Map("2013" -> BigDecimal(1234.56), "2014" -> BigDecimal(0))))
      when(service.httpWrapper.get[CalculationResult]("someUrl/2013-07-18")).thenReturn(stubbedCalculationResult)

      val calculationResult = service.calculateWithdrawBenefit(carBenefit, new LocalDate(2013, 7, 18))
      calculationResult.result.get("2013") shouldBe Some(BigDecimal(1234.56))
      calculationResult.result.get("2014") shouldBe Some(BigDecimal(0))
    }
  }

  private def actions(nino: String, year: Int, esn: Int): Map[String, String] = {
    Map("removeCar" -> s"/paye/$nino/benefits/$year/$esn/update/cars")
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

