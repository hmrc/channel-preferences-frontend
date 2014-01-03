package controllers.bt

import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc._
import uk.gov.hmrc.common.microservice.epaye.EpayeConnector
import controllers.common.service.Connectors
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeAccountSummary
import controllers.common.actions.{HeaderCarrier, PageVisibilityPredicate}
import uk.gov.hmrc.common.microservice.sa.SaConnector
import uk.gov.hmrc.common.microservice.ct.CtConnector
import uk.gov.hmrc.common.microservice.ct.domain.{CtAccountBalance, CtAccountSummary}
import uk.gov.hmrc.common.microservice.vat.VatConnector
import uk.gov.hmrc.common.microservice.vat.domain.{VatAccountBalance, VatAccountSummary}
import scala.concurrent._
import uk.gov.hmrc.common.StickyMdcExecutionContext.global


class EpayePaymentPredicate(epayeConnector: EpayeConnector) extends PageVisibilityPredicate {

  def this() = this(Connectors.epayeConnector)

  def isVisible(user: User, request: Request[AnyContent]): Future[Boolean] = {
    val accountSummary = user.regimes.epaye.get.accountSummary(epayeConnector, HeaderCarrier(request))
    accountSummary map {
      case Some(EpayeAccountSummary(Some(rti), None)) => true
      case Some(EpayeAccountSummary(None, Some(nonRti))) => true
      case _ => false
    }
  }

}

object EpayePaymentPredicate extends EpayePaymentPredicate

class SaPaymentPredicate(saConnector: SaConnector) extends PageVisibilityPredicate {

  def this() = this(Connectors.saConnector)

  def isVisible(user: User, request: Request[AnyContent]): Future[Boolean] = {
    val accountSummary = user.regimes.sa.get.accountSummary(saConnector, HeaderCarrier(request))
    accountSummary map {
      case Some(ac) => true
      case _ => false
    }
  }
}

object SaPaymentPredicate extends SaPaymentPredicate

class CtPaymentPredicate(ctConnector: CtConnector) extends PageVisibilityPredicate {

  def this() = this(Connectors.ctConnector)

  implicit val connector = ctConnector

  def isVisible(user: User, request: Request[AnyContent]): Future[Boolean] = {
    implicit val hc = HeaderCarrier(request)
    val accountSummaryF = user.regimes.ct.get.accountSummary
    accountSummaryF.map {
      case Some(CtAccountSummary(Some(CtAccountBalance(Some(balance))), Some(date))) => true
      case _ => false
    }
  }
}

object CtPaymentPredicate extends CtPaymentPredicate

class VatPaymentPredicate(vatConnector: VatConnector) extends PageVisibilityPredicate {

  def this() = this(Connectors.vatConnector)

  def isVisible(user: User, request: Request[AnyContent]): Future[Boolean] = {
    val accountSummary = user.regimes.vat.get.accountSummary(vatConnector, HeaderCarrier(request))
    accountSummary map {
      case Some(VatAccountSummary(Some(VatAccountBalance(Some(balance))), Some(date))) => true
      case _ => false
    }
  }

}

object VatPaymentPredicate extends VatPaymentPredicate