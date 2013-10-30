package uk.gov.hmrc.common.microservice.ct

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import play.api.test.{FakeApplication, WithApplication}
import org.mockito.Mockito._
import uk.gov.hmrc.microservice.MicroServiceException
import play.api.libs.ws.Response
import uk.gov.hmrc.common.microservice.ct.domain.CtJsonRoot

class CtConnectorSpec extends BaseSpec {

  "Requesting the CT root" should {

    "return the root object for a successful response" in new CtConnectorApplication {

      val ctRootUri = "/ct/1234512345"
      val root = CtJsonRoot(Map("someLink" -> "somePath", "someOtherLink" -> "someOTherPath"))

      when(mockHttpClient.get[CtJsonRoot](ctRootUri)).thenReturn(Some(root))

      connector.root(ctRootUri) shouldBe root
    }

    "return a root object with an empty set of links for a 404 response" in new CtConnectorApplication {
      val ctRootUri = "/ct/55555"
      val emptyRoot = CtJsonRoot(Map.empty)

      when(mockHttpClient.get[CtJsonRoot](ctRootUri)).thenReturn(None)

      connector.root(ctRootUri) shouldBe emptyRoot
    }

    "propagate any exception that gets thrown" in new CtConnectorApplication {
      val ctRootUri = "/ct/55555"

      when(mockHttpClient.get[CtJsonRoot](ctRootUri)).thenThrow(new MicroServiceException("exception thrown by external service", mock[Response]))

      evaluating(connector.root(ctRootUri)) should produce[MicroServiceException]
    }
  }
}

abstract class CtConnectorApplication extends WithApplication(FakeApplication()) with MockitoSugar {
  
  val mockHttpClient = mock[HttpWrapper]
  
  val connector = new CtConnector {
    override def httpGet[A](uri: String)(implicit m: Manifest[A]): Option[A] = mockHttpClient.get[A](uri)
  }
  
  class HttpWrapper {
    def get[T](uri: String): Option[T] = None
  }
}
