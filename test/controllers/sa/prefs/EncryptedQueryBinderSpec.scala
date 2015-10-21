package controllers.sa.prefs

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import play.api.mvc.QueryStringBindable
import uk.gov.hmrc.crypto._
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.hmrc.play.test.UnitSpec


class EncryptedQueryBinderSpec extends UnitSpec with MockitoSugar {
  
  var decryptedEmail: Option[String] = None
  
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
      decryptedEmail = Some("test@test.com")
      binder.bind("exampleKey", Map.empty) should be (Some(Right(Encrypted(EmailAddress("test@test.com")))))
    }
    "Give an error for an invalid email" in new TestCase {
      when(stringBinder.bind(any(), any())).thenReturn(Some(Right(encryptedData)))
      decryptedEmail = Some("asdfasdf")
      binder.bind("exampleKey", Map.empty) should be (Some(Left("exampleKey is not valid")))
    }
    "Give an error if decryption throws an exception" in new TestCase {
      when(stringBinder.bind(any(), any())).thenReturn(Some(Right(encryptedData)))
      decryptedEmail = None
      binder.bind("exampleKey", Map.empty) should be (Some(Left("Could not decrypt value for exampleKey")))
    }
  }

  trait TestCase {
    val stringBinder = mock[QueryStringBindable[String]]
    val crypto = new Encrypter with Decrypter {
      override def decrypt(reversiblyEncrypted: Crypted): PlainText = decryptedEmail.map(PlainText).getOrElse(throw new RuntimeException())

      override def encrypt(plain: PlainContent): Crypted = ???

      override def decryptAsBytes(reversiblyEncrypted: Crypted): PlainBytes = ???
    }
    val binder = new EncryptedQueryBinder[EmailAddress](crypto, EmailAddress.apply, _.value)(stringBinder)
    val encryptedData: String = "encrypted Data"
  }
}
