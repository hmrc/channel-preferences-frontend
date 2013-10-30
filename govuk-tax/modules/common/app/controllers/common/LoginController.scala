package controllers.common

import play.api.mvc.{ AnyContent, Action }
import play.api.Logger
import play.api.data._
import play.api.data.Forms._
import uk.gov.hmrc.common.microservice.governmentgateway.{GovernmentGatewayMicroService, GovernmentGatewayResponse, Credentials}
import uk.gov.hmrc.microservice.{ForbiddenException, UnauthorizedException}
import controllers.common.service.FrontEndConfig
import java.util.UUID
import uk.gov.hmrc.common.microservice.saml.SamlMicroService
import uk.gov.hmrc.common.microservice.audit.AuditMicroService
import uk.gov.hmrc.common.microservice.auth.AuthMicroService


class LoginController(samlMicroService : SamlMicroService,
                      governmentGatewayMicroService : GovernmentGatewayMicroService,
                      override val auditMicroService: AuditMicroService)
                     (implicit override val authMicroService: AuthMicroService)
  extends BaseController2
  with Actions {

  def login = WithNewSessionTimeout(UnauthorisedAction { implicit request =>
    Ok(views.html.login())
  })

  def samlLogin = WithNewSessionTimeout(UnauthorisedAction { implicit request =>
    val authRequestFormData = samlMicroService.create
    Ok(views.html.saml_auth_form(authRequestFormData.idaUrl, authRequestFormData.samlRequest))
  })

  def businessTaxLogin = WithNewSessionTimeout(UnauthorisedAction { implicit request =>
    Ok(views.html.ggw_login_form())
  })

  def governmentGatewayLogin: Action[AnyContent] = WithNewSessionTimeout(UnauthorisedAction { implicit request =>

    val loginForm = Form(
      mapping(
        "userId" -> nonEmptyText,
        "password" -> nonEmptyText
      )(Credentials.apply)(Credentials.unapply)
    )
    val boundForm: Form[Credentials] = loginForm.bindFromRequest()
    if (boundForm.hasErrors) {
      Ok(views.html.ggw_login_form(boundForm))
    } else {
      try {
        val response: GovernmentGatewayResponse = governmentGatewayMicroService.login(boundForm.value.get)
        FrontEndRedirect.toBusinessTax
          .withSession("sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"), "userId" -> encrypt(response.authId), "name" -> encrypt(response.name), "affinityGroup" -> encrypt(response.affinityGroup), "token" -> encrypt(response.encodedGovernmentGatewayToken))
      } catch {
        case _: UnauthorizedException => Unauthorized(views.html.ggw_login_form(boundForm.withGlobalError("Invalid User ID or Password")))
        case _: ForbiddenException => Forbidden(notOnBusinessTaxWhitelistPage)
      }
    }
  })

  private[common] def notOnBusinessTaxWhitelistPage = views.html.whitelist_notice()

  case class SAMLResponse(response: String)

  val responseForm = Form(
    mapping(
      "SAMLResponse" -> nonEmptyText
    )(SAMLResponse.apply)(SAMLResponse.unapply)
  )

  def idaLogin = WithNewSessionTimeout(UnauthorisedAction { implicit request =>
    responseForm.bindFromRequest.fold(
      errors => {
        Logger.warn("SAML authentication response received without SAMLResponse data")
        Unauthorized(views.html.login_error())
      },
      samlResponse => {
        val validationResult = samlMicroService.validate(samlResponse.response)
        if (validationResult.valid) {
          authMicroService.authorityByPidAndUpdateLoginTime(validationResult.hashPid.get) match {
            case Some(authority) => {
              val target = FrontEndRedirect.forSession(session)
              target.withSession("userId"-> encrypt(authority.id), "sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"))
            }
            case _ => {
              Logger.warn(s"No record found in Auth for the PID ${validationResult.hashPid.get}")
              Unauthorized(views.html.login_error())
            }
          }
        } else {
          Logger.warn("SAMLResponse failed validation")
          Unauthorized(views.html.login_error())
        }
      }
    )
  })

  def logout = Action {
    Redirect(FrontEndConfig.portalSsoInLogoutUrl)
  }

  def loggedout = WithNewSessionTimeout(UnauthorisedAction {
    implicit request =>
      Ok(views.html.logged_out()).withNewSession
  })
}
