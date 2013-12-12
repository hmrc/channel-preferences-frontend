package controllers.paye.data

import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.paye.PayeConnector
import controllers.common.BaseController
import controllers.common.actions.Actions
import controllers.common.validators.Validators
import controllers.paye.{PayeRegimeRoots, TaxYearSupport}
import controllers.common.service.Connectors
import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import play.api.libs.ws.WS
import scala.concurrent.Future

class DataController(override val auditConnector: AuditConnector, override val authConnector: AuthConnector)
                    (implicit payeConnector: PayeConnector) extends BaseController
with Actions
with Validators
with TaxYearSupport
with PayeRegimeRoots {

  def this() = this(Connectors.auditConnector, Connectors.authConnector)(Connectors.payeConnector)

  def carBenefits(year: Int) = AuthorisedFor(PayeRegime).async {
    user =>
      implicit request =>
        user.getPaye.links.get("benefit-cars").map { uri =>
          WS.url("http://localhost:8600" + uri.replace("{taxYear}", year.toString)).withHeaders(hc.headers: _*).get.map { response =>
            Ok(response.body).withHeaders(("Content-Type", "application/json"))
          }
        }.getOrElse {
          Future.successful(BadRequest)
        }
  }

}
