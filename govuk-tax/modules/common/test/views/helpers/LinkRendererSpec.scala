package views.helpers

import uk.gov.hmrc.common.BaseSpec
import play.api.test.{FakeApplication, WithApplication}
import RenderableMessageProperty.Link._

class LinkRendererSpec extends BaseSpec {

  "render a non sso link" should {

    "render a link with href and text" in new WithApplication(FakeApplication()) {
      val linkMessage = RenderableLinkMessage(LinkMessage(href = "someHref", text = "someText", sso = false))
      val result = linkMessage.render.toString().trim
      result shouldBe "<A href=\"someHref\">someText</A>"
    }

    "render a link with href, text and postlinkText" in new WithApplication(FakeApplication()) {
      val linkMessage = RenderableLinkMessage(LinkMessage(href = "someHref", text = "someText", id = None, newWindow = false, postLinkText = Some("somePostLinkText"), sso = false))
      val result = linkMessage.render.toString().trim
      result shouldBe "<A href=\"someHref\">someText</A> somePostLinkText"
    }

    "render a link with href, text and new window option enabled" in new WithApplication(FakeApplication()) {
      val linkMessage = RenderableLinkMessage(LinkMessage(href = "someHref", text = "someText", id = None, newWindow = true, sso = false))
      val result = linkMessage.render.toString().trim
      result shouldBe "<A href=\"someHref\" target=\"_blank\">someText</A>"
    }

    "render a link with href, text, postLinkText and new window option enabled" in new WithApplication(FakeApplication()) {
      val linkMessage = RenderableLinkMessage(LinkMessage(href = "someHref", text = "someText", id = None, newWindow = true, postLinkText = Some("somePostLinkText"), sso = false))
      val result = linkMessage.render.toString().trim
      result shouldBe "<A href=\"someHref\" target=\"_blank\">someText</A> somePostLinkText"
    }

    "render a link with href, id and text" in new WithApplication(FakeApplication()) {
      val linkMessage = RenderableLinkMessage(LinkMessage(href = "someHref", text = "someText", id = Some("someId"), sso = false))
      val result = linkMessage.render.toString().trim
      result shouldBe "<A href=\"someHref\" id=\"someId\">someText</A>"
    }

    "render a link with href, id, text and postLinkText" in new WithApplication(FakeApplication()) {
      val linkMessage = RenderableLinkMessage(LinkMessage(href = "someHref", text = "someText", id = Some("someId"), newWindow = false, postLinkText = Some("somePostLinkText"), sso = false))
      val result = linkMessage.render.toString().trim
      result shouldBe "<A href=\"someHref\" id=\"someId\">someText</A> somePostLinkText"
    }

    "render a link with href, id, text and new window option enabled" in new WithApplication(FakeApplication()) {
      val linkMessage = RenderableLinkMessage(LinkMessage(href = "someHref", text = "someText", id = Some("someId"), newWindow = true, sso = false))
      val result = linkMessage.render.toString().trim
      result shouldBe "<A href=\"someHref\" id=\"someId\" target=\"_blank\">someText</A>"
    }

    "render a link with href, id, text, postLinkText and new window option enabled" in new WithApplication(FakeApplication()) {
      val linkMessage = RenderableLinkMessage(LinkMessage(href = "someHref", text = "some > Text", id = Some("someId"), newWindow = true, postLinkText = Some("somePostLinkText"), sso = false))
      val result = linkMessage.render.toString().trim
      result shouldBe "<A href=\"someHref\" id=\"someId\" target=\"_blank\">some &gt; Text</A> somePostLinkText"
    }
  }

