package controllers.common

import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.deskpro.HmrcDeskproConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import controllers.common.actions.Actions
import controllers.common.service.Connectors
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.Request
import uk.gov.hmrc.common.microservice.domain.User
import scala.concurrent.Future
import uk.gov.hmrc.common.microservice.deskpro.domain.TicketId

class FeedbackController(override val auditConnector: AuditConnector, hmrcDeskproConnector: HmrcDeskproConnector)(implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with AllRegimeRoots {
  def this() = this(Connectors.auditConnector, Connectors.hmrcDeskproConnector)(Connectors.authConnector)


  import FeedbackFormConfig._

  val form = Form[FeedbackForm](mapping(
    "feedback-rating" -> text
      .verifying("error.common.feedback.rating_mandatory", rating => !rating.trim.isEmpty)
      .verifying("error.common.feedback.rating_valid", rating => if (!rating.trim.isEmpty) validExperiences.contains(rating) else true),
    "feedback-name" -> text
      .verifying("error.common.feedback.name_mandatory", name => !name.trim.isEmpty)
      .verifying("error.common.feedback.name_too_long", name => name.size < 70),
    "feedback-email" -> email.verifying("error.email_too_long", email => email.size < 320),
    "feedback-comments" -> text
      .verifying("error.common.comments_mandatory", comment => !comment.trim.isEmpty)
      .verifying("error.common.comments_too_long", comment => comment.size < 2000)
  )(FeedbackForm.apply)(FeedbackForm.unapply))

  def feedbackForm = WithNewSessionTimeout(AuthenticatedBy(AnyAuthenticationProvider)({
    implicit user => implicit request => renderForm
  }))

  def submit = WithNewSessionTimeout(AuthenticatedBy(AnyAuthenticationProvider).async({
    implicit user => implicit request => doSubmit
  }))

  def thanks = WithNewSessionTimeout(AuthenticatedBy(AnyAuthenticationProvider).async({
    implicit user => implicit request => doThanks
  }))

  private[common] def doSubmit(implicit user: User, request: Request[AnyRef]) = {
    form.bindFromRequest()(request).fold(
      error => {
        Future(BadRequest(views.html.feedback(error)))
      },
      data => {
        import data._
        redirectToConfirmationPage(hmrcDeskproConnector.createFeedback(name, email, experienceRating, "Beta feedback submission", comments, "referrer", true, request, Some(user)))
      })
  }

  private[common] def redirectToConfirmationPage(ticketId: Future[Option[TicketId]])(implicit user: User, request: Request[AnyRef]) = {
    ticketId.map(_ => Redirect(routes.FeedbackController.thanks()))
  }

  private[common] def renderForm(implicit user: User, request: Request[AnyRef]) = Ok(views.html.feedback(form.fill(FeedbackForm("", "", "", ""))))

  private[common] def doThanks(implicit user: User, request: Request[AnyRef]) = Future(Ok(views.html.feedback_confirmation()))

}

case class FeedbackForm(experienceRating: String, name: String, email: String, comments: String)

object FeedbackFormConfig {
  val validExperiences = Seq("Very good", "Good", "Unsure", "Bad", "Very bad")
  val feedbackRatings = validExperiences.map(v => (v, v))
}
