package controllers.common.actions

import org.slf4j.MDC

trait MdcHelper {

  def fromMDC(): Map[String, String] = {
    import collection.JavaConversions._
    MDC.getCopyOfContextMap.toMap.asInstanceOf[Map[String, String]]
  }
}
