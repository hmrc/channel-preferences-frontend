package controllers

import play.api.mvc._
import controllers.service._
import microservice.domain.{ RegimeRoots, TaxRegime, User }
import microservice.auth.domain.UserAuthority
import play.Logger

trait ActionWrappers extends MicroServices {
  self: Controller =>

  object AuthorisedForAction {

    def apply[A <: TaxRegime](action: (User => (Request[AnyContent] => Result))): Action[AnyContent] = Action {
      request =>
        // Today, as there is no IDA we'll assume that you are John Densmore
        // He has a oid of /auth/oid/jdensmore, so we'll get that from the auth service
        // TODO: This will need to handle session management / authentication when we support IDA

        request.cookies.get("userId") match {

          case Some(cookie) =>

            val userId = cookie.value
            val userAuthority = authMicroService.authority(userId)

            Logger.debug(s"Received user authority: $userAuthority")

            userAuthority match {
              case Some(ua: UserAuthority) => action(User(user = userId, regimes = getRegimeRootsObject(ua.regimes), userAuthority = ua))(request)
              case _ => Unauthorized(s"No authority found for user id '$userId'")
            }
          case None => {
            Logger.debug("No identity cookie found - redirecting to login.")
            Redirect(routes.HomeController.landing())
          }

        }

    }
  }

  //todo maybe move this logic into UserAuthority object?
  private def getRegimeRootsObject(regimes: Map[String, String]): RegimeRoots = {
    val payeRootUri = regimes.get("paye")
    val saRootUri = regimes.get("sa")
    println("payeRootUri: " + payeRootUri + ", saRootUri: " + saRootUri)

    RegimeRoots(
      paye = payeRootUri match {
        case Some(x: String) => Some(payeMicroService.root(x))
        case _ => None
      },
      sa = saRootUri match {
        case Some(x: String) => Some(saMicroService.root(x))
        case _ => None
      }
    )
  }
}
