package controllers.common.support

import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.deskpro.HmrcDeskproConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import controllers.common.actions.{HeaderCarrier, Actions}
import controllers.common.service.Connectors
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.Request
import uk.gov.hmrc.common.microservice.domain.User
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.common.microservice.deskpro.domain.TicketId
import controllers.common.{AnyAuthenticationProvider, AllRegimeRoots, BaseController}
import ExecutionContext.Implicits.global

class FeedbackController(override val auditConnector: AuditConnector, hmrcDeskproConnector: HmrcDeskproConnector, ticketCache: TicketCache)(implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with AllRegimeRoots {
  def this() = this(Connectors.auditConnector, Connectors.hmrcDeskproConnector, TicketCache())(Connectors.authConnector)

  import controllers.common.support.FeedbackFormConfig._

  val formId = "FeedbackForm"

  val form = Form[FeedbackForm](mapping(
    "feedback-rating" -> optional(text)
      .verifying("error.common.feedback.rating_mandatory", rating => rating.isDefined && !rating.get.trim.isEmpty)
      .verifying("error.common.feedback.rating_valid", rating => rating.map(validExperiences.contains(_)).getOrElse(true)),
    "feedback-name" -> text
      .verifying("error.common.feedback.name_mandatory", name => !name.trim.isEmpty)
      .verifying("error.common.feedback.name_too_long", name => name.size <= 70),
    "feedback-email" -> email.verifying("error.email_too_long", email => email.size <= 320),
    "feedback-comments" -> text
      .verifying("error.common.comments_mandatory", comment => !comment.trim.isEmpty)
      .verifying("error.common.comments_too_long", comment => comment.size <= 2000),
    "isJavascript" -> boolean,
    "referer" -> text
  )(FeedbackForm.apply)((feedbackForm: FeedbackForm) => {
    import feedbackForm._
    Some((Some(experienceRating), name, email, comments, javascriptEnabled, referrer))
  }))

  def feedbackForm = WithNewSessionTimeout(AuthenticatedBy(AnyAuthenticationProvider)({
    implicit user => implicit request => authenticatedFeedback
  }))

  def unauthenticatedFeedbackForm = UnauthorisedAction {
    implicit request => unauthenticatedFeedback
  }

  def submit = WithNewSessionTimeout(AuthenticatedBy(AnyAuthenticationProvider).async({
    implicit user => implicit request => doSubmit(Some(user))
  }))

  def submitUnauthenticated = UnauthorisedAction.async {
    implicit request => doSubmit(None)
  }

  def thanks = WithNewSessionTimeout(AuthenticatedBy(AnyAuthenticationProvider).async({
    implicit user => implicit request => doThanks(Some(user), request)
  }))

  def unauthenticatedThanks = UnauthorisedAction.async({
    implicit request => doThanks(None, request)
  })

  private[common] def doSubmit(user: Option[User])(implicit request: Request[AnyRef]) = {
    form.bindFromRequest()(request).fold(
      error => {
        Future(BadRequest(views.html.support.feedback(error, user)(request)))
      },
      data => {
        import data._
        redirectToConfirmationPage(hmrcDeskproConnector.createFeedback(name, email, experienceRating, "Beta feedback submission", comments, referrer, javascriptEnabled, request, user), user)
      })
  }

  private[common] def redirectToConfirmationPage(ticketId: Future[Option[TicketId]], user: Option[User])(implicit request: Request[AnyRef]) =
    ticketId.map {
      ticketOption => {
        ticketCache.stashTicket(ticketOption, formId)
        Redirect(user.map(_ => routes.FeedbackController.thanks()).getOrElse(routes.FeedbackController.unauthenticatedThanks()))
      }
    }


  private[common] def authenticatedFeedback(implicit user: User, request: Request[AnyRef]) = Ok(views.html.support.feedback(form.fill(FeedbackForm(request.headers.get("Referer").getOrElse("n/a"))), Some(user)))

  private[common] def unauthenticatedFeedback(implicit request: Request[AnyRef]) = Ok(views.html.support.feedback(form.fill(FeedbackForm(request.headers.get("Referer").getOrElse("n/a"))), None))

  private[common] def doThanks(implicit user: Option[User], request: Request[AnyRef]) = {
    implicit val hc = HeaderCarrier(request)
    ticketCache.popTicket(formId).map {
      ticketId => Ok(views.html.support.feedback_confirmation(ticketId, user)(request))
    }
  }

}

case class FeedbackForm(experienceRating: String, name: String, email: String, comments: String, javascriptEnabled: Boolean, referrer: String)

object FeedbackForm {
  def apply(referer: String): FeedbackForm = FeedbackForm("", "", "", "", false, referer)

  def apply(experienceRating: Option[String], name: String, email: String, comments: String, javascriptEnabled: Boolean, referrer: String): FeedbackForm =
    FeedbackForm(experienceRating.getOrElse(""), name, email, comments, javascriptEnabled, referrer)

}

object FeedbackFormConfig {
  val validExperiences = Seq("Very good", "Good", "Unsure", "Bad", "Very bad")
  val feedbackRatings = validExperiences.map(v => (v, v))
}
