package controllers.bt.otherServices

import uk.gov.hmrc.common.BaseSpec
import controllers.bt.otherservices.ManageTaxesLink
import play.api.test.{WithApplication, FakeApplication}
import views.helpers.LinkMessage

class ManageTaxesLinkSpec extends BaseSpec {

  val dummyPortalUrlBuilder = (x: String) => x

  "buildLinks" should {

    "build sso link" in new WithApplication(FakeApplication()) {
      val ssoLinkKey = "destinationPath.manageTaxes.machinegames"
      val ssoLinkMessage = Seq("otherservices.manageTaxes.link.hmrcemcsorg")

      val link = new ManageTaxesLink(dummyPortalUrlBuilder, ssoLinkKey, ssoLinkMessage, true)

      val expectedLink = ssoLinkKey
      val expectedMessage = LinkMessage(expectedLink, ssoLinkMessage(0), None, false, None, true)

      val result = link.buildLinks

      result(0).linkMessage shouldBe expectedMessage
    }

    "build sso links" in new WithApplication(FakeApplication()) {
      val ssoLinkKey = "destinationPath.manageTaxes.machinegames"
      val ssoLinkMessage = Seq("otherservices.manageTaxes.link.hmrcemcsorg", "otherservices.manageTaxes.link.hmrcemcsorg")

      val link = new ManageTaxesLink(dummyPortalUrlBuilder, ssoLinkKey, ssoLinkMessage, true)

      val expectedLink = ssoLinkKey
      val expectedMessage1 = LinkMessage(expectedLink, ssoLinkMessage(0), None, false, None, true)
      val expectedMessage2 = LinkMessage(expectedLink, ssoLinkMessage(1), None, false, None, true)

      val result = link.buildLinks

      result(0).linkMessage shouldBe expectedMessage1
      result(1).linkMessage shouldBe expectedMessage2
    }

    "build non sso link including post link text" in new WithApplication(FakeApplication(
        additionalConfiguration = Map("govuk-tax.Test.externalLinks.businessTax.manageTaxes.servicesHome" ->
        "https://secure.hmce.gov.uk/ecom/login/index.html")
    )) {
      val nonSsoLinkKey = "businessTax.manageTaxes.servicesHome"
      val nonSsoLinkMessage = Seq("otherservices.manageTaxes.link.hmceddes")

      val link = new ManageTaxesLink(dummyPortalUrlBuilder, nonSsoLinkKey, nonSsoLinkMessage, false)

      val expectedLink = "https://secure.hmce.gov.uk/ecom/login/index.html"
      val expectedPostLinkText = "otherservices.manageTaxes.postLink.additionalLoginRequired"
      val expectedMessage = LinkMessage(expectedLink, nonSsoLinkMessage(0), None, true, Some(expectedPostLinkText))

      val result = link.buildLinks

      result(0).linkMessage shouldBe expectedMessage
    }

    "build non sso links including post link text" in new WithApplication(FakeApplication(
      additionalConfiguration = Map("govuk-tax.Test.externalLinks.businessTax.manageTaxes.servicesHome" ->
        "https://secure.hmce.gov.uk/ecom/login/index.html")
    )) {
      val nonSsoLinkKey = "businessTax.manageTaxes.servicesHome"
      val nonSsoLinkMessage = Seq("otherservices.manageTaxes.link.hmceddes", "otherservices.manageTaxes.link.hmceebtiorg")

      val link = new ManageTaxesLink(dummyPortalUrlBuilder, nonSsoLinkKey, nonSsoLinkMessage, false)

      val expectedLink = "https://secure.hmce.gov.uk/ecom/login/index.html"
      val expectedPostLinkText = "otherservices.manageTaxes.postLink.additionalLoginRequired"
      val expectedMessage1 = LinkMessage(expectedLink, nonSsoLinkMessage(0), None, true, Some(expectedPostLinkText))
      val expectedMessage2 = LinkMessage(expectedLink, nonSsoLinkMessage(1), None, true, Some(expectedPostLinkText))

      val result = link.buildLinks

      result(0).linkMessage shouldBe expectedMessage1
      result(1).linkMessage shouldBe expectedMessage2
    }
  }
}
