package controllers.bt.otherservices

import uk.gov.hmrc.common.microservice.domain.{RegimeRoots, User}
import uk.gov.hmrc.common.PortalUrlBuilder
import views.helpers.{LinkMessage, RenderableLinkMessage}
import play.api.mvc.Request

class OtherServicesFactory extends PortalUrlBuilder {

  private val linkToHmrcWebsite = "http://www.hmrc.gov.uk/online/new.htm#2"
  private val hmrcWebsiteLinkText = "HMRC website"

  def create(user: User)(implicit request: Request[AnyRef]): OtherServicesSummary = {
    OtherServicesSummary(createManageYourTaxes(user), createOnlineServicesEnrolment(user, request), createBusinessTaxesRegistration(user))
  }

  //TODO implement this method
  private[otherservices] def createManageYourTaxes(user: User)(implicit request: Request[AnyRef]): Option[ManageYourTaxes] =
    None

  // TODO: Avoid direct testing of private methods, remove package from the private declaration
  private[otherservices] def createOnlineServicesEnrolment(implicit user: User, request: Request[AnyRef]): OnlineServicesEnrolment =
    OnlineServicesEnrolment(RenderableLinkMessage(LinkMessage(buildPortalUrl("otherServicesEnrolment")(request, user), "here")))


  // TODO: Avoid direct testing of private methods, remove package from the private declaration
  private[otherservices] def createBusinessTaxesRegistration(user: User) = {

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
    val link = linkText.map(text => RenderableLinkMessage(LinkMessage(linkToHmrcWebsite, text)))
    BusinessTaxesRegistration(link, RenderableLinkMessage(LinkMessage(linkToHmrcWebsite, hmrcWebsiteLinkText)))
  }
}
