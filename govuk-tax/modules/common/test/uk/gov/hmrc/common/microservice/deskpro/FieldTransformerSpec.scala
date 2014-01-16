package uk.gov.hmrc.common.microservice.deskpro

import uk.gov.hmrc.common.BaseSpec
import uk.gov.hmrc.common.microservice.deskpro.domain.{FieldTransformer, Ticket}
import play.api.test.FakeRequest
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.common.microservice.vat.domain.VatRoot
import uk.gov.hmrc.domain.Vrn
import uk.gov.hmrc.common.microservice.domain.{RegimeRoots, User}
import uk.gov.hmrc.common.microservice.auth.domain.{CreationAndLastModifiedDetail, Accounts, Credentials, Authority}
import controllers.common.actions.HeaderCarrier

class FieldTransformerSpec extends BaseSpec {

  "Field Transformer" should {

    "transforms javascript not enabled" in new FieldTransformerScope {
      transformer.ynValueOf(false) shouldBe "N"
    }

    "transforms javascript  enabled" in new FieldTransformerScope {
      transformer.ynValueOf(true) shouldBe "Y"
    }

    "transforms user to area of tax equals Business Tax" in new FieldTransformerScope {
      transformer.areaOfTaxOf(Some(bizTaxUser)) shouldBe "biztax"
    }

    "transforms user to area of tax equals PAYE" in new FieldTransformerScope {
      transformer.areaOfTaxOf(Some(user)) shouldBe "paye"
    }

    "transforms no user to an unknown area of tax" in new FieldTransformerScope {
      transformer.areaOfTaxOf(None) shouldBe "n/a"
    }

    "transforms userId in the header carrier to user id" in new FieldTransformerScope {
      transformer.userIdFrom(hc) shouldBe userId
    }

    "transforms no userId in the header carrier to n/a" in new FieldTransformerScope {
      transformer.userIdFrom(hc.copy(userId = None)) shouldBe "n/a"
    }

    "transforms  sessionId in the header carrier to session id" in new FieldTransformerScope {
      transformer.sessionIdFrom(hc) shouldBe sessionId
    }

    "transforms no sessionId in the header carrier to n/a" in new FieldTransformerScope {
      transformer.sessionIdFrom(hc.copy(sessionId = None)) shouldBe "n/a"
    }

    "transforms user agent in the request headers to user agent" in new FieldTransformerScope {
      transformer.userAgentOf(request) shouldBe userAgent
    }

    "transforms no user agent in the request headers to n/a" in new FieldTransformerScope {
      transformer.userAgentOf(FakeRequest()) shouldBe "n/a"
    }
  }

}

class FieldTransformerScope {
  val transformer = new FieldTransformer {}


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

