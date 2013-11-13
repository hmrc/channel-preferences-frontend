package controllers.bt.otherservices

import uk.gov.hmrc.common.microservice.domain.{RegimeRoots, User}
import views.helpers.{HrefKey, LinkMessage, RenderableLinkMessage}
import uk.gov.hmrc.common.microservice.governmentgateway.GovernmentGatewayConnector
import uk.gov.hmrc.common.AffinityGroupParser
import play.api.mvc.Request


class OtherServicesFactory(governmentGatewayConnector: GovernmentGatewayConnector) extends AffinityGroupParser {

  private val hmrcWebsiteLinkText = "HMRC website"

  def createManageYourTaxes(buildPortalUrl: String => String)(implicit user: User, request: Request[AnyRef]): Option[ManageYourTaxes] = {
    import uk.gov.hmrc.common.microservice.governmentgateway.AffinityGroupValue._
    import ManageYourTaxesConf._

    val profile = governmentGatewayConnector.profile(user.userId).getOrElse(throw new RuntimeException("Could not retrieve user profile from Government Gateway service"))
    val affinityGroup = parseAffinityGroup
    affinityGroup match {
      case INDIVIDUAL => {
        val linkMessages = getLinksAndMessagesForIndividual(profile.activeEnrolments.toList.map(_.key.toLowerCase), buildPortalUrl).flatMap {
          case link: ManageTaxesLink => link.buildLinks
        }.toList
        Some(ManageYourTaxes(linkMessages))
      }
      case ORGANISATION => {
        val linkMessages = getLinksAndMessagesForOrganisation(profile.activeEnrolments.toList.map(_.key.toLowerCase), buildPortalUrl).flatMap {
          case link: ManageTaxesLink => link.buildLinks
        }.toList
        Some(ManageYourTaxes(linkMessages))
      }
      case _ => None
    }
  }

  def createOnlineServicesEnrolment(buildPortalUrl: String => String): OnlineServicesEnrolment =
    OnlineServicesEnrolment(RenderableLinkMessage(LinkMessage.portalLink(href = buildPortalUrl("otherServicesEnrolment"), text = Some("here"), id = Some("otherServicesEnrolmentHref"))))

  def createOnlineServicesDeEnrolment(buildPortalUrl: String => String): OnlineServicesEnrolment =
    OnlineServicesEnrolment(RenderableLinkMessage(LinkMessage.portalLink(href = buildPortalUrl("servicesDeEnrolment"), text = Some("here"), id = Some("servicesDeEnrolmentHref"))))

  def createBusinessTaxesRegistration(buildPortalUrl: String => String)(implicit user: User) = {

    def appendInactiveRegimes(inactiveRegimes: List[String]): String = {
      inactiveRegimes match {
        case x :: Nil => s"$x"
        case x :: xl :: Nil => s"$x, or $xl"
        case x :: xs => s"$x, ${appendInactiveRegimes(xs)}"
        case Nil => ""
      }
    }

    val linkText: Option[String] = user.regimes match {
      case RegimeRoots(_, sa, Some(vat), Some(epaye), ct, _) if sa.isDefined || ct.isDefined => None
      case regimes: RegimeRoots => {
        val allRegimes = List((regimes.sa, "SA"), (regimes.ct, "CT"), (regimes.epaye, "employers PAYE"), (regimes.vat, "VAT"))
        val inactiveRegimes = allRegimes.filter(!_._1.isDefined).map(_._2)
        Some(s"Register for ${appendInactiveRegimes(inactiveRegimes)}")
      }
    }
    val link = linkText.map(text => RenderableLinkMessage(LinkMessage.portalLink(href = buildPortalUrl("businessRegistration"), text = Some(text), id = Some("businessRegistrationHref"))))
    BusinessTaxesRegistration(link, RenderableLinkMessage(LinkMessage.externalLink(hrefKey = HrefKey("businessTax.registration.otherWays"), text = hmrcWebsiteLinkText, id = Some("otherWaysHref"))))
  }
}

object ManageYourTaxesConf {

  def ssoLink(keyToLink: String, keysToLinkText: Seq[String], buildPortalUrl: String => String) = new ManageTaxesLink(buildPortalUrl, keyToLink, keysToLinkText, isSso = true)

  def nonSsoLink(keyToLink: String, keysToLinkText: Seq[String], buildPortalUrl: String => String) = new ManageTaxesLink(buildPortalUrl, keyToLink, keysToLinkText, isSso = false)

