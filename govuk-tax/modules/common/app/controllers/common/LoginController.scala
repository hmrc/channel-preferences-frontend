package controllers.common

import play.api.mvc.{Session, AnyContent, Action}
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
  with Actions
  with AllRegimeRoots {

  def login = WithNewSessionTimeout(UnauthorisedAction {
    implicit request =>
      Ok(views.html.login())
  })

  def this() = this(Connectors.samlConnector, Connectors.governmentGatewayConnector, Connectors.auditConnector)(Connectors.authConnector)

  def samlLogin = WithNewSessionTimeout(UnauthorisedAction {
    implicit request =>
      val authRequestFormData = samlConnector.create(HeaderCarrier(request))
      Ok(views.html.saml_auth_form(authRequestFormData.idaUrl, authRequestFormData.samlRequest))
  })

  def businessTaxLogin = WithNewSessionTimeout(UnauthorisedAction {
    implicit request =>
      Ok(views.html.ggw_login_form())
  })

  def governmentGatewayLogin: Action[AnyContent] = WithNewSessionTimeout(UnauthorisedAction {
    implicit request =>

      val loginForm = Form(
        mapping(
          "userId" -> nonEmptyText,
          "password" -> nonEmptyText
        )(Credentials.apply)(Credentials.unapply)
      )
      val form = loginForm.bindFromRequest()
      form.fold (
        erroredForm => Ok(views.html.ggw_login_form(erroredForm)),
        credentials => {
          try {
            val response = governmentGatewayConnector.login(credentials)(HeaderCarrier(request))
            val sessionId = s"session-${UUID.randomUUID().toString}"
            auditConnector.audit(
              AuditEvent(
                auditType = "TxSucceded",
                tags = Map(
                  "transactionName" -> "GG Login Completion",
                  HeaderNames.xSessionId -> sessionId
                ) ++ hc.headers.toMap,
                detail = Map("authId" -> response.authId)
              )
            )
            FrontEndRedirect.toBusinessTax.withSession(Session(Map(
              "sessionId" -> sessionId,
              "userId" -> response.authId,
              "name" -> response.name,
              "token" -> response.encodedGovernmentGatewayToken.encodeBase64
            ).mapValues(encrypt)))
          } catch {
            case _: UnauthorizedException => Unauthorized(views.html.ggw_login_form(form.withGlobalError("Invalid username or password. Try again.")))
            case _: ForbiddenException => Forbidden(notOnBusinessTaxWhitelistPage)
          }
        }
      )
  })

  def notOnBusinessTaxWhitelistPage = views.html.whitelist_notice()

  case class SAMLResponse(response: String)

  val responseForm = Form(
    mapping(
      "SAMLResponse" -> nonEmptyText
    )(SAMLResponse.apply)(SAMLResponse.unapply)
  )

  def idaLogin = WithNewSessionTimeout(UnauthorisedAction {
    implicit request =>
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
                    tags = Map("transactionName" -> "IDA Login Completion", xRequestId + "-Original" -> hc.requestId.getOrElse("")) ++ updatedHC.headers.toMap,
                    detail = Map("hashPid" -> hashPid, "authId" -> authority.uri)
                      ++ authority.accounts.sa.map("saUtr" -> _.utr.utr).toMap
                      ++ authority.accounts.vat.map("vrn" -> _.vrn.vrn).toMap
                      ++ authority.accounts.ct.map("ctUtr" -> _.utr.utr).toMap
                      ++ authority.accounts.epaye.map("empRef" -> _.empRef.toString).toMap
                      ++ authority.accounts.paye.map("nino" -> _.nino.nino).toMap
                      ++ authority.accounts.agent.map("uar" -> _.uar.uar).toMap
                  )
                )
                val target = FrontEndRedirect.forSession(session)
                target.withSession("userId" -> encrypt(authority.uri), "sessionId" -> encrypt(updatedHC.sessionId.get))
              }
              case _ => {
                Logger.warn(s"No record found in Auth for the PID $hashPid")
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
