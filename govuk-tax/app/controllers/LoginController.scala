package controllers

import play.api.mvc.{ AnyContent, Action }
import play.api.Logger
import play.api.data._
import play.api.data.Forms._
import microservice.governmentgateway.{ GovernmentGatewayResponse, Credentials }
import microservice.UnauthorizedException

class LoginController extends BaseController with ActionWrappers with CookieEncryption {

  def login = Action {
    Ok(views.html.login())
  }

  def samlLogin = Action {
    val authRequestFormData = samlMicroService.create
    Ok(views.html.saml_auth_form(authRequestFormData.idaUrl, authRequestFormData.samlRequest))
  }

  def businessTaxLogin = Action {
    Ok(views.html.business_tax_login_form())
  }

  def governmentGatewayLogin: Action[AnyContent] = Action { implicit request =>

    val loginForm = Form(
      mapping(
        "userId" -> nonEmptyText,
        "password" -> nonEmptyText
      )(Credentials.apply)(Credentials.unapply)
    )
    val boundForm: Form[Credentials] = loginForm.bindFromRequest()
    if (boundForm.hasErrors) {
      Ok(views.html.business_tax_login_form(boundForm))
    } else {
      try {
        val response: GovernmentGatewayResponse = governmentGatewayMicroService.login(boundForm.value.get)
        Redirect(routes.BusinessTaxController.home()).withSession("userId" -> encrypt(response.authId), "nameFromGovernmentGateway" -> response.name)
      } catch {
        case e: UnauthorizedException => {
          Ok(views.html.business_tax_login_form(boundForm.withGlobalError("Invalid User ID or Password")))
        }
      }
    }
  }

  def enterAsJohnDensmore = Action {
    Redirect(routes.HomeController.home).withSession(("userId", encrypt("/auth/oid/newjdensmore")))
  }

  def enterAsGeoffFisher = Action {
    val credentials = Credentials("805933359724", "passw0rd")

    try {
      val loginResponse: GovernmentGatewayResponse = governmentGatewayMicroService.login(credentials)
      Redirect(routes.BusinessTaxController.home()).withSession("userId" -> encrypt(loginResponse.authId), "nameFromGovernmentGateway" -> loginResponse.name)
    } catch {
      case e: UnauthorizedException => {
        val loginForm = Form(
          mapping(
            "userId" -> nonEmptyText,
            "password" -> nonEmptyText
          )(Credentials.apply)(Credentials.unapply)
        )
        val boundForm: Form[Credentials] = loginForm.fill(credentials)
        Ok(views.html.business_tax_login_form(boundForm.withGlobalError("Invalid User ID or Password")))
      }
    }
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
        Unauthorized(views.html.login_error())
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
              Unauthorized(views.html.login_error())
            }
          }
        } else {
          Logger.warn("SAMLResponse failed validation")
          Unauthorized(views.html.login_error())
        }
      }
    )
  }

}
