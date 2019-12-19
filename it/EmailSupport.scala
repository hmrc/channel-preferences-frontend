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

import org.scalatest.concurrent.{ Eventually, IntegrationPatience }
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Await, Future }
import scala.util.matching.Regex.Match

trait EmailSupport extends TestCaseWithFrontEndAuthentication with IntegrationPatience with Eventually {

  import EmailSupport._

  import scala.concurrent.duration._

  implicit val emailReads = Json.reads[Email]
  implicit val emailTokenWrites = Json.writes[Token]

  private lazy val mailgunStubUrl = servicesConfig.baseUrl("mailgun")
  private lazy val emailBaseUrl = servicesConfig.baseUrl("email")
  private lazy val prefsBaseUrl = servicesConfig.baseUrl("preferences")
  private lazy val timeout = 5.seconds

  def clearEmails() = {
    eventually(
      wsClient.url(s"$emailBaseUrl/test-only/hmrc/email-admin/process-email-queue").post("").futureValue.status must be(
        200))
    wsClient.url(s"$mailgunStubUrl/v2/reset").get().futureValue
  }

  def emails: Future[List[Email]] = {
    val resp = wsClient.url(s"$mailgunStubUrl/v2/email").get()
    resp.futureValue.status must be(200)
    resp.map(r => r.json.as[List[Email]])
  }

  def verificationTokenFromEmail() = {
    val emailList = Await.result(emails, timeout)

    val regex = "/sa/print-preferences/verification/([-a-f0-9]+)".r

    val token: Option[Match] = regex.findFirstMatchIn(emailList.head.text.get)
    token.map(matches => matches.group(1)).get
  }

  def verificationTokenFromMultipleEmailsFor(emailRecipient: String) = {
    val emailList = Await.result(emails, timeout)
    val emailMatchedList = emailList.filter(x => x.to.contains(emailRecipient))

    val regex = "/sa/print-preferences/verification/([-a-f0-9]+)".r

    val token: Option[Match] = regex.findFirstMatchIn(emailMatchedList.head.text.get)
    token.map(matches => matches.group(1)).get
  }

}

object EmailSupport {
  //TODO simplify this type
  case class Email(
    from: String,
    to: Option[String],
    subject: String,
    text: Option[String],
    html: Option[String],
    cc: Option[String],
    bcc: Option[String])
  case class Token(token: String)
}
