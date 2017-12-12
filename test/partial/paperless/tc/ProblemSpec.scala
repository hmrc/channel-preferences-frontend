package partial.paperless.tc

import _root_.helpers.ConfigHelper
import org.jsoup.Jsoup
import org.scalatestplus.play.OneAppPerSuite
import play.api.Application
import play.api.i18n.Messages.Implicits.applicationMessages
import uk.gov.hmrc.play.test.UnitSpec
import html.problem
import play.api.data.FormError
import play.api.test.FakeRequest

class ProblemSpec extends UnitSpec with OneAppPerSuite {

  override implicit lazy val app: Application = ConfigHelper.fakeApp

  "problem partial" should {
    "render the correct content in english" in {
      val errors = Seq((FormError("ErrorKey", Seq("Error Message"), Seq()), "Outer Error Message"))
      val document = Jsoup.parse(problem(errors)(FakeRequest(), applicationMessages).toString())

      document.getElementById("error-summary-heading").text() shouldBe "There is a problem"
    }
  }
}