package controllers.common


import scala.concurrent._
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import controllers.common.actions.HeaderCarrier
import uk.gov.hmrc.common.microservice.auth.domain.Authority


trait AllRegimeRoots extends RegimeRootBase {
  def regimeRoots(authority: Authority)(implicit hc: HeaderCarrier): Future[RegimeRoots] = {
    val roots = (payeRoot(authority),saRoot(authority),vatRoot(authority),epayeRoot(authority),ctRoot(authority))
    for {
      paye <- roots._1
      sa <- roots._2
      vat <- roots._3
      epaye <- roots._4
      ct <- roots._5
    } yield RegimeRoots(paye = paye, sa = sa, vat = vat, epaye = epaye, ct = ct)
  }
}
