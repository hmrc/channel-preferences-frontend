package controllers.bt

import scala.concurrent._
import controllers.common.RegimeRootBase
import uk.gov.hmrc.common.microservice.auth.domain.Authority
import controllers.common.actions.HeaderCarrier
import uk.gov.hmrc.common.microservice.domain.{RegimeRoot, RegimeRoots}
import uk.gov.hmrc.common.microservice.MicroServiceException
import java.net.ConnectException
import uk.gov.hmrc.common.MdcLoggingExecutionContext


trait BusinessTaxRegimeRoots extends RegimeRootBase {

  import MdcLoggingExecutionContext._

  def regimeRoots(authority: Authority)(implicit hc: HeaderCarrier): Future[RegimeRoots] = {
    for {
      sa <- recover(saRoot(authority))
      vat <- recover(vatRoot(authority))
      epaye <- recover(epayeRoot(authority))
      ct <- recover(ctRoot(authority))
    } yield RegimeRoots(sa = sa, vat = vat, epaye = epaye, ct = ct)
  }

  def recover[T <: RegimeRoot[_]](futureOptionRegimeRoot: Future[Option[T]])(implicit executionContext: ExecutionContext) = futureOptionRegimeRoot.recover {
    case _:ConnectException | _:MicroServiceException => None
  }
}
