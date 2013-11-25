package controllers.common.actions

import play.api.mvc._
import uk.gov.hmrc.common.microservice.domain.TaxRegime
import play.api.Logger
import controllers.common.{CookieEncryption, AuthenticationProvider}
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import controllers.common.service.Connectors._
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import scala.Some
import play.api.mvc.SimpleResult
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import uk.gov.hmrc.common.microservice.vat.domain.VatRoot
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeRoot
import uk.gov.hmrc.common.microservice.ct.domain.CtRoot
import views.html.login

trait UserActionWrapper
  extends Results
  with CookieEncryption {

  protected implicit val authConnector: AuthConnector

  private[actions] def WithUserAuthorisedBy(authenticationProvider: AuthenticationProvider,
                                            taxRegime: Option[TaxRegime],
                                            redirectToOrigin: Boolean)
                                           (action: User => Action[AnyContent]): Action[AnyContent] =
    Action.async {
      request =>
        val handle = authenticationProvider.handleNotAuthenticated(request, redirectToOrigin) orElse handleAuthenticated(request, taxRegime)

        handle((request.session.get("userId"), request.session.get("token"))) match {
          case Left(successfullyFoundUser) => action(successfullyFoundUser)(request)
          case Right(resultOfFailure) => Action(resultOfFailure)(request)
        }
    }

  private def handleAuthenticated(request: Request[AnyContent], taxRegime: Option[TaxRegime]): PartialFunction[(Option[String], Option[String]), Either[User, SimpleResult]] = {
    case (Some(encryptedUserId), tokenOption) =>

      implicit val hc = HeaderCarrier(request)
      val userId = decrypt(encryptedUserId)
      val token = tokenOption.map(decrypt)
      val userAuthority = authConnector.authority(userId)
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
                regimes = regimeRoots(ua)(HeaderCarrier(request)),
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

  protected def regimeRoots(authority: UserAuthority)(implicit hc: HeaderCarrier): RegimeRoots

}