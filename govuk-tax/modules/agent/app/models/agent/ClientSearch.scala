package models.agent

import org.joda.time.LocalDate

case class ClientSearch(nino:String,firstName:String,lastName:String, dob:Option[LocalDate])
