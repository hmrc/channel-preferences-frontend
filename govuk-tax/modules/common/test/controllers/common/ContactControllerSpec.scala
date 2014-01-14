package controllers.common

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
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import org.mockito.Mockito._
import org.mockito.Matchers.any
import org.mockito.Matchers.{eq => meq}
import scala.concurrent.Future
import scala.concurrent.Future._
import controllers.common.actions.HeaderCarrier
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.common.microservice.auth.domain.Accounts
import uk.gov.hmrc.common.microservice.auth.domain.Authority
import scala.Some
import uk.gov.hmrc.common.microservice.auth.domain.Credentials
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import uk.gov.hmrc.common.microservice.deskpro.Ticket
import play.api.test.FakeApplication
import uk.gov.hmrc.common.microservice.deskpro.TicketId

class ContactControllerSpec extends BaseSpec with MockitoSugar {

  private abstract class WithContactController extends WithApplication(FakeApplication()) {
    lazy val deskProConnector: HmrcDeskproConnector = mock[HmrcDeskproConnector]
    lazy val keyStoreConnector = mock[KeyStoreConnector]
    lazy val controller = new ContactController(mock[AuditConnector], deskProConnector, keyStoreConnector)(mock[AuthConnector])
    val user = {
      val root = PayeRoot("nino", "mr", "John", None, "Densmore", "JD", "DOB", Map.empty, Map.empty, Map.empty)
      User("123", Authority("/auth/oid/123", Credentials(), Accounts(), None, None, CreationAndLastModifiedDetail()), RegimeRoots(Some(root)))
    }
  }

  type StoredTicket = Map[String, String]

  "Contact page" should {


    "redirect to the confirmation page when ticket is created" in new WithContactController {
      when(deskProConnector.createTicket(any[Ticket])(any[HeaderCarrier])).thenReturn(Future.successful(Some(TicketId(123))))

      import controller._


      private val keyStoreData: StoredTicket= Map(ticketKey -> "123")
      when(keyStoreConnector.addKeyStoreEntry[StoredTicket](meq(actionId), meq(source), meq(formId), meq(keyStoreData), meq(false))(any(classOf[Manifest[StoredTicket]]), any[HeaderCarrier])).thenReturn(successful(None))

      val submit = controller.doSubmit(user, FakeRequest().withFormUrlEncodedBody("contact-name" -> "Foo", "contact-email" -> "foo@bar.com", "contact-comments" -> "it works", "isJavascript" -> "true", "referer" -> "mozilllaaaaa"))
      val page = Jsoup.parse(contentAsString(submit))

      status(submit) shouldBe 303
      submit.header.headers("Location") shouldBe "/contact-hmrc/thanks"

    }


    "return 404 if not authenticated" in new WithContactController {
      private val index = controller.index(FakeRequest().withSession(SessionKeys.lastRequestTimestamp -> DateTimeUtils.now.getMillis.toString))
      status(index) shouldBe 404
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
      page.getElementsByClass("error-notification").get(0).text() shouldBe "Please provide your name."
      page.getElementsByClass("error-notification").get(1).text() shouldBe "Valid email required"
      page.getElementsByClass("error-notification").get(2).text() shouldBe "Please provide details"
    }

    "create a ticket when the form is correct" in new WithContactController {

      val request = FakeRequest().withHeaders("User-Agent" -> "UA").withSession("sessionId" -> "123")

      val ticket = controller.createTicket(ContactForm("My Name", "my@email.com", "TP Rocks!", false, "SomeUrl"))(user, request)

      ticket.name shouldBe "My Name"
      ticket.email shouldBe "my@email.com"
      ticket.message shouldBe "TP Rocks!"
      ticket.javascriptEnabled shouldBe "N"
      ticket.referrer shouldBe "SomeUrl"
      ticket.userAgent shouldBe "UA"
      ticket.sessionId shouldBe "123"
      ticket.subject shouldBe "Contact form submission"
      ticket.areaOfTax shouldBe "paye"

    }

  }

  "Contact Confirmation Page" should {
    "retrieve the ticket id from the keystore and display it" in new WithContactController {

      import controller._
      private val keyStoreData: StoredTicket= Map(ticketKey -> "123")
      when(keyStoreConnector.getEntry[StoredTicket](meq(actionId), meq(source), meq(formId), meq(false))(any(classOf[Manifest[StoredTicket]]), any[HeaderCarrier])).
        thenReturn(successful(Some(keyStoreData)))

      val result = controller.doThanks(user, FakeRequest())

      val page = Jsoup.parse(contentAsString(result))

      status(result) shouldBe 200

      page.getElementById("ticketId").text() shouldBe "123"
    }
  }

}
