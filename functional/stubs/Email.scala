package stubs

import com.github.tomakehurst.wiremock.client.WireMock._

object Email {

  def `POST /validate-email-address`(emailAddress: String) =
    post(urlEqualTo("/validate-email-address"))

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
