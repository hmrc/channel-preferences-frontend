package controllers.common

import play.api.mvc.{AnyContent, Action}
import play.api.Logger
import play.api.data._
import play.api.data.Forms._
import uk.gov.hmrc.common.microservice.governmentgateway.{GovernmentGatewayConnector, GovernmentGatewayResponse, Credentials}
import uk.gov.hmrc.microservice.{ForbiddenException, UnauthorizedException}
import controllers.common.service.{Connectors, FrontEndConfig}
import java.util.UUID
import uk.gov.hmrc.common.microservice.saml.SamlConnector
import uk.gov.hmrc.common.microservice.audit.{AuditEvent, AuditConnector}
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import controllers.common.actions.{HeaderCarrier, Actions}


class LoginController(samlConnector: SamlConnector,
                      governmentGatewayConnector: GovernmentGatewayConnector,
                      override val auditConnector: AuditConnector)
                     (implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions {

  def login = WithNewSessionTimeout(UnauthorisedAction { implicit request =>
    Ok(views.html.login())
  })

  def this() = this(Connectors.samlConnector, Connectors.governmentGatewayConnector, Connectors.auditConnector)(Connectors.authConnector)

  def samlLogin = WithNewSessionTimeout(UnauthorisedAction { implicit request =>
    val authRequestFormData = samlConnector.create(HeaderCarrier(request))
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
        val response: GovernmentGatewayResponse = governmentGatewayConnector.login(boundForm.value.get)(HeaderCarrier(request))
        FrontEndRedirect.toBusinessTaxFromLogin
          .withSession("sessionId" -> encrypt(s"session-${UUID.randomUUID().toString}"), "userId" -> encrypt(response.authId), "name" -> encrypt(response.name), "affinityGroup" -> encrypt(response.affinityGroup), "token" -> encrypt(response.encodedGovernmentGatewayToken.encodeBase64))
      } catch {
        case _: UnauthorizedException => Unauthorized(views.html.ggw_login_form(boundForm.withGlobalError("Invalid username or password. Try again.")))
        case _: ForbiddenException => Forbidden(notOnBusinessTaxWhitelistPage)
      }
    }
  })

  def notOnBusinessTaxWhitelistPage = views.html.whitelist_notice()

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
        val validationResult = samlConnector.validate(samlResponse.response)
        if (validationResult.valid) {
          val updatedHC = hc.copy(requestId = validationResult.originalRequestId, sessionId = Some(s"session-${UUID.randomUUID().toString}"))
          val hashPid = validationResult.hashPid.get
          authConnector.authorityByPidAndUpdateLoginTime(hashPid)(updatedHC) match {
            case Some(authority) => {
              auditConnector.audit(
                AuditEvent(
                  auditType = "TxSucceded",
                  tags = Map("transactionName" -> "IDA Login Completion", xRequestId+"-Original" -> hc.requestId.getOrElse("")) ++ updatedHC.headers.toMap,
                  detail = Map("hashPid" -> hashPid, "authId" -> authority.id)
                    ++ authority.saUtr.map("saUtr" -> _.utr).toMap
                    ++ authority.vrn.map("vrn" -> _.vrn).toMap
                    ++ authority.ctUtr.map("ctUtr" -> _.utr).toMap
                    ++ authority.empRef.map("empRef" -> _.toString).toMap
                    ++ authority.nino.map("nino" -> _.nino).toMap
                    ++ authority.uar.map("uar" -> _.uar).toMap
                )
              )
              val target = FrontEndRedirect.forSession(session)
              target.withSession("userId" -> encrypt(authority.id), "sessionId" -> encrypt(updatedHC.sessionId.get))
            }
            case _ => {
              Logger.warn(s"No record found in Auth for the PID ${hashPid}")
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
