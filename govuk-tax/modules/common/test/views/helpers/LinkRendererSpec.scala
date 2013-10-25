package views.helpers

import uk.gov.hmrc.common.BaseSpec
import play.api.test.{FakeApplication, WithApplication}

class LinkRendererSpec extends BaseSpec {

  "render link" should {

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
      val linkMessage = RenderableLinkMessage(LinkMessage("someHref", "someText", Some("someId"), true, Some("somePostLinkText")))
      val result = linkMessage.render.toString().trim
      result shouldBe "<A href=\"someHref\" id=\"someId\" target=\"_blank\">someText</A> somePostLinkText"
    }

  }

}
