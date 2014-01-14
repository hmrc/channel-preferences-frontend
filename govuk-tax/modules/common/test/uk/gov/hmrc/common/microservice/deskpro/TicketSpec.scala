package uk.gov.hmrc.common.microservice.deskpro

import uk.gov.hmrc.common.BaseSpec
import controllers.common.actions.HeaderCarrier
import play.api.test.FakeRequest
import uk.gov.hmrc.common.microservice.domain.{RegimeRoots, User}
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.common.microservice.auth.domain.{CreationAndLastModifiedDetail, Accounts, Credentials, Authority}
import uk.gov.hmrc.common.microservice.vat.domain.VatRoot
import uk.gov.hmrc.domain.Vrn

class TicketSpec extends BaseSpec {

  "Ticket constructor" should {
    "create a Ticket" in new TicketScope {
      val ticket = Ticket(name, email, subject, message, referrer, true, hc, request, Some(user))

      ticket.name shouldBe name
      ticket.email shouldBe email
      ticket.subject shouldBe subject
      ticket.message shouldBe message
      ticket.referrer shouldBe referrer
      ticket.javascriptEnabled shouldBe "Y"
      ticket.areaOfTax shouldBe "paye"
      ticket.authId shouldBe userId
      ticket.sessionId shouldBe sessionId
      ticket.userAgent shouldBe userAgent
    }

    "create a Ticket without javascript" in new TicketScope {
      val ticket = Ticket(name, email, subject, message, referrer, false, hc, request, Some(user))
      ticket.javascriptEnabled shouldBe "N"
    }

    "create a Ticket with area of tax equals Business Tax" in new TicketScope {
      val ticket = Ticket(name, email, subject, message, referrer, false, hc, request, Some(bizTaxUser))
      ticket.areaOfTax shouldBe "biztax"
    }

    "create a Ticket without a user and so an unknown area of tax" in new TicketScope {
      val ticket = Ticket(name, email, subject, message, referrer, false, hc, request, None)
      ticket.areaOfTax shouldBe "n/a"
    }

    "create a Ticket with no userId in the header carrier" in new TicketScope {
      val ticket = Ticket(name, email, subject, message, referrer, false, hc.copy(userId = None), request, Some(bizTaxUser))
      ticket.authId shouldBe "n/a"
    }

    "create a Ticket with no sessionId in the header carrier" in new TicketScope {
      val ticket = Ticket(name, email, subject, message, referrer, false, hc.copy(sessionId = None), request, Some(bizTaxUser))
      ticket.sessionId shouldBe "n/a"
    }

    "create a Ticket with no user agent in the request headers" in new TicketScope {
      val ticket = Ticket(name, email, subject, message, referrer, false, hc, FakeRequest(), Some(bizTaxUser))
      ticket.userAgent shouldBe "n/a"
    }
  }

}

class TicketScope {
  val root = PayeRoot("nino", "mr", "John", None, "Densmore", "JD", "DOB", Map.empty, Map.empty, Map.empty)
  val bizTaxRoot = VatRoot(Vrn("134123421"), Map.empty[String, String])
  val userId: String = "456"
  val user = User(userId, Authority(s"/auth/oid/$userId", Credentials(), Accounts(), None, None, CreationAndLastModifiedDetail()), RegimeRoots(Some(root)))
  val bizTaxUser = user.copy(regimes = RegimeRoots(vat = Some(bizTaxRoot)))
  val sessionId: String = "sessionIdValue"
  val hc = HeaderCarrier(userId = Some(user.userId), sessionId = Some(sessionId))
  val userAgent: String = "Mozilla"
  val name: String = "name"
  val email: String = "email"
  val subject: String = "subject"
  val message: String = "message"
  val referrer: String = "referer"
  val request = FakeRequest().withHeaders(("User-Agent", userAgent))
}
