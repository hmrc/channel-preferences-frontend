package controllers.paye.validation

import play.api.mvc.Results._
import scala.Some
import controllers.paye.routes
import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc.{Request, SimpleResult, Session}
import scala.util.Try
import scala.concurrent.Future
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import controllers.common.actions.{Actions, HeaderCarrier}
import uk.gov.hmrc.common.MdcLoggingExecutionContext._
import controllers.common.SessionKeys
import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime

object BenefitFlowHelper {
  type HodVersionNumber = Int

  val redirectToCarBenefitHome = Redirect(routes.CarBenefitHomeController.carBenefitHome().url)

  /**
   * This will check that there is an NPS version number in the play session, and
   * that it corresponds to the version number of the user. If they differ, this indicates that some
   * aspect of the user data was changed during the course of the page flow that was in progress.
   *
   * If the version number is absent from the session, this indicates that the page was accessed
   * directly, outside the normal flow, or that the session timed out.
   *
   * @return Either a SimpleResult, which means that something went wrong and the controller
   *         should use this result, or the HOD Version Number
   */
  def validateVersionNumber(user: User, session: Session)(implicit payeConnector: PayeConnector, hc: HeaderCarrier): Future[Either[SimpleResult, HodVersionNumber]] = {

    object Int {def unapply(s: String): Option[Int] = Try {s.toInt}.toOption}

    val noVersion = redirectToCarBenefitHome
    val errorLookingUpLatestVersion = redirectToCarBenefitHome
    val versionMismatch = Redirect(routes.VersionChangedController.versionChanged().url)

    val sessionVersion = session.get(SessionKeys.npsVersion)

    sessionVersion match {
      case None => Future.successful(Left(noVersion))
      case Some(Int(version)) => {
        user.getPaye.version.map { latestNpsVersion =>
          if (version != latestNpsVersion) {
            Left(versionMismatch)
          } else {
            Right(version)
          }
        }.recover { case _ => Left(errorLookingUpLatestVersion)}
      }
    }
  }
}
