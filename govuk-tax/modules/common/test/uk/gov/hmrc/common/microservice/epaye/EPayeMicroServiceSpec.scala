package uk.gov.hmrc.common.microservice.epaye

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import play.api.test.{FakeApplication, WithApplication}
import org.mockito.Mockito._
import uk.gov.hmrc.domain.EmpRef
import uk.gov.hmrc.common.microservice.epaye.domain.EPayeDomain.{NonRTI, EPayeAccountSummary, EPayeRoot}

class EPayeMicroServiceSpec extends BaseSpec {

   private val uri = "someUri"

  "Requesting the EPaye root" should {

    "Make an HTTP call to the service and return the root data" in new WithEPayeMicroService {
      val empRef = EmpRef("ABC", "12345")
      val rootObject = EPayeRoot(empRef, Map("someLink" -> "/some/path"))

      when(mockHttpClient.get[EPayeRoot](uri)).thenReturn(Some(rootObject))
      epayeMicroService.root(uri) shouldBe rootObject
    }

    // TODO [JJS] Exception handling here is strange - can we even see what error is returned from the service?
//    "Make an HTTP call to the service and throw an IllegalStateException if an exception is thrown" in new WithEPayeMicroService {
//      when(mockHttpClient.get[EPayeRoot](uri)).thenThrow(new Exception("Malformed result"))
//      epayeMicroService.root(uri) shouldBe None
//    }

    "Make an HTTP call to the service and throw an IllegalStateException if there is no root data" in new WithEPayeMicroService {
      when(mockHttpClient.get[EPayeRoot](uri)).thenReturn(None)
      evaluating(epayeMicroService.root(uri)) should produce[IllegalStateException]
    }
  }

  "Requesting the EPaye account summary" should {

    "Return the correct response for an example with account summary information" in new WithEPayeMicroService {
      val empRef = EmpRef("ABC", "12345")
      val summary = EPayeAccountSummary(nonRti = Some(NonRTI(BigDecimal(50D), 2013)))

      when(mockHttpClient.get[EPayeAccountSummary](uri)).thenReturn(Some(summary))
      epayeMicroService.accountSummary(uri) shouldBe Some(summary)
    }

    "Return None for an example with invalid data - containing neither RTI nor Non-RTI information" in new WithEPayeMicroService {
      val empRef = EmpRef("ABC", "12345")
      val invalidSummary = EPayeAccountSummary(rti = None, nonRti = None)

      when(mockHttpClient.get[EPayeAccountSummary](uri)).thenReturn(Some(invalidSummary))
      epayeMicroService.accountSummary(uri) shouldBe None
    }

    "Return None for an example where no data is returned" in new WithEPayeMicroService {
      when(mockHttpClient.get[EPayeAccountSummary](uri)).thenReturn(None)
      epayeMicroService.accountSummary(uri) shouldBe None
    }

    // TODO [JJS] Exception handling here is strange - can we even see what error is returned from the service?
//    "Return None for an example where an exception is thrown" in new WithEPayeMicroService {
//      when(mockHttpClient.get[EPayeAccountSummary](uri)).thenThrow(new Exception("Malformed result"))
//      epayeMicroService.accountSummary(uri) shouldBe None
//    }
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
