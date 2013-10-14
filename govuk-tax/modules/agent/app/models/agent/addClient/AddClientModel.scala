package models.agent.addClient

import org.joda.time.LocalDate
import controllers.agent.addClient.PreferredClientController

case class PotentialClient(clientSearch: Option[ClientSearch],
                           confirmation: Option[ConfirmClient],
                           preferredContact: Option[PreferredContactData])

case class ClientSearch(nino:String, firstName:Option[String], lastName:Option[String], dob:Option[LocalDate])
object ClientSearch { val empty = ClientSearch("", None, None, None) }

case class ConfirmClient(correctClient:Boolean, authorised: Boolean, internalClientReference: Option[String])
object ConfirmClient { val empty = ConfirmClient(false, false, None) }

case class PreferredContactData(pointOfContact: String, contactName: String, contactPhone:String, contactEmail: String)
object PreferredContactData { val empty = PreferredContactData(PreferredClientController.FieldIds.me, "", "", "") }
