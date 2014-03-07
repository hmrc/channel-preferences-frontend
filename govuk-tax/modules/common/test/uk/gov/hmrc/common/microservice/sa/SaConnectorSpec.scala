package uk.gov.hmrc.common.microservice.sa

import org.scalatest.mock.MockitoSugar
import play.api.test.WithApplication
import org.mockito.Mockito._
import org.mockito.Matchers
import uk.gov.hmrc.common.{MockGet, BaseSpec}
import uk.gov.hmrc.common.microservice.sa.domain._
import uk.gov.hmrc.common.microservice.ct.domain.CtJsonRoot
import play.api.libs.ws.Response
import uk.gov.hmrc.common.microservice.sa.domain.SaName
import uk.gov.hmrc.common.microservice.sa.domain.SaPerson
import uk.gov.hmrc.common.microservice.sa.domain.SaJsonRoot
import play.api.test.FakeApplication
import uk.gov.hmrc.common.microservice.sa.domain.SaIndividualAddress
import scala.Some
import uk.gov.hmrc.common.microservice.MicroServiceException
import controllers.common.actions.HeaderCarrier
import org.scalatest.concurrent.ScalaFutures

class SaConnectorSpec extends BaseSpec with ScalaFutures {

  "Requesting the SA root" should {

    "return the root object for a successful response" in new SaConnectorApplication {

      val saRoot = SaJsonRoot(Map.empty)

      when(mockHttpClient.getF[SaJsonRoot]("/sa/individual/12345")).thenReturn(Some(saRoot))

      whenReady(connector.root("/sa/individual/12345"))(_ shouldBe saRoot)
    }

    "return a root object with an empty set of links for a 404 response" in new SaConnectorApplication {

      when(mockHttpClient.getF[SaJsonRoot]("/sa/individual/1234567890")).thenReturn(None)

      whenReady(connector.root("/sa/individual/1234567890"))(_ shouldBe SaJsonRoot(Map.empty))
    }

    "Propagate any exception that gets thrown" in new SaConnectorApplication {
      val saRootUri = "/sa/55555"

      when(mockHttpClient.getF[CtJsonRoot](saRootUri)).thenThrow(new MicroServiceException("exception thrown by external service", mock[Response]))

      evaluating(connector.root(saRootUri)) should produce[MicroServiceException]
    }
  }

  "SaConnectorTest person service" should {

    "call the microservice with the correct uri and get the contents" in new SaConnectorApplication {

      val saName = SaName("Mr", "Tim", None, "Smith", None)
      val saPerson = Some(SaPerson(saName, SaIndividualAddress("line1", "line2", Some("line3"), Some("line4"), Some("line5"), Some("46353"), Some("Malta"), None)))
      when(mockHttpClient.getF[SaPerson]("/sa/individual/12345/address")).thenReturn(saPerson)

      val result = connector.person("/sa/individual/12345/address")

      result.futureValue shouldBe saPerson
      verify(mockHttpClient).getF[SaPerson](Matchers.eq("/sa/individual/12345/address"))

    }
  }

  "SaConnectorTest account summary service" should {

    "call the microservice with the correct uri and get the contents" in new SaConnectorApplication {

      val saAccountSummary = Some(SaAccountSummary(Some(AmountDue(BigDecimal(1367.29), requiresPayment = true)), None, Some(BigDecimal(34.03))))
      when(mockHttpClient.getF[SaAccountSummary]("/sa/individual/12345/accountSummary")).thenReturn(saAccountSummary)
      implicit val hc = HeaderCarrier()
      val result = connector.accountSummary("/sa/individual/12345/accountSummary")

      await(result) shouldBe saAccountSummary
      verify(mockHttpClient).getF[SaAccountSummary](Matchers.eq("/sa/individual/12345/accountSummary"))
    }
  }
}

abstract class SaConnectorApplication extends WithApplication(FakeApplication()) with MockitoSugar {
  val connector = new SaConnector with MockGet
  val mockHttpClient = connector.mockHttpClient
}