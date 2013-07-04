package controllers

import play.api.mvc.{ AnyContent, Action }
import play.api.Logger
import play.api.data._
import play.api.data.Forms._
import microservice.ggw.{ GovernmentGatewayResponse, Credentials }
import microservice.UnauthorizedException

class LoginController extends BaseController with ActionWrappers with CookieEncryption {

  def login = Action {
    Ok(views.html.login())
  }

  def samlLogin = Action {
    val authRequestFormData = samlMicroService.create
    Ok(views.html.saml_auth_form(authRequestFormData.idaUrl, authRequestFormData.samlRequest))
  }

  def saLogin = Action {
    Ok(views.html.sa_login_form())
  }

  def ggwLogin: Action[AnyContent] = Action { implicit request =>

    val loginForm = Form(
      mapping(
        "userId" -> nonEmptyText,
        "password" -> nonEmptyText
      )(Credentials.apply)(Credentials.unapply)
    )
    val boundForm: Form[Credentials] = loginForm.bindFromRequest()
    if (boundForm.hasErrors) {
      Ok(views.html.sa_login_form(boundForm))
    } else {
      try {
        val ggwResponse: GovernmentGatewayResponse = ggwMicroService.login(boundForm.value.get)
        Redirect(routes.SaController.home()).withSession("userId" -> encrypt("/auth/oid/gfisher"), "ggwName" -> ggwResponse.name)
      } catch {
        case e: UnauthorizedException => {
          Ok(views.html.sa_login_form(boundForm.withGlobalError("Invalid user ID or password")))
        }
      }
    }
  }

  def enterAsJohnDensmore = Action {
    Redirect(routes.HomeController.home).withSession(("userId", encrypt("/auth/oid/newjdensmore")))
  }

  def enterAsGeoffFisher = Action {
    Redirect(routes.HomeController.home).withSession(("userId", encrypt("/auth/oid/gfisher")))
  }

  case class SAMLResponse(response: String)

  val responseForm = Form(
    mapping(
      "SAMLResponse" -> nonEmptyText
    )(SAMLResponse.apply)(SAMLResponse.unapply)
  )

  def idaLogin = Action { implicit request =>
    responseForm.bindFromRequest.fold(
      errors => {
        Logger.warn("SAML authentication response received without SAMLResponse data")
        Unauthorized(views.html.login())
      },
      samlResponse => {
        val validationResult = samlMicroService.validate(samlResponse.response)
        if (validationResult.valid) {
          authMicroService.authority(s"/auth/pid/${validationResult.hashPid.get}") match {
            case Some(authority) => {
              Redirect(routes.HomeController.home).withSession(("userId", encrypt(authority.id)))
            }
            case _ => {
              Logger.warn(s"No record found in Auth for the PID ${validationResult.hashPid.get}")
              Unauthorized(views.html.login())
            }
          }
        } else {
          Logger.warn("SAMLResponse failed validation")
          Unauthorized(views.html.login())
        }
      }
    )
  }

}
