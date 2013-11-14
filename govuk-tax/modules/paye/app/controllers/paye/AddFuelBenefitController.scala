package controllers.paye

import controllers.common.{BaseController, Ida, Actions}
import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import controllers.common.validators.Validators
import controllers.common.service.Connectors
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import play.api.mvc.Request
import controllers.paye.validation.WithValidatedFuelRequest
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import uk.gov.hmrc.common.microservice.txqueue.TxQueueConnector
import play.api.Logger
import play.api.data.Forms._
import uk.gov.hmrc.common.microservice.paye.domain.TaxYearData
import scala.Some
import play.api.mvc.SimpleResult
import uk.gov.hmrc.common.microservice.domain.User
import play.api.data.Form
import FuelBenefitFormFields._


class AddFuelBenefitController(override val auditConnector: AuditConnector, override val authConnector: AuthConnector)
                              (implicit payeConnector: PayeConnector, txQueueConnector: TxQueueConnector)extends BaseController
with Actions
with Validators
with TaxYearSupport {

  def this() = this(Connectors.auditConnector, Connectors.authConnector)(Connectors.payeConnector, Connectors.txQueueConnector)

  def startAddFuelBenefit(taxYear: Int, employmentSequenceNumber: Int) =
    ActionAuthorisedBy(Ida)(taxRegime = Some(PayeRegime), redirectToOrigin = true) {
      user =>
        request =>
          startAddFuelBenefitAction(user, request, taxYear, employmentSequenceNumber)
    }

  private def carBenefitForm() = Form[FuelBenefitData](
    mapping(
      employerPayFuel -> optional(text),
      dateFuelWithdrawn -> optional(text)
    )(FuelBenefitData.apply)(FuelBenefitData.unapply)
  )

  private[paye] def startAddFuelBenefitAction: (User, Request[_], Int, Int) => SimpleResult = WithValidatedFuelRequest {
    (request, user, taxYear, employmentSequenceNumber, payeRootData) => {
      findEmployment(employmentSequenceNumber, payeRootData) match {
        case Some(employment) => {
          Ok(views.html.paye.add_fuel_benefit_form(carBenefitForm(), employment.employerName)(user))
        }
        case None => {
          Logger.debug(s"Unable to find employment for user ${user.oid} with sequence number $employmentSequenceNumber")
          BadRequest
        }
      }
  }
  }

  private def findEmployment(employmentSequenceNumber: Int, payeRootData: TaxYearData) = {
    payeRootData.employments.find(_.sequenceNumber == employmentSequenceNumber)
  }
}

case class FuelBenefitData(employerPayFuel: Option[String], dateFuelWithdrawn: Option[String])

object FuelBenefitFormFields {
  val employerPayFuel = "employerPayFuel"
  val dateFuelWithdrawn = "dateFuelWithdrawn"
}