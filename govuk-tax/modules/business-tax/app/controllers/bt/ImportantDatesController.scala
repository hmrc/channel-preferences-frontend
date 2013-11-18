package controllers.bt

import controllers.common.{BaseController, GovernmentGateway}
import uk.gov.hmrc.common.PortalUrlBuilder
import controllers.common.service.Connectors
import play.api.mvc.Request
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import org.joda.time.LocalDate
import java.text.SimpleDateFormat
import views.helpers._
import views.helpers.LinkMessage._
import uk.gov.hmrc.common.microservice.domain.User
import uk.gov.hmrc.common.microservice.ct.domain.{CtRoot, CalendarEvent}
import views.helpers.RenderableLinkMessage
import scala.Some
import views.formatting.Dates
import uk.gov.hmrc.common.microservice.ct.CtConnector
import controllers.common.actions.Actions


class ImportantDatesController(ctConnector: CtConnector, override val auditConnector: AuditConnector)(implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with PortalUrlBuilder {

  def this() = this(Connectors.ctConnector, Connectors.auditConnector)(Connectors.authConnector)

  implicit val dateFormat = new SimpleDateFormat("d MMMM yyy")

  def importantDates = AuthorisedBy(GovernmentGateway) {
    user => request => importantDatesPage(user, request)
  }

  private[bt] def importantDatesPage(implicit user: User, request: Request[AnyRef]) = {
    val regimes = List(user.regimes.ct)
    val events = regimes.flatMap {
      regime => regime match {
        case Some(ctRoot: CtRoot) => ctConnector.calendar(ctRoot.links("calendar"))
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
    val textMessageKey =s"$service.message.importantDates.text.$eventType"
    val additionalTextMessageKey =s"$service.message.importantDates.additionalText.$eventType"

    val (link, text, additionalText):(Option[RenderableLinkMessage], String, Option[String])  = (service, eventType, event.accountingPeriod.returnFiled) match {
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
    }

    ImportantDate(event.eventDate, text, args, additionalText, link)

  }
}