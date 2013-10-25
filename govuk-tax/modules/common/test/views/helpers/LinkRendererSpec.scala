package views.helpers

import uk.gov.hmrc.common.BaseSpec
import play.api.test.{FakeApplication, WithApplication}

class LinkRendererSpec extends BaseSpec {

  "render a non sso link" should {

    "render a link with href and text" in new WithApplication(FakeApplication()) {
      val linkMessage = RenderableLinkMessage(LinkMessage("someHref", "someText"))
      val result = linkMessage.render.toString().trim
      result shouldBe "<A href=\"someHref\">someText</A>"
    }

    "render a link with href, text and postlinkText" in new WithApplication(FakeApplication()) {
      val linkMessage = RenderableLinkMessage(LinkMessage("someHref", "someText", None, false, Some("somePostLinkText")))
      val result = linkMessage.render.toString().trim
      result shouldBe "<A href=\"someHref\">someText</A> somePostLinkText"
    }

    "render a link with href, text and new window option enabled" in new WithApplication(FakeApplication()) {
      val linkMessage = RenderableLinkMessage(LinkMessage("someHref", "someText", None, true))
      val result = linkMessage.render.toString().trim
      result shouldBe "<A href=\"someHref\" target=\"_blank\">someText</A>"
    }

    "render a link with href, text, postLinkText and new window option enabled" in new WithApplication(FakeApplication()) {
      val linkMessage = RenderableLinkMessage(LinkMessage("someHref", "someText", None, true, Some("somePostLinkText")))
      val result = linkMessage.render.toString().trim
      result shouldBe "<A href=\"someHref\" target=\"_blank\">someText</A> somePostLinkText"
    }

    "render a link with href, id and text" in new WithApplication(FakeApplication()) {
      val linkMessage = RenderableLinkMessage(LinkMessage("someHref", "someText", Some("someId")))
      val result = linkMessage.render.toString().trim
      result shouldBe "<A href=\"someHref\" id=\"someId\">someText</A>"
    }

    "render a link with href, id, text and postLinkText" in new WithApplication(FakeApplication()) {
      val linkMessage = RenderableLinkMessage(LinkMessage("someHref", "someText", Some("someId"), false, Some("somePostLinkText")))
      val result = linkMessage.render.toString().trim
      result shouldBe "<A href=\"someHref\" id=\"someId\">someText</A> somePostLinkText"
    }

    "render a link with href, id, text and new window option enabled" in new WithApplication(FakeApplication()) {
      val linkMessage = RenderableLinkMessage(LinkMessage("someHref", "someText", Some("someId"), true))
      val result = linkMessage.render.toString().trim
      result shouldBe "<A href=\"someHref\" id=\"someId\" target=\"_blank\">someText</A>"
    }

    "render a link with href, id, text, postLinkText and new window option enabled" in new WithApplication(FakeApplication()) {
      val linkMessage = RenderableLinkMessage(LinkMessage("someHref", "some > Text", Some("someId"), true, Some("somePostLinkText")))
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

  }

}
