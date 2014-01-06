package controllers.paye

import scala.concurrent._
import play.api.mvc._

import config.DateTimeProvider
import controllers.common.{SessionTimeoutWrapper, BaseController}
import controllers.common.actions.Actions
import controllers.common.service.Connectors

import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector
import uk.gov.hmrc.common.microservice.paye.domain._

import models.paye.RemoveCarBenefitFormData
import models.paye.CarFuelBenefitDates
import play.api.mvc.SimpleResult
import uk.gov.hmrc.common.microservice.domain.User

class ShowReplaceBenefitFormController(keyStoreService: KeyStoreConnector, override val authConnector: AuthConnector, override val auditConnector: AuditConnector)
                                     (implicit payeConnector: PayeConnector, txQueueConnector: TxQueueConnector)
  extends BaseController
  with Actions
  with SessionTimeoutWrapper
  with TaxYearSupport
  with DateTimeProvider
  with PayeRegimeRoots {

  def this() = this(Connectors.keyStoreConnector, Connectors.authConnector, Connectors.auditConnector)(Connectors.payeConnector, Connectors.txQueueConnector)


  def showReplaceCarBenefitForm(taxYear: Int, employmentSequenceNumber: Int): Action[SimpleResult] = ???



  private[paye] def showReplaceCarBenefitFormAction(user: User, request: Request[_], taxYear: Int, employmentSequenceNumber: Int): Future[SimpleResult] = ???

  def replaceCarBenefit(activeCarBenefit: CarBenefit, primaryEmployment: Employment, dates: Option[CarFuelBenefitDates], defaults: Option[RemoveCarBenefitFormData], user: User) = ???

}
