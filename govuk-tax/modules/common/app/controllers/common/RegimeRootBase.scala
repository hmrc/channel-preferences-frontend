package controllers.common

import controllers.common.service.Connectors._
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot
import uk.gov.hmrc.common.microservice.vat.domain.VatRoot
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeRoot
import uk.gov.hmrc.common.microservice.ct.domain.CtRoot
import scala.concurrent._
import uk.gov.hmrc.common.microservice.auth.domain.UserAuthority
import controllers.common.actions.HeaderCarrier
import uk.gov.hmrc.common.microservice.domain.RegimeRoots
import uk.gov.hmrc.common.microservice.paye.domain.PayeRoot
import uk.gov.hmrc.common.microservice.agent.AgentRoot

trait RegimeRootBase {
  implicit val executionContext = ExecutionContext.Implicits.global

  /**
   * Turns an Option of a Future into a Future of an Option:
   * Some(Future[T]) becomes Future(Some[T])
   * None becomes Future.successful(None)
   */
  implicit def sequence[T](of: Option[Future[T]]): Future[Option[T]] = of.map(f => f.map(Option(_))).getOrElse(Future.successful(None))

  def payeRoot(authority: UserAuthority)(implicit hc: HeaderCarrier): Future[Option[PayeRoot]] = authority.regimes.paye.map(uri => payeConnector.root(uri.toString))

  def saRoot(authority: UserAuthority)(implicit hc: HeaderCarrier): Future[Option[SaRoot]] = authority.regimes.sa.flatMap(uri => authority.saUtr.map (utr => saConnector.root(uri.toString).map(SaRoot(utr, _))))

  def vatRoot(authority: UserAuthority)(implicit hc: HeaderCarrier): Future[Option[VatRoot]] = authority.regimes.vat.flatMap(uri => authority.vrn.map (utr => vatConnector.root(uri.toString).map(VatRoot(utr, _))))

  def epayeRoot(authority: UserAuthority)(implicit hc: HeaderCarrier): Future[Option[EpayeRoot]] = authority.regimes.epaye.flatMap(uri => authority.empRef.map (utr => epayeConnector.root(uri.toString).map(EpayeRoot(utr, _))))

  def ctRoot(authority: UserAuthority)(implicit hc: HeaderCarrier): Future[Option[CtRoot]] = authority.regimes.ct.flatMap(uri => authority.ctUtr.map (utr => ctConnector.root(uri.toString).map(CtRoot(utr, _))))

  def agentRoot(authority: UserAuthority)(implicit hc: HeaderCarrier): Future[Option[AgentRoot]] = authority.regimes.agent.map(uri => agentConnectorRoot.root(uri.toString))

  def regimeRoots(authority: UserAuthority)(implicit hc: HeaderCarrier): Future[RegimeRoots]
}
