package controllers.bt.otherservices

import uk.gov.hmrc.common.BaseSpec
import play.api.test.{FakeApplication, WithApplication}
import uk.gov.hmrc.common.microservice.governmentgateway.AffinityGroupValue._
import scala.Some
import uk.gov.hmrc.common.microservice.governmentgateway.{Enrolment, AffinityGroup, ProfileResponse}
import views.helpers.{LinkMessage, RenderableLinkMessage}

class ManageYourTaxesConfSpec extends BaseSpec {

  "processLinks" should {

    "return None when the user affinity group is agent" in {

      val expectedResponse = ProfileResponse(
        affinityGroup = AffinityGroup(AGENT),
        activeEnrolments = Set.empty)

      val result = ManageYourTaxesConf.processLinks(AGENT, expectedResponse, (s: String) => s)
      result shouldBe None
    }


    "return a list with links ordered by key for the user enrolments when the affinity group is individual" in new ManageYourTaxesConfForTest {
      val profile = ProfileResponse(
        affinityGroup = AffinityGroup(INDIVIDUAL),
        activeEnrolments = Set(
          Enrolment("HMCE-ECSL-ORG"),
          Enrolment("HMCE-VATRSL-ORG"),
          Enrolment("HMRC-EU-REF-ORG")
        ))

      val result = ManageYourTaxesConf.processLinks(INDIVIDUAL, profile, (s: String) => s"http://$s")
      val postLinkText = Some("otherservices.manageTaxes.postLink.additionalLoginRequired")

      val expectedResult = ManageYourTaxes(
        Seq(
          /* hmceecslorg */ RenderableLinkMessage(LinkMessage("https://customs.hmrc.gov.uk/ecsl/httpssl/start.do", "otherservices.manageTaxes.link.hmceecslorg", Some("hmceecslorgHref"), true, postLinkText, false)),
          /* hmcevatrslorg */ RenderableLinkMessage(LinkMessage("https://customs.hmrc.gov.uk/rcsl/httpssl/Home.do", "otherservices.manageTaxes.link.hmcevatrslorg", Some("hmcevatrslorgHref"), true, postLinkText, false)),
          /* hmrceureforg */ RenderableLinkMessage(LinkMessage("http://manageTaxes.euvat", "otherservices.manageTaxes.link.hmrceureforg", Some("hmrceureforgHref"), false, None, true))
        )
      )

      result.get shouldBe expectedResult
    }

        "return a list with links ordered by key for the user enrolments when the affinity group is organisation" in new ManageYourTaxesConfForTest {

          val profile = ProfileResponse(
            affinityGroup = AffinityGroup(ORGANISATION),
            activeEnrolments = Set(
              Enrolment("HMCE-DDES"),
              Enrolment("HMCE-EBTI-ORG"),
              Enrolment("HMCE-VATRSL-ORG"),
              Enrolment("HMRC-EMCS-ORG"),
              Enrolment("HMRC-ICS-ORG"),
              Enrolment("HMRC-MGD-ORG"),
              Enrolment("HMCE-NCTS-ORG"),
              Enrolment("HMCE-NES"),
              Enrolment("HMRC-NOVA-ORG"),
              Enrolment("HMCE-RO"),
              Enrolment("HMRC-ECW-IND"),
              Enrolment("HMCE-TO"),
              Enrolment("HMCE-ECSL-ORG"),
              Enrolment("HMRC-EU-REF-ORG")
            ))

          val postLinkText = Some("otherservices.manageTaxes.postLink.additionalLoginRequired")
          val expectedResult = Some(
            ManageYourTaxes(
              Seq(
                /* hmceddes */ RenderableLinkMessage(LinkMessage("https://secure.hmce.gov.uk/ecom/login/index.html", "otherservices.manageTaxes.link.hmceddes", Some("hmceddesHref"), true, postLinkText, false)),
                /* hmceebtiorg */ RenderableLinkMessage(LinkMessage("https://secure.hmce.gov.uk/ecom/login/index.html", "otherservices.manageTaxes.link.hmceebtiorg", Some("hmceebtiorgHref"), true, postLinkText, false)),
                /* hmceecslorg */ RenderableLinkMessage(LinkMessage("https://customs.hmrc.gov.uk/ecsl/httpssl/start.do", "otherservices.manageTaxes.link.hmceecslorg", Some("hmceecslorgHref"), true, postLinkText, false)),
                /* hmcenctsorg */ RenderableLinkMessage(LinkMessage("https://customs.hmrc.gov.uk/nctsPortalWebApp/ncts.portal?_nfpb=true&pageLabel=httpssIPageOnlineServicesAppNCTS_Home", "otherservices.manageTaxes.link.hmcenctsorg", Some("hmcenctsorgHref"), true, postLinkText, false)),
                /* hmcenes */ RenderableLinkMessage(LinkMessage("https://secure.hmce.gov.uk/ecom/login/index.html", "otherservices.manageTaxes.link.hmcenes", Some("hmcenesHref"), true, postLinkText, false)),
                /* hmcero1 */ RenderableLinkMessage(LinkMessage("https://secure.hmce.gov.uk/ecom/login/index.html", "otherservices.manageTaxes.link.hmcero1", Some("hmcero1Href"), true, postLinkText, false)),
                /* hmcero2 */ RenderableLinkMessage(LinkMessage("https://secure.hmce.gov.uk/ecom/login/index.html", "otherservices.manageTaxes.link.hmcero2", Some("hmcero2Href"), true, postLinkText, false)),
                /* hmcero3 */ RenderableLinkMessage(LinkMessage("https://secure.hmce.gov.uk/ecom/login/index.html", "otherservices.manageTaxes.link.hmcero3", Some("hmcero3Href"), true, postLinkText, false)),
                /* hmceto */ RenderableLinkMessage(LinkMessage("https://secure.hmce.gov.uk/ecom/login/index.html", "otherservices.manageTaxes.link.hmceto", Some("hmcetoHref"), true, postLinkText, false)),
                /* hmcevatrslorg */ RenderableLinkMessage(LinkMessage("https://customs.hmrc.gov.uk/rcsl/httpssl/Home.do", "otherservices.manageTaxes.link.hmcevatrslorg", Some("hmcevatrslorgHref"), true, postLinkText, false)),
                /* hmrcemcsorg */ RenderableLinkMessage(LinkMessage("http://manageTaxes.emcs", "otherservices.manageTaxes.link.hmrcemcsorg", Some("hmrcemcsorgHref"), false, None, true)),
                /* hmrceureforg */ RenderableLinkMessage(LinkMessage("http://manageTaxes.euvat", "otherservices.manageTaxes.link.hmrceureforg", Some("hmrceureforgHref"), false, None, true)),
                /* hmrcicsorg */ RenderableLinkMessage(LinkMessage("http://manageTaxes.ics", "otherservices.manageTaxes.link.hmrcicsorg", Some("hmrcicsorgHref"), false, None, true)),
                /* hmrcmgdorg */ RenderableLinkMessage(LinkMessage("http://manageTaxes.machinegames", "otherservices.manageTaxes.link.hmrcmgdorg", Some("hmrcmgdorgHref"), false, None, true))
              )
            )
          )

          val result = ManageYourTaxesConf.processLinks(ORGANISATION, profile, (s: String) => s"http://$s")

          result shouldBe expectedResult
        }

  }

  abstract class ManageYourTaxesConfForTest
    extends WithApplication(FakeApplication(additionalConfiguration = Map(
      "govuk-tax.Test.externalLinks.businessTax.manageTaxes.servicesHome" -> "https://secure.hmce.gov.uk/ecom/login/index.html",
      "govuk-tax.Test.externalLinks.businessTax.manageTaxes.ncts" -> "https://customs.hmrc.gov.uk/nctsPortalWebApp/ncts.portal?_nfpb=true&pageLabel=httpssIPageOnlineServicesAppNCTS_Home",
      "govuk-tax.Test.externalLinks.businessTax.manageTaxes.rcsl" -> "https://customs.hmrc.gov.uk/rcsl/httpssl/Home.do",
      "govuk-tax.Test.externalLinks.businessTax.manageTaxes.ecsl" -> "https://customs.hmrc.gov.uk/ecsl/httpssl/start.do",
      "govuk-tax.Test.externalLinks.businessTax.registration.otherWays" -> "http://www.hmrc.gov.uk/online/new.htm#2")))
}
