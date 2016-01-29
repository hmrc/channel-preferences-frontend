package stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.endtoend.sa.{ToAbsoluteUrl, Page}

object Page {

  case class StubbedPage(title: String,
                         relativeUrl: String,
                         name: String,
                         responseBody: String,
                         responseHeader: (String, String)*) extends Page {
    stubFor(get(urlEqualTo(s"/$relativeUrl")).willReturn {
      val resp = aResponse withStatus 200 withBody responseBody
      responseHeader.foldLeft(resp) { (r, h) => r.withHeader(h._1, h._2) }
    })

    override def toString() = name
  }

  implicit def stubbedUrls[T <: Page.StubbedPage]: ToAbsoluteUrl[T] =
    ToAbsoluteUrl.fromRelativeUrl[T](host = "localhost", port = 8080)

}