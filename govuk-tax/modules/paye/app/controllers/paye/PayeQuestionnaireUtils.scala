package controllers.paye

import play.api.data.Form
import play.api.data.Forms._
import models.paye._
import controllers.paye.PayeQuestionnaireFormData

case class PayeQuestionnaireFormData(transactionId: String, journeyType: Option[String], wasItEasy: Option[Int] = None, secure: Option[Int] = None, comfortable: Option[Int] = None, easyCarUpdateDetails: Option[Int] = None,
                                     onlineNextTime: Option[Int] = None, overallSatisfaction: Option[Int] = None, commentForImprovements: Option[String] = None)

case class IllegalJourneyTypeException(message: String) extends RuntimeException(message)

object PayeQuestionnaireUtils {

  val transactionId = "transactionId"
  val typeOfJourney = "journeyType"
  val wasItEasy = "q1"
  val secure = "q2"
  val comfortable = "q3"
  val easyCarUpdateDetails = "q4"
  val onlineNextTime = "q5"
  val overallSatisfaction = "q6"
  val commentForImprovements = "q7"

  def toJourneyType(journeyType: String): PayeJourney = {
    val acceptedTypes = Map (
      AddCar.toString -> AddCar,
      AddFuel.toString -> AddFuel,
      RemoveCar.toString -> RemoveCar,
      RemoveFuel.toString -> RemoveFuel,
      ReplaceCar.toString -> ReplaceCar
    )
    acceptedTypes.getOrElse(journeyType, throw IllegalJourneyTypeException(s"The string: $journeyType does not represent a valid PAYE journey type"))
  }

  private[paye] def payeQuestionnaireForm = Form[PayeQuestionnaireFormData](
    mapping(
      transactionId -> text.verifying("some.error.code", !_.trim.isEmpty),
      typeOfJourney -> optional(text),
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
