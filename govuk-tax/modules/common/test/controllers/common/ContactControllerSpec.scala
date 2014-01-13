package controllers.common

import uk.gov.hmrc.common.BaseSpec
import play.api.test.{FakeRequest, WithApplication}
import play.api.test.Helpers._
import org.jsoup.Jsoup
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.deskpro.HmrcDeskproConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import play.api.test.FakeApplication
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.common.microservice.domain.{RegimeRoots, User}
import uk.gov.hmrc.common.microservice.auth.domain.{CreationAndLastModifiedDetail, Accounts, Credentials, Authority}
import uk.gov.hmrc.utils.DateTimeUtils
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot

class ContactControllerSpec extends BaseSpec with MockitoSugar {

  private abstract class WithContactController extends WithApplication(FakeApplication()) {
    lazy val controller = new ContactController(mock[AuditConnector], mock[HmrcDeskproConnector])(mock[AuthConnector])
  }

  "Contact page" should {

    "return 404 if not authenticated" in  new WithContactController {
      private val index = controller.index(FakeRequest().withSession(SessionKeys.lastRequestTimestamp -> DateTimeUtils.now.getMillis.toString))
      status(index) shouldBe 404
    }

    "inject the referer if available" in new WithContactController {
      val user = {
        val root = PayeRoot("nino", "mr", "John", None, "Densmore", "JD", "DOB", Map.empty, Map.empty, Map.empty)
        User("123", Authority("/auth/oid/123", Credentials(), Accounts(), None, None, CreationAndLastModifiedDetail()), RegimeRoots(Some(root)))
      }
      private val index = controller.renderIndex(user, FakeRequest().withHeaders("Referer" -> "SomeURL"))
      val page = Jsoup.parse(contentAsString(index))

      page.getElementById("referer").attr("value") shouldBe "SomeURL"

    }

    "verify that all fields are set" in new WithContactController {
      val user = {
        val root = PayeRoot("nino", "mr", "John", None, "Densmore", "JD", "DOB", Map.empty, Map.empty, Map.empty)
        User("123", Authority("/auth/oid/123", Credentials(), Accounts(), None, None, CreationAndLastModifiedDetail()), RegimeRoots(Some(root)))
      }
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

      val user = User("123", Authority("/auth/oid/123", Credentials(), Accounts(), None, None, CreationAndLastModifiedDetail()), RegimeRoots())
      val ticket =  controller.createTicket(ContactForm("My Name", "my@email.com", "TP Rocks!", false, "SomeUrl"))(user, request)

      ticket.name shouldBe "My Name"
      ticket.email shouldBe "my@email.com"
      ticket.message shouldBe "TP Rocks!"
      ticket.javascriptEnabled shouldBe "N"
      ticket.referrer shouldBe "SomeUrl"
      ticket.userAgent shouldBe "UA"
      ticket.sessionId shouldBe "123"
      ticket.subject shouldBe "Contact form submission"
      ticket.areaOfTax shouldBe "biztax"

    }

  }

}
