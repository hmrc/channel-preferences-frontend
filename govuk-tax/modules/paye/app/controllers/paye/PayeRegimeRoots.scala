package controllers.paye

import scala.concurrent._
import controllers.common.RegimeRootBase
import uk.gov.hmrc.common.microservice.auth.domain.Authority
import controllers.common.actions.HeaderCarrier
import uk.gov.hmrc.common.microservice.domain.RegimeRoots

trait PayeRegimeRoots extends RegimeRootBase {
   def regimeRoots(authority: Authority)(implicit hc: HeaderCarrier): Future[RegimeRoots] = {
    for (paye <- payeRoot(authority)) yield RegimeRoots(paye = paye)
  }
}
