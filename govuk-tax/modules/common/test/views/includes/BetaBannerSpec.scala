package views.includes

import uk.gov.hmrc.common.BaseSpec
import views.html.includes.betaBanner
import org.jsoup.Jsoup
import play.api.test.Helpers._
import views.utils.JsoupDocumentExtensions.JsoupDocumentWrapper
class BetaBannerSpec extends BaseSpec {

  "Beta Banner" should {
    "have feedback link" in {
      val doc = Jsoup.parse(contentAsString(betaBanner(None)))
      doc.elementTextForId("feedback-link") shouldBe Some("feedback")
    }
  }
}


