package controllers.bt

import scala.concurrent._
import controllers.common.RegimeRootBase
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import controllers.common.actions.HeaderCarrier
import uk.gov.hmrc.common.microservice.domain.RegimeRoots

trait BusinessTaxRegimeRoots extends RegimeRootBase {
  def regimeRoots(authority: UserAuthority)(implicit hc: HeaderCarrier): Future[RegimeRoots] = {
    for {
      sa <- saRoot(authority)
      vat <- vatRoot(authority)
      epaye <- epayeRoot(authority)
      ct <- ctRoot(authority)
    } yield RegimeRoots(sa = sa, vat = vat, epaye = epaye, ct = ct)
  }
}