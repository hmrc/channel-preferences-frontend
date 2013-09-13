package uk.gov.hmrc.common.microservice.sa

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.microservice.sa.SaMicroService
import play.api.test.WithApplication
import uk.gov.hmrc.microservice.sa.domain._
import org.mockito.Mockito._
import org.mockito.Matchers
import uk.gov.hmrc.common.BaseSpec
import uk.gov.hmrc.microservice.sa.domain.SaAccountSummary
import uk.gov.hmrc.microservice.sa.domain.SaPerson
import play.api.test.FakeApplication
import uk.gov.hmrc.microservice.sa.domain.SaRoot
import uk.gov.hmrc.microservice.sa.domain.SaIndividualAddress
import scala.Some

class SaMicroServiceTest extends BaseSpec {

  "SaMicroServiceTest root service" should {

    "call the microservice with the correct uri and get the contents " in new WithApplication(FakeApplication()) {

      val service = new HttpMockedSaMicroService
      val saRoot = SaRoot("12345", Map.empty)
      when(service.httpWrapper.get[SaRoot]("/sa/individual/12345")).thenReturn(Some(saRoot))

      val result = service.root("/sa/individual/12345")

      result shouldBe saRoot
      verify(service.httpWrapper).get[SaRoot](Matchers.eq("/sa/individual/12345"))

    }

    "call the microservice with the correct uri but SA root is not found" in new WithApplication(FakeApplication()) {

      val service = new HttpMockedSaMicroService
      when(service.httpWrapper.get[SaRoot]("/sa/individual/12345")).thenReturn(None)

      evaluating(service.root("/sa/individual/12345")) should produce[IllegalStateException]
      verify(service.httpWrapper).get[SaRoot](Matchers.eq("/sa/individual/12345"))

    }

  }

  "SaMicroServiceTest person service" should {

    "call the microservice with the correct uri and get the contents" in new WithApplication(FakeApplication()) {

      val service = new HttpMockedSaMicroService
      val saPerson = Some(SaPerson("tim", "12345", SaIndividualAddress("line1", "line2", "line3", "line4", "line5", "46353", "Malta", "")))
      when(service.httpWrapper.get[SaPerson]("/sa/individual/12345/address")).thenReturn(saPerson)

      val result = service.person("/sa/individual/12345/address")

      result shouldBe saPerson
      verify(service.httpWrapper).get[SaPerson](Matchers.eq("/sa/individual/12345/address"))

    }
  }

  "SaMicroServiceTest account summary service" should {

    "call the microservice with the correct uri and get the contents" in new WithApplication(FakeApplication()) {

      val service = new HttpMockedSaMicroService
      val saAccountSummary = Some(SaAccountSummary(Some(AmountDue(BigDecimal(1367.29), true)), None, Some(BigDecimal(34.03))))
      when(service.httpWrapper.get[SaAccountSummary]("/sa/individual/12345/accountSummary")).thenReturn(saAccountSummary)

      val result = service.accountSummary("/sa/individual/12345/accountSummary")

      result shouldBe saAccountSummary
      verify(service.httpWrapper).get[SaAccountSummary](Matchers.eq("/sa/individual/12345/accountSummary"))

    }

  }

}

class HttpMockedSaMicroService extends SaMicroService with MockitoSugar {

  val httpWrapper = mock[HttpWrapper]

  override def httpGet[A](uri: String)(implicit m: Manifest[A]): Option[A] = httpWrapper.get[A](uri)

  class HttpWrapper {
    def get[T](uri: String): Option[T] = None
  }

}
