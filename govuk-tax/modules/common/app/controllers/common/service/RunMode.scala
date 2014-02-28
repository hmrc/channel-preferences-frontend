package controllers.common.service

import play.api.{Mode, Play}

trait RunMode {

  import play.api.Play.current

  lazy val env = {
    if (Play.mode.equals(Mode.Test)) "Test"
    else
      Play.configuration.getString("run.mode").getOrElse("Dev")
  }
}

object RunMode extends RunMode