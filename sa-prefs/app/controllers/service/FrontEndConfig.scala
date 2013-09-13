package controllers.service

import scala.collection.JavaConversions._
import java.util.Collections
import play.api.Play

object FrontEndConfig {

  import play.api.Play.current
  lazy val env = Play.mode

  lazy val redirectDomainWhiteList = Play.configuration.getStringList(s"sa-prefs.$env.portal.redirectDomainWhiteList").getOrElse(Collections.emptyList()).toSet

}