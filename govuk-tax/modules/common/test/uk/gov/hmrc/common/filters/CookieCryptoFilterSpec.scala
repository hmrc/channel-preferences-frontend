package uk.gov.hmrc.common.filters

import uk.gov.hmrc.common.{RequestHeaderEquivalence, BaseSpec}
import org.scalatest.mock.MockitoSugar
import play.api.mvc._
import scala.concurrent.Future
import play.api.test._
import org.mockito.Mockito._
import org.mockito.Matchers._
import controllers.common.CookieCrypto
import org.mockito.ArgumentCaptor
import org.scalatest.{Inspectors, OptionValues}
import controllers.common.service.{Encrypter, Decrypter}
import play.api.mvc.Cookie
import play.api.mvc.SimpleResult
import org.scalatest.concurrent.ScalaFutures
import play.api.http.HeaderNames
import play.api.mvc.SimpleResult
import play.api.mvc.Cookie
import org.scalautils.{TypeCheckedTripleEquals, Equivalence}
import play.api.mvc.SimpleResult
import play.api.test.FakeApplication
import play.api.mvc.Cookie

class CookieCryptoFilterSpec extends BaseSpec with MockitoSugar with CookieCrypto with OptionValues with ScalaFutures with Inspectors with TypeCheckedTripleEquals {

  trait MockedCrypto extends Encrypter with Decrypter {
    override def decrypt(id: String): String = ???
    override def encrypt(id: String): String = ???
  }


  private trait Setup extends Results {
    implicit val headerEquiv = RequestHeaderEquivalence

    val cookieName = "someCookieName"
    val encryptedCookie = Cookie(cookieName, "encryptedValue")
    val unencryptedCookie = encryptedCookie.copy(value="decryptedValue")
    val corruptEncryptedCookie = encryptedCookie.copy(value="invalidEncryptedValue")
    val emptyCookie = encryptedCookie.copy(value="")

    val normalCookie1 = Cookie("AnotherCookie1", "normalValue1")
    val normalCookie2 = Cookie("AnotherCookie2", "normalValue2")

    val resultFromAction: SimpleResult = Ok

    lazy val action = {
      val mockAction = mock[(RequestHeader) => Future[SimpleResult]]
      val outgoingResponse = Future.successful(resultFromAction)
      when(mockAction.apply(any())).thenReturn(outgoingResponse)
      mockAction
    }

    val filter = new CookieCryptoFilter with MockedCrypto {
      override val cookieName = Setup.this.cookieName
      override def decrypt(id: String): String = id match {
        case "encryptedValue" => "decryptedValue"
        case somethingElse => fail(s"Unexpectedly tried to decrypt $somethingElse")
      }
      override def encrypt(id: String): String = id match {
        case "decryptedValue" => "encryptedValue"
        case somethingElse => fail(s"Unexpectedly tried to encrypt $somethingElse")
      }
    }

    def requestPassedToAction = {
      val updatedRequest = ArgumentCaptor.forClass(classOf[RequestHeader])
      verify(action).apply(updatedRequest.capture())
      updatedRequest.getValue
    }
  }

  "During request pre-processing, the filter" should {

    "do nothing with no cookie header in the request" in new WithApplication(FakeApplication()) with Setup {
      val incomingRequest = FakeRequest()
      filter(action)(incomingRequest)
      requestPassedToAction should === (incomingRequest)
    }

    "decrypt the cookie" in new WithApplication(FakeApplication()) with Setup {
      val incomingRequest = FakeRequest().withCookies(encryptedCookie)
      filter(action)(incomingRequest)
      requestPassedToAction should === (FakeRequest().withCookies(unencryptedCookie))
    }

    "leave empty cookies unchanged" in new WithApplication(FakeApplication()) with Setup {
      val incomingRequest = FakeRequest().withCookies(emptyCookie)
      filter(action)(incomingRequest)
      requestPassedToAction should === (incomingRequest)
    }

    "leave other cookies alone if our cookie is not present" in new WithApplication(FakeApplication()) with Setup {
      val incomingRequest = FakeRequest().withCookies(normalCookie1, normalCookie2)
      filter(action)(incomingRequest)
      requestPassedToAction should === (incomingRequest)
    }

    "leave other cookies alone when ours is present" in new WithApplication(FakeApplication()) with Setup {
      val incomingRequest = FakeRequest().withCookies(normalCookie1, encryptedCookie, normalCookie2)
      filter(action)(incomingRequest)
      requestPassedToAction should === (FakeRequest().withCookies(unencryptedCookie, normalCookie1, normalCookie2))
    }

   "remove the cookie header if the decryption fails and there are no other cookies" in new WithApplication(FakeApplication()) with Setup {
      val incomingRequest = FakeRequest().withCookies(corruptEncryptedCookie)
      filter(action)(incomingRequest)
      requestPassedToAction should === (FakeRequest())
    }

    "remove the cookie (but leave other cookies intact) if with the decryption fails" in new WithApplication(FakeApplication()) with Setup {
      val incomingRequest = FakeRequest().withCookies(normalCookie1, corruptEncryptedCookie, normalCookie2)
      filter(action)(incomingRequest)
      requestPassedToAction should === (FakeRequest().withCookies(normalCookie1, normalCookie2))
    }
  }

  "During result post-processing, the filter" should {

    "do nothing with the result if there are no cookies" in new WithApplication(FakeApplication()) with Setup {
      filter(action)(FakeRequest()).futureValue should be(resultFromAction)
    }

    "do nothing with the result if there are cookies, but not our cookie" in new WithApplication(FakeApplication()) with Setup {
      override val resultFromAction = Ok.withCookies(normalCookie1, normalCookie2)
      filter(action)(FakeRequest()).futureValue should be(resultFromAction)
    }

    "encrypt the cookie value before returning it" in new WithApplication(FakeApplication()) with Setup {
      override val resultFromAction = Ok.withCookies(unencryptedCookie)
      filter(action)(FakeRequest()).futureValue should be(Ok.withCookies(encryptedCookie))
    }

    "encrypt the cookie value before returning it, leaving other cookies unchanged" in new WithApplication(FakeApplication()) with Setup {
      override val resultFromAction = Ok.withCookies(normalCookie1, unencryptedCookie, normalCookie2)
      filter(action)(FakeRequest()).futureValue should be(Ok.withCookies(normalCookie1, normalCookie2, encryptedCookie))
    }

    "do nothing with the cookie value if it is empty" in new WithApplication(FakeApplication()) with Setup {
      override val resultFromAction = Ok.withCookies(emptyCookie)
      filter(action)(FakeRequest()).futureValue should be(Ok.withCookies(emptyCookie))
    }
  }
}
