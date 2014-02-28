package config

import play.api.Play
import controllers.common.service.RunMode

object PortalConfig extends RunMode {

  import play.api.Play.current

  //TODO: Rename to trustedSsoOutDomain
  def destinationRoot = s"${Play.configuration.getString(s"govuk-tax.$env.portal.destinationRoot").getOrElse("http://localhost:8080/portal/ssoin")}"

  def ssoUrl = s"${Play.configuration.getString(s"govuk-tax.$env.portal.ssoUrl").getOrElse("http://localhost:8080")}"

  def getDestinationUrl(pathKey: String): String = {
    getPath(pathKey)
  }

  private def getPath(pathKey: String): String = {
    s"${Play.configuration.getString(s"govuk-tax.$env.portal.destinationPath.$pathKey").getOrElse("")}"
  }
}

