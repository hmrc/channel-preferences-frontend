package uk.gov.hmrc.common

package object crypto {
  val SessionCookieCrypto = CryptoWithKeysFromConfig(baseConfigKey = "cookie.encryption")
  val QueryParameterCrypto = CryptoWithKeysFromConfig(baseConfigKey = "queryParameter.encryption")
  val SsoPayloadCrypto = CryptoWithKeysFromConfig(baseConfigKey = "sso.encryption")
}