  def getLinksAndMessagesForIndividual(keys: Seq[String], buildPortalUrl: String => String): Seq[ManageTaxesLink] = {


    val linksForIndividual = Map(
      "hmce-ecsl-org" -> nonSsoLink("businessTax.manageTaxes.ecsl", Seq("otherservices.manageTaxes.link.hmceecslorg"), buildPortalUrl),
      "hmrc-eu-ref-org" -> ssoLink("manageTaxes.euvat", Seq("otherservices.manageTaxes.link.hmrceureforg"), buildPortalUrl),
      "hmce-vatrsl-org" -> nonSsoLink("businessTax.manageTaxes.rcsl", Seq("otherservices.manageTaxes.link.hmcevatrslorg"), buildPortalUrl)
    )

    keys.sorted flatMap linksForIndividual.get
  }


  def getLinksAndMessagesForOrganisation(keys: Seq[String], buildPortalUrl: String => String): Seq[ManageTaxesLink] = {


    val linksForOrganisation = Map(
      "hmce-ddes" -> nonSsoLink("businessTax.manageTaxes.servicesHome", Seq("otherservices.manageTaxes.link.hmceddes"), buildPortalUrl),
      "hmce-ebti-org" -> nonSsoLink("businessTax.manageTaxes.servicesHome", Seq("otherservices.manageTaxes.link.hmceebtiorg"), buildPortalUrl),
      "hmrc-emcs-org" -> ssoLink("manageTaxes.emcs", Seq("otherservices.manageTaxes.link.hmrcemcsorg"), buildPortalUrl),
      "hmrc-ics-org" -> ssoLink("manageTaxes.ics", Seq("otherservices.manageTaxes.link.hmrcicsorg"), buildPortalUrl),
      "hmrc-mgd-org" -> ssoLink("manageTaxes.machinegames", Seq("otherservices.manageTaxes.link.hmrcmgdorg"), buildPortalUrl),
      "hmce-ncts-org" -> nonSsoLink("businessTax.manageTaxes.ncts", Seq("otherservices.manageTaxes.link.hmcenctsorg"), buildPortalUrl),
      "hmce-nes" -> nonSsoLink("businessTax.manageTaxes.servicesHome", Seq("otherservices.manageTaxes.link.hmcenes"), buildPortalUrl),
      "hmce-ro" -> nonSsoLink("businessTax.manageTaxes.servicesHome", Seq("otherservices.manageTaxes.link.hmcero1", "otherservices.manageTaxes.link.hmcero2", "otherservices.manageTaxes.link.hmcero3"), buildPortalUrl),
      "hmce-to" -> nonSsoLink("businessTax.manageTaxes.servicesHome", Seq("otherservices.manageTaxes.link.hmceto"), buildPortalUrl),
      "hmce-ecsl-org" -> nonSsoLink("businessTax.manageTaxes.ecsl", Seq("otherservices.manageTaxes.link.hmceecslorg"), buildPortalUrl),
      "hmrc-eu-ref-org" -> ssoLink("manageTaxes.euvat", Seq("otherservices.manageTaxes.link.hmrceureforg"), buildPortalUrl),
      "hmce-vatrsl-org" -> nonSsoLink("businessTax.manageTaxes.rcsl", Seq("otherservices.manageTaxes.link.hmcevatrslorg"), buildPortalUrl)
    )

    val links = keys.sorted flatMap linksForOrganisation.get
    links :+ ssoLink("manageTaxes.nova", Seq("otherservices.manageTaxes.link.hmrcnovaorg"), buildPortalUrl)
  }
}

class ManageTaxesLink(buildPortalUrl: String => String, keyToLink: String, keysToLinkText: Seq[String], isSso: Boolean) {

  def buildLinks: Seq[RenderableLinkMessage] = {

    keysToLinkText.map(text => {

      val id = s"${text.substring(text.lastIndexOf(".") + 1)}Href"

      if (isSso) {
        RenderableLinkMessage(LinkMessage.portalLink(href = buildPortalUrl(keyToLink), text = Some(text), id = Some(id)))
      } else {
        RenderableLinkMessage(LinkMessage.externalLink(hrefKey = HrefKey(keyToLink), text = text, id = Some(id), postLinkText = Some("otherservices.manageTaxes.postLink.additionalLoginRequired")))
      }
    })
  }
}


