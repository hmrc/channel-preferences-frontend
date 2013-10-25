package controllers.common.actions

import org.slf4j.MDC

trait MdcHelper {

  def fromMDC(): Map[String, String] = {
    import collection.JavaConversions._
    Option(MDC.getCopyOfContextMap)
      .map(_.toMap.asInstanceOf[Map[String, String]])
      .getOrElse(Map[String, String]())
  }
}
