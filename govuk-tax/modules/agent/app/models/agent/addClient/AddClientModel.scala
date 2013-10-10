package models.agent.addClient

import org.joda.time.LocalDate

case class PotentialClient(clientSearch: Option[ClientSearch],
                           confirmation: Option[ConfirmClient],
                           preferredContact: Option[PreferredContact])

case class ClientSearch(nino:String, firstName:Option[String], lastName:Option[String], dob:Option[LocalDate])

case class ConfirmClient(correctClient:Boolean, authorised: Boolean, internalClientReference: Option[String])

case class PreferredContact(pointOfContact: String, contactName: String, contactPhone:String, contactEmail: String)
