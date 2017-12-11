package views

import _root_.helpers.{ConfigHelper, TestFixtures}
import controllers.AuthorityUtils.saAuthority
import controllers.internal.OptInCohort
import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.mvc.Call
import play.api.test.FakeRequest
import uk.gov.hmrc.play.frontend.auth.AuthContext
import uk.gov.hmrc.play.test.UnitSpec
import views.html.sa_printing_preference_verify_email

class SaPrintingPreferenceVerifyEmailSpec extends UnitSpec with OneAppPerSuite {

  override implicit lazy val app: Application = ConfigHelper.fakeApp

  "preference print template" should {
    "render the correct content for the IPage cohort " in {
      val email = "a@a.com"
      val user = AuthContext(authority = saAuthority("userId", "1234567890"), nameFromSession = Some("Ciccio"), governmentGatewayToken = None)

      val document = Jsoup.parse(sa_printing_preference_verify_email(email, OptInCohort.fromId(8).get, Call("GET", "/"), "redirectUrl")(user, FakeRequest("GET", "/"), applicationMessages, TestFixtures.sampleHostContext).toString())

      document.getElementsByTag("title").get(0).text() shouldBe "Check your email address"
      document.getElementById("email-is-not-correct-link").text() shouldBe "Change this email address"
    }
  }
}
