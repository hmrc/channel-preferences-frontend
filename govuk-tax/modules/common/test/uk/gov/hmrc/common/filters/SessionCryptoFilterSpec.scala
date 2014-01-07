package uk.gov.hmrc.common.filters

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import play.api.mvc.{AnyContentAsEmpty, SimpleResult, RequestHeader}
import scala.concurrent.Future
import play.api.test.{WithApplication, FakeApplication, FakeHeaders, FakeRequest}
import org.mockito.Mockito._
import org.mockito.Matchers._
import controllers.common.CookieEncryption

class SessionCryptoFilterSpec extends BaseSpec with MockitoSugar with CookieEncryption {

  private abstract class Setup extends WithApplication(FakeApplication()) {
    val action = mock[(RequestHeader) => Future[SimpleResult]]
    val incomingRequest = FakeRequest()
    val outgoingResponse = Future.successful(mock[SimpleResult])
  }


  "The filter" should {

    "do nothing with the session if it is empty" in  new Setup {
      when(action.apply(any())).thenReturn(outgoingResponse)

      SessionCryptoFilter(action)(incomingRequest) should be (outgoingResponse)

      verify(action).apply(incomingRequest)
    }

    "decrypt all keys and values in the session" in {
      pending
    }

    "discard the session if any entry cannot be decrypted" in {
      pending
    }
  }

}
