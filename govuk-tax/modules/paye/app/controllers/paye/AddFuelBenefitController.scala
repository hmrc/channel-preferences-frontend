package controllers.paye

import controllers.common.{BaseController, Ida, Actions}
import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import controllers.common.validators.Validators
import controllers.common.service.Connectors
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector


class AddFuelBenefitController(override val auditConnector: AuditConnector, override val authConnector: AuthConnector) extends BaseController
with Actions
with Validators {

  def this() = this(Connectors.auditConnector, Connectors.authConnector)

  def startAddFuelBenefit(taxYear: Int, employmentSequenceNumber: Int) =
    ActionAuthorisedBy(Ida)(taxRegime = Some(PayeRegime), redirectToOrigin = true) {
      user =>
        request => Ok
    }
}
