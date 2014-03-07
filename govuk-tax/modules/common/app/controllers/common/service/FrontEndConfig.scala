package controllers.common.service

import scala.collection.JavaConversions._
import java.util.Collections
import play.api.Play

object FrontEndConfig extends RunMode {

  import play.api.Play.current

  lazy private val frontendPort = Play.configuration.getInt(s"govuk-tax.$env.platform.frontend.port").map(port => s":$port").getOrElse("")
  lazy val frontendUrl = s"${Play.configuration.getString(s"govuk-tax.$env.platform.frontend.protocol").getOrElse("http")}://${Play.configuration.getString(s"govuk-tax.$env.platform.frontend.host").getOrElse("localhost")}$frontendPort"

  lazy val domainWhiteList = Play.configuration.getStringList(s"govuk-tax.$env.portal.domainWhiteList").getOrElse(Collections.emptyList()).toSet
  lazy val portalLoggedOutUrl = Play.configuration.getString(s"govuk-tax.$env.portal.loggedOutUrl").getOrElse("http://localhost:8080/portal/loggedout")
  lazy val portalSsoInLogoutUrl = Play.configuration.getString(s"govuk-tax.$env.portal.ssoInLogoutUrl").getOrElse("http://localhost:8080/ssoin/logout")

  lazy val redirectDomainWhiteList = Play.configuration.getStringList(s"govuk-tax.$env.portal.redirectDomainWhiteList").getOrElse(Collections.emptyList()).toSet
  lazy val tokenTimeout = Play.configuration.getInt(s"govuk-tax.$env.portal.tokenTimeout").getOrElse(240)
  lazy val portalHome = Play.configuration.getString(s"govuk-tax.$env.portal.destinationRoot").getOrElse("http://hmrc.gov.uk") + Play.configuration.getString(s"govuk-tax.$env.portal.destinationPath.home").getOrElse("")
}