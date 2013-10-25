package uk.gov.hmrc.common.microservice.paye

import org.scalatest.mock.MockitoSugar
import org.mockito.Matchers._
import org.mockito.Matchers
import org.mockito.Mockito._
import play.api.libs.json.JsValue
import uk.gov.hmrc.common.microservice.paye.domain._
import org.joda.time.LocalDate
import play.api.test.WithApplication
import org.mockito.ArgumentCaptor
import controllers.common.domain.Transform
import uk.gov.hmrc.common.BaseSpec
import uk.gov.hmrc.common.microservice.paye.domain.Benefit
import uk.gov.hmrc.common.microservice.paye.domain.RemoveBenefitCalculationResponse
import scala.Some
import uk.gov.hmrc.common.microservice.paye.domain.RevisedBenefit
import uk.gov.hmrc.common.microservice.paye.domain.RemoveBenefit
import uk.gov.hmrc.common.microservice.paye.domain.Car
import play.api.test.FakeApplication

class PayeMicroServiceSpec extends BaseSpec {

  val carBenefit = Benefit(31, 2013, 321.42, 2, null, null, null, null, null, null,
    Some(Car(None, Some(new LocalDate(2012, 6, 1)), Some(new LocalDate(2012, 12, 12)), Some(0), Some("diesel"), Some(124), Some(1400), Some("A"), Some(BigDecimal("12343.21")), None, None)),
    actions("AB123456C", 2013, 1), Map("withdraw" -> "someUrl/{withdrawDate}"))

  "Remove a benefit" should {

    "forward the version as a Version header" in new WithApplication(FakeApplication()) {

      val service = new HttpMockedPayeMicroService

      val headers: Map[String, String] = Map("Version" -> "22")
      val dateCarWithdrawn = new LocalDate(2013, 7, 18)
      val version = 22
      val grossAmount = BigDecimal(123.45)
      service.removeBenefits("/paye/AB123456C/benefits/2013/1/remove/31", "AB123456C", version, Seq(RevisedBenefit(carBenefit,grossAmount)), dateCarWithdrawn)

      val capturedBody = ArgumentCaptor.forClass(classOf[JsValue])
      verify(service.httpWrapper, times(1)).post(any[String], capturedBody.capture, any[Map[String, String]])

      val capturedRemovedCarBenefit = Transform.fromResponse[RemoveBenefit](capturedBody.getValue.toString())
      capturedRemovedCarBenefit.benefits(0).revisedAmount shouldBe grossAmount
      capturedRemovedCarBenefit.withdrawDate shouldBe dateCarWithdrawn
      capturedRemovedCarBenefit.version shouldBe version
    }

  }
  
  "Add a benefit" should {
    
    "Accept the correct payload for car and fuel benefits" in {
      val service = new HttpMockedPayeMicroService
      val uri: String = "/paye/AB123456C/benefits/2013/1/add"

      val benefitData =  NewBenefitCalculationData(carRegisteredBefore98 = false, fuelType = "diesel", co2Emission = Some(200), engineCapacity = Some(1200),
        userContributingAmount =  Some(9000), listPrice = 25000, carBenefitStartDate = Some(new LocalDate(2013, 7, 1)), carBenefitStopDate = Some(new LocalDate(2014, 2, 1)),
        numDaysCarUnavailable = None, employeePayments = Some(250), employerPayFuel = "true", fuelBenefitStopDate = None)

      when(service.httpWrapper.post[NewBenefitCalculationResponse](Matchers.eq(uri), any[JsValue], any[Map[String, String]])).thenReturn(Some(NewBenefitCalculationResponse(Some(123), Some(456))))

      val response = service.calculateBenefitValue(uri, benefitData)

      val capturedBody = ArgumentCaptor.forClass(classOf[JsValue])
      verify(service.httpWrapper).post(Matchers.eq(uri), capturedBody.capture, any[Map[String, String]])

      val capturedAddedBenefit = Transform.fromResponse[NewBenefitCalculationData](capturedBody.getValue.toString())
      capturedAddedBenefit shouldBe benefitData

      response.get.carBenefitValue shouldBe Some(123)
      response.get.fuelBenefitValue shouldBe Some(456)
    }
  }

  "Calculations" should {

    "return a value when a request for to Calculate Withdraw Benefit is made" in new WithApplication(FakeApplication()) {
      val service = new HttpMockedPayeMicroService

      val stubbedCalculationResult = Option(new RemoveBenefitCalculationResponse(Map("2013" -> BigDecimal(1234.56), "2014" -> BigDecimal(0))))
      when(service.httpWrapper.get[RemoveBenefitCalculationResponse]("someUrl/2013-07-18")).thenReturn(stubbedCalculationResult)

      val calculationResult = service.calculateWithdrawBenefit(carBenefit, new LocalDate(2013, 7, 18))
      calculationResult.result.get("2013") shouldBe Some(BigDecimal(1234.56))
      calculationResult.result.get("2014") shouldBe Some(BigDecimal(0))
    }
  }

  private def actions(nino: String, year: Int, esn: Int): Map[String, String] = {
    Map("removeCar" -> s"/paye/$nino/benefits/$year/$esn/remove/")
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

