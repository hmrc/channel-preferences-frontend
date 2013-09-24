package uk.gov.hmrc.common.microservice.epaye

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import play.api.test.{FakeApplication, WithApplication}
import uk.gov.hmrc.common.microservice.epaye.domain.EPayeDomain.EPayeRoot
import org.mockito.Mockito._
import uk.gov.hmrc.domain.EmpRef

class EPayeMicroServiceSpec extends BaseSpec {

  object constants {
    val uri = "someUri"
    val empRef = EmpRef("ABC", "12345")
    val links = Map("someLink" -> "/some/path")
    val rootObjectWithRtiInfo = EPayeRoot(empRef, links)
  }

  "Requesting the EPaye root" should {

    "Make an HTTP call to the service and return the root data" in new WithEPayeMicroService {
      import constants._

      when(mockHttpClient.get[EPayeRoot](uri)).thenReturn(Some(rootObject))
      epayeMicroService.root(uri) shouldBe rootObject
    }

    "Make an HTTP call to the service and return the response with EPAYE Non-RTI information" in new WithEPayeMicroService {
      pending
    }

    "Make an HTTP call to the service and return the response with invalid (neither RTI nor Non-RTI) information" in new WithEPayeMicroService {
      pending
    }

    "Make an HTTP call to the service and return None if the response is None" in new WithEPayeMicroService {
      pending
    }

    "Make an HTTP call to the service and return None if a MicroServiceException is thrown" in new WithEPayeMicroService {
      pending
    }
  }

  "Requesting the EPaye account summary" should {

    ""
    "Walk the links and return the correct response for an example with EPAYE RTI information" in new WithEPayeMicroService {
      pending
    }

    "Walk the links and return the correct response for an example with EPAYE Non-RTI information" in new WithEPayeMicroService {
      pending
    }

    "Walk the links and return the correct response for an example with invalid data - containing neither RTI nor Non-RTI information" in new WithEPayeMicroService {
      pending
    }

    "Walk the links and return the correct response for an example where no data is returned (404 from back-end)" in new WithEPayeMicroService {
      pending
    }

    "Walk the links and return the correct response for an example where an exception is thrown" in new WithEPayeMicroService {
      pending
    }
  }

}

abstract class WithEPayeMicroService extends WithApplication(FakeApplication()) with MockitoSugar {

  val mockHttpClient = mock[HttpWrapper]

  val epayeMicroService = new EPayeMicroService {
    override def httpGet[A](uri: String)(implicit m: Manifest[A]): Option[A] = mockHttpClient.get[A](uri)
  }

  class HttpWrapper {
    def get[T](uri: String): Option[T] = None
  }
}
