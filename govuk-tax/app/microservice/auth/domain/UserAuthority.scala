package microservice.auth.domain

import org.joda.time.DateTime

case class UserAuthority(id: String, regimes: Map[String, String], previouslyLoggedInAt: Option[DateTime])