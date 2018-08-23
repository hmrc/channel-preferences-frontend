package stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.endtoend.sa.{RelativeUrl, AbsoluteUrl, Page => E2EPage}

object Page {

  case class StubbedPage(title: String,
                         relativeUrl: String,
                         name: String,
                         responseBody: String,
                         responseHeader: (String, String)*) extends E2EPage with RelativeUrl {
    stubFor(get(urlEqualTo(s"/$relativeUrl")).willReturn {
      val resp = aResponse withStatus 200 withBody responseBody
      responseHeader.foldLeft(resp) { (r, h) => r.withHeader(h._1, h._2) }
    })

    override def toString() = name
  }

  implicit def stubbedUrls[T <: Page.StubbedPage]: AbsoluteUrl[T] =
    AbsoluteUrl.fromRelativeUrl[T](host = "localhost", port = 8080)

}