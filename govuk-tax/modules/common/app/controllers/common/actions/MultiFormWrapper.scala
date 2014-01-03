package controllers.common.actions

import play.api.mvc._
import uk.gov.hmrc.common.microservice.keystore.KeyStoreConnector
import scala.concurrent._
import uk.gov.hmrc.common.StickyMdcExecutionContext.global

trait MultiFormWrapper {
  val keyStoreConnector: KeyStoreConnector

  object MultiFormAction extends MultiFormAction(keyStoreConnector)

}

case class MultiFormConfiguration(actionId: String, source: String, stepsList: List[MultiFormStep], currentStep: String, unauthorisedStep: MultiFormStep, ignoreSession: Boolean)

case class MultiFormStep(stepName: String, stepCall: Call)

class MultiFormAction(keyStore: KeyStoreConnector) extends Results {

  import uk.gov.hmrc.common.microservice.domain.User

  def apply(conf: MultiFormConfiguration)(action: (User => Request[AnyContent] => SimpleResult)): (User => Request[AnyContent] => Future[SimpleResult]) = {
    implicit user =>
      implicit request =>
        keyStore.getDataKeys(conf.actionId, conf.source, conf.ignoreSession)(HeaderCarrier(request)) map {
          case None => if (conf.currentStep == conf.stepsList.head.stepName) action(user)(request) else Redirect(conf.unauthorisedStep.stepCall)
          case Some(dataKeys) =>
            val next = nextStep(conf.stepsList, dataKeys)
            if (next.stepName == conf.currentStep) action(user)(request)
            else Redirect(next.stepCall)
        }
  }

  def async(conf: MultiFormConfiguration)(action: (User => Request[AnyContent] => Future[SimpleResult])): (User => Request[AnyContent] => Future[SimpleResult]) = {
    implicit user =>
      implicit request =>
        keyStore.getDataKeys(conf.actionId, conf.source, conf.ignoreSession)(HeaderCarrier(request)) flatMap {
          case None =>
            if (conf.currentStep == conf.stepsList.head.stepName) action(user)(request)
            else Future.successful(Redirect(conf.unauthorisedStep.stepCall))
          case Some(dataKeys) =>
            val next = nextStep(conf.stepsList, dataKeys)
            if (next.stepName == conf.currentStep) action(user)(request)
            else Future.successful(Redirect(next.stepCall))
        }
  }

  private def nextStep(stepsList: List[MultiFormStep], dataKeys: Set[String]): MultiFormStep = {
    stepsList match {
      case step :: Nil => step
      case step :: steps => if (!dataKeys.contains(step.stepName)) step else nextStep(steps, dataKeys)
    }
  }
}