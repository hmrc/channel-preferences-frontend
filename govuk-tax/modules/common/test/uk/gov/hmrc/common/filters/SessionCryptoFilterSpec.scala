package uk.gov.hmrc.common.filters

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import play.api.mvc.{SimpleResult, RequestHeader}
import scala.concurrent.Future
import play.api.test.{WithApplication, FakeApplication, FakeRequest}
import org.mockito.Mockito._
import org.mockito.Matchers._
import controllers.common.CookieCrypto

class SessionCryptoFilterSpec extends BaseSpec with MockitoSugar with CookieCrypto {

  private trait Setup {
    val filter = new SessionCryptoFilter {
      def decrypt(id: String): String = ???

      def decrypt(id: Option[String]): Option[String] = ???

      def encrypt(id: String): String = ???
    }
    val action = mock[(RequestHeader) => Future[SimpleResult]]
    val incomingRequest = FakeRequest()
    val outgoingResponse = Future.successful(mock[SimpleResult])
  }

  "The filter" should {

    "do nothing with the session if it is missing" in new WithApplication(FakeApplication()) {
      new Setup {
        when(action.apply(any())).thenReturn(outgoingResponse)
        filter(action)(incomingRequest) should be(outgoingResponse)
        verify(action).apply(incomingRequest)
      }
    }

    "do nothing with the session if it is empty" in new WithApplication(FakeApplication()) {
      new Setup {
        override val incomingRequest = FakeRequest().withSession()
        when(action.apply(any())).thenReturn(outgoingResponse)
        filter(action)(incomingRequest) should be(outgoingResponse)
        verify(action).apply(incomingRequest)
      }
    }

    "decrypt all keys and values in the session before passing them on to the next filter" in new WithApplication(FakeApplication()) {
      new Setup {
        override val incomingRequest = FakeRequest().withSession("encryptedKey" -> "encryptedValue")
        override val filter = new SessionCryptoFilter {
          def decrypt(id: Option[String]): Option[String] = ???

          def decrypt(id: String): String = id match {
            case "encryptedKey" => "decryptedKey"
            case "encryptedValue" => "decryptedValue"
            case somethingElse => fail(s"Unexpectedly tried to decrypt $somethingElse")
          }

          def encrypt(id: String): String = ???
        }
        filter(action)(incomingRequest)
        verify(action).apply(incomingRequest.withSession("decryptedKey" -> "decryptedValue"))
      }
    }

    "discard the session if any entry cannot be decrypted" in {
      pending
    }
  }

}
