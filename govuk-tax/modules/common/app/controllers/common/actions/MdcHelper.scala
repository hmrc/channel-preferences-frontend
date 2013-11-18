package controllers.common.actions

import org.slf4j.MDC
import collection.JavaConversions._

private[actions] trait MdcHelper {

  protected def fromMDC(): Map[String, String] = Option(MDC.getCopyOfContextMap)
    .map(_.toMap.asInstanceOf[Map[String, String]])
    .getOrElse(Map[String, String]())
}
