package models.agent.addClient

import org.joda.time.LocalDate

case class ClientSearch(nino:String, firstName:Option[String], lastName:Option[String], dob:Option[LocalDate])