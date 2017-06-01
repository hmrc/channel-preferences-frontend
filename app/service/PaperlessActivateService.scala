package service

import connectors.{AuthConnector, PaperlessPreference, PreferencesConnector}
import controllers.internal.routes
import model.HostContext
import uk.gov.hmrc.domain.TaxIds.TaxIdWithName
import uk.gov.hmrc.play.frontend.auth
import uk.gov.hmrc.play.http.{HeaderCarrier, NotImplementedException}

import scala.concurrent.{ExecutionContext, Future}


trait PaperlessActivateService {

  def authorityConnector: AuthConnector

  def preferenceConnector: PreferencesConnector

  def paperlessPreference(hostContext: HostContext, service: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ActivateResponse] = {

    def autoEnrolTaxId(serviceToAutoEnrol: String, taxIdWithMaybePaperlessPreference: Seq[(TaxIdWithName, Option[PaperlessPreference])]): Option[TaxIdWithName] = {
      taxIdWithMaybePaperlessPreference.collect {
        case (taxId, None) if serviceToAutoEnrol == "default" => taxId
      }.headOption
    }

    val servicesForAuthTaxIds = for {
      authTaxIds <- authorityConnector.currentTaxIdentifiers
      taxIdWithMaybePaperlessPreference <- Future.traverse(authTaxIds.toSeq) { taxId => preferenceConnector.getPreferencesStatus(taxId.name, taxId.value).map(f => taxId -> f) }
    } yield {

      val missingTaxIds = authTaxIds.isEmpty
      val maybeTaxIdToAutoEnrolToService = autoEnrolTaxId(service, taxIdWithMaybePaperlessPreference)

      val preferencesHavingService = for {
        (taxId, maybePreference) <- taxIdWithMaybePaperlessPreference
        paperlessPreference <- maybePreference
        preferenceContainingService <- paperlessPreference.services.get(service).map(_ => paperlessPreference)
      } yield (preferenceContainingService)

      (missingTaxIds, preferencesHavingService, maybeTaxIdToAutoEnrolToService)
    }

    servicesForAuthTaxIds.flatMap {
      case (missingTaxIds, _, _) if missingTaxIds => Future.successful(UnAuthorised)
      case (_, foundPreferences, _) if foundPreferences.isEmpty => Future.successful(
        //        TODO: DC-970: Disabled for production and the user will optIn from the old form
        //        RedirectToOptInPage(
        //          service,
        //          routes.ChoosePaperlessController.redirectToDisplayServiceFormWithCohort(service, None, hostContext).url)

        RedirectToOptInPage(
          service,
          routes.ChoosePaperlessController.redirectToDisplayFormWithCohort(None, hostContext).url)
      )
      case (_, Seq(singlePaperlessPreference), Some(taxIdToAutoEnrolToDefault)) =>
        preferenceConnector.autoEnrol(singlePaperlessPreference, taxIdToAutoEnrolToDefault.name, taxIdToAutoEnrolToDefault.value, "default").map { _ =>
          UserAutoEnrol(taxIdToAutoEnrolToDefault, service)
        }
      case _ => Future.successful(PreferenceFound)
    }
  }

}

trait ActivateResponse

case class RedirectToOptInPage(service: String, url: String) extends ActivateResponse

case class UserAutoEnrol(to: TaxIdWithName, service: String) extends ActivateResponse

case object PreferenceFound extends ActivateResponse

case object UnAuthorised extends ActivateResponse


object PaperlessActivateService extends PaperlessActivateService {

  lazy val authorityConnector: AuthConnector = AuthConnector

  lazy val preferenceConnector: PreferencesConnector = PreferencesConnector

  lazy val authConnector: auth.connectors.AuthConnector = AuthConnector
}
