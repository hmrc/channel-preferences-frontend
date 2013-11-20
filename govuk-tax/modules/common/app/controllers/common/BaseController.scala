package controllers.common

import play.api.mvc._
import play.api.http.MimeTypes
import controllers.common.actions.HeaderCarrier

abstract class BaseController extends Controller {

  override def JSON(implicit codec: Codec) = s"${MimeTypes.JSON};charset=utf-8"

  override def HTML(implicit codec: Codec) = s"${MimeTypes.HTML};charset=utf-8"

  implicit def hc(request:Request[_]): HeaderCarrier = HeaderCarrier(request)
}