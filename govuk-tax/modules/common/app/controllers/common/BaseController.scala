package controllers.common

import play.api.mvc._
import play.api.http.MimeTypes
import controllers.common.actions.HeaderCarrier
import scala.concurrent._


abstract class BaseController extends Controller {

  override def JSON(implicit codec: Codec) = s"${MimeTypes.JSON};charset=utf-8"

  override def HTML(implicit codec: Codec) = s"${MimeTypes.HTML};charset=utf-8"

  implicit def hc(implicit request: Request[_]): HeaderCarrier = HeaderCarrier(request)

  implicit class SessionKeyRemover(simpleResult: Future[SimpleResult]) {
    def removeSessionKey(key: String)(implicit request: Request[_], ctx:ExecutionContext) = simpleResult.map {_.withSession(request.session - key)}
  }

}