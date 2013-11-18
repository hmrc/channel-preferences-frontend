package controllers.common.actions

import org.slf4j.MDC
import collection.JavaConversions._

trait MdcHelper {

  private[actions] def fromMDC(): Map[String, String] = Option(MDC.getCopyOfContextMap)
    .map(_.toMap.asInstanceOf[Map[String, String]])
    .getOrElse(Map[String, String]())
}
