package controllers.domain

import org.json4s._
import java.net.URI
import org.json4s.JsonAST.JString

object Transform {

  import org.json4s._
  import org.json4s.jackson.JsonMethods._

  private implicit val formats = DefaultFormats + UriSerializer

  def fromResponse[A](body: String)(implicit m: Manifest[A]): A = Option(body) match {
    case Some(b) if (b.length > 0) => parse(b).extract
    case _ => throw new IllegalArgumentException("A string value is required for transformation")
  }
}

case object UriSerializer extends CustomSerializer[URI](format => ({
  case JString(uri) => URI.create(uri)
  case JNull => null
}, {
  case uri: URI => JString(uri.toString)
}
))

