package controllers.bt

import controllers.common.{BaseController, GovernmentGateway}
import uk.gov.hmrc.common.PortalUrlBuilder
import controllers.common.service.Connectors
import play.api.mvc.Request
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import org.joda.time.LocalDate
import java.text.SimpleDateFormat
import views.helpers.LinkMessage._
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.ct.domain.CtRoot
import views.helpers.RenderableLinkMessage
import scala.Some
import views.formatting.Dates
import uk.gov.hmrc.common.microservice.ct.CtConnector
import controllers.common.actions.Actions
import uk.gov.hmrc.common.microservice.vat.VatConnector
import uk.gov.hmrc.common.microservice.vat.domain.VatRoot
import play.api.Logger
import uk.gov.hmrc.domain.CalendarEvent


class ImportantDatesController(ctConnector: CtConnector, vatConnector: VatConnector, override val auditConnector: AuditConnector)(implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with PortalUrlBuilder {

  def this() = this(Connectors.ctConnector, Connectors.vatConnector, Connectors.auditConnector)(Connectors.authConnector)

  implicit val dateFormat = new SimpleDateFormat("d MMMM yyy")

  def importantDates = AuthenticatedBy(GovernmentGateway) {
    user => request => importantDatesPage(user, request)
  }

  private[bt] def importantDatesPage(implicit user: User, request: Request[AnyRef]) = {
    val regimes = List(user.regimes.ct, user.regimes.vat)

    val events = regimes.flatMap {
      regime => regime match {
        case Some(ctRoot: CtRoot) => ctRoot.links.get("calendar").map(ctConnector.calendar).getOrElse(None)
        case Some(vatRoot: VatRoot) => vatRoot.links.get("calendar").map(vatConnector.calendar).getOrElse(None)
        //Other cases for VAT etc. go here when we implement them
        case None => List.empty
      }
    }.flatten

    val dates = events map (ImportantDate.create(_, buildPortalUrl))
    Ok(views.html.important_dates(dates.sortBy(_.date.toDate))(user))
  }
}

case class ImportantDate(date: LocalDate, text: String, args: Seq[String] = Seq.empty, grayedoutText: Option[String] = None, link: Option[RenderableLinkMessage] = None)

object ImportantDate {
  def create(event: CalendarEvent, buildPortalUrl: (String) => String)(implicit user: User): ImportantDate = {

    val args = Seq(Dates.formatDate(event.accountingPeriod.startDate), Dates.formatDate(event.accountingPeriod.endDate))
    val service: String = event.regime.toLowerCase
    val eventType: String = event.eventType.toLowerCase
    val linkTextKey = s"$service.message.importantDates.link.$eventType"
    val textMessageKey = s"$service.message.importantDates.text.$eventType"
    val additionalTextMessageKey = s"$service.message.importantDates.additionalText.$eventType"

    val (link, text, additionalText): (Option[RenderableLinkMessage], String, Option[String]) = (service, eventType, event.accountingPeriod.returnFiled) match {
      case ("ct", "payment", _) =>
        (Some(RenderableLinkMessage(internalLink(routes.PaymentController.makeCtPayment().url, linkTextKey))), textMessageKey, None)
      case ("vat", "payment", _) =>
        (Some(RenderableLinkMessage(internalLink(routes.PaymentController.makeVatPayment().url, linkTextKey))), textMessageKey, None)
      case (service, "filing", true) =>
        (None, additionalTextMessageKey, Some(textMessageKey))
      case (service, "filing", false) =>
        (Some(RenderableLinkMessage(portalLink(buildPortalUrl(s"${service}FileAReturn"), Some(linkTextKey)))),
          textMessageKey,
          None)

      case _ => Logger.error(s"Could not render $event"); throw new MatchError(event.toString)
    }

    ImportantDate(event.eventDate, text, args, additionalText, link)

  }
}