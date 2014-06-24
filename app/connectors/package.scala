import java.net.{URI, URL}
import play.api.libs.json._
import play.api.libs.json.JsString

package object connectors {

  implicit def urlWrites = new Writes[URL] {
    override def writes(url: URL): JsValue = JsString(url.toExternalForm)
  }

  implicit def uriWrites = new Writes[URI] {
    override def writes(uri: URI): JsValue = JsString(uri.toASCIIString)
  }

  implicit def uriReads = new Reads[URI] {
    override def reads(json: JsValue): JsResult[URI] = json.validate[String].map(URI.create)
  }

}