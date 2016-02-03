package stubs

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.endtoend.sa.config.UserWithUtr

object Preferences {

  def `GET /preferences/sa/individual/<utr>/print-suppression`(implicit user: UserWithUtr) =
    get(urlEqualTo(s"/preferences/sa/individual/${user.utr}/print-suppression"))

  def `GET verified-email-address`(implicit user: UserWithUtr) =
    get(urlEqualTo(s"/portal/preferences/sa/individual/${user.utr}/print-suppression/verified-email-address"))

  def `POST /preferences/sa/individual/<utr>/terms-and-conditions`(genericAccepted: Boolean)(implicit user: UserWithUtr) =
    postRequestedFor(urlEqualTo(s"/preferences/sa/individual/${user.utr}/terms-and-conditions")).withRequestBody(
      equalToJson(s"""{ "generic": { "accepted": $genericAccepted } } """)
    )

  def `POST /preferences/sa/individual/<utr>/terms-and-conditions`(implicit user: UserWithUtr) =
    post(urlMatching(s"/preferences/sa/individual/${user.utr}/terms-and-conditions"))


  val optedInPreferenceJson =
    s"""
       |{
       |    "digital": true
       |}""".stripMargin

  val optedOutPreferenceJson =
    s"""
       |{
       |    "digital": false
       |}""".stripMargin

  val verifiedEmailJson =
    s"""
       |{
       |    "email": "some@email.com"
       |}""".stripMargin

}
