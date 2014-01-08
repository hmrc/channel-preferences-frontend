package controllers.bt

import controllers.common.{BaseController, GovernmentGateway}
import uk.gov.hmrc.common.{MdcLoggingExecutionContext, PortalUrlBuilder}
import controllers.common.service.Connectors
import play.api.mvc.{SimpleResult, Request}
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import org.joda.time.LocalDate
import java.text.SimpleDateFormat
import uk.gov.hmrc.common.microservice.domain.{RegimeRoot, User}
import uk.gov.hmrc.common.microservice.ct.domain.CtRoot
import views.helpers.{PortalLink, Link, InternalLink}
import scala.Some
import views.formatting.Dates
import uk.gov.hmrc.common.microservice.ct.CtConnector
import controllers.common.actions.{HeaderCarrier, Actions}
import uk.gov.hmrc.common.microservice.vat.VatConnector
import uk.gov.hmrc.common.microservice.vat.domain.VatRoot
import play.api.Logger
import uk.gov.hmrc.domain.CalendarEvent
import scala.concurrent._


class ImportantDatesController(ctConnector: CtConnector, vatConnector: VatConnector, override val auditConnector: AuditConnector)(implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with PortalUrlBuilder
  with BusinessTaxRegimeRoots {

  def this() = this(Connectors.ctConnector, Connectors.vatConnector, Connectors.auditConnector)(Connectors.authConnector)

  implicit val dateFormat = new SimpleDateFormat("d MMMM yyy")

  def importantDates = AuthenticatedBy(GovernmentGateway).async {
    user => request => importantDatesPage(user, request)
  }

  private[bt] def importantDatesPage(implicit user: User, request: Request[AnyRef]): Future[SimpleResult] = {
    val regimes: List[RegimeRoot[_]] = List(user.regimes.ct, user.regimes.vat).flatten

    implicit val hc = HeaderCarrier(request)
    val eventsLF = regimes.map {
      case ctRoot: CtRoot if ctRoot.links.get("calendar").isDefined => getCalendarEvents(ctConnector.calendar(ctRoot.links("calendar")).map(_.getOrElse(List.empty[CalendarEvent])))
      case vatRoot: VatRoot if vatRoot.links.get("calendar").isDefined => getCalendarEvents(vatConnector.calendar(vatRoot.links("calendar")).map(_.getOrElse(List.empty[CalendarEvent])))
      case _ => Future(List.empty[CalendarEventWithShowLink])
    }

    val eventsF = Future.sequence(eventsLF).map(_.flatten)
    val datesF = eventsF.map(_.map(ImportantDate.create(_, buildPortalUrl)))

    datesF.map(dates => Ok(views.html.important_dates(dates.sortBy(_.date.toDate))(user)))
  }
  
  private def getCalendarEvents(calendarEvents: Future[List[CalendarEvent]])(implicit hc:HeaderCarrier):Future[List[CalendarEventWithShowLink]] = {
    calendarEvents.map{CalendarEventWithShowLink.addShowLinkToCalendarEvents(_)}
  }

}

case class CalendarEventWithShowLink(info: CalendarEvent, showLink: Boolean)
object CalendarEventWithShowLink {

  private val eventTypes = List("filing", "payment", "payment-card", "payment-online")

  def addShowLinkToCalendarEvents(events: List[CalendarEvent]): List[CalendarEventWithShowLink] = {
    val result = addShowLinkToCalendarEventsHelper(events, List.empty)
    result
  }

  private def addShowLinkToCalendarEventsHelper(events: List[CalendarEvent], paymentsSelected: List[String], newEvents: List[CalendarEventWithShowLink] = List.empty): List[CalendarEventWithShowLink] = {
    events match {
      case Nil =>
        newEvents
      case e::es if !e.accountingPeriod.returnFiled && !paymentsSelected.contains(e.eventType.toLowerCase) && eventTypes.contains(e.eventType.toLowerCase) =>
        addShowLinkToCalendarEventsHelper(es, paymentsSelected :+ e.eventType.toLowerCase, newEvents :+ CalendarEventWithShowLink(e, true))
      case e::es =>
        addShowLinkToCalendarEventsHelper(es, paymentsSelected, newEvents :+ CalendarEventWithShowLink(e, false))
    }
  }

}

case class ImportantDate(service: String, eventType: String, date: LocalDate, text: String, args: Seq[String] = Seq.empty, grayedoutText: Option[String] = None, link: Option[Link] = None, linkId: Option[String] = None)
object ImportantDate {
  def create(event: CalendarEventWithShowLink, buildPortalUrl: (String) => String)(implicit user: User): ImportantDate = {

    val args = Seq(Dates.formatDate(event.info.accountingPeriod.startDate), Dates.formatDate(event.info.accountingPeriod.endDate))
    val service: String = event.info.regime.toLowerCase
    val eventType: String = event.info.eventType.toLowerCase

    val textMessageKey = s"$service.message.importantDates.text.$eventType"
    val additionalTextMessageKey = s"$service.message.importantDates.additionalText.$eventType"

    val (link, text, additionalText, linkId): (Option[Link], String, Option[String], Option[String]) = (service, eventType, event.info.accountingPeriod.returnFiled, event.showLink) match {
      case ("ct", "payment", _, true) => (Some(InternalLink(routes.PaymentController.makeCtPayment().url)), textMessageKey, None, generateLinkId(service, eventType))
      case ("ct", "payment", _, false) => (None, textMessageKey, None, None)
      case ("vat", "payment-directdebit", _, _) => (None, additionalTextMessageKey, Some(textMessageKey), None)
      case ("vat", "payment-cheque", _, _) => (None, textMessageKey, None, None)
      case ("vat", "payment-card", _, true) | ("vat", "payment-online", _, true) => (Some(InternalLink(routes.PaymentController.makeVatPayment().url)), textMessageKey, None, generateLinkId(service, eventType))
      case ("vat", "payment-card", _, false) | ("vat", "payment-online", _, false) => (None, textMessageKey, None, generateLinkId(service, eventType))
      case (serviceKey, "filing", true, _) => (None, additionalTextMessageKey, Some(textMessageKey), None)
      case (serviceKey, "filing", false, true) => (Some(PortalLink(buildPortalUrl(s"${service}FileAReturn"))), textMessageKey, None, generateLinkId(service, eventType))
      case (serviceKey, "filing", false, false) => (None, textMessageKey, None, None)
      case _ => Logger.error(s"Could not render $event"); throw new MatchError(event.toString)
    }

    ImportantDate(service, eventType, event.info.eventDate, text, args, additionalText, link, linkId)

  }

  private def generateLinkId(regime: String, eventType: String) = Some(s"$regime-$eventType-href")
}
