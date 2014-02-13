package views.helpers

import views.html.helper.FieldConstructor
import views.html.helpers.simpleFieldConstructor

object CustomHelpers {

  implicit val myFields = FieldConstructor(simpleFieldConstructor.f)

}
