package controllers

import play.api.mvc._
import controllers.service._
import microservice.domain.{ RegimeRoots, TaxRegime, User }
import microservice.auth.domain.UserAuthority
import play.Logger

trait ActionWrappers extends MicroServices {
  self: Controller =>

  object AuthorisedForAction {

    def apply[A <: TaxRegime](action: (User => Request[AnyContent] => Result)): Action[AnyContent] = Action {
      request =>
        // Today, as there is no IDA we'll assume that you are John Densmore
        // He has a oid of /auth/oid/jdensmore, so we'll get that from the auth service
        // TODO: This will need to handle session management / authentication when we support IDA

        request.cookies.get("userId") match {

          case Some(cookie) =>

            val userId = cookie.value
            val userAuthority = authMicroService.authority(userId)

            Logger.debug(s"Reveived user authority: $userAuthority")

            userAuthority match {
              case Some(ua: UserAuthority) => action(User(user = userId, regimes = getRegimeRootObjects(ua.regimes), userAuthority = ua))(request)
              case _ => Unauthorized(s"No authority found for user id '$userId'")
            }
          case None => {
            Logger.debug("No identity cookie found - redirecting to login.")
            Redirect(routes.HomeController.landing())
          }

        }

    }
  }

  private def getRegimeRootObjects(regimes: Map[String, String]): RegimeRoots = {
    val payeRegimeUri = regimes("paye")
    RegimeRoots(paye = Some(payeMicroService.root(payeRegimeUri)))
  }
}
