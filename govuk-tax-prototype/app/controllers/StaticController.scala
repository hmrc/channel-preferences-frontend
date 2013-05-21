package controllers

object StaticController extends StaticController

private[controllers] class StaticController extends BaseController {

  import play.api.templates.Html
  import play.api.i18n.Messages
  import play.api.mvc.Action

  import views.html.main

  private val contentPackage = "views.html.content"

  def render(path: String) = Action {
    implicit request =>
      val name = path.replaceAll("/", "\\.")
      val resourceClass = Class.forName(contentPackage + "." + name)
      val renderMethod: java.lang.reflect.Method = resourceClass.getMethod("apply")
      val title = Messages("contents." + name)
      Ok(main(title)(renderMethod.invoke(null).asInstanceOf[Html]))
  }
}