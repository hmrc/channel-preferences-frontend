package controllers.sa.prefs

import controllers.common.RegimeRootBase
import uk.gov.hmrc.common.microservice.auth.domain.Authority
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.common.microservice.domain.{RegimeRoot, RegimeRoots}
import java.net.ConnectException
import uk.gov.hmrc.common.microservice.MicroServiceException
import uk.gov.hmrc.play.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.connectors.HeaderCarrier

trait SaRegimeRoots extends RegimeRootBase {

  def regimeRoots(authority: Authority)(implicit hc: HeaderCarrier): Future[RegimeRoots] =
    for {possibleSaRoot <- recover(saRoot(authority))} yield RegimeRoots(sa = possibleSaRoot)

  def recover[T <: RegimeRoot[_]](futureOptionRegimeRoot: Future[Option[T]])(implicit executionContext: ExecutionContext) = futureOptionRegimeRoot.recover {
    case _:ConnectException | _:MicroServiceException => None
  }
}
