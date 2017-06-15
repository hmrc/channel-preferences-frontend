package views.taxCredits.prefs.helpers

import views.html.helper.FieldConstructor
import views.html.sa.prefs.helpers.simpleFieldConstructor

object CustomHelpers {

  implicit val myFields = FieldConstructor(simpleFieldConstructor.f)

}
