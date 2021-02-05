/*
 * Copyright 2021 HM Revenue & Customs
 *
 */

package service
import com.google.inject.ImplementedBy
import connectors.{ EmailPreference, PreferenceResponse }
import model.StatusName.{ Alright, BouncedEmail, EmailNotVerified, NewCustomer, NoEmail, Paper }
import model._

@ImplementedBy(classOf[PreferencesBasedStatusService])
trait PaperlessStatusService {

  def determineStatus(preference: Option[PreferenceResponse]): StatusName

}

class PreferencesBasedStatusService extends PaperlessStatusService {

  def determineStatus(preference: Option[PreferenceResponse]): StatusName =
    preference match {
      case Some(p @ PreferenceResponse(_, Some(email))) if p.genericTermsAccepted =>
        determinePaperlessStatus(email)
      case Some(p @ PreferenceResponse(_, None)) if p.genericTermsAccepted =>
        NoEmail
      case Some(p: PreferenceResponse) if !p.genericTermsAccepted =>
        Paper
      case _ => NewCustomer
    }

  private def determinePaperlessStatus(email: EmailPreference): StatusName =
    if (email.hasBounces)
      BouncedEmail
    else if (!email.isVerified)
      EmailNotVerified
    else
      Alright
}
