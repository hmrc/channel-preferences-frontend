package views.helpers

case class InputType(inputType: String, key: String, value: String, divClass: Option[String] = None, labelClass: Option[String] = None, inputClass: Option[String] = None)

case class RadioButton(key: String, value: String, divClass: Option[String] = None, labelClass: Option[String] = None)

case class FormField(field: play.api.data.Field, inputs: Seq[InputType])