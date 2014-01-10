package uk.gov.hmrc.common

package object crypto {
  val SessionCookieCrypto = CryptoWithKeyFromConfig(configKey = "cookie.encryption.key")
  val QueryParameterCrypto = CryptoWithKeyFromConfig(configKey = "queryParameter.encryption.key")
  val SsoPayloadCrypto = CryptoWithKeyFromConfig(configKey = "sso.encryption.key")
}
