package controllers.bt

import controllers.common.{BaseController, GovernmentGateway}
import uk.gov.hmrc.common.PortalUrlBuilder
import controllers.common.service.Connectors
import play.api.mvc.{SimpleResult, Request}
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import org.joda.time.LocalDate
import java.text.SimpleDateFormat
import uk.gov.hmrc.common.microservice.domain.{RegimeRoot, User}
import uk.gov.hmrc.common.microservice.ct.domain.CtRoot
import views.helpers.Link
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
    calendarEvents.map{CalendarEventWithShowLink.addShowLinkToCalendarEvents}
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
        addShowLinkToCalendarEventsHelper(es, paymentsSelected :+ e.eventType.toLowerCase, newEvents :+ CalendarEventWithShowLink(e, showLink = true))
      case e::es =>
        addShowLinkToCalendarEventsHelper(es, paymentsSelected, newEvents :+ CalendarEventWithShowLink(e, showLink = false))
    }
  }

}

case class ImportantDate(service: String, eventType: String, date: LocalDate, text: String, args: Seq[String] = Seq.empty, grayedoutText: Option[String] = None, link: Option[Link] = None)

object ImportantDate {

  case class ImportantDateInfo(link: Option[Link] = None, text: String, additionalText: Option[String] = None)

  def create(event: CalendarEventWithShowLink, buildPortalUrl: (String) => String)(implicit user: User): ImportantDate = {

    val args = Seq(Dates.formatDate(event.info.accountingPeriod.startDate), Dates.formatDate(event.info.accountingPeriod.endDate))
    val service: String = event.info.regime.toLowerCase
    val eventType: String = event.info.eventType.toLowerCase

    val textMessageKey = s"$service.message.importantDates.text.$eventType"
    val additionalTextMessageKey = s"$service.message.importantDates.additionalText.$eventType"

    val info: ImportantDateInfo = (service, eventType, event.info.accountingPeriod.returnFiled, event.showLink) match {
      case ("ct", "payment", _, true) => 
        ImportantDateInfo(
          link = Some(Link.toInternalPage(url = routes.PaymentController.makeCtPayment().url, value = toValue(service, eventType), id = toLinkId(service, eventType))),
          text = textMessageKey)
      case ("ct", "payment", _, false) => 
        ImportantDateInfo(text = textMessageKey)
      case ("vat", "payment-directdebit", _, _) => 
        ImportantDateInfo(text = additionalTextMessageKey, additionalText = Some(textMessageKey))
      case ("vat", "payment-cheque", _, _) => 
        ImportantDateInfo(text = textMessageKey)
      case ("vat", "payment-card", _, true) | ("vat", "payment-online", _, true) => 
        ImportantDateInfo(
          link = Some(Link.toInternalPage(url = routes.PaymentController.makeVatPayment().url, value = toValue(service, eventType), id = toLinkId(service, eventType))),
          text = textMessageKey)
      case ("vat", "payment-card", _, false) | ("vat", "payment-online", _, false) => 
        ImportantDateInfo(text = textMessageKey)
      case (serviceKey, "filing", true, _) => 
        ImportantDateInfo(
          text = additionalTextMessageKey, 
          additionalText = Some(textMessageKey))
      case (serviceKey, "filing", false, true) => 
        ImportantDateInfo(
          link = Some(Link.toPortalPage(url = buildPortalUrl(s"${service}FileAReturn"), value = toValue(service, eventType), id = toLinkId(service, eventType))),
          text = textMessageKey)
      case (serviceKey, "filing", false, false) =>
        ImportantDateInfo(text = textMessageKey)
      case _ =>
        Logger.error(s"Could not render $event") 
        throw new MatchError(event.toString)
    }

    ImportantDate(service, eventType, event.info.eventDate, info.text, args, info.additionalText, info.link)

  }

  private def toValue(service: String, eventType: String) = Some(s"$service.message.importantDates.link.$eventType")

  private def toLinkId(regime: String, eventType: String) = Some(s"$regime-$eventType-href")
}
