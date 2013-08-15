package controllers.common.service

import scala.collection.JavaConversions._
import java.util.Collections
import play.api.Play

object FrontEndConfig {

  import play.api.Play.current
  lazy val env = Play.mode

  lazy val domainWhiteList = Play.configuration.getStringList(s"govuk-tax.$env.portal.domainWhiteList").getOrElse(Collections.emptyList()).toSet
  lazy val portalLoggedOutUrl = Play.configuration.getString(s"govuk-tax.$env.portal.loggedOutUrl").getOrElse("http://localhost:8080/portal/loggedout")
  lazy val portalSsoInLogoutUrl = Play.configuration.getString(s"govuk-tax.$env.portal.ssoInLogoutUrl").getOrElse("http://localhost:8080/ssoin/logout")
}