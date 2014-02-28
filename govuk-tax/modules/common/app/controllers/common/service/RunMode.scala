package controllers.common.service

import play.api.Play

trait RunMode {

  import play.api.Play.current

  lazy val env = Play.configuration.getString("run.mode").getOrElse("Dev")
}

object RunMode extends RunMode