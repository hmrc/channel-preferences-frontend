package controllers.bt

import controllers.common.{BaseController, GovernmentGateway}
import uk.gov.hmrc.common.PortalUrlBuilder
import controllers.common.service.Connectors
import play.api.mvc.Request
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import controllers.common.actions.{HeaderCarrier, Actions}
import uk.gov.hmrc.common.microservice.domain.User
import views.helpers.PortalLink
import uk.gov.hmrc.common.microservice.governmentgateway.{ProfileResponse, GovernmentGatewayConnector}
import config.PortalConfig

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
        Ok(views.html.other_services(links(profile),
          PortalLink(PortalConfig.getDestinationUrl("otherServicesEnrolment")),
          PortalLink(PortalConfig.getDestinationUrl("servicesDeEnrolment")),
          PortalLink(PortalConfig.getDestinationUrl("businessRegistration"))))
    }
  }

  def links(profile: ProfileResponse): List[OtherServiceLink] = {

    profile.affinityGroup match {
      case uk.gov.hmrc.common.microservice.governmentgateway.AffinityGroupValue.INDIVIDUAL =>
        profile.activeEnrolments map { enrolment => enrolment.toLowerCase } collect {
          case "hmce-ecsl-org" => OtherServiceLink("hmceecslorgHref", "https://customs.hmrc.gov.uk/ecsl/httpssl/start.do", "otherservices.manageTaxes.link.hmceecslorg", true)
          case "hmrc-eu-ref-org" => OtherServiceLink("hmceecslorgHref", PortalConfig.getDestinationUrl("manageTaxes.euvat"), "otherservices.manageTaxes.link.hmrceureforg", false, true)
          case "hmce-vatrsl-org" => OtherServiceLink("hmcevatrslorgHref", "https://customs.hmrc.gov.uk/rcsl/httpssl/Home.do", "otherservices.manageTaxes.link.hmcevatrslorg", true)
       }
      case uk.gov.hmrc.common.microservice.governmentgateway.AffinityGroupValue.ORGANISATION =>
        profile.activeEnrolments map { enrolment => enrolment.toLowerCase } collect {
          case "hmce-ddes" => OtherServiceLink("hmceddesHref", "https://secure.hmce.gov.uk/ecom/login/index.html", "otherservices.manageTaxes.link.hmceddes", true)
          case "hmce-ebti-org" => OtherServiceLink("hmceebtiorgHref", "https://secure.hmce.gov.uk/ecom/login/index.html", "otherservices.manageTaxes.link.hmceebtiorg", true)
          case "hmrc-emcs-org" => OtherServiceLink("hmrcemcsorgHref", PortalConfig.getDestinationUrl("manageTaxes.emcs"), "otherservices.manageTaxes.link.hmrcemcsorg", false, true)
          case "hmrc-ics-org" => OtherServiceLink("hmrcicsorgHref", PortalConfig.getDestinationUrl("manageTaxes.ics"), "otherservices.manageTaxes.link.hmrcicsorg", false, true)
          case "hmrc-mgd-org" => OtherServiceLink("hmrcmgdorgHref", PortalConfig.getDestinationUrl("manageTaxes.machinegames"), "otherservices.manageTaxes.link.hmrcmgdorg", false, true)
          case "hmce-ncts-org" => OtherServiceLink("hmcenctsorgHref", "https://customs.hmrc.gov.uk/nctsPortalWebApp/ncts.portal?_nfpb=true&pageLabel=httpssIPageOnlineServicesAppNCTS_Home", "otherservices.manageTaxes.link.hmcenctsorg", true)
          case "hmce-nes" => OtherServiceLink("hmcenesHref", "https://secure.hmce.gov.uk/ecom/login/index.html", "otherservices.manageTaxes.link.hmcenes", true)
          case "hmce-ro" => { OtherServiceLink("hmcero1Href", "https://secure.hmce.gov.uk/ecom/login/index.html", "otherservices.manageTaxes.link.hmcero1", true)
                              OtherServiceLink("hmcero2Href", "https://secure.hmce.gov.uk/ecom/login/index.html", "otherservices.manageTaxes.link.hmcero2", true) }
          case "hmce-to" => OtherServiceLink("hmcetoHref", "https://secure.hmce.gov.uk/ecom/login/index.html", "otherservices.manageTaxes.link.hmceto", true)
          case "hmce-ecsl-org" => OtherServiceLink("hmceecslorgHref", "https://customs.hmrc.gov.uk/ecsl/httpssl/start.do", "otherservices.manageTaxes.link.hmceecslorg", true)
          case "hmrc-eu-ref-org" => OtherServiceLink("hmceecslorgHref", PortalConfig.getDestinationUrl("manageTaxes.euvat"), "otherservices.manageTaxes.link.hmrceureforg", false, true)
          case "hmce-vatrsl-org" => OtherServiceLink("hmcevatrslorgHref", "https://customs.hmrc.gov.uk/rcsl/httpssl/Home.do", "otherservices.manageTaxes.link.hmcevatrslorg", true)
        }
      case _ => List.empty
    }

  }


}

case class OtherServiceLink(id: String, url: String, text: String, newWindow: Boolean = false, sso: Boolean = false)
