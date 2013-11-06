package controllers.bt

import uk.gov.hmrc.common.microservice.domain.User
import play.api.mvc._
import uk.gov.hmrc.common.microservice.epaye.EpayeConnector
import controllers.common.service.Connectors
import uk.gov.hmrc.common.microservice.epaye.domain.EpayeAccountSummary
import controllers.common.actions.PageVisibilityPredicate


class EpayePaymentPredicate(epayeConnector: EpayeConnector) extends PageVisibilityPredicate {

  def this() = this(Connectors.epayeConnector)

  def isVisible(user: User, request: Request[AnyContent]): Boolean = {
    val accountSummary = user.regimes.epaye.get.accountSummary(epayeConnector)
    accountSummary match {
      case Some(EpayeAccountSummary(Some(rti), None)) => true
      case Some(EpayeAccountSummary(None, Some(nonRti))) => true
      case _ => false
    }
  }

}

object EpayePaymentPredicate extends EpayePaymentPredicate