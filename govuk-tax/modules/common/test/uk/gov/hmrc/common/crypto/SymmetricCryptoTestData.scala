package uk.gov.hmrc.common.crypto

import org.apache.commons.codec.binary.Base64

object SymmetricCryptoTestData {
  
  private val rawKey = Array[Byte](0, 1, 2, 3, 4, 5 ,6 ,7, 8 ,9, 10, 11, 12, 13, 14, 15)

  val key = Base64.encodeBase64String(rawKey)

  val plainMessage = "this is my message"
  val encryptedMessage = "up/76On5j54pAjzqZR1mqM5E28skTl8Aw0GkKi+zjkk="
}
