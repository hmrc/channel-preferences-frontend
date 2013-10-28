package controllers.common.actions


import play.api.mvc._
import uk.gov.hmrc.common.microservice.domain.{TaxRegime, User}
import views.html.login
import play.api.Logger
import controllers.common.{ServiceRoots, AuthorisationTypes}

trait UserActionWrapper extends AuthorisationTypes
                           with Results
                           with ServiceRoots {

  import uk.gov.hmrc.common.microservice.auth.AuthMicroService

  implicit val authMicroService : AuthMicroService

  object WithUserAuthorisedBy {
    def apply(authenticationType: AuthorisationType)
             (taxRegime: Option[TaxRegime] = None, redirectToOrigin: Boolean = false)
             (action: User => Action[AnyContent]): Action[AnyContent] =
      Action.async { request =>
        val handle = authenticationType.handleNotAuthorised(request, redirectToOrigin) orElse handleAuthorised(request, taxRegime)

        handle((request.session.get("userId"), request.session.get("token"))) match {
          case Left(successfullyFoundUser) => action(successfullyFoundUser)(request)
          case Right(resultOfFailure) => Action(resultOfFailure)(request)
        }
      }

    def handleAuthorised(request: Request[AnyContent], taxRegime: Option[TaxRegime]): PartialFunction[(Option[String], Option[String]), Either[User, SimpleResult]] = {
      case (Some(encryptedUserId), tokenOption) =>
        val userId = decrypt(encryptedUserId)
        val token = tokenOption.map(decrypt)
        val userAuthority = authMicroService.authority(userId)
        Logger.debug(s"Received user authority: $userAuthority")

        userAuthority match {
          case (Some(ua)) => {
            taxRegime match {
              case Some(regime) if !regime.isAuthorised(ua.regimes) =>
                Logger.info("user not authorised for " + regime.getClass)
                Right(Redirect(regime.unauthorisedLandingPage))
              case _ =>
                Left(User(
                  userId = userId,
                  userAuthority = ua,
                  regimes = regimeRoots(ua),
                  nameFromGovernmentGateway = decrypt(request.session.get("name")),
                  decryptedToken = token))
            }
          }
          case _ => {
            Logger.warn(s"No authority found for user id '$userId' from '${request.remoteAddress}'")
            Right(Unauthorized(login()).withNewSession)
          }
        }
    }
  }
}