package controllers

import play.api.mvc.Action
import play.api.Logger
import play.api.data._
import play.api.data.Forms._
import microservice.ggw.Credentials
import microservice.auth.domain.UserAuthority
import play.api.i18n.Messages
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

  def ggwLogin = Action { implicit request =>

    val loginForm = Form(
      mapping(
        "userId" -> nonEmptyText,
        "password" -> nonEmptyText
      )(Credentials.apply)(Credentials.unapply)
    )

    loginForm.bindFromRequest.fold(
      formWithErrors => {

        //todo fix this  - the map should not contain the field name key if that field has no error(s)
        val allErrors = Map("userId" -> formWithErrors("userId").errors.map(error => Messages(error.message)), "password" -> formWithErrors("password").errors.map(error => Messages(error.message)))
        Ok(views.html.sa_login_form(allErrors))
      },
      credentials => {
        try {
          val userAuthority: UserAuthority = ggwMicroService.login(credentials)
          userAuthority.regimes.get("sa") match {
            case Some(uri: String) => Redirect(routes.SaController.home()).withSession(("userId", encrypt("/auth/oid/gfisher")))
            //todo display a link to enrolment here - gets html escaped if we just type it in here - also make sure the error message is clear to the user
            case _ => Ok(views.html.sa_login_form(Map("global" -> Seq("""You are not enrolled for Self Assessment (SA) services at the Government Gateway. Please enrol first."""))))
          }
        } catch {
          case e: UnauthorizedException => {
            Ok(views.html.sa_login_form(Map("global" -> Seq("Invalid user ID or password"))))
          }
        }

      }
    )

  }

  def enterAsJohnDensmore = Action {
    Redirect(routes.HomeController.home()).withSession(("userId", encrypt("/auth/oid/jdensmore")))
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
