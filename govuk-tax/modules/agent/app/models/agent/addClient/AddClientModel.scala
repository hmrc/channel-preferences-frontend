package models.agent.addClient

import org.joda.time.LocalDate

case class PotentialClient(clientSearch: Option[ClientSearch],
                           confirmation: Option[ConfirmClient],
                           preferredContact: Option[PreferredContact])

case class ClientSearch(nino:String, firstName:Option[String], lastName:Option[String], dob:Option[LocalDate])
object ClientSearch { val empty = ClientSearch("", None, None, None) }

case class ConfirmClient(correctClient:Boolean, authorised: Boolean, internalClientReference: Option[String])
object ConfirmClient { val empty = ConfirmClient(false, false, None) }

case class PreferredContact(pointOfContact: String, contactName: String, contactPhone:String, contactEmail: String)
