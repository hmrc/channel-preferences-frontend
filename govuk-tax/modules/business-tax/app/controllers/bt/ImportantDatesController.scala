package controllers.bt

import controllers.common.{BaseController, Actions, GovernmentGateway}
import uk.gov.hmrc.common.PortalUrlBuilder
import uk.gov.hmrc.common.microservice.domain.User
import controllers.common.service.Connectors
import play.api.mvc.Request
import uk.gov.hmrc.common.microservice.audit.AuditConnector
import uk.gov.hmrc.common.microservice.auth.AuthConnector
import org.joda.time.LocalDate
import java.text.SimpleDateFormat
import views.helpers.{RenderableDateMessage, ImportantDateMessage, RenderableLinkMessage, LinkMessage}


class ImportantDatesController(override val auditConnector: AuditConnector)(implicit override val authConnector: AuthConnector)
  extends BaseController
  with Actions
  with PortalUrlBuilder {

  def this() = this(Connectors.auditConnector)(Connectors.authConnector)

  implicit val dateFormat = new SimpleDateFormat("d MMMM yyy")

  def importantDates = ActionAuthorisedBy(GovernmentGateway)() {
    user => request => importantDatesPage(user, request)
  }

  private[bt] def importantDatesPage(implicit user: User, request: Request[AnyRef]) = {
    val date1 = ImportantDateMessage(
      RenderableDateMessage(new LocalDate(2013, 9, 10)),
      "some text to show",
      RenderableDateMessage(new LocalDate(2013, 11, 10)),
      RenderableDateMessage(new LocalDate(2014, 11, 9)),
      Some(RenderableLinkMessage(LinkMessage.internalLink("someUrl", "Make a payment")))

    )
    val date2 = ImportantDateMessage(
      RenderableDateMessage(new LocalDate(2013, 9, 10))(new SimpleDateFormat("d MMMM")),
      "some text to show",
      RenderableDateMessage(new LocalDate(2013, 11, 10))(new SimpleDateFormat("d MMMM")),
      RenderableDateMessage(new LocalDate(2014, 11, 9))(new SimpleDateFormat("d MMMM")),
      None
    )
    Ok(views.html.important_dates(List(date1, date2)))
  }
}
