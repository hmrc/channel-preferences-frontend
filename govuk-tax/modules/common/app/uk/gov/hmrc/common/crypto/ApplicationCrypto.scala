package uk.gov.hmrc.common.crypto

object ApplicationCrypto {

  private def sessionCookieCrypto = CryptoWithKeysFromConfig(baseConfigKey = "cookie.encryption")
  private def ssoPayloadCrypto = CryptoWithKeysFromConfig(baseConfigKey = "sso.encryption")
  private def queryParameterCrypto = CryptoWithKeysFromConfig(baseConfigKey = "queryParameter.encryption")

  lazy val SessionCookieCrypto = sessionCookieCrypto
  lazy val SsoPayloadCrypto = ssoPayloadCrypto
  lazy val QueryParameterCrypto = queryParameterCrypto

  def verifyConfiguration() {
    sessionCookieCrypto
    queryParameterCrypto
    ssoPayloadCrypto
  }
}
