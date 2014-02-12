package controllers.common.support


import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc.{SimpleResult, Request}

import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.deskpro.HmrcDeskproConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import uk.gov.hmrc.common.microservice.domain.{RegimeRoots, User}
import scala.concurrent.Future
import uk.gov.hmrc.common.microservice.deskpro.domain.TicketId

import controllers.common.{NoRegimeRoots, AnyAuthenticationProvider, BaseController}
import controllers.common.actions.{HeaderCarrier, Actions}
import controllers.common.service.Connectors

import controllers.common.validators.Validators._
import uk.gov.hmrc.common.microservice.preferences.PreferencesConnector
import uk.gov.hmrc.common.microservice.sa.domain.SaRoot


class FeedbackController(override val auditConnector: AuditConnector,
                         hmrcDeskproConnector: HmrcDeskproConnector,
                         ticketCache: TicketCache,
                         preferencesConnector: PreferencesConnector)(implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with NoRegimeRoots {
  def this() = this(Connectors.auditConnector, Connectors.hmrcDeskproConnector, TicketCache(), Connectors.preferencesConnector)(Connectors.authConnector)

  import controllers.common.support.FeedbackFormConfig._

  val formId = "FeedbackForm"

  val form = Form[FeedbackForm](mapping(
    "feedback-rating" -> optional(text)
      .verifying("error.common.feedback.rating_mandatory", rating => rating.isDefined && !rating.get.trim.isEmpty)
      .verifying("error.common.feedback.rating_valid", rating => rating.map(validExperiences.contains(_)).getOrElse(true)),
    "feedback-name" -> text
      .verifying("error.common.feedback.name_mandatory", name => !name.trim.isEmpty)
      .verifying("error.common.feedback.name_too_long", name => name.size <= 70),
    "feedback-email" -> emailWithDomain.verifying("deskpro.email_too_long", email => email.size <= 255),
    "feedback-comments" -> text
      .verifying("error.common.comments_mandatory", comment => !comment.trim.isEmpty)
      .verifying("error.common.comments_too_long", comment => comment.size <= 2000),
    "isJavascript" -> boolean,
    "referer" -> text
  )(FeedbackForm.apply)((feedbackForm: FeedbackForm) => {
    import feedbackForm._
    Some((Some(experienceRating), name, email, comments, javascriptEnabled, referrer))
  }))

  def emptyForm(implicit request: Request[AnyRef]) = form.fill(FeedbackForm(request.headers.get("Referer").getOrElse("n/a")))

  def feedbackForm = WithNewSessionTimeout(AuthenticatedBy(AnyAuthenticationProvider).async {
    implicit user => implicit request => authenticatedFeedback
  })

  def unauthenticatedFeedbackForm = UnauthorisedAction.async {
    implicit request => unauthenticatedFeedback
  }

  def submit = WithNewSessionTimeout {
    AuthenticatedBy(AnyAuthenticationProvider).async {
      implicit user => implicit request => doSubmit(Some(user))
    }
  }

  def submitUnauthenticated = UnauthorisedAction.async {
    implicit request => doSubmit(None)
  }

  def thanks = WithNewSessionTimeout {
    AuthenticatedBy(AnyAuthenticationProvider).async {
      implicit user => implicit request => doThanks(Some(user), request)
    }
  }

  def unauthenticatedThanks = UnauthorisedAction.async {
    implicit request => doThanks(None, request)
  }

  private[common] def doSubmit(user: Option[User])(implicit request: Request[AnyRef]) =
    form.bindFromRequest()(request).fold(
      error => feedbackView(user, error).map(BadRequest(_)),
      data => {
        import data._
        redirectToConfirmationPage(
          hmrcDeskproConnector.createFeedback(name, email, experienceRating, "Beta feedback submission", comments, referrer, javascriptEnabled, request),
          user)
      }
    )

  private[common] def redirectToConfirmationPage(ticketId: Future[Option[TicketId]], user: Option[User])(implicit request: Request[AnyRef]) =
    ticketId.map(ticketCache.stashTicket(_, formId)).map(_ => thanksFor(user))

  private def thanksFor(user: Option[User]) = Redirect(user.map(_ => routes.FeedbackController.thanks()).getOrElse(routes.FeedbackController.unauthenticatedThanks()))

  private[common] def authenticatedFeedback(implicit user: User, request: Request[AnyRef]) = feedbackView(Some(user), emptyForm).map(Ok(_))

  private[common] def unauthenticatedFeedback(implicit request: Request[AnyRef]) = feedbackView(user = None, emptyForm).map(Ok(_))

  private def feedbackView(user: Option[User], form: Form[FeedbackForm])(implicit request: Request[AnyRef]) = checkPreferencesAvailable(user, request).map (
     hasPref => views.html.support.feedback(form, user, hasPref))

  private[common] def doThanks(implicit user: Option[User], request: Request[AnyRef]): Future[SimpleResult] = {
    ticketCache.popTicket(formId).flatMap {
      ticketId =>
        checkPreferencesAvailable.map {
          hasPreferences => views.html.support.feedback_confirmation(ticketId, user, hasPreferences)
        }
    }.map(Ok(_))
  }

  private def checkPreferencesAvailable(implicit user: Option[User], request: Request[AnyRef]): Future[Boolean] = {
    implicit val hc = HeaderCarrier(request)

    user.map(_.regimes) match {
      case Some(RegimeRoots(_, Some(sa: SaRoot), _, _, _)) =>
        preferencesConnector.getPreferences(sa.utr).map(_.isDefined)
      case Some(regimeRoots) if regimeRoots.hasBusinessTaxRegime =>
        Future.successful(true)
      case _ =>
        Future.successful(false)
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
  val validExperiences = (5 to 1 by -1) map (_.toString)
  val feedbackRatings = validExperiences zip Seq("Very good", "Good", "Neutral", "Bad", "Very bad")
}
