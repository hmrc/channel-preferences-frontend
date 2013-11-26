package controllers.common.actions

import play.api.mvc._
import uk.gov.hmrc.common.microservice.domain.{RegimeRoots, TaxRegime, User}
import play.api.Logger
import controllers.common.{CookieEncryption, AuthenticationProvider}
import uk.gov.hmrc.common.microservice.auth.AuthConnector

import scala.Some
import play.api.mvc.SimpleResult
import views.html.login
import scala.concurrent._
import ExecutionContext.Implicits.global
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import uk.gov.hmrc.common.microservice.vat.domain.VatRoot
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeRoot
import uk.gov.hmrc.common.microservice.ct.domain.CtRoot
import controllers.common.service.Connectors


trait UserActionWrapper
  extends Results
  with CookieEncryption {

  protected implicit val authConnector: AuthConnector

  private[actions] def WithUserAuthorisedBy(authenticationProvider: AuthenticationProvider,
                                            taxRegime: Option[TaxRegime],
                                            redirectToOrigin: Boolean)
                                           (userAction: User => Action[AnyContent]): Action[AnyContent] =
    Action.async { request =>
      val handle = authenticationProvider.handleNotAuthenticated(request, redirectToOrigin) orElse handleAuthenticated(request, taxRegime)

      handle((request.session.get("userId"), request.session.get("token"))).flatMap {
        case Left(successfullyFoundUser) => userAction(successfullyFoundUser)(request)
        case Right(resultOfFailure) => Action(resultOfFailure)(request)
      }
    }

  private def handleAuthenticated(request: Request[AnyContent], taxRegime: Option[TaxRegime]): PartialFunction[(Option[String], Option[String]), Future[Either[User, SimpleResult]]] = {
    case (Some(encryptedUserId), tokenOption) =>

      implicit val hc = HeaderCarrier(request)
      val userId = decrypt(encryptedUserId)
      val token = tokenOption.map(decrypt)
      val userAuthority = authConnector.authority(userId)
      Logger.debug(s"Received user authority: $userAuthority")

      userAuthority.map { ua =>
        taxRegime match {
          case Some(regime) if !regime.isAuthorised(ua.regimes) =>
            Logger.info("user not authorised for " + regime.getClass)
            Future.successful(Right(Redirect(regime.unauthorisedLandingPage)))
          case _ =>
            regimeRoots(ua).map { regimeRoots =>
              Left(User(
                userId = userId,
                userAuthority = ua,
                regimes = regimeRoots,
                nameFromGovernmentGateway = decrypt(request.session.get("name")),
                decryptedToken = token))
            }
        }
      }.getOrElse {
        Logger.warn(s"No authority found for user id '$userId' from '${request.remoteAddress}'")
        Future.successful(Right(Unauthorized(login()).withNewSession))
      }
  }

  /**
   * NOTE: THE DEFAULT IMPLEMENTATION WILL BE REMOVED SHORTLY
   */
  protected def regimeRoots(authority: UserAuthority)(implicit hc: HeaderCarrier): Future[RegimeRoots] = {
    import controllers.common.service.Connectors._

    /**
     * Turns an Option of a Future into a Future of an Option:
     * Some(Future[T]) becomes Future(Some[T])
     * None becomes Future.successful(None)
     */
    def sequence[T](of: Option[Future[T]]): Future[Option[T]] = of.map(f => f.map(Option(_))).getOrElse(Future.successful(None))

    val regimes = authority.regimes
    val payefo = sequence(regimes.paye.map(uri => Connectors.payeConnector.root(uri.toString)))
    val safo = sequence(regimes.sa.flatMap(uri => authority.saUtr map (utr => saConnector.root(uri.toString).map(SaRoot(utr, _)))))
    val vatfo = sequence(regimes.vat.flatMap(uri => authority.vrn map (utr => vatConnector.root(uri.toString).map(VatRoot(utr, _)))))
    val epayefo = sequence(regimes.epaye.flatMap(uri => authority.empRef map (utr => epayeConnector.root(uri.toString).map(EpayeRoot(utr, _)))))
    val ctfo = sequence(regimes.ct.flatMap(uri => authority.ctUtr map (utr => ctConnector.root(uri.toString).map(CtRoot(utr, _)))))
    val agentfo = sequence(regimes.agent.map(uri => agentConnectorRoot.root(uri.toString)))

    for {
      paye <- payefo
      sa <- safo
      vat <- vatfo
      epaye <- epayefo
      ct <- ctfo
      agent <- agentfo
    } yield RegimeRoots(paye = paye, sa = sa, vat = vat, epaye = epaye, ct = ct, agent = agent)
  }
}