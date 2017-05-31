package service

import connectors.{AuthConnector, PreferencesConnector}
import controllers.internal.routes
import model.HostContext
import uk.gov.hmrc.domain.TaxIds.TaxIdWithName
import uk.gov.hmrc.play.frontend.auth
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}


trait PaperlessActivateService {

  def authorityConnector: AuthConnector

  def preferenceConnector: PreferencesConnector

  def paperlessPreference(hostContext: HostContext, service: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[ActivateResponse] = {

    val servicesForAuthTaxIds = for {
      authTaxIds <- authorityConnector.currentTaxIdentifiers
      taxIdWithMaybePaperlessPreference <- Future.traverse(authTaxIds.toSeq) { taxId => preferenceConnector.getPreferencesStatus(taxId.name, taxId.value).map(f => taxId -> f) }
    } yield {

      val missingTaxIds = authTaxIds.isEmpty
      val maybeTaxIdToAutoEnrolToDefault = taxIdWithMaybePaperlessPreference.collect {
        case (taxId, None) if service == "default" => taxId
      }.headOption

      val preferencesHavingService = for {
        (taxId, maybePreference) <- taxIdWithMaybePaperlessPreference
        paperlessPreference <- maybePreference
        preferenceContainingService <- paperlessPreference.services.get(service).map(_ => paperlessPreference)
      } yield (preferenceContainingService)

      (missingTaxIds, preferencesHavingService, maybeTaxIdToAutoEnrolToDefault)
    }

    servicesForAuthTaxIds.flatMap {
      case (missingTaxIds, _, _) if missingTaxIds => Future.successful(UnAuthorised)
      case (_, foundPreferences, _) if foundPreferences.isEmpty => Future.successful(
        RedirectToOptInPage(
          service,
          routes.ChoosePaperlessController.redirectToDisplayServiceFormWithCohort(service, None, hostContext).url)
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


object PaperlessActivateService extends PaperlessActivateService{

  lazy val authorityConnector: AuthConnector = AuthConnector

  lazy val preferenceConnector: PreferencesConnector = PreferencesConnector

  lazy val authConnector: auth.connectors.AuthConnector = AuthConnector
}
