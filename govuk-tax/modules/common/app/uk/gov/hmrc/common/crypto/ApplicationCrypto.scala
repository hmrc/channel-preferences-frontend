package uk.gov.hmrc.common.crypto

import uk.gov.hmrc.crypto.CompositeSymmetricCrypto

object ApplicationCrypto {

  lazy val SessionCookieCrypto = CryptoWithKeysFromConfig(baseConfigKey = "cookie.encryption")
  lazy val QueryParameterCrypto = CryptoWithKeysFromConfig(baseConfigKey = "queryParameter.encryption")
  lazy val SsoPayloadCrypto = CryptoWithKeysFromConfig(baseConfigKey = "sso.encryption")

  def initialiseAndAssertKeyValidity(): Seq[CompositeSymmetricCrypto] = Seq(SessionCookieCrypto, QueryParameterCrypto, SsoPayloadCrypto)
}
