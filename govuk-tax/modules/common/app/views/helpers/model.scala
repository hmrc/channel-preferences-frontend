package views.helpers

trait FieldType

case class InputType(inputType: String, key: String, value: String,  divClass: Option[String] = None, labelClass: Option[String] = None, inputClass: Option[String] = None, label:Option[String] = None) extends FieldType

object RadioButton {
  def apply(key: String, value: String, divClass: Option[String] = None, labelClass: Option[String] = None, inputClass: Option[String] = None) = {
    InputType("radio", key, value, divClass, labelClass, inputClass)
  }
}

object InputText {
  def apply(fieldLabel: String, divClass: Option[String] = None, labelClass: Option[String] = None, inputClass: Option[String] = None, label:Option[String] = None) = {
    InputType("text", "", fieldLabel, divClass, labelClass, inputClass, label)
  }
}

case class DateControl(yearRange: Range) extends FieldType

case class FormField(field: play.api.data.Field, inputs: Seq[FieldType])