package controllers.sa.prefs.config

import config.FrontendGlobal
import controllers.sa.prefs.internal.{OptInCohort, CohortCalculator}
import play.api.Application
import uk.gov.hmrc.crypto.ApplicationCrypto

trait PreferencesGlobal extends FrontendGlobal {

  val cohortCalculator: CohortCalculator[OptInCohort]

  override def onStart(app: Application) {
    super.onStart(app)
    ApplicationCrypto.verifyConfiguration()
    cohortCalculator.verifyConfiguration()
  }
}

object PreferencesGlobal extends PreferencesGlobal {
  override val cohortCalculator = OptInCohortCalculatorVerifier
}

object OptInCohortCalculatorVerifier extends CohortCalculator[OptInCohort] {
  override val values: List[OptInCohort] = OptInCohort.values
}
