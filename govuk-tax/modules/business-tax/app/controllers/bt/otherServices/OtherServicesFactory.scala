package controllers.bt.otherservices

import uk.gov.hmrc.common.microservice.domain.{RegimeRoots, User}
import uk.gov.hmrc.common.PortalUrlBuilder
import views.helpers.{LinkMessage, RenderableLinkMessage}
import play.api.mvc.Request
import uk.gov.hmrc.common.microservice.governmentgateway.GovernmentGatewayMicroService


class OtherServicesFactory(governmentGatewayMicroService: GovernmentGatewayMicroService) extends PortalUrlBuilder {

  private val linkToHmrcWebsite = "http://www.hmrc.gov.uk/online/new.htm#2"
  val linkToHmrcOnlineRegistration = "https://online.hmrc.gov.uk/registration/newbusiness/business-allowed"
  private val hmrcWebsiteLinkText = "HMRC website"

  //TODO waiting for links confirmation
  def createManageYourTaxes(implicit request: Request[AnyRef], user: User): Option[ManageYourTaxes] = {
    import uk.gov.hmrc.common.microservice.governmentgateway.AffinityGroupValue._
    import ManageYourTaxesConf._

    val profile = governmentGatewayMicroService.profile(user.userId).getOrElse(throw new RuntimeException("Could not retrieve user profile from Government Gateway service"))
    profile.affinityGroup.identifier match {
      case INDIVIDUAL | ORGANISATION => {
        val linkMessages = getLinksAndMessages(profile.activeEnrolments.toList.map(_.key.toLowerCase)).flatMap {
          case link => link.buildPortalLinks
        }.toList
        Some(ManageYourTaxes(linkMessages))
      }
      case _ => None
    }
  }

  def createOnlineServicesEnrolment(implicit request: Request[AnyRef], user: User): OnlineServicesEnrolment =
    OnlineServicesEnrolment(RenderableLinkMessage(LinkMessage(buildPortalUrl("otherServicesEnrolment")(request, user), "here")))

  def createBusinessTaxesRegistration(implicit user: User) = {

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

  import ManageTaxesLink._

  def getLinksAndMessages(keys: Seq[String]): Seq[ManageTaxesLink] = keys.sorted flatMap links.get

  private val links = Map(
    "hmce-ddes" -> nonSsoLink("businessTax.manageTaxes.servicesHome", Seq("otherservices.manageTaxes.link.hmceddes")),
    "hmce-ebti-org" -> nonSsoLink("businessTax.manageTaxes.servicesHome", Seq("otherservices.manageTaxes.link.hmceebtiorg")),
    "hmrc-emcs-org" -> ssoLink("destinationPath.manageTaxes.emcs", Seq("otherservices.manageTaxes.link.hmrcemcsorg")),
    "hmrc-ics-org" -> ssoLink("destinationPath.manageTaxes.ics", Seq("otherservices.manageTaxes.link.hmrcicsorg")),
    "hmrc-mgd-org" -> ssoLink("destinationPath.manageTaxes.machinegames", Seq("otherservices.manageTaxes.link.hmrcmgdorg")),
    "hmce-ncts-org" -> ssoLink("destinationPath.manageTaxes.ncts", Seq("otherservices.manageTaxes.link.hmcenctsorg")),
    "hmce-nes" -> nonSsoLink("businessTax.manageTaxes.servicesHome", Seq("otherservices.manageTaxes.link.hmcenes")),
    "hmrc-nova-org" -> ssoLink("destinationPath.manageTaxes.nova", Seq("otherservices.manageTaxes.link.hmrcnovaorg")),
    "hmce-ro" -> nonSsoLink("destinationPath.manageTaxes.servicesHome", Seq("otherservices.manageTaxes.link.hmcero1", "otherservices.manageTaxes.link.hmcero2", "otherservices.manageTaxes.link.hmcero3")),
    "hmrc-ecw-ind" -> ssoLink("destinationPath.manageTaxes.er1", Seq("otherservices.manageTaxes.link.hmrcecwind")),
    "hmce-to" -> nonSsoLink("businessTax.manageTaxes.servicesHome", Seq("otherservices.manageTaxes.link.hmceto")),
    "hmce-ecsl-org" -> ssoLink("destinationPath.manageTaxes.ecsl", Seq("otherservices.manageTaxes.link.hmceecslorg")),
    "hmrc-eu-ref-org" -> ssoLink("destinationPath.manageTaxes.euvat", Seq("otherservices.manageTaxes.link.hmrceureforg")),
    "hmrc-vatrsl-org" -> ssoLink("destinationPath.manageTaxes.rcsl", Seq("otherservices.manageTaxes.link.hmrcvatrslorg"))
  )
}

class ManageTaxesLink(keyToLink: String, keysToLinkText: Seq[String], isSso: Boolean) extends PortalUrlBuilder {

  def buildPortalLinks(implicit request: Request[AnyRef], user: User): Seq[RenderableLinkMessage] = {

    keysToLinkText.map(text => {
      if (isSso) {
        RenderableLinkMessage(LinkMessage(href = buildPortalUrl(keyToLink), text = text))
      } else {
        RenderableLinkMessage(LinkMessage.externalLink(hrefKey = keyToLink, text = text,
          postLinkText = Some("otherservices.manageTaxes.postLink.additionalLoginRequired")))
      }
    })
  }
}

object ManageTaxesLink {
  
  def ssoLink(keyToLink: String, keysToLinkText: Seq[String]) = new ManageTaxesLink(keyToLink, keysToLinkText, isSso = true)
  
  def nonSsoLink(keyToLink: String, keysToLinkText: Seq[String]) = new ManageTaxesLink(keyToLink, keysToLinkText, isSso = false)
}