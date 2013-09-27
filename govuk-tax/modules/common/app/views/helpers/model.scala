package views.helpers

trait FieldType {
  val inputType: String
  val key: String
  val value: String
  val divClass: Option[String]
  val labelClass: Option[String]
  val inputClass: Option[String]
}

case class InputType(inputType: String, key: String, value: String,  divClass: Option[String] = None, labelClass: Option[String] = None, inputClass: Option[String] = None) extends FieldType

case class RadioButton(key: String, value: String, divClass: Option[String] = None, labelClass: Option[String] = None, inputClass: Option[String] = None, inputType: String = "radio") extends FieldType

case class DateControl(inputType: String = "", key: String ="", value: String = "", divClass: Option[String] = None, labelClass: Option[String] = None, inputClass: Option[String] = None) extends FieldType

case class FormField(field: play.api.data.Field, inputs: Seq[FieldType])