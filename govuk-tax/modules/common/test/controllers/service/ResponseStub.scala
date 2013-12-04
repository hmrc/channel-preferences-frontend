package controllers.service

import controllers.common.domain.UriSerializer
import uk.gov.hmrc.rest._
import play.api.libs.ws.Response

class ResponseStub[A](responseBody: A, statusCode: Int = 200)(implicit val m: Manifest[A]) extends Response(null) {

  import org.json4s._
  import org.json4s.Extraction._
  import org.json4s.jackson.JsonMethods._

  private implicit val formats = DefaultFormats + UriSerializer + ctUtrFormatSerializer + empRefFormatSerializer + ninoFormatSerializer + saUtrFormatSerializer + uarFormatSerializer + vrnFormatSerializer

  override lazy val body = compact(decompose(responseBody))

  override def status = statusCode
}
