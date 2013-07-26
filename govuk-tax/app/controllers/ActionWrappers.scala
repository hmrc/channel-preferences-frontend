package controllers

import play.api.mvc._
import controllers.service._
import microservice.domain.{ RegimeRoots, TaxRegime, User }
import microservice.auth.domain.{ Regimes, UserAuthority }
import play.Logger
import java.net.URI

trait ActionWrappers extends MicroServices with CookieEncryption {
  self: Controller =>

  //todo test what happens if user is not authorised to be in this regime - at the time of writing front-end does not do a check
  object AuthorisedForAction {

    def apply[A <: TaxRegime](action: (User => (Request[AnyContent] => Result))): Action[AnyContent] = Action {
      request =>

        request.session.get("userId") match {

          case Some(encryptedUserId) =>

            val userId = decrypt(encryptedUserId)

            val userAuthority = authMicroService.authority(userId)

            Logger.debug(s"Received user authority: $userAuthority")

            userAuthority match {
              case Some(ua: UserAuthority) => action(User(user = userId, regimes = getRegimeRootsObject(ua.regimes), userAuthority = ua, nameFromGovernmentGateway = request.session.get("nameFromGovernmentGateway")))(request)
              case _ => Unauthorized(s"No authority found for user id '$userId'")
            }
          case None => {
            Logger.debug("No identity cookie found - redirecting to login.")
            Redirect(routes.HomeController.landing())
          }

        }

    }
  }

  private def getRegimeRootsObject(regimes: Regimes): RegimeRoots = RegimeRoots(
    paye = regimes.paye match {
      case Some(x: URI) => Some(payeMicroService.root(x.toString))
      case _ => None
    },
    sa = regimes.sa match {
      case Some(x: URI) => Some(saMicroService.root(x.toString))
      case _ => None
    },
    vat = if (regimes.vat.isEmpty) { None } else { Some("#") } //todo change it once we have VAT service / its stub ready
  )

}
