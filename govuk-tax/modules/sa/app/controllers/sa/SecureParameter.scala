package controllers.sa

import play.api.libs.json.Json
import org.joda.time.{ DateTimeZone, DateTime }
import controllers.common.CookieEncryption
import java.net.URLEncoder
import scala.util.Try

case class SecureParameter(value: String, timestamp: DateTime) {

  import SecureParameter.encryption

  def encrypt: String = {
    val json = Json.stringify(Json.obj(("value", value), ("timestamp", timestamp.getMillis)))

    encryption.encrypt(json)
  }
}

object SecureParameter {

  def decrypt(encryptedValue: String): Try[SecureParameter] = {

    Try {
      val value = encryption.decrypt(encryptedValue)
      val jsonObject = Json.parse(value)

      SecureParameter((jsonObject \ "value").as[String], new DateTime((jsonObject \ "timestamp").as[BigDecimal].toLong, DateTimeZone.UTC))
    }
  }

  private[SecureParameter] object encryption extends CookieEncryption
}