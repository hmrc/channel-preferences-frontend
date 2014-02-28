package controllers.common.service

import play.api.Play

trait RunMode {

  import play.api.Play.current

  lazy val env = Play.configuration.getString("run.mode").getOrElse("Prod")
}

object RunMode extends RunMode