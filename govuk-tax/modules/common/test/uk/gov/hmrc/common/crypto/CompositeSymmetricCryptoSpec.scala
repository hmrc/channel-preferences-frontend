package uk.gov.hmrc.common.crypto

import uk.gov.hmrc.common.BaseSpec
import org.mockito.Mockito._

class CompositeSymmetricCryptoSpec extends BaseSpec {

  trait Setup {

    trait Crypto extends Encrypter with Decrypter

    val currentCrypto = mock[Crypto]
    val previousCryptos = Seq.empty[Decrypter]

    lazy val crypto = new CompositeSymmetricCrypto(currentCrypto, previousCryptos)

    val message = "some message"
    val encryptedMessage = "encrypted message"
  }

  trait SetupWithSinglePreviousCrypto extends Setup {
    val previousCrypto = mock[Decrypter]
    override val previousCryptos = Seq(previousCrypto)
  }

  trait SetupWithTwoPreviousCryptos extends Setup {
    val previousCrypto1 = mock[Decrypter]
    val previousCrypto2 = mock[Decrypter]
    override val previousCryptos = Seq(previousCrypto1, previousCrypto2)
  }

  "With only the currentCrypto defined, the CompositeSymmetricCrypto" should {

    "use the current crypto to encrypt values" in new Setup {
      when(currentCrypto.encrypt(message)).thenReturn(encryptedMessage)
      crypto.encrypt(message) shouldBe encryptedMessage
    }

    "use the current crypto to decrypt values" in new Setup {
      when(currentCrypto.decrypt(encryptedMessage)).thenReturn(message)
      crypto.decrypt(encryptedMessage) shouldBe message
    }

    "throw a SecurityException if the current crypto cannot decrypt the message" in new Setup {
      when(currentCrypto.decrypt(encryptedMessage)).thenThrow(new RuntimeException("Can't decrypt"))
      evaluating(crypto.decrypt(encryptedMessage)) should produce[SecurityException]
    }
  }

  "With a current and a single previous crypto defined, the CompositeSymmetricCrypto" should {

    "use the current crypto to encrypt values" in new SetupWithSinglePreviousCrypto {
      when(currentCrypto.encrypt(message)).thenReturn(encryptedMessage)
      crypto.encrypt(message) shouldBe encryptedMessage
    }

    "attempt to decrypt with the current crypto first, and return the value if successful" in new SetupWithSinglePreviousCrypto {
      when(currentCrypto.decrypt(encryptedMessage)).thenReturn(message)
      crypto.decrypt(encryptedMessage) shouldBe message
      verifyZeroInteractions(previousCrypto)
    }

    "if decrypting with the currentCrypto fails, attempt to use the previous crypto, returning the value if successful" in new SetupWithSinglePreviousCrypto {
      when(currentCrypto.decrypt(encryptedMessage)).thenThrow(new RuntimeException("Can't decrypt"))
      when(previousCrypto.decrypt(encryptedMessage)).thenReturn(message)
      crypto.decrypt(encryptedMessage) shouldBe message
      verify(currentCrypto).decrypt(encryptedMessage)
    }

    "throw a SecurityException if neither the current nor previous crypto can decrypt the message" in new SetupWithSinglePreviousCrypto {
      when(currentCrypto.decrypt(encryptedMessage)).thenThrow(new RuntimeException("Can't decrypt"))
      when(previousCrypto.decrypt(encryptedMessage)).thenThrow(new RuntimeException("Still can't decrypt"))
      evaluating(crypto.decrypt(encryptedMessage)) should produce[SecurityException]
      verify(currentCrypto).decrypt(encryptedMessage)
      verify(previousCrypto).decrypt(encryptedMessage)
    }
  }

  "With a current and two previous cryptos defined, the CompositeSymmetricCrypto" should {

    "use the current crypto to encrypt values" in new Setup {
      when(currentCrypto.encrypt(message)).thenReturn(encryptedMessage)
      crypto.encrypt(message) shouldBe encryptedMessage
    }

    "attempt to decrypt with the current crypto first, and return the value if successful" in new SetupWithTwoPreviousCryptos {
      when(currentCrypto.decrypt(encryptedMessage)).thenReturn(message)
      crypto.decrypt(encryptedMessage) shouldBe message
      verifyZeroInteractions(previousCrypto1, previousCrypto2)
    }

    "if decrypting with the currentCrypto fails, attempt to use the first previous crypto, returning the value if successful" in new SetupWithTwoPreviousCryptos {
      when(currentCrypto.decrypt(encryptedMessage)).thenThrow(new RuntimeException("Can't decrypt"))
      when(previousCrypto1.decrypt(encryptedMessage)).thenReturn(message)
      crypto.decrypt(encryptedMessage) shouldBe message
      verify(currentCrypto).decrypt(encryptedMessage)
      verifyZeroInteractions(previousCrypto2)
    }

    "if decrypting with both the currentCrypto and the first previous crypto fail, attempt to use the second previous crypto, returning the value if successful" in new SetupWithTwoPreviousCryptos {
      when(currentCrypto.decrypt(encryptedMessage)).thenThrow(new RuntimeException("Can't decrypt"))
      when(previousCrypto1.decrypt(encryptedMessage)).thenThrow(new RuntimeException("Still can't decrypt"))
      when(previousCrypto2.decrypt(encryptedMessage)).thenReturn(message)
      crypto.decrypt(encryptedMessage) shouldBe message
      verify(currentCrypto).decrypt(encryptedMessage)
      verify(previousCrypto1).decrypt(encryptedMessage)
    }

    "throw a SecurityException if none of the cryptos can decrypt the message" in new SetupWithTwoPreviousCryptos {
      when(currentCrypto.decrypt(encryptedMessage)).thenThrow(new RuntimeException("Can't decrypt"))
      when(previousCrypto1.decrypt(encryptedMessage)).thenThrow(new RuntimeException("Still can't decrypt"))
      when(previousCrypto2.decrypt(encryptedMessage)).thenThrow(new RuntimeException("Nope"))
      evaluating(crypto.decrypt(encryptedMessage)) should produce[SecurityException]
      verify(currentCrypto).decrypt(encryptedMessage)
      verify(previousCrypto1).decrypt(encryptedMessage)
      verify(previousCrypto2).decrypt(encryptedMessage)
    }
  }
}
