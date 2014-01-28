package controllers.common.support

import uk.gov.hmrc.common.BaseSpec
import play.api.test.{FakeRequest, WithApplication}
import play.api.test.Helpers._
import org.jsoup.Jsoup
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.deskpro.HmrcDeskproConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.auth.domain.CreationAndLastModifiedDetail
import uk.gov.hmrc.utils.DateTimeUtils
import scala.concurrent.Future
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import scala.util.Random
import uk.gov.hmrc.common.microservice.auth.domain.Accounts
import uk.gov.hmrc.common.microservice.auth.domain.Authority
import scala.Some
import uk.gov.hmrc.common.microservice.auth.domain.Credentials
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import uk.gov.hmrc.common.microservice.deskpro.domain.TicketId
import controllers.common.SessionKeys
import org.mockito.Mockito._
import controllers.common.actions.HeaderCarrier
import org.mockito.Matchers

class ContactControllerSpec extends BaseSpec with MockitoSugar {

  private abstract class WithContactController extends WithApplication  {
    lazy val deskProConnector: HmrcDeskproConnector = mock[HmrcDeskproConnector]
    lazy val ticketCache = mock[TicketCache]

    lazy val controller = new ContactController(mock[AuditConnector], deskProConnector, ticketCache)(mock[AuthConnector])
    val user = {
      val root = PayeRoot("nino", "mr", "John", None, "Densmore", "JD", "DOB", Map.empty, Map.empty, Map.empty)
      User("123", Authority("/auth/oid/123", Credentials(), Accounts(), None, None, CreationAndLastModifiedDetail()), RegimeRoots(Some(root)))
    }
  }

  "Contact page" should {

    "redirect to the confirmation page when ticket is created" in new WithContactController {
      val ticket = Some(TicketId(123))

      when(ticketCache.stashTicket(Matchers.eq(ticket), Matchers.eq("ContactForm"))(Matchers.any[HeaderCarrier])).thenReturn(Future.successful("stored"))
      val submit = controller.redirectToThanks(Future.successful(ticket))(FakeRequest())

      status(submit) shouldBe 303
      submit.header.headers("Location") shouldBe "/contact-hmrc/thanks"
    }

    "return 404 if not authenticated" in new WithContactController {
      private val index = controller.index(FakeRequest().withSession(SessionKeys.lastRequestTimestamp -> DateTimeUtils.now.getMillis.toString))
      status(index) shouldBe 303
    }

    "inject the referer if available" in new WithContactController {

      private val index = controller.renderIndex(user, FakeRequest().withHeaders("Referer" -> "SomeURL"))
      val page = Jsoup.parse(contentAsString(index))

      page.getElementById("referer").attr("value") shouldBe "SomeURL"

    }

    "verify that all fields are set" in new WithContactController {

      val submit = controller.doSubmit(user, FakeRequest().withFormUrlEncodedBody("contact-name" -> "", "contact-email" -> "", "contact-comments" -> ""))
      val page = Jsoup.parse(contentAsString(submit))

      status(submit) shouldBe 400

      page.getElementsByClass("error-notification").size() shouldBe 3
      page.getElementsByClass("error-notification").get(0).text() shouldBe "Please provide your name"
      page.getElementsByClass("error-notification").get(1).text() shouldBe "Enter a valid email address"
      page.getElementsByClass("error-notification").get(2).text() shouldBe "Enter your comments."
    }

    "fail if the comment is longer than 2000 chars" in new WithContactController {
      val submit = controller.doSubmit(user, FakeRequest().withFormUrlEncodedBody("contact-name" -> "Name", "contact-email" -> "a@b.com", "contact-comments" -> Random.alphanumeric.take(2001).mkString))
      val page = Jsoup.parse(contentAsString(submit))

      status(submit) shouldBe 400

      page.getElementsByClass("error-notification").size() shouldBe 1
      page.getElementsByClass("error-notification").first().text() shouldBe "The comment cannot be longer than 2000 characters"
    }

    "fail if the name is longer than 70 chars" in new WithContactController {
      val submit = controller.doSubmit(user, FakeRequest().withFormUrlEncodedBody("contact-name" -> Random.alphanumeric.take(71).mkString, "contact-email" -> "a@b.com", "contact-comments" -> "A comment"))
      val page = Jsoup.parse(contentAsString(submit))

      status(submit) shouldBe 400

      page.getElementsByClass("error-notification").size() shouldBe 1
      page.getElementsByClass("error-notification").first().text() shouldBe "Your name cannot be longer than 70 characters"
    }

    "fail if the email is longer than 255 chars" in new WithContactController {
      val email = s"${Random.alphanumeric.take(250).mkString}@b.com"
      val submit = controller.doSubmit(user, FakeRequest().withFormUrlEncodedBody("contact-name" -> "Name", "contact-email" -> email, "contact-comments" -> "A comment"))
      val page = Jsoup.parse(contentAsString(submit))

      status(submit) shouldBe 400

      page.getElementsByClass("error-notification").size() shouldBe 1
      page.getElementsByClass("error-notification").first().text() shouldBe "The email cannot be longer than 255 characters"
    }

    "fail if the email has invalid syntax (for DeskPRO)" in new WithContactController {
      val submit = controller.doSubmit(user, FakeRequest().withFormUrlEncodedBody("contact-name" -> "Name", "contact-email" -> "a@b", "contact-comments" -> "A comment"))
      val page = Jsoup.parse(contentAsString(submit))

      status(submit) shouldBe 400

      page.getElementsByClass("error-notification").size() shouldBe 1
      page.getElementsByClass("error-notification").first().text() shouldBe "Enter a valid email address"
    }

  }

  "Contact Confirmation Page" should {
    "retrieve the ticket id from the keystore and display it" in new WithContactController {
      when(ticketCache.popTicket(Matchers.eq("ContactForm"))(Matchers.any[HeaderCarrier])).thenReturn(Future.successful("321"))
      val result = controller.doThanks(user, FakeRequest())

      val page = Jsoup.parse(contentAsString(result))

      status(result) shouldBe 200

      page.getElementById("ticketId").text() shouldBe "321"
    }
  }
}

