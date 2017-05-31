package service

import connectors.{AuthConnector, PaperlessPreference, PreferencesConnector}
import controllers.Authentication
import controllers.internal.routes
import model.HostContext
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.domain.TaxIds.TaxIdWithName
import uk.gov.hmrc.play.config.AppName
import uk.gov.hmrc.play.frontend.auth.Actions
import uk.gov.hmrc.play.frontend.controller.FrontendController

import scala.concurrent.Future


//trait PaperlessActivateService extends Actions with AppName with Authentication {
//
//    val hostUrl: String
//
//    def authorityConnector: AuthConnector
//
//    def preferenceConnector: PreferencesConnector
//
//    def paperlessPreference(hostContext: HostContext, service: String): Future[Result] = {
//          val servicesForAuthTaxIds: Future[(Seq[PaperlessPreference], Boolean, Option[TaxIdWithName])] = for {
//            authTaxIds <- authorityConnector.currentTaxIdentifiers
//            taxIdWithMaybePaperlessPreference <- Future.traverse(authTaxIds.toSeq) { taxId => preferenceConnector.getPreferencesStatus(taxId.name, taxId.value).map(f => taxId -> f) }
//          } yield {
//
//            val canAutoEnrol: Boolean = authTaxIds.size == 2 && service == "default"
//            val taxIdToEnrol: Option[TaxIdWithName] = taxIdWithMaybePaperlessPreference.collect {
//              case (taxId, None) => taxId
//            }.headOption
//
//            val preferencesHavingService = for {
//              (taxId, maybePreference) <- taxIdWithMaybePaperlessPreference
//              paperlessPreference <- maybePreference
//              preferenceContainingService <- paperlessPreference.services.get(service).map(_ => paperlessPreference)
//            } yield (preferenceContainingService)
//
//            (preferencesHavingService, canAutoEnrol, taxIdToEnrol)
//          }
//
//          servicesForAuthTaxIds.flatMap {
//            case (Seq(),_, _) => Future.successful(PreconditionFailed(Json.obj(
//              "redirectUserTo" -> (hostUrl + routes.ChoosePaperlessController.redirectToDisplayServiceFormWithCohort(None, hostContext, service).url)
//            )))
//            case (Seq(paperlessPreference), canAutoEnrol, Some(taxId)) if canAutoEnrol =>
//              preferenceConnector.autoEnrol(paperlessPreference, taxId.name, taxId.value).map { _ =>
//                Ok(Json.obj("reason" -> "autoEnrol"))
//              }
//            case _ => Future.successful(Ok((Json.obj())))
//          }
//    }
//}

trait ActivateResponse
case class RedirectToOptInPage(service: String, url: String) extends ActivateResponse
case class UserAutoEnrol(to: TaxIdWithName, service: String) extends ActivateResponse
case class PreferenceFound(preference: PaperlessPreference)

//object PaperlessActivateService extends PaperlessActivateService{
//
//}
