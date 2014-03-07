package controllers.common.domain

import org.json4s._
import java.net.URI
import uk.gov.hmrc.rest._
import org.json4s.JsonAST.JString
import scala.Some

object Transform {

  import org.json4s._
  import org.json4s.jackson.JsonMethods._

  private implicit val formats = DefaultFormats + UriSerializer + Json4sDateTimeSerializer + Json4sLocalDateSerializer + Json4sJIntToBigDecimalSerializer + ctUtrFormatSerializer + empRefFormatSerializer + ninoFormatSerializer + saUtrFormatSerializer + uarFormatSerializer + vrnFormatSerializer

  def fromResponse[A](body: String)(implicit m: Manifest[A]): A = Option(body) match {
    case Some(b) if b.length > 0 => parse(b, useBigDecimalForDouble = true).extract
    case _ => throw new IllegalArgumentException("A string value is required for transformation")
  }

  def toRequestBody[A](obj: A): String = {
    compact(render(Extraction.decompose(obj)))
  }
}

case object UriSerializer extends CustomSerializer[URI](format => ({
  case JString(uri) => URI.create(uri)
  case JNull => null
}, {
  case uri: URI => JString(uri.toString)
}
))