  "render an sso link" should {

    "render an sso link with href and text" in new WithApplication(FakeApplication()) {
      val linkMessage = RenderableLinkMessage(LinkMessage("someHref", "someText", None, false, None, true))
      val result = linkMessage.render.toString().trim
      result shouldBe "<A href=\"someHref\" data-sso=\"true\">someText</A>"
    }

    "render an sso link with href, text and postlinkText" in new WithApplication(FakeApplication()) {
      val linkMessage = RenderableLinkMessage(LinkMessage("someHref", "someText", None, false, Some("somePostLinkText"), true))
      val result = linkMessage.render.toString().trim
      result shouldBe "<A href=\"someHref\" data-sso=\"true\">someText</A> somePostLinkText"
    }

    "render an sso link with href, text and new window option enabled" in new WithApplication(FakeApplication()) {
      val linkMessage = RenderableLinkMessage(LinkMessage("someHref", "someText", None, true, None, true))
      val result = linkMessage.render.toString().trim
      result shouldBe "<A href=\"someHref\" target=\"_blank\" data-sso=\"true\">someText</A>"
    }

    "render an sso link with href, text, postLinkText and new window option enabled" in new WithApplication(FakeApplication()) {
      val linkMessage = RenderableLinkMessage(LinkMessage("someHref", "someText", None, true, Some("somePostLinkText"), true))
      val result = linkMessage.render.toString().trim
      result shouldBe "<A href=\"someHref\" target=\"_blank\" data-sso=\"true\">someText</A> somePostLinkText"
    }

    "render an sso link with href, id and text" in new WithApplication(FakeApplication()) {
      val linkMessage = RenderableLinkMessage(LinkMessage("someHref", "someText", Some("someId"), false, None, true))
      val result = linkMessage.render.toString().trim
      result shouldBe "<A href=\"someHref\" id=\"someId\" data-sso=\"true\">someText</A>"
    }

    "render an sso link with href, id, text and postLinkText" in new WithApplication(FakeApplication()) {
      val linkMessage = RenderableLinkMessage(LinkMessage("someHref", "someText", Some("someId"), false, Some("somePostLinkText"), true))
      val result = linkMessage.render.toString().trim
      result shouldBe "<A href=\"someHref\" id=\"someId\" data-sso=\"true\">someText</A> somePostLinkText"
    }

    "render an sso link with href, id, text and new window option enabled" in new WithApplication(FakeApplication()) {
      val linkMessage = RenderableLinkMessage(LinkMessage("someHref", "someText", Some("someId"), true, None, true))
      val result = linkMessage.render.toString().trim
      result shouldBe "<A href=\"someHref\" id=\"someId\" target=\"_blank\" data-sso=\"true\">someText</A>"
    }

    "render an sso link with href, id, text, postLinkText and new window option enabled" in new WithApplication(FakeApplication()) {
      val linkMessage = RenderableLinkMessage(LinkMessage("someHref", "someText", Some("someId"), true, Some("somePostLinkText"), true))
      val result = linkMessage.render.toString().trim
      result shouldBe "<A href=\"someHref\" id=\"someId\" target=\"_blank\" data-sso=\"true\">someText</A> somePostLinkText"
    }

    "render a link with overridden values " should {

      "render a link and override the text value with the one specified" in new WithApplication(FakeApplication()) {
        val linkMessage = RenderableLinkMessage(LinkMessage("someHref", "someText", sso = false))
        val textToUse = "someTextToUseToOverride"
        val result = linkMessage.set((TEXT, textToUse)).render.toString().trim
        result shouldBe "<A href=\"someHref\">someTextToUseToOverride</A>"
      }

      "render a link and override the id value with the one specified" in new WithApplication(FakeApplication()) {
        val linkMessage = RenderableLinkMessage(LinkMessage("someHref", "someText", id = Some("defaultId"), sso = false))
        val idToUse = "someIdToUseToOverride"
        val result = linkMessage.set((ID, idToUse)).render.toString().trim
        result shouldBe "<A href=\"someHref\" id=\"someIdToUseToOverride\">someText</A>"
      }

      "render a link and override the id and the text values with the ones specified" in new WithApplication(FakeApplication()) {
        val linkMessage = RenderableLinkMessage(LinkMessage("someHref", "someText", id = Some("defaultId"), sso = false))
        val textToUse = "someTextToUseToOverride"
        val idToUse = "someIdToUseToOverride"
        val result = linkMessage.set((ID, idToUse)).set((TEXT, textToUse)).render.toString().trim
        result shouldBe "<A href=\"someHref\" id=\"someIdToUseToOverride\">someTextToUseToOverride</A>"
      }

      "render a link and override the id value with the one specified also if there is no default id" in new WithApplication(FakeApplication()) {
        val linkMessage = RenderableLinkMessage(LinkMessage("someHref", "someText", sso = false))
        val idToUse = "someIdToUseToOverride"
        val result = linkMessage.set((ID, idToUse)).render.toString().trim
        result shouldBe "<A href=\"someHref\" id=\"someIdToUseToOverride\">someText</A>"
      }

      "render a link and override the id and the text values with the ones specified also if there is no default id" in new WithApplication(FakeApplication()) {
        val linkMessage = RenderableLinkMessage(LinkMessage("someHref", "someText", sso = false))
        val textToUse = "someTextToUseToOverride"
        val idToUse = "someIdToUseToOverride"
        val result = linkMessage.set((ID, "someIdToUseToOverride")).set((TEXT, textToUse)).render.toString().trim
        result shouldBe "<A href=\"someHref\" id=\"someIdToUseToOverride\">someTextToUseToOverride</A>"
      }
    }
  }

}
