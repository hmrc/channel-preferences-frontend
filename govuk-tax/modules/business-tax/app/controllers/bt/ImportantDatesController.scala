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
      case ctRoot: CtRoot if ctRoot.links.get("calendar").isDefined => ctConnector.calendar(ctRoot.links("calendar")).map(_.getOrElse(List.empty[CalendarEvent]))
      case vatRoot: VatRoot if vatRoot.links.get("calendar").isDefined => vatConnector.calendar(vatRoot.links("calendar")).map(_.getOrElse(List.empty[CalendarEvent]))
      case _ => Future(List.empty[CalendarEvent])
    }

    val eventsF = Future.sequence(eventsLF).map(_.flatten)
    val datesF = eventsF.map(_.map(ImportantDate.create(_, buildPortalUrl)))

    datesF.map(dates => Ok(views.html.important_dates(dates.sortBy(_.date.toDate))(user)))
  }
}

case class ImportantDate(service: String, eventType: String, date: LocalDate, text: String, args: Seq[String] = Seq.empty, grayedoutText: Option[String] = None, link: Option[Link] = None)

object ImportantDate {
  def create(event: CalendarEvent, buildPortalUrl: (String) => String)(implicit user: User): ImportantDate = {

    val args = Seq(Dates.formatDate(event.accountingPeriod.startDate), Dates.formatDate(event.accountingPeriod.endDate))
    val service: String = event.regime.toLowerCase
    val eventType: String = event.eventType.toLowerCase

    val textMessageKey = s"$service.message.importantDates.text.$eventType"
    val additionalTextMessageKey = s"$service.message.importantDates.additionalText.$eventType"

    val (link, text, additionalText): (Option[Link], String, Option[String]) = (service, eventType, event.accountingPeriod.returnFiled) match {
      case ("ct", "payment", _) => (Some(InternalLink(routes.PaymentController.makeCtPayment().url)), textMessageKey, None)
      case ("vat", "payment-directdebit", _) => (None, additionalTextMessageKey, Some(textMessageKey))
      case ("vat", "payment-cheque", _) => (None, textMessageKey, None)
      case ("vat", "payment-card", _) | ("vat", "payment-online", _) => (Some(InternalLink(routes.PaymentController.makeVatPayment().url)), textMessageKey, None)
      case (serviceKey, "filing", true) => (None, additionalTextMessageKey, Some(textMessageKey))
      case (serviceKey, "filing", false) => (Some(PortalLink(buildPortalUrl(s"${service}FileAReturn"))), textMessageKey, None)
      case _ => Logger.error(s"Could not render $event"); throw new MatchError(event.toString)
    }

    ImportantDate(service, eventType, event.eventDate, text, args, additionalText, link)

  }
}