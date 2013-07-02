package controllers

import play.api.mvc.Action
import play.api.Logger
import play.api.data._
import play.api.data.Forms._
import microservice.ggw.Credentials
import microservice.auth.domain.UserAuthority
import play.mvc.Result

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
    import play.api.data.Forms._

    val loginForm = Form(
      mapping(
        "userId" -> text,
        "password" -> text
      )(Credentials.apply)(Credentials.unapply)
    )
    //todo - send request to GGW service /government-gateway/login with Credentials object - that will communicate
    //todo  with GGW, create the Auth record and return an Authority object here - if all is good we drop cookie here and redirect to SaController

    loginForm.bindFromRequest.fold(
      formWithErrors => {
        //todo form binding errors
      },
      credentials => {
        val userAuthority:UserAuthority = ggwMicroService.login(credentials) //todo 401 might come back - sort out exception mapping
        //todo check if sa is there
      }
    )

    Redirect(routes.SaController.home()).withSession(("userId", encrypt("/auth/oid/gfisher")))
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
