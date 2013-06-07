package controllers

import play.api.mvc._
import play.api.http.MimeTypes

trait BaseController extends Controller {

  override def JSON(implicit codec: Codec) = s"${MimeTypes.JSON};charset=utf-8"

  override def HTML(implicit codec: Codec) = s"${MimeTypes.HTML};charset=utf-8"
}
