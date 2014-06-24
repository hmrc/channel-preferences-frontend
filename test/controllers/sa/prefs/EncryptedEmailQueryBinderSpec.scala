package controllers.sa.prefs

import controllers.sa.EncryptedEmailBinder
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.mvc.QueryStringBindable
import uk.gov.hmrc.common.crypto.Encrypted
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.test.UnitSpec


class EncryptedEmailQueryBinderSpec extends UnitSpec with MockitoSugar {
  "Binding a Encrypted[Email]" should {
    "Pass through any failure from the string binder" in new TestCase {
      when(stringBinder.bind(any(), any())).thenReturn(Some(Left("an error")))
      binder.bind("exampleKey", Map.empty) should be (Some(Left("an error")))
    }
    "Pass through a None from the string binder" in new TestCase {
      when(stringBinder.bind(any(), any())).thenReturn(None)
      binder.bind("exampleKey", Map.empty) should be (None)
    }
    "Process a validly encrypted valid email" in new TestCase {
      when(stringBinder.bind(any(), any())).thenReturn(Some(Right(encryptedData)))
      when(crypto.decrypt(encryptedData)).thenReturn("test@test.com")
      binder.bind("exampleKey", Map.empty) should be (Some(Right(Encrypted(EmailAddress("test@test.com")))))
    }
    "Give an error for an invalid email" in new TestCase {
      when(stringBinder.bind(any(), any())).thenReturn(Some(Right(encryptedData)))
      when(crypto.decrypt(encryptedData)).thenReturn("asdfasdf")
      binder.bind("exampleKey", Map.empty) should be (Some(Left("Not a valid email address")))
    }
    "Give an error if decryption throws an exception" in new TestCase {
      when(stringBinder.bind(any(), any())).thenReturn(Some(Right(encryptedData)))
      when(crypto.decrypt(encryptedData)).thenThrow(new RuntimeException())
      binder.bind("exampleKey", Map.empty) should be (Some(Left("Could not decrypt value")))
    }
  }

  trait TestCase {
    val stringBinder = mock[QueryStringBindable[String]]
    trait Crypto extends Encrypter with Decrypter // appease the Mockito fairies
    val crypto = mock[Crypto]
    val binder = new EncryptedEmailBinder(crypto, stringBinder)
    val encryptedData: String = "encrypted Data"
  }
}
