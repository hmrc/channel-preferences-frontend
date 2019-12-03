/*
 * Copyright 2019 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.UUID

import javax.inject.{ Inject, Singleton }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Seconds, Span }
import play.api.libs.json.{ JsArray, JsNumber, JsObject, Json, _ }
import play.api.libs.ws.WSClient
import uk.gov.hmrc.auth.core.{ Enrolment, EnrolmentIdentifier }
import uk.gov.hmrc.domain.TaxIds.TaxIdWithName
import uk.gov.hmrc.domain._

import scala.concurrent.Future

@Singleton
class AuthHelper @Inject()(ws: WSClient) extends ScalaFutures {

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
    "affinityGroup"      -> "Organisation",
    "confidenceLevel"    -> 200,
    "credentialStrength" -> "none",
    "enrolments"         -> JsArray()
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

  def authorisedTokenFor(ids: TaxIdentifier*): Future[String] =
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
    val bearerToken = authorisedTokenFor(taxId).futureValue
    ("Authorization", bearerToken)
  }

  def buildUserToken(payload: String): Future[String] = {
    val response = ws
      .url(s"http://localhost:$ggAuthPort/government-gateway/session/login")
      .withHttpHeaders(("Content-Type", "application/json"))
      .post(payload)
      .futureValue(timeout(Span(10, Seconds)))
    Future.successful(response.header("Authorization").get)
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
