package controllers.paye.validation

import uk.gov.hmrc.common.BaseSpec
import play.api.mvc.Request
import uk.gov.hmrc.common.microservice.domain.{RegimeRoots, User}
import scala.Int
import uk.gov.hmrc.common.microservice.paye.domain.{PayeRoot, TaxYearData}
import org.scalatest.mock.MockitoSugar
import play.api.test.{FakeApplication, WithApplication, FakeRequest}
import uk.gov.hmrc.common.microservice.auth.domain.{Regimes, UserAuthority}
import play.api.test.Helpers._
import controllers.paye.routes
import scala.concurrent.Future

class WithValidVersionNumberSpec extends BaseSpec  {
  val validVersion: Int = 22
  val invalidVersion: Int = 21
  val payeRoot = PayeRoot("", validVersion, "", "", None, "", "", "", Map(), Map(), Map())
  val ua = UserAuthority("dummyAuth", Regimes())
  val user = User("dummy", ua, RegimeRoots(paye = Some(payeRoot)))

  "The WithValidVersionNumber wrapper" should {
    "delegate to the wrapped action when the session version matches the user version" in new WithApplication(FakeApplication()) {

      object results extends play.api.mvc.Results {
        def ok(uri: String) = Ok(uri)
      }

      val successBody = "successfulResult"
      val successfulResult = Future.successful(results.ok(successBody))

      def action(user: User, request: Request[_], taxYear: Int, sequenceNumber: Int) = { successfulResult }

      val f = WithValidVersionNumber(action)
      val result = f(user, FakeRequest().withSession((WithValidVersionNumber.npsVersionKey, validVersion.toString)), 0, 0)
      status(result) shouldBe 200
      contentAsString(result) shouldBe successBody
    }

    "redirect to the home page when the version is not present in the session" in new WithApplication(FakeApplication()) {
      val result = WithValidVersionNumber(dummyAction)(user, FakeRequest().withSession(("foo", "bar")), 0, 0)

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.CarBenefitHomeController.carBenefitHome().url)
    }

    "redirect to an error page when the version in the session does not match the version of the user" in new WithApplication(FakeApplication()) {
      val result = WithValidVersionNumber(dummyAction)(user, FakeRequest().withSession((WithValidVersionNumber.npsVersionKey, invalidVersion.toString)), 0, 0)

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some(routes.VersionChangedController.versionChanged().url)
    }
  }
  def dummyAction(user: User, request: Request[_], taxYear: Int, sequenceNumber: Int) = { ??? }
}
