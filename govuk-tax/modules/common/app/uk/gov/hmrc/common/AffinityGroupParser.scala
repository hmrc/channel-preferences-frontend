package uk.gov.hmrc.common

import play.api.mvc.Request
import controllers.common.CookieEncryption
import play.api.Logger

trait AffinityGroupParser extends CookieEncryption{

  def parseAffinityGroup(implicit request: Request[AnyRef]): String = {
    request.session.get("affinityGroup") match {
      case Some(affinityGroup) => decrypt(affinityGroup)
      case None => {
        Logger.error("Affinity Group not found")
        throw new InternalError("Affinity Group not found")
      }
    }
  }

}
