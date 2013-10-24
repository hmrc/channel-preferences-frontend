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
    
    def buildPortalLinks(link: String, messages: Seq[String]): Seq[RenderableLinkMessage] = {
      messages.map(text => RenderableLinkMessage(LinkMessage(buildPortalUrl(link), Messages(text))))
    }
    
    val profile = governmentGatewayMicroService.profile(user.userId).getOrElse(throw new RuntimeException("Could not retrieve user profile from Government Gateway service"))
    profile.affinityGroup.identifier match {
      case INDIVIDUAL | ORGANISATION => {
        val linkMessages = getLinksAndMessages(profile.activeEnrolments.toList.map(_.key.toLowerCase)).flatMap {
          case (link, messages) => buildPortalLinks(link, messages)
        }.toList
        Some(ManageYourTaxes(linkMessages))
      }
      case _  => None
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
  
  def getLinksAndMessages(keys: Seq[String]) = keys.sorted flatMap config.get
  
  private val config = Map(
    "hmce-ddes" ->("destinationPath.managedtaxes.servicesHome", Seq("otherservices.managetaxes.link.hmceddes")),
    "hmce-ebti-org" ->("destinationPath.managedtaxes.servicesHome", Seq("otherservices.managetaxes.link.hmceebtiorg")),
    "hmrc-emcs-org" ->("destinationPath.managedtaxes.emcs", Seq("otherservices.managetaxes.link.hmrcemcsorg")),
    "hmrc-ics-org" ->("destinationPath.managedtaxes.ics", Seq("otherservices.managetaxes.link.hmrcicsorg")),
    "hmrc-mgd-org" ->("destinationPath.managedtaxes.machinegames", Seq("otherservices.managetaxes.link.hmrcmgdorg")),
    "hmce-ncts-org" ->("destinationPath.managedtaxes.ncts", Seq("otherservices.managetaxes.link.hmcenctsorg")),
    "hmce-nes" ->("destinationPath.managedtaxes.servicesHome", Seq("otherservices.managetaxes.link.hmcenes")),
    "hmrc-nova-org" ->("destinationPath.managedtaxes.nova", Seq("otherservices.managetaxes.link.hmrcnovaorg")),
    "hmce-ro" ->("destinationPath.managedtaxes.servicesHome", Seq("otherservices.managetaxes.link.hmcero1", "otherservices.managetaxes.link.hmcero2", "otherservices.managetaxes.link.hmcero3")),
    "hmrc-ecw-ind" ->("destinationPath.managedtaxes.er1", Seq("otherservices.managetaxes.link.hmrcecwind")),
    "hmce-to" ->("destinationPath.managedtaxes.servicesHome", Seq("otherservices.managetaxes.link.hmceto")),
    "hmce-ecsl-org" ->("destinationPath.managedtaxes.ecsl", Seq("otherservices.managetaxes.link.hmceecslorg")),
    "hmrc-eu-ref-org" ->("destinationPath.managedtaxes.euvat", Seq("otherservices.managetaxes.link.hmrceureforg")),
    "hmrc-vatrsl-org" ->("destinationPath.managedtaxes.rcsl", Seq("otherservices.managetaxes.link.hmrcvatrslorg"))
  )
}
