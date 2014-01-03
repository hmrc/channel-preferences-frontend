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
      val carBenefit = CarBenefit(2013, 1, new LocalDate, new LocalDate, 0.0, 0, "Diesel", Some(1400), Some(125), 3000, 0, 0, new LocalDate)

      val capturedBody = ArgumentCaptor.forClass(classOf[JsValue])

      when(service.httpWrapper.postF[AddBenefitResponse](Matchers.eq(uri), capturedBody.capture, Matchers.any())).
        thenReturn(Some(AddBenefitResponse(TransactionId("24242t"), Some("456TR"), Some(12345))))
      val response = service.addBenefits(uri, version, employmentSeqNumber, carBenefit.toBenefits)
      response.get.newTaxCode shouldBe Some("456TR")
      response.get.netCodedAllowance shouldBe Some(12345)

      val capturedAddedBenefit = Transform.fromResponse[AddBenefit](capturedBody.getValue.toString())
      capturedAddedBenefit should have(
        'version(version),
        'employmentSequence(employmentSeqNumber),
        'benefits(carBenefit.toBenefits)
      )
    }
  }

  def localDate(year: Int, month: Int, day: Int) = new LocalDate(year, month, day, ISOChronology.getInstanceUTC)

  private def actions(nino: String, year: Int, esn: Int): Map[String, String] = {
    Map("removeCar" -> s"/paye/$nino/benefits/$year/$esn/remove/")
  }

}

class HttpMockedPayeConnector extends PayeConnector with MockitoSugar {

  val httpWrapper = mock[HttpWrapper]

  override def httpGetF[A](uri: String)(implicit m: Manifest[A], hc: HeaderCarrier): Future[Option[A]] = httpWrapper.getF[A](uri)

  override def httpPostF[A, B](uri: String, body: A, headers: Map[String, String] = Map.empty)(implicit a: Manifest[A], b: Manifest[B], headerCarrier: HeaderCarrier): Future[Option[B]] =
    httpWrapper.postF[A, B](uri, body, headers)

  class HttpWrapper {
    def getF[T](uri: String): Future[Option[T]] = Future.successful(None)
    def postF[A, B](uri: String, body: A, headers: Map[String, String] = Map.empty): Future[Option[B]] = Future.successful(None)
  }

}

