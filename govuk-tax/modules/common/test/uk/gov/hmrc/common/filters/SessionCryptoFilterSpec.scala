package uk.gov.hmrc.common.filters

import uk.gov.hmrc.common.BaseSpec
import org.scalatest.mock.MockitoSugar
import play.api.mvc.{Session, Cookie, SimpleResult, RequestHeader}
import scala.concurrent.Future
import play.api.test.{WithApplication, FakeApplication, FakeRequest}
import org.mockito.Mockito._
import org.mockito.Matchers._
import controllers.common.CookieCrypto
import org.mockito.ArgumentCaptor
import org.scalatest.OptionValues

class SessionCryptoFilterSpec extends BaseSpec with MockitoSugar with CookieCrypto with OptionValues {

  private trait Setup {

    val filter = new SessionCryptoFilter {

      override def decrypt(id: String): String = ???

      override def decrypt(id: Option[String]): Option[String] = ???

      override def encrypt(id: String): String = ???
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

    "decrypt the session" in new WithApplication(FakeApplication()) {
      new Setup {
        val encryptedCookie = Cookie(name = Session.COOKIE_NAME, value = "encryptedValue")
        override val incomingRequest = FakeRequest().withCookies(encryptedCookie)
        override val filter = new SessionCryptoFilter {
          def decrypt(id: Option[String]): Option[String] = ???

          def decrypt(id: String): String = id match {
            case "encryptedValue" => "decryptedValue"
            case somethingElse => fail(s"Unexpectedly tried to decrypt $somethingElse")
          }

          def encrypt(id: String): String = ???
        }
        filter(action)(incomingRequest)

        val updatedRequest = ArgumentCaptor.forClass(classOf[RequestHeader])
        verify(action).apply(updatedRequest.capture())
        updatedRequest.getValue.cookies.get(Session.COOKIE_NAME).value should be (encryptedCookie.copy(value = "decryptedValue"))
      }
    }

    "decrypt even an empty session" in new WithApplication(FakeApplication()) {
      new Setup {
        override val incomingRequest = FakeRequest().withSession()
        when(action.apply(any())).thenReturn(outgoingResponse)
        filter(action)(incomingRequest) should be(outgoingResponse)
        verify(action).apply(incomingRequest)
      }
    }

    "discard the session if it contains anything other than the encrypted entry" in new WithApplication(FakeApplication()) {
      new Setup {
        pending
      }
    }

    "discard the session if it cannot be decrypted" in new WithApplication(FakeApplication()) {
      new Setup {
        pending
      }
    }

    "do nothing with the session result if it is empty" in new WithApplication(FakeApplication()) {
      new Setup {
        pending
      }
    }

    "replace the values in the resulting session with a single encrypted value" in new WithApplication(FakeApplication()) {
      new Setup {
        pending
      }
    }
  }
}
