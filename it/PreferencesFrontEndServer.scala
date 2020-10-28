/*
 * Copyright 2020 HM Revenue & Customs
 *
 */

import java.net.URLEncoder
import java.util.UUID

import org.scalatest.concurrent.{ IntegrationPatience, ScalaFutures }
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.{ PlaySpec, WsScalaTestClient }
import play.api.test.Helpers._
import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.libs.ws.{ DefaultWSCookie, WSClient, WSCookie, WSRequest }
import uk.gov.hmrc.crypto.{ ApplicationCrypto, PlainText }
import uk.gov.hmrc.domain._
import uk.gov.hmrc.http.{ HeaderCarrier, SessionKeys }
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.time.DateTimeUtils
import views.sa.prefs.helpers.DateFormat
import org.scalatest.time.{ Millis, Seconds, Span }
import org.scalatest.concurrent.{ AbstractPatienceConfiguration, PatienceConfiguration }

trait TestUser {
  def userId = "SA0055"

  val password = "testing123"

  val utr = GenerateRandom.utr()
  val nino = GenerateRandom.nino()

}

class TestCase
    extends PlaySpec with TestUser with GuiceOneServerPerSuite with WsScalaTestClient with ScalaFutures
    with IntegrationPatience {

  private val itPatience: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(30, Seconds)),
    interval = scaled(Span(200, Millis))
  )

  implicit override val patienceConfig: PatienceConfig = itPatience

  val applicatinCrypto = app.injector.instanceOf[ApplicationCrypto]

  val servicesConfig = app.injector.instanceOf[ServicesConfig]
  lazy val entityResolverUrl = servicesConfig.baseUrl("entity-resolver")
  val todayDate = DateFormat.longDateFormat(Some(DateTimeUtils.now.toLocalDate)).get.body

  implicit val wsClient = app.injector.instanceOf[WSClient]
  val myPublicAddress = s"localhost:$port"

  def uniqueEmail = s"${UUID.randomUUID().toString}@email.com"

  def changedUniqueEmail = uniqueEmail

  def encryptAndEncode(value: String) =
    URLEncoder.encode(applicatinCrypto.QueryParameterCrypto.encrypt(PlainText(value)).value, "UTF-8")

  def urlWithHostContext(url: String) = new {
    def apply(returnUrl: String = "", returnLinkText: String = "") =
      wsUrl(s"$url?returnUrl=${encryptAndEncode(returnUrl)}&returnLinkText=${encryptAndEncode(returnLinkText)}")
  }

  def `/sa/print-preferences/assets/`(file: String) =
    wsUrl(s"/sa/print-preferences/assets/$file")

  val `/paperless/resend-verification-email` = urlWithHostContext("/paperless/resend-verification-email")
  val `/paperless/manage` = urlWithHostContext("/paperless/manage")
  val `/paperless/check-settings` = urlWithHostContext("/paperless/check-settings")

  val payeFormTypeBody = Json.parse(s"""{"active":true}""")

  def `/preferences`(header: (String, String)) = new {
    def getPreference =
      wsClient.url("http://localhost:8015/preferences").withHttpHeaders(header).get

    def putPendingEmail(email: String) =
      wsClient
        .url("http://localhost:8015/preferences/pending-email")
        .withHttpHeaders(header)
        .put(Json.parse(s"""{"email":"$email"}"""))
  }

  val `/portal/preferences` = new {

    def getForUtr(utr: String) = wsUrl(s"http://localhost:8015/portal/preferences/sa/$utr").get().futureValue

    def getForNino(nino: String) = wsUrl(s"http://localhost:8015/portal/preferences/paye/$nino").get().futureValue
  }

  def `/preferences/terms-and-conditions`(header: (String, String)) = new {
    def postGenericOptIn(pendingEmail: String) =
      wsClient
        .url(s"http://localhost:8015/preferences/terms-and-conditions")
        .withHttpHeaders(header)
        .post(Json.parse(s"""{
                            |  "generic": {
                            |    "accepted": true,
                            |    "optInPage":{
                            |      "version": {"major":2,"minor":1}, "cohort":1, "pageType":"IPage"}
                            |  },
                            |  "email":"$pendingEmail",
                            |  "language": "en"
                            |}""".stripMargin))

    def postGenericOptOut() =
      wsClient
        .url(s"http://localhost:8015/preferences/terms-and-conditions")
        .withHttpHeaders(header)
        .post(Json.parse(s"""{
                            |  "generic": {
                            |    "accepted": false,
                            |    "optInPage":{
                            |      "version": {"major":2,"minor":1}, "cohort":1, "pageType":"IPage"}
                            |  },
                            |  "language": "en"
                            |}""".stripMargin))
  }

  def `/entity-resolver/sa/:utr`(utr: String) = {
    val response = wsClient.url(s"http://localhost:8015/entity-resolver/sa/$utr").get().futureValue
    response.status must be(OK)
    (response.json \ "_id").as[String]
  }

  def `/entity-resolver/paye/:nino`(nino: String) = {
    val response = wsClient.url(s"http://localhost:8015/entity-resolver/paye/$nino").get().futureValue
    response.status must be(OK)
    (response.json \ "_id").as[String]
  }

  val `/preferences-admin/sa/individual` = new {
    def verifyEmailFor(entityId: String) =
      wsClient.url(s"http://localhost:8025/preferences-admin/$entityId/verify-email").post("")

    def postExpireVerificationLink(entityId: String) =
      wsClient.url(s"http://localhost:8025/preferences-admin/$entityId/expire-email-verification-link").post("")
  }

  val `/preferences-admin/bounce-email` = new {
    def post(emailAddress: String) =
      wsClient.url("http://localhost:8025/preferences-admin/bounce-email").post(Json.parse(s"""{
                                                                                              |"emailAddress": "$emailAddress"
                                                                                              |}""".stripMargin))
  }

  def `/preferences-admin/remove-language`(entityId: String) = {
    val response =
      wsClient.url(s"http://localhost:8025/preferences-admin/remove-language/$entityId").put("{}").futureValue
    response.status must be(OK)
  }

  val `/preferences-admin/sa/bounce-email-inbox-full` = new {
    def post(emailAddress: String) =
      wsClient.url("http://localhost:8025/preferences-admin/bounce-email").post(Json.parse(s"""{
                                                                                              |"emailAddress": "$emailAddress",
                                                                                              |"code": 552
                                                                                              |}""".stripMargin))
  }

  def `/paperless/warnings`: WSRequest = urlWithHostContext("/paperless/warnings")()

  def `/paperless/status`: WSRequest = urlWithHostContext("/paperless/status")()
}

