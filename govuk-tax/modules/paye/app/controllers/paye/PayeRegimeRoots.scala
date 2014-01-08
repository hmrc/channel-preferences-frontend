package controllers.paye

import scala.concurrent._
import controllers.common.RegimeRootBase
import uk.gov.hmrc.common.microservice.auth.domain.Authority
import controllers.common.actions.HeaderCarrier
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import uk.gov.hmrc.common.MdcLoggingExecutionContext

trait PayeRegimeRoots extends RegimeRootBase {

  import MdcLoggingExecutionContext._

   def regimeRoots(authority: Authority)(implicit hc: HeaderCarrier): Future[RegimeRoots] = {
    for (paye <- payeRoot(authority)) yield RegimeRoots(paye = paye)
  }
}
