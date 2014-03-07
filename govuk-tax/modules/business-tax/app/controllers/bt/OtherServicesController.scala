package controllers.bt

import controllers.common.{BaseController, GovernmentGateway}
import uk.gov.hmrc.common.PortalUrlBuilder
import controllers.common.service.Connectors
import play.api.mvc.Request
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import controllers.common.actions.{HeaderCarrier, Actions}
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.governmentgateway.{ProfileResponse, GovernmentGatewayConnector}
import config.PortalConfig
import views.helpers.Link
import play.api.i18n.Messages

class OtherServicesController(governmentGatewayConnector: GovernmentGatewayConnector,
                              override val auditConnector: AuditConnector)
                             (implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with PortalUrlBuilder
  with BusinessTaxRegimeRoots {

  def this() = this(Connectors.governmentGatewayConnector, Connectors.auditConnector)(Connectors.authConnector)

  def otherServices = AuthenticatedBy(GovernmentGateway).async {
    user => request => otherServicesPage(user, request)
  }

  private[bt] def otherServicesPage(implicit user: User, request: Request[AnyRef]) = {

    governmentGatewayConnector.profile(user.userId)(HeaderCarrier(request)).map {
      profile =>
        Ok(views.html.other_services(links(profile)))
    }
  }

  def links(profile: ProfileResponse): Option[List[Link]] = {

    profile.affinityGroup match {
      case uk.gov.hmrc.common.microservice.governmentgateway.AffinityGroupValue.INDIVIDUAL =>
        Some(profile.activeEnrolments map {
          enrolment => enrolment.toLowerCase
        } collect {
          case "hmce-ecsl-org" => Link.toExternalPage(id = Some("hmceecslorgHref"), url = "https://customs.hmrc.gov.uk/ecsl/httpssl/start.do", value = Some(Messages("otherservices.manageTaxes.link.hmceecslorg")))
          case "hmrc-eu-ref-org" => Link.toPortalPage(id = Some("hmrceureforgHref"), url = PortalConfig.getDestinationUrl("manageTaxes.euvat"), value = Some(Messages("otherservices.manageTaxes.link.hmrceureforg")))
          case "hmce-vatrsl-org" => Link.toExternalPage(id = Some("hmcevatrslorgHref"), url = "https://customs.hmrc.gov.uk/rcsl/httpssl/Home.do", value = Some(Messages("otherservices.manageTaxes.link.hmcevatrslorg")))
        })
      case uk.gov.hmrc.common.microservice.governmentgateway.AffinityGroupValue.ORGANISATION =>
        val enrolments = profile.activeEnrolments map {
          enrolment => enrolment.toLowerCase
        }
        val links = enrolments collect {
          case "hmce-ddes" => Link.toExternalPage(id = Some("hmceddesHref"), url = "https://secure.hmce.gov.uk/ecom/login/index.html", value = Some(Messages("otherservices.manageTaxes.link.hmceddes")))
          case "hmce-ebti-org" => Link.toExternalPage(id = Some("hmceebtiorgHref"), url = "https://secure.hmce.gov.uk/ecom/login/index.html", value = Some(Messages("otherservices.manageTaxes.link.hmceebtiorg")))
          case "hmrc-emcs-org" => Link.toPortalPage(id = Some("hmrcemcsorgHref"), url = PortalConfig.getDestinationUrl("manageTaxes.emcs"), value = Some(Messages("otherservices.manageTaxes.link.hmrcemcsorg")))
          case "hmrc-ics-org" => Link.toPortalPage(id = Some("hmrcicsorgHref"), url = PortalConfig.getDestinationUrl("manageTaxes.ics"), value = Some(Messages("otherservices.manageTaxes.link.hmrcicsorg")))
          case "hmrc-mgd-org" => Link.toPortalPage(id = Some("hmrcmgdorgHref"), url = PortalConfig.getDestinationUrl("manageTaxes.machinegames"), value = Some(Messages("otherservices.manageTaxes.link.hmrcmgdorg")))
          case "hmce-ncts-org" => Link.toExternalPage(id = Some("hmcenctsorgHref"), url = "https://customs.hmrc.gov.uk/nctsPortalWebApp/ncts.portal?_nfpb=true&pageLabel=httpssIPageOnlineServicesAppNCTS_Home", value = Some(Messages("otherservices.manageTaxes.link.hmcenctsorg")))
          case "hmce-nes" => Link.toExternalPage(id = Some("hmcenesHref"), url = "https://secure.hmce.gov.uk/ecom/login/index.html", value = Some(Messages("otherservices.manageTaxes.link.hmcenes")))
          case "hmce-to" => Link.toExternalPage(id = Some("hmcetoHref"), url = "https://secure.hmce.gov.uk/ecom/login/index.html", value = Some(Messages("otherservices.manageTaxes.link.hmceto")))
          case "hmce-ecsl-org" => Link.toExternalPage(id = Some("hmceecslorgHref"), url = "https://customs.hmrc.gov.uk/ecsl/httpssl/start.do", value = Some(Messages("otherservices.manageTaxes.link.hmceecslorg")))
          case "hmrc-eu-ref-org" => Link.toPortalPage(id = Some("hmrceureforgHref"), url = PortalConfig.getDestinationUrl("manageTaxes.euvat"), value = Some(Messages("otherservices.manageTaxes.link.hmrceureforg")))
          case "hmce-vatrsl-org" => Link.toExternalPage(id = Some("hmcevatrslorgHref"), url = "https://customs.hmrc.gov.uk/rcsl/httpssl/Home.do", value = Some(Messages("otherservices.manageTaxes.link.hmcevatrslorg")))
        }

        // and there is obviously the special case:
        if (enrolments.contains("hmce-ro")) {
          val hmceRo = List(
            Link.toExternalPage(id = Some("hmcero1Href"), url = "https://secure.hmce.gov.uk/ecom/login/index.html", value = Some(Messages("otherservices.manageTaxes.link.hmcero1"))),
            Link.toExternalPage(id = Some("hmcero2Href"), url = "https://secure.hmce.gov.uk/ecom/login/index.html", value = Some(Messages("otherservices.manageTaxes.link.hmcero2"))),
            Link.toExternalPage(id = Some("hmcero3Href"), url = "https://secure.hmce.gov.uk/ecom/login/index.html", value = Some(Messages("otherservices.manageTaxes.link.hmcero3")))
          )
          Some(links ::: hmceRo)
        } else Some(links)

      case _ => None
    }

  }
}
