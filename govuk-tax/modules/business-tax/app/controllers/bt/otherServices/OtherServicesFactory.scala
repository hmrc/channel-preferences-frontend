package controllers.bt.otherservices

import uk.gov.hmrc.common.microservice.domain.{RegimeRoots, User}
import uk.gov.hmrc.common.PortalUrlBuilder
import views.helpers.{LinkMessage, RenderableLinkMessage}
import play.api.mvc.Request
import uk.gov.hmrc.common.microservice.governmentgateway.GovernmentGatewayMicroService
import play.api.i18n.Messages


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

  def getLinksAndMessages(keys: Seq[String]): Seq[ManageTaxesLink] = keys.sorted flatMap config.get

  private val config = Map(
    "hmce-ddes" -> ManageTaxesLink("businessTax.manageTaxes.servicesHome", Seq("otherservices.manageTaxes.link.hmceddes"), false),
    "hmce-ebti-org" -> ManageTaxesLink("businessTax.manageTaxes.servicesHome", Seq("otherservices.manageTaxes.link.hmceebtiorg"), false),
    "hmrc-emcs-org" -> ManageTaxesLink("destinationPath.manageTaxes.emcs", Seq("otherservices.manageTaxes.link.hmrcemcsorg"), true),
    "hmrc-ics-org" -> ManageTaxesLink("destinationPath.manageTaxes.ics", Seq("otherservices.manageTaxes.link.hmrcicsorg"), true),
    "hmrc-mgd-org" -> ManageTaxesLink("destinationPath.manageTaxes.machinegames", Seq("otherservices.manageTaxes.link.hmrcmgdorg"), true),
    "hmce-ncts-org" -> ManageTaxesLink("destinationPath.manageTaxes.ncts", Seq("otherservices.manageTaxes.link.hmcenctsorg"), true),
    "hmce-nes" -> ManageTaxesLink("businessTax.manageTaxes.servicesHome", Seq("otherservices.manageTaxes.link.hmcenes"), false),
    "hmrc-nova-org" -> ManageTaxesLink("destinationPath.manageTaxes.nova", Seq("otherservices.manageTaxes.link.hmrcnovaorg"), true),
    "hmce-ro" -> ManageTaxesLink("destinationPath.manageTaxes.servicesHome", Seq("otherservices.manageTaxes.link.hmcero1", "otherservices.manageTaxes.link.hmcero2", "otherservices.manageTaxes.link.hmcero3"), false),
    "hmrc-ecw-ind" -> ManageTaxesLink("destinationPath.manageTaxes.er1", Seq("otherservices.manageTaxes.link.hmrcecwind"), true),
    "hmce-to" -> ManageTaxesLink("businessTax.manageTaxes.servicesHome", Seq("otherservices.manageTaxes.link.hmceto"), false),
    "hmce-ecsl-org" -> ManageTaxesLink("destinationPath.manageTaxes.ecsl", Seq("otherservices.manageTaxes.link.hmceecslorg"), true),
    "hmrc-eu-ref-org" -> ManageTaxesLink("destinationPath.manageTaxes.euvat", Seq("otherservices.manageTaxes.link.hmrceureforg"), true),
    "hmrc-vatrsl-org" -> ManageTaxesLink("destinationPath.manageTaxes.rcsl", Seq("otherservices.manageTaxes.link.hmrcvatrslorg"), true)
  )

}

class ManageTaxesLink(keyToLink: String, keysToLinkText: Seq[String], isSso: Boolean) extends PortalUrlBuilder {


  def buildPortalLinks(implicit request: Request[AnyRef], user: User): Seq[RenderableLinkMessage] = {

    keysToLinkText.map(text => {
      if (isSso) {
        RenderableLinkMessage(LinkMessage(
          href = buildPortalUrl(keyToLink),
          text = text))
      } else {
        RenderableLinkMessage(LinkMessage.externalLink(
          hrefKey = keyToLink,
          text = text,
          postLinkText = Some("otherservices.manageTaxes.postLink.additionalLoginRequired")))
      }
    })
  }
}

object ManageTaxesLink {
  def apply(keyToLink: String, keysToLinkText: Seq[String], isSso: Boolean) = {
    new ManageTaxesLink(keyToLink, keysToLinkText, isSso)
  }
}
