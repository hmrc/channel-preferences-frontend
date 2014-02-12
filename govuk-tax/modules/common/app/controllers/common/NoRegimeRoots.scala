package controllers.common

import uk.gov.hmrc.common.microservice.auth.domain.Authority
import controllers.common.actions.HeaderCarrier
import scala.concurrent.Future
import uk.gov.hmrc.common.microservice.domain.RegimeRoots

trait NoRegimeRoots extends RegimeRootsProvider {

  def regimeRoots(authority: Authority)(implicit hc: HeaderCarrier): Future[RegimeRoots] = Future.successful(RegimeRoots())
}
