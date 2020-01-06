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
import org.scalatest.matchers.{ HavePropertyMatchResult, HavePropertyMatcher, Matcher }
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse

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

  val emptyJsonValue = Json.parse("{}")

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

  val `/sa/print-preferences/verification` = new {
    def verify(token: String) = wsUrl(s"/sa/print-preferences/verification/$token").get()
  }

  def withReceivedEmails(expectedCount: Int)(assertions: List[Email] => Unit) {
    val listOfMails = eventually {
      val emailList = emails.futureValue
      emailList must have size expectedCount
      emailList
    }
    assertions(listOfMails)
  }

  def aVerificationEmailIsReceivedFor(email: String) {
    withReceivedEmails(1) {
      case List(mail) =>
        mail must have(
          'to (Some(email)),
          'subject ("HMRC electronic communications: verify your email address")
        )
    }
  }

  def beForAnExpiredOldEmail: Matcher[Future[WSResponse]] =
    have(statusWith(200)) and
      have(bodyWith("You&#x27;ve used a link that has now expired")) and
      have(bodyWith("It may have been sent to an old or alternative email address.")) and
      have(bodyWith("Please use the link in the latest verification email sent to your specified email address."))

  def bodyWith(expected: String) = new HavePropertyMatcher[Future[WSResponse], String] {
    def apply(response: Future[WSResponse]) = HavePropertyMatchResult(
      matches = response.futureValue.body.contains(expected),
      propertyName = "Response Body",
      expectedValue = expected,
      actualValue = response.futureValue.body
    )
  }
  def statusWith(expected: Int) = new HavePropertyMatcher[Future[WSResponse], Int] {
    def apply(response: Future[WSResponse]) = HavePropertyMatchResult(
      matches = response.futureValue.status.equals(expected),
      propertyName = "Response Status",
      expectedValue = expected,
      actualValue = response.futureValue.status
    )
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
