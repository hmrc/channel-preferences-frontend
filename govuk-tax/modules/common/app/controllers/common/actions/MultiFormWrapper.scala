package controllers.common.actions

import controllers.common.service.MicroServices
import play.api.mvc._
import scala.Some
import uk.gov.hmrc.common.microservice.keystore.KeyStoreMicroService

trait MultiFormWrapper extends MicroServices{
  object MultiFormAction extends MultiFormAction(keyStoreMicroService)
}

case class MultiFormConfiguration(id: String, source: String, stepsList: List[MultiFormStep], currentStep: String, unauthorisedStep: MultiFormStep)

case class MultiFormStep(stepName: String, stepCall: Call)

class MultiFormAction(keyStore: KeyStoreMicroService = MicroServices.keyStoreMicroService) extends Results {

  import uk.gov.hmrc.common.microservice.domain.User

  def apply(config: User => MultiFormConfiguration)(action: (User => Request[AnyContent] => Result)): (User => Request[AnyContent] => Result) = {
    implicit user =>
      implicit request =>
        val conf = config(user)
        keyStore.getDataKeys(conf.id, conf.source) match {
          case None => if (conf.currentStep == conf.stepsList.head.stepName) action(user)(request) else Redirect(conf.unauthorisedStep.stepCall)
          case Some(dataKeys) =>
            val next = nextStep(conf.stepsList, dataKeys)
            if (next.stepName == conf.currentStep) action(user)(request)
            else Redirect(next.stepCall)
        }
  }

  private def nextStep(stepsList: List[MultiFormStep], dataKeys: Set[String]): MultiFormStep = {
    stepsList match {
      case step :: Nil => step
      case step :: steps => if (!dataKeys.contains(step.stepName)) step else nextStep(steps, dataKeys)
    }
  }
}