trait TestCaseWithFrontEndAuthentication extends TestCase with SessionCookieEncryptionSupport {

  implicit val hc = HeaderCarrier()
  val authHelper = app.injector.instanceOf[ItAuthHelper]
  def ggAuthHeaderWithUtr = authHelper.authHeader(utr)
  def ggAuthHeaderWithNino = authHelper.authHeader(nino)

  val returnUrl = "/test/return/url"
  val returnLinkText = "Continue"

  val encryptedReturnUrl =
    URLEncoder.encode(applicatinCrypto.QueryParameterCrypto.encrypt(PlainText(returnUrl)).value, "UTF-8")
  val encryptedReturnText =
    URLEncoder.encode(applicatinCrypto.QueryParameterCrypto.encrypt(PlainText(returnLinkText)).value, "UTF-8")

  def `/paperless/activate-from-token/:svc/:token`(svc: String, token: String) = new {

    val queryParamsMap: Map[String, Option[String]] = Map(
      "returnUrl"      -> Some(returnUrl),
      "returnLinkText" -> Some(returnLinkText)
    )

    val url = wsUrl(s"/paperless/activate-from-token/$svc/$token")
      .withSession((SessionKeys.authToken -> authHelper.authorisedTokenFor().futureValue._1))()
      .withQueryString(
        queryParamsMap.collect {
          case (key, Some(value)) => (key -> applicatinCrypto.QueryParameterCrypto.encrypt(PlainText(value)).value)
        }.toSeq: _*
      )

    val formTypeBody = Json.parse("""{"active":true}""")

    def put() =
      url.put(formTypeBody)
  }

  def `/paperless/activate`(taxIdentifiers: TaxIdentifier*)(
    termsAndConditions: Option[String] = None,
    emailAddress: Option[String] = None,
    language: Option[String] = None) =
    new {

      val queryParamsMap: Map[String, Option[String]] = Map(
        "returnUrl"          -> Some(returnUrl),
        "returnLinkText"     -> Some(returnLinkText),
        "termsAndConditions" -> termsAndConditions,
        "email"              -> emailAddress
      )

      val header = authHelper.authorisedTokenFor(taxIdentifiers: _*).futureValue

      val url = wsUrl(s"/paperless/activate")
        .withSession(
          (SessionKeys.authToken -> header._1)
        )(language)
        .withQueryString(
          queryParamsMap.collect {
            case (key, Some(value)) => (key -> applicatinCrypto.QueryParameterCrypto.encrypt(PlainText(value)).value)
          }.toSeq: _*
        )

      private val formTypeBody = Json.parse("""{"active":true}""")

      def put() = url.put(formTypeBody)
    }

}
