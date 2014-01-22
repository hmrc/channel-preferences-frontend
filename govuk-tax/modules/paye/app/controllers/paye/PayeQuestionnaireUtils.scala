package controllers.paye

import play.api.data.Form
import play.api.data.Forms._
import models.paye._

case class PayeQuestionnaireFormData(transactionId: String, journeyType: Option[String], oldTaxCode: Option[String] = None, newTaxCode: Option[String] = None, wasItEasy: Option[Int] = None, secure: Option[Int] = None, comfortable: Option[Int] = None, easyCarUpdateDetails: Option[Int] = None,
                                     onlineNextTime: Option[Int] = None, overallSatisfaction: Option[Int] = None, commentForImprovements: Option[String] = None)

case class IllegalJourneyTypeException(message: String) extends RuntimeException(message)

object PayeQuestionnaireUtils {

  object FormFields {
    val transactionId = "transactionId"
    val journeyType = "journeyType"
    val oldTaxCode = "oldTaxCode"
    val newTaxCode = "newTaxCode"
    val wasItEasy = "q1"
    val secure = "q2"
    val comfortable = "q3"
    val easyCarUpdateDetails = "q4"
    val onlineNextTime = "q5"
    val overallSatisfaction = "q6"
    val commentForImprovements = "q7"
  }

  def toJourneyType(journeyType: String): PayeJourney = {
    val acceptedTypes = Map (
      AddCar.toString -> AddCar,
      AddFuel.toString -> AddFuel,
      RemoveCar.toString -> RemoveCar,
      RemoveFuel.toString -> RemoveFuel,
      RemoveCarAndFuel.toString -> RemoveCarAndFuel,
      ReplaceCar.toString -> ReplaceCar
    )
    acceptedTypes.getOrElse(journeyType, throw IllegalJourneyTypeException(s"The string: $journeyType does not represent a valid PAYE journey type"))
  }

  def getBenefitType: PartialFunction[PayeJourney, Seq[String]] = {
    case RemoveCar => Seq("car")
    case RemoveFuel => Seq("fuel")
    case RemoveCarAndFuel => Seq("car", "fuel")
  }

  def getJourneyType: PartialFunction[Seq[String], PayeJourney] = {
    case Seq("car") => RemoveCar
    case Seq("fuel") => RemoveFuel
    case Seq("car", "fuel") => RemoveCarAndFuel
  }

  private[paye] def payeQuestionnaireForm = Form[PayeQuestionnaireFormData](
    mapping(
      FormFields.transactionId -> text.verifying("some.error.code", !_.trim.isEmpty),
      FormFields.journeyType -> optional(text),
      FormFields.oldTaxCode -> optional(text),
      FormFields.newTaxCode -> optional(text),
      FormFields.wasItEasy -> optional(number),
      FormFields.secure -> optional(number),
      FormFields.comfortable -> optional(number),
      FormFields.easyCarUpdateDetails -> optional(number),
      FormFields.onlineNextTime -> optional(number),
      FormFields.overallSatisfaction -> optional(number),
      FormFields.commentForImprovements -> optional(text)
    )(PayeQuestionnaireFormData.apply)(PayeQuestionnaireFormData.unapply)
  )
}
