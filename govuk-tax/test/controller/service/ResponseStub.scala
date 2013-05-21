package controller.service

import play.api.libs.ws.Response

class ResponseStub[A](responseBody: A, statusCode: Int = 200)(implicit val m: Manifest[A]) extends Response(null) {

  import org.json4s._
  import org.json4s.Extraction._
  import org.json4s.jackson.JsonMethods._
  import controllers.domain.UriSerializer

  private implicit val formats = DefaultFormats + UriSerializer

  override lazy val body = compact(decompose(responseBody))

  override def status = statusCode
}
