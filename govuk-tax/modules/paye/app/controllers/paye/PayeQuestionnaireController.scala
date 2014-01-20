package controllers.paye

import controllers.common.BaseController
import controllers.common.actions.Actions
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import controllers.common.service.Connectors
import uk.gov.hmrc.common.microservice.paye.domain.PayeRegime
import play.mvc.Http.Request
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.audit.AuditEvent
import play.api.mvc.SimpleResult
import PayeQuestionnaireFormAdaptor._
import play.api.data.Forms._

import play.api.data.Form

class PayeQuestionnaireController(override val auditConnector: AuditConnector, override val authConnector: AuthConnector)
  extends BaseController with Actions with PayeRegimeRoots {

  def this() = this(Connectors.auditConnector, Connectors.authConnector)

  def submitQuestionnaire(transactionId: String) = AuthorisedFor(PayeRegime) {
    ??? //user => request => submitQuestionnaireAction
  }

  private[paye] def submitQuestionnaireAction(implicit request: Request, user: User): SimpleResult = {

//    payeQuestionnaireForm.bindFromRequest().fold(
//      errors => BadRequest,
//      (formData: PayeQuestionnaireFormData) => {
//        audit(buildAuditEvent(formData))
//        Ok
//      }
//    )
    ???
  }

  private[paye] def payeQuestionnaireForm = Form[PayeQuestionnaireFormData](
    mapping(
      transactionId -> text.verifying("some.error.code", !_.isEmpty),
      wasItEasy -> optional(number),
      secure -> optional(number),
      comfortable -> optional(number),
      easyCarUpdateDetails -> optional(number),
      onlineNextTime -> optional(number),
      overallSatisfaction -> optional(number),
      commentForImprovements -> optional(text)
    )(PayeQuestionnaireFormData.apply)(PayeQuestionnaireFormData.unapply)
  )

  private[paye] def buildAuditEvent(formData: PayeQuestionnaireFormData)(implicit user: User): AuditEvent = {
    ???
  }

  private[paye] def audit(auditEvent: AuditEvent): Unit = {
    ???
  }

  // TODO: Going 'home' is acceptable for the JS version of the solution. Waiting for non-JS user experience flow from Gail/Chris
  private[paye] def buildPageResponse = {
    ??? //Ok(views.html.paye.paye_home)
  }
}

case class PayeQuestionnaireFormData(transactionId: String, wasItEasy: Option[Int], secure: Option[Int], comfortable: Option[Int], easyCarUpdateDetails: Option[Int],
                                     onlineNextTime: Option[Int], overallSatisfaction: Option[Int], commentForImprovements: Option[String])

object PayeQuestionnaireFormAdaptor {
  val transactionId = "transactionId"
  val wasItEasy = "q1"
  val secure = "q2"
  val comfortable = "q3"
  val easyCarUpdateDetails = "q4"
  val onlineNextTime = "q5"
  val overallSatisfaction = "q6"
  val commentForImprovements = "q7"
}

object payeQuestionnaireUtils {
  private[paye] def payeQuestionnaireForm = Form[PayeQuestionnaireFormData](
    mapping(
      transactionId -> text.verifying("some.error.code", !_.isEmpty),
      wasItEasy -> optional(number),
      secure -> optional(number),
      comfortable -> optional(number),
      easyCarUpdateDetails -> optional(number),
      onlineNextTime -> optional(number),
      overallSatisfaction -> optional(number),
      commentForImprovements -> optional(text)
    )(PayeQuestionnaireFormData.apply)(PayeQuestionnaireFormData.unapply)
  )
}
