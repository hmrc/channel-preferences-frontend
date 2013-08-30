package controllers.common.actions

import controllers.common.service.MicroServices
import controllers.common.{ HeaderNames, CookieEncryption }
import play.api.mvc._
import uk.gov.hmrc.microservice.domain.User
import scala.Some

trait MultiFormWrapper extends MicroServices with CookieEncryption with HeaderNames {
  self: Controller =>

  object MultiFormAction {

    def apply(config: User => MultiFormConfiguration)(action: (User => Request[AnyContent] => Result)): (User => Request[AnyContent] => Result) = {
      implicit user =>
        implicit request =>
          val conf = config(user)
          keyStoreMicroService.getDataKeys(conf.id, conf.source) match {
            case None => if (conf.currentStep == conf.stepsList.head.stepName) action(user)(request) else Redirect(conf.unauthorisedStep.stepCall)
            case Some(dataKeys) =>
              val next = nextStep(conf.stepsList, dataKeys)
              if (next.stepName == conf.currentStep) action(user)(request)
              else Redirect(next.stepCall)
          }
    }
  }

  private def nextStep(stepsList: List[MultiFormStep], dataKeys: Set[String]): MultiFormStep = {
    stepsList match {
      case step :: Nil => step
      case step :: steps => if (!dataKeys.contains(step.stepName)) step else nextStep(steps, dataKeys)
    }
  }

}

case class MultiFormConfiguration(id: String, source: String, stepsList: List[MultiFormStep], currentStep: String, unauthorisedStep: MultiFormStep)

case class MultiFormStep(stepName: String, stepCall: Call)

