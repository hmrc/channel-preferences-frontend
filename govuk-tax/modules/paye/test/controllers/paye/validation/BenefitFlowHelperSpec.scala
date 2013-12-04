package controllers.paye.validation

import uk.gov.hmrc.common.BaseSpec
import uk.gov.hmrc.common.microservice.domain.{RegimeRoots, User}
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import play.api.test.{FakeApplication, WithApplication, FakeRequest}
import uk.gov.hmrc.common.microservice.auth.domain.{Regimes, UserAuthority}
import controllers.paye.routes

import BenefitFlowHelper._

class BenefitFlowHelperSpec extends BaseSpec {
  val validVersion: Int = 22
  val invalidVersion: Int = 21
  val payeRoot = PayeRoot("", validVersion, "", "", None, "", "", "", Map(), Map(), Map())
  val ua = UserAuthority("dummyAuth", Regimes())
  val user = User("dummy", ua, RegimeRoots(paye = Some(payeRoot)))

  "validateVersionNumber" should {
    "return None to indicate the version check passed" in new WithApplication(FakeApplication()) {
      val session = FakeRequest().withSession((npsVersionKey, validVersion.toString)).session
      val result = validateVersionNumber(user, session)
      result.right.toOption.isDefined shouldBe true
    }

    "redirect to the home page when the version is not present in the session" in new WithApplication(FakeApplication()) {
      val session = FakeRequest().withSession(("foo", "bar")).session
      val result = validateVersionNumber(user, session)

      result.left.toOption.isDefined shouldBe true
      result.left.map{result =>
        result.header.status shouldBe 303
        result.header.headers.get("Location") shouldBe Some(routes.CarBenefitHomeController.carBenefitHome().url)
      }
    }

    "redirect to an error page when the version in the session does not match the version of the user" in new WithApplication(FakeApplication()) {
      val session = FakeRequest().withSession((npsVersionKey, invalidVersion.toString)).session
      val result = validateVersionNumber(user, session)

      result.left.toOption.isDefined shouldBe true
      result.left.map{result =>
        result.header.status shouldBe 303
        result.header.headers.get("Location") shouldBe Some(routes.VersionChangedController.versionChanged().url)
      }
    }
  }
}
