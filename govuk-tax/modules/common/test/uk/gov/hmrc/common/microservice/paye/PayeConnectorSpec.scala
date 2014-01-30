package uk.gov.hmrc.common.microservice.paye

import org.scalatest.mock.MockitoSugar
import org.mockito.Matchers
import org.mockito.Mockito._
import org.joda.time.LocalDate
import play.api.test.WithApplication
import org.mockito.ArgumentCaptor
import uk.gov.hmrc.common.BaseSpec
import uk.gov.hmrc.common.microservice.paye.domain._
import uk.gov.hmrc.common.microservice.paye.domain.Car
import play.api.test.FakeApplication
import uk.gov.hmrc.common.microservice.paye.domain.WriteBenefitResponse
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

      val capturedBody = ArgumentCaptor.forClass(classOf[AddBenefit])

      when(service.httpWrapper.postF[WriteBenefitResponse, AddBenefit](Matchers.eq(uri), capturedBody.capture, Matchers.any())).
        thenReturn(Some(WriteBenefitResponse(TransactionId("24242t"), Some("456TR"), Some(12345))))

      val response = service.addBenefits(uri, version, employmentSeqNumber, carBenefit.toBenefits)

      response.get.taxCode shouldBe Some("456TR")
      response.get.allowance shouldBe Some(12345)

      val capturedAddBenefit: AddBenefit = capturedBody.getValue
      capturedAddBenefit shouldBe AddBenefit(version, employmentSeqNumber, carBenefit.toBenefits)

    }
  }

  "Looking up the current version" should {
    "delegate correctly to the paye service" in new WithApplication(FakeApplication()) {
      val service = new HttpMockedPayeConnector
      val uri = ""

      when(service.httpWrapper.getF[Int](uri)).
        thenReturn(Future.successful(Some(10)))
      val response = service.version(uri)
      response.futureValue shouldBe 10
    }
    "throw an exception if the paye service does not return a valid version" in new WithApplication(FakeApplication()) {
      val service = new HttpMockedPayeConnector
      val uri = ""

      when(service.httpWrapper.getF[Int](uri)).
        thenReturn(Future.successful(None))
      val response = service.version(uri)
      val ise = response.failed.futureValue
      ise shouldBe an[IllegalStateException]
      ise should have message s"Expected paye version number not found at URI '$uri'"
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

  override def httpPostF[TResult, TBody](uri: String, body: Option[TBody], headers: Map[String, String] = Map.empty)
                                        (implicit bodyManifest: Manifest[TBody], resultManifest: Manifest[TResult], headerCarrier: HeaderCarrier): Future[Option[TResult]] =
    body.map(body => httpWrapper.postF[TResult, TBody](uri, body, headers)).get

  class HttpWrapper {
    def getF[T](uri: String): Future[Option[T]] = Future.successful(None)

    def postF[TResult, TBody](uri: String, body: TBody, headers: Map[String, String] = Map.empty): Future[Option[TResult]] = Future.successful(None)
  }

}

