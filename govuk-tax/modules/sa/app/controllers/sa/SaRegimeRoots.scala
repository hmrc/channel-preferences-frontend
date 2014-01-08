package controllers.sa

import scala.concurrent._
import controllers.common.RegimeRootBase
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import controllers.common.actions.HeaderCarrier
import uk.gov.hmrc.common.microservice.auth.domain.Authority
import uk.gov.hmrc.common.MdcLoggingExecutionContext


trait SaRegimeRoots extends RegimeRootBase {

  import MdcLoggingExecutionContext._

  def regimeRoots(authority: Authority)(implicit hc: HeaderCarrier): Future[RegimeRoots] = {
    val sar = saRoot(authority)
    for {
      sa <- sar
    } yield RegimeRoots(sa = sa)
  }
}
