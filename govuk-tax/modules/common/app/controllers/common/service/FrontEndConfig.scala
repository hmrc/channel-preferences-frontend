package controllers.common.service

import scala.collection.JavaConversions._
import java.util.Collections
import play.api.Play

object FrontEndConfig {

  import play.api.Play.current
  lazy val env = Play.mode

  lazy val domainWhiteList = Play.configuration.getStringList(s"govuk-tax.$env.portal.domainWhiteList").getOrElse(Collections.emptyList()).toSet
}