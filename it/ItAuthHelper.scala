/*
 * Copyright 2019 HM Revenue & Customs
 *
 */

import java.net.URLEncoder
import java.util.UUID

import javax.inject.{ Inject, Singleton }
import org.apache.commons.codec.CharEncoding
import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Seconds, Span }
import play.api.Play
import play.api.http.HeaderNames
import play.api.libs.crypto.CookieSigner
import play.api.libs.json.{ JsArray, JsNumber, JsObject, Json, _ }
import play.api.libs.ws.{ WSClient, WSRequest }
import play.api.mvc.Session
import play.api.test.{ HasApp, Injecting }
import uk.gov.hmrc.auth.core.{ Enrolment, EnrolmentIdentifier }
import uk.gov.hmrc.crypto.{ CryptoGCMWithKeysFromConfig, PlainText }
import uk.gov.hmrc.domain.TaxIds.TaxIdWithName
import uk.gov.hmrc.domain._
import uk.gov.hmrc.http.SessionKeys

import scala.concurrent.Future

@Singleton
class ItAuthHelper @Inject()(ws: WSClient) extends ScalaFutures {

  lazy val authPort = 8500
  lazy val ggAuthPort = 8585

  private def makeEnrolment(identifier: TaxIdWithName, identifierKey: String): Enrolment =
    Enrolment(
      identifier.name,
      Seq(EnrolmentIdentifier(identifierKey, identifier.value)),
      "Activated"
    )

  private def makeFhddsEnrolment(fhddsIdent: HmrcObtdsOrg) =
    makeEnrolment(fhddsIdent, "EtmpRegistrationNumber")

  private def makeVatEnrolement(vatIdent: HmrcMtdVat) =
    makeEnrolment(vatIdent, "VRN")

  private def makeVrnEnrolement(vrnIdent: Vrn) =
    makeEnrolment(vrnIdent, "VRN")

  private def makeCtUtrEnrolement(ctUtr: CtUtr) =
    makeEnrolment(ctUtr, "ctutr")

  private val STRIDE_PAYLOAD = Json.obj(
    "clientId"   -> "id",
    "enrolments" -> JsArray(),
    "ttl"        -> JsNumber(1200)
  )

  private def GG_BASE_PAYLOAD: JsObject = Json.obj(
    "credId"             -> s"${UUID.randomUUID.toString}",
    "affinityGroup"      -> "Individual",
    "confidenceLevel"    -> 200,
    "credentialStrength" -> "strong",
    "enrolments"         -> JsArray(),
    "usersName"          -> "Lisa Nicole Brennan",
    "email"              -> "lisa.brennan@some.domain.com"
  )

  private def taxIdKey(taxId: TaxIdentifier) = taxId match {
    case _: SaUtr => "IR-SA"
    case _CtUtr   => "IR-CT"
  }

  private def enrolmentPayload(taxId: TaxIdentifier) =
    Json.obj(
      "key" -> s"${taxIdKey(taxId)}",
      "identifiers" -> JsArray(
        IndexedSeq(
          Json.obj(
            "key"   -> "UTR",
            "value" -> taxId.value
          )
        )
      ),
      "state" -> "Activated"
    )

  private def addUtrToPayload(payload: JsObject, utr: TaxIdentifier) =
    payload
      .transform((__ \ "enrolments").json.update(__.read[JsArray].map {
        case JsArray(arr) => JsArray(arr :+ enrolmentPayload(utr))
      }))
      .get

  private def addEnrolmentToPayload(payload: JsObject, enrolment: Enrolment) = {
    implicit val idaFormat = Json.format[EnrolmentIdentifier]
    val enrolmentWrites = Json.writes[Enrolment]
    payload
      .transform(
        (__ \ "enrolments").json.update(
          __.read[JsArray]
            .map { case JsArray(arr) => JsArray(arr :+ Json.toJson(enrolment)(enrolmentWrites)) }
        )
      )
      .get
  }

  private def addNinoToPayload(payload: JsObject, nino: Nino) =
    payload ++ Json.obj("nino" -> nino.value)

  def authorisedTokenFor(ids: TaxIdentifier*): Future[(String, String)] =
    buildUserToken(
      ids
        .foldLeft(GG_BASE_PAYLOAD)(
          (payload, taxId) =>
            taxId match {
              case saUtr: SaUtr        => addUtrToPayload(payload, saUtr)
              case nino: Nino          => addNinoToPayload(payload, nino)
              case ctUtr: CtUtr        => addEnrolmentToPayload(payload, makeCtUtrEnrolement(ctUtr))
              case fhdds: HmrcObtdsOrg => addEnrolmentToPayload(payload, makeFhddsEnrolment(fhdds))
              case vat: HmrcMtdVat     => addEnrolmentToPayload(payload, makeVatEnrolement(vat))
              case vrn: Vrn            => addEnrolmentToPayload(payload, makeVrnEnrolement(vrn))
          }
        )
        .toString
    )

  def authHeader(taxId: TaxIdentifier): (String, String) = {
    val (bearerToken, userId) = authorisedTokenFor(taxId).futureValue
    ("Authorization", bearerToken)
  }

  def authExchange(taxId: TaxIdentifier): (String, String) = {
    val (bearerToken, userId) = authorisedTokenFor(taxId).futureValue
    (bearerToken, userId)
  }

  def buildUserToken(payload: String): Future[(String, String)] = {
    val response = ws
      .url(s"http://localhost:$ggAuthPort/government-gateway/session/login")
      .withHttpHeaders(("Content-Type", "application/json"))
      .post(payload)
      .futureValue(timeout(Span(10, Seconds)))
    val authUri = response.header("Location").getOrElse("")
    val authToken = response.header("Authorization").getOrElse("")
    Future.successful(authToken, authUri)

  }

  def buildStrideToken(): Future[String] = {
    val response = ws
      .url(s"http://localhost:$authPort/auth/sessions")
      .withHttpHeaders(("Content-Type", "application/json"))
      .post(STRIDE_PAYLOAD)
      .futureValue(timeout(Span(10, Seconds)))
    Future.successful(response.header("Authorization").get)
  }

}

trait SessionCookieEncryptionSupport extends Injecting {
  self: HasApp =>

  val signer = inject[CookieSigner]

  val SignSeparator = "-"
  val mdtpSessionCookie = "mdtp"

  lazy val cipher = new CryptoGCMWithKeysFromConfig("cookie.encryption", Play.current.configuration.underlying)

  implicit class WSRequestWithSession(request: WSRequest) {
    def withSession(pair: (String, String)*): WSRequest = {
      val payload = pair.toSeq
        .map {
          case (k, v) => s"$k=${URLEncoder.encode(v, CharEncoding.UTF_8)}"
        }
        .mkString("&")
      // Please refer to below link concerning how play signs the cookie. The steps are plays first signs the cookie before encryption filter is executed.
      // https://github.com/playframework/playframework/blob/master/framework/src/play/src/main/scala/play/api/mvc/Http.scala#encode
      val signedPayload = signer.sign(payload) + SignSeparator + payload
      val encryptedSignedPayload: String = cipher.encrypt(PlainText(signedPayload)).value
      val sessionCookie = s"""$mdtpSessionCookie=$encryptedSignedPayload"""
      request.withHttpHeaders((HeaderNames.COOKIE, sessionCookie))
    }
  }
}
