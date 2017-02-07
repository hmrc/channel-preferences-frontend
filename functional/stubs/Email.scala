package stubs

import com.github.tomakehurst.wiremock.client.WireMock._

object Email {

  def `GET /hmrc/validate-email-address`(emailAddress: String) =
    get(urlEqualTo(s"/hmrc/validate-email-address?email=${emailAddress.replace("@","%40")}"))

  val validEmailJson =
    s"""
       |{
       |    "valid": true
       |}""".stripMargin

  val invalidEmailJson =
    s"""
       |{
       |    "valid": false
       |}""".stripMargin
}
