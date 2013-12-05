package controllers.paye

import controllers.common.{SessionTimeoutWrapper, BaseController}
import controllers.common.actions.Actions
import config.DateTimeProvider
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import controllers.common.service.Connectors
import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import play.api.mvc.Request
import scala.concurrent._
import controllers.paye.validation.RemoveBenefitFlow
import uk.gov.hmrc.common.microservice.paye.domain.BenefitTypes._
import views.html.paye.{remove_benefit_form, remove_car_benefit_form}
import play.api.mvc.SimpleResult
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector

class RemoveBenefitFormController(keyStoreService: KeyStoreConnector, override val authConnector: AuthConnector, override val auditConnector: AuditConnector)
                                 (implicit payeConnector: PayeConnector, txQueueConnector: TxQueueConnector)
  extends BaseController
  with Actions
  with SessionTimeoutWrapper
  with TaxYearSupport
  with DateTimeProvider
  with PayeRegimeRoots {

  def this() = this(Connectors.keyStoreConnector, Connectors.authConnector, Connectors.auditConnector)(Connectors.payeConnector, Connectors.txQueueConnector)

  import RemovalUtils._

  def benefitRemovalForm(benefitTypes: String, taxYear: Int, employmentSequenceNumber: Int) = AuthorisedFor(PayeRegime).async {
    user => request => benefitRemovalFormAction(user, request, benefitTypes, taxYear, employmentSequenceNumber)
  }

  private[paye] val benefitRemovalFormAction: (User, Request[_], String, Int, Int) => Future[SimpleResult] = RemoveBenefitFlow {
    (user, request, benefit, payeRootData) =>
      future {
        val benefitStartDate = getStartDate(benefit.benefit)
        val dates = getCarFuelBenefitDates(request)

        benefit.benefit.benefitType match {
          case CAR => {
            val carWithUnremovedFuel = hasUnremovedFuelBenefit(payeRootData, benefit.benefit.employmentSequenceNumber)
            Ok(remove_car_benefit_form(benefit, carWithUnremovedFuel, updateBenefitForm(benefitStartDate, carWithUnremovedFuel, dates), currentTaxYearYearsRange)(user))
          }
          case _ => Ok(content = remove_benefit_form(benefit, hasUnremovedCarBenefit(payeRootData, benefit.benefit.employmentSequenceNumber), updateBenefitForm(benefitStartDate, false, dates), currentTaxYearYearsRange)(user))
        }
      }
  }


}
