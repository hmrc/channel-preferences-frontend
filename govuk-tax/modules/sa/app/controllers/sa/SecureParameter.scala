package controllers.sa

import play.api.libs.json.Json
import org.joda.time.{ DateTimeZone, DateTime }
import scala.util.Try
import uk.gov.hmrc.common.crypto.ApplicationCrypto.QueryParameterCrypto

case class SecureParameter(value: String, timestamp: DateTime) {

  def encrypt: String =
    QueryParameterCrypto.encrypt(Json.stringify(Json.obj(("value", value), ("timestamp", timestamp.getMillis))))
}

object SecureParameter {

  def decrypt(encryptedValue: String): Try[SecureParameter] = Try {
    val value = QueryParameterCrypto.decrypt(encryptedValue)
    val jsonObject = Json.parse(value)

    SecureParameter((jsonObject \ "value").as[String], new DateTime((jsonObject \ "timestamp").as[BigDecimal].toLong, DateTimeZone.UTC))
  }
}