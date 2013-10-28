package controllers.bt.otherservices

import uk.gov.hmrc.common.microservice.domain.{RegimeRoots, User}
import views.helpers.{LinkMessage, RenderableLinkMessage}
import uk.gov.hmrc.common.microservice.governmentgateway.GovernmentGatewayMicroService


class OtherServicesFactory(governmentGatewayMicroService: GovernmentGatewayMicroService) {

  private val linkToHmrcWebsite = "http://www.hmrc.gov.uk/online/new.htm#2"
  val linkToHmrcOnlineRegistration = "https://online.hmrc.gov.uk/registration/newbusiness/business-allowed"
  private val hmrcWebsiteLinkText = "HMRC website"

  //TODO waiting for links confirmation
  def createManageYourTaxes(buildPortalUrl: String => String)(implicit user: User): Option[ManageYourTaxes] = {
    import uk.gov.hmrc.common.microservice.governmentgateway.AffinityGroupValue._
    import ManageYourTaxesConf._

    val profile = governmentGatewayMicroService.profile(user.userId).getOrElse(throw new RuntimeException("Could not retrieve user profile from Government Gateway service"))
    profile.affinityGroup.identifier match {
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
    OnlineServicesEnrolment(RenderableLinkMessage(LinkMessage(buildPortalUrl("otherServicesEnrolment"), "here")))

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
    val link = linkText.map(text => RenderableLinkMessage(LinkMessage(linkToHmrcOnlineRegistration, text)))
    BusinessTaxesRegistration(link, RenderableLinkMessage(LinkMessage(linkToHmrcWebsite, hmrcWebsiteLinkText)))
  }
}

object ManageYourTaxesConf {

  def ssoLink(keyToLink: String, keysToLinkText: Seq[String], buildPortalUrl: String => String) = new ManageTaxesLink(buildPortalUrl, keyToLink, keysToLinkText, isSso = true)

  def nonSsoLink(keyToLink: String, keysToLinkText: Seq[String], buildPortalUrl: String => String) = new ManageTaxesLink(buildPortalUrl, keyToLink, keysToLinkText, isSso = false)

  def getLinksAndMessagesForIndividual(keys: Seq[String], buildPortalUrl: String => String): Seq[ManageTaxesLink] = {


    val linksForIndividual = Map(
      "hmce-ecsl-org" -> nonSsoLink("businessTax.manageTaxes.ecsl", Seq("otherservices.manageTaxes.link.hmceecslorg"), buildPortalUrl),
      "hmrc-eu-ref-org" -> ssoLink("manageTaxes.euvat", Seq("otherservices.manageTaxes.link.hmrceureforg"), buildPortalUrl),
      "hmrc-vatrsl-org" -> nonSsoLink("businessTax.manageTaxes.rcsl", Seq("otherservices.manageTaxes.link.hmrcvatrslorg"), buildPortalUrl)
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
      "hmrc-nova-org" -> ssoLink("manageTaxes.nova", Seq("otherservices.manageTaxes.link.hmrcnovaorg"), buildPortalUrl),
      "hmce-ro" -> nonSsoLink("businessTax.manageTaxes.servicesHome", Seq("otherservices.manageTaxes.link.hmcero1", "otherservices.manageTaxes.link.hmcero2", "otherservices.manageTaxes.link.hmcero3"), buildPortalUrl),
      "hmce-to" -> nonSsoLink("businessTax.manageTaxes.servicesHome", Seq("otherservices.manageTaxes.link.hmceto"), buildPortalUrl),
      "hmce-ecsl-org" -> nonSsoLink("businessTax.manageTaxes.ecsl", Seq("otherservices.manageTaxes.link.hmceecslorg"), buildPortalUrl),
      "hmrc-eu-ref-org" -> ssoLink("manageTaxes.euvat", Seq("otherservices.manageTaxes.link.hmrceureforg"), buildPortalUrl),
      "hmrc-vatrsl-org" -> nonSsoLink("businessTax.manageTaxes.rcsl", Seq("otherservices.manageTaxes.link.hmrcvatrslorg"), buildPortalUrl)
    )

    keys.sorted flatMap linksForOrganisation.get
  }
}

class ManageTaxesLink(buildPortalUrl: String => String, keyToLink: String, keysToLinkText: Seq[String], isSso: Boolean) {

  def buildLinks: Seq[RenderableLinkMessage] = {

    keysToLinkText.map(text => {
      if (isSso) {
        RenderableLinkMessage(LinkMessage.portalLink(buildPortalUrl(keyToLink), text))
      } else {
        RenderableLinkMessage(LinkMessage.externalLink(hrefKey = keyToLink, text = text,
          postLinkText = Some("otherservices.manageTaxes.postLink.additionalLoginRequired")))
      }
    })
  }
}


