import org.scalatest.Matchers._
import org.scalatest.concurrent.Eventually
import play.api.libs.json.Json
import play.api.libs.ws.WS
import play.api.mvc.Results.EmptyContent
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.test.ResponseMatchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.matching.Regex.Match

trait EmailSupport extends ResponseMatchers with Eventually with ServicesConfig {

  import EmailSupport._

import scala.concurrent.duration._

  implicit val emailReads = Json.reads[Email]
  implicit val emailTokenWrites = Json.writes[Token]

  private implicit lazy val app = play.api.Play.current

  private lazy val mailgunStubUrl = baseUrl("mailgun")
  private lazy val emailBaseUrl = baseUrl("email")
  private lazy val prefsBaseUrl = baseUrl("preferences")
  private lazy val timeout = 5.seconds

  def clearEmails() = {
    eventually(WS.url(s"$emailBaseUrl/email-admin/process-email-queue").post(EmptyContent()) should have (status (200)))
    Await.result(WS.url(s"$mailgunStubUrl/v2/reset").get(), timeout)
  }

  def emails: Future[List[Email]] = {
    val resp = WS.url(s"$mailgunStubUrl/v2/email").get()
    resp.futureValue.status should be(200)
    resp.map(r => r.json.as[List[Email]])
  }

  def  verificationTokenFromEmail() = {
    val emailList = Await.result(emails, timeout)

    val regex = "/sa/print-preferences/verification/([-a-f0-9]+)".r

    val token: Option[Match] = regex.findFirstMatchIn(emailList.head.text.get)
    token.map(matches => matches.group(1)).get
  }

  def  verificationTokenFromMultipleEmailsFor(emailRecipient: String) = {
    val emailList = Await.result(emails, timeout)
    val emailMatchedList = emailList.filter(x => x.to.contains(emailRecipient))

    val regex = "/sa/print-preferences/verification/([-a-f0-9]+)".r

    val token: Option[Match] = regex.findFirstMatchIn(emailMatchedList.head.text.get)
    token.map(matches => matches.group(1)).get
  }

}

object EmailSupport {
  //TODO simplify this type
  case class Email(from: String, to: Option[String], subject: String, text: Option[String], html: Option[String], cc: Option[String], bcc: Option[String])
  case class Token(token: String)
}




