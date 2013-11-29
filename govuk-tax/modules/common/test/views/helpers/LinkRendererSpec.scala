package views.helpers

import uk.gov.hmrc.common.BaseSpec
import play.api.test.{FakeApplication, WithApplication}

class LinkRendererSpec extends BaseSpec {

  "render a non sso link" should {

    "render a link with href and text" in new WithApplication(FakeApplication()) {
      val linkMessage = RenderableLinkMessage(LinkMessage(id="someId", href = "someHref", text = "someText", sso = false))
      val result = linkMessage.render.toString().trim
      result shouldBe "<a id=\"someId\" href=\"someHref\" target=\"_self\" data-sso=\"false\">someText</a>"
    }

    "render a link with href, text and new window option enabled" in new WithApplication(FakeApplication()) {
      val linkMessage = RenderableLinkMessage(LinkMessage(href = "someHref", text = "someText", id="someId", newWindow = true, sso = false))
      val result = linkMessage.render.toString().trim
      result shouldBe "<a id=\"someId\" href=\"someHref\" target=\"_blank\" data-sso=\"false\">someText</a>"
    }

    "render a link with href, id and text" in new WithApplication(FakeApplication()) {
      val linkMessage = RenderableLinkMessage(LinkMessage(href = "someHref", text = "someText", id="someId", sso = false))
      val result = linkMessage.render.toString().trim
      result shouldBe "<a id=\"someId\" href=\"someHref\" target=\"_self\" data-sso=\"false\">someText</a>"
    }

    "render a link with href, id, text and new window option enabled" in new WithApplication(FakeApplication()) {
      val linkMessage = RenderableLinkMessage(LinkMessage(href = "someHref", text = "someText", id="someId", newWindow = true, sso = false))
      val result = linkMessage.render.toString().trim
      result shouldBe "<a id=\"someId\" href=\"someHref\" target=\"_blank\" data-sso=\"false\">someText</a>"
    }

  }

  "render an sso link" should {

    "render an sso link with href, id and text" in new WithApplication(FakeApplication()) {
      val linkMessage = RenderableLinkMessage(LinkMessage("someHref", "someText", "someId", false, true))
      val result = linkMessage.render.toString().trim
      result shouldBe "<a id=\"someId\" href=\"someHref\" target=\"_self\" data-sso=\"true\">someText</a>"
    }

    "render an sso link with href, id, text and new window option enabled" in new WithApplication(FakeApplication()) {
      val linkMessage = RenderableLinkMessage(LinkMessage("someHref", "someText", "someId", true, true))
      val result = linkMessage.render.toString().trim
      result shouldBe "<a id=\"someId\" href=\"someHref\" target=\"_blank\" data-sso=\"true\">someText</a>"
    }

  }

}
