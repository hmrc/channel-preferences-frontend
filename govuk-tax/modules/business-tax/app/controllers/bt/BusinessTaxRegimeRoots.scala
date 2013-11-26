package controllers.bt

import scala.concurrent._
import controllers.common.RegimeRootBase
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import controllers.common.actions.HeaderCarrier
import uk.gov.hmrc.common.microservice.domain.RegimeRoots

trait BusinessTaxRegimeRoots extends RegimeRootBase {
  def regimeRoots(authority: UserAuthority)(implicit hc: HeaderCarrier): Future[RegimeRoots] = {
    val roots = (saRoot(authority), vatRoot(authority), epayeRoot(authority), ctRoot(authority))
    for {
      sa <- roots._1
      vat <- roots._2
      epaye <- roots._3
      ct <- roots._4
    } yield RegimeRoots(sa = sa, vat = vat, epaye = epaye, ct = ct)
  }
}