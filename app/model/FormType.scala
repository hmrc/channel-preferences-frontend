package model

import play.api.mvc.PathBindable

sealed trait FormType {
  def value: String
}
case object SaAll extends FormType {val value = "sa-all"}
case object NoticeOfCoding extends FormType {val value = "notice-of-coding"}

object FormType {

  private val allFormTypes = Seq(SaAll, NoticeOfCoding)

  implicit val formTypeBinder = new PathBindable[FormType] {

    override def bind(key: String, value: String): Either[String, FormType] =
      allFormTypes.find(_.value == value) map (Right(_)) getOrElse Left(s"$value is not a valid form-type")

    override def unbind(key: String, formType: FormType): String = formType.value
  }
}