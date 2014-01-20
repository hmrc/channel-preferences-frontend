package controllers.paye

import play.api.data.Form
import play.api.data.Forms._

case class PayeQuestionnaireFormData(transactionId: String, wasItEasy: Option[Int] = None, secure: Option[Int] = None, comfortable: Option[Int] = None, easyCarUpdateDetails: Option[Int] = None,
                                     onlineNextTime: Option[Int] = None, overallSatisfaction: Option[Int] = None, commentForImprovements: Option[String] = None)

object PayeQuestionnaireUtils {

  val transactionId = "transactionId"
  val wasItEasy = "q1"
  val secure = "q2"
  val comfortable = "q3"
  val easyCarUpdateDetails = "q4"
  val onlineNextTime = "q5"
  val overallSatisfaction = "q6"
  val commentForImprovements = "q7"

  private[paye] def payeQuestionnaireForm = Form[PayeQuestionnaireFormData](
    mapping(
      transactionId -> text.verifying("some.error.code", !_.trim.isEmpty),
      wasItEasy -> optional(number),
      secure -> optional(number),
      comfortable -> optional(number),
      easyCarUpdateDetails -> optional(number),
      onlineNextTime -> optional(number),
      overallSatisfaction -> optional(number),
      commentForImprovements -> optional(text)
    )(PayeQuestionnaireFormData.apply)(PayeQuestionnaireFormData.unapply)
  )
}
