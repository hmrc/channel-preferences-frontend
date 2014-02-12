package controllers.common

import controllers.common.service.{ConnectorsApi, Connectors}
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import uk.gov.hmrc.common.microservice.vat.domain.VatRoot
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeRoot
import uk.gov.hmrc.common.microservice.ct.domain.CtRoot
import scala.concurrent._
import uk.gov.hmrc.common.microservice.auth.domain.Authority
import controllers.common.actions.HeaderCarrier
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.common.MdcLoggingExecutionContext

trait RegimeRootBase extends RegimeRootsProvider {

  import MdcLoggingExecutionContext._

  val connectors: ConnectorsApi = Connectors
  import connectors._
  /**
   * Turns an Option of a Future into a Future of an Option:
   * Some(Future[T]) becomes Future(Some[T])
   * None becomes Future.successful(None)
   */
  implicit def sequence[T](of: Option[Future[T]])(implicit hc: HeaderCarrier): Future[Option[T]] = of.map(f => f.map(Option(_))).getOrElse(Future.successful(None))

  def payeRoot(authority: Authority)(implicit hc: HeaderCarrier): Future[Option[PayeRoot]] = authority.accounts.paye.map(paye => payeConnector.root(paye.link))

  def saRoot(authority: Authority)(implicit hc: HeaderCarrier): Future[Option[SaRoot]] = authority.accounts.sa.map(sa => saConnector.root(sa.link).map(SaRoot(sa.utr, _)))

  def vatRoot(authority: Authority)(implicit hc: HeaderCarrier): Future[Option[VatRoot]] = authority.accounts.vat.map(vat => vatConnector.root(vat.link).map(VatRoot(vat.vrn, _)))

  def epayeRoot(authority: Authority)(implicit hc: HeaderCarrier): Future[Option[EpayeRoot]] = authority.accounts.epaye.map(epaye => epayeConnector.root(epaye.link).map(EpayeRoot(epaye.empRef, _)))

  def ctRoot(authority: Authority)(implicit hc: HeaderCarrier): Future[Option[CtRoot]] = authority.accounts.ct.map(ct => ctConnector.root(ct.link).map(CtRoot(ct.utr, _)))
}
