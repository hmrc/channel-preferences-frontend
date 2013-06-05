package controllers

import play.api.mvc._
import controllers.service._
import microservice.domain.{ RegimeRoots, TaxRegime, User }

trait ActionWrappers extends MicroServices {
  self: Controller =>

  object AuthorisedForAction {

    def apply[A <: TaxRegime](action: (User => Request[AnyContent] => Result)): Action[AnyContent] = Action {
      request =>
        // Today, as there is no IDA we'll assume that you are John Densmore
        // He has a oid of /auth/oid/jdensmore, so we'll get that from the auth service
        // TODO: This will need to handle session management / authentication when we support IDA

        val userId = "/auth/oid/jdensmore"
        val userAuthority = authMicroService.authority(userId)

        action(User(
          regime = getRegimeRootObjects(userAuthority.regimes),
          userAuthority = userAuthority))(request)
    }
  }

  private def getRegimeRootObjects(regimes: Map[String, String]): RegimeRoots = {
    val payeRegimeUri = regimes("paye")

    RegimeRoots(paye = Some(payeMicroService.root(payeRegimeUri)))
  }
}